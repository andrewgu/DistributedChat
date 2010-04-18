package protocol;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

import protocol.data.ServerAddress;
import protocol.data.ServerID;
import protocol.data.ServerLoad;
import protocol.data.ServerPriorityListing;
import protocol.data.ServerStats;
import protocol.packets.CoreMessage;
import protocol.packets.RingStat;
import protocol.packets.ServerUpdate;

/**
 * Central class for maintaining statistics crucial to program decision
 * making. This class is suitable for concurrent access. Added methods
 * should be certain to synchronize on any instance variables they 
 * modify.
 * 
 * @author dew47
 *
 */
public class StatCenter implements Runnable {

	/*
	 * Sort a group of ServerStats based on their load component.
	 * Sort from least to greatest load.
	 */
	private static final Comparator<ServerStats> LOAD_SORTED_SS =
		new Comparator<ServerStats>() {
		@Override
		public int compare(ServerStats o1, ServerStats o2) {
			return o1.load.compareTo(o2.load);
		}
	};

	public final ServerID serverID;
	public final ServerAddress serverAddress;
	private final LoadQueryable lq;
	private long queryInt;
	
	/*
	 * The objects pointed to here may be changed, but they 
	 * should not be directly modified. It will interfere
	 * with the synchronization methods put in place.
	 */
	private RingStat latestRingStat;
	private ServerLoad currentLoad;
	// this should be marked null whenever it needs to be updated
	private ServerUpdate latestUpdate;

	public StatCenter(ServerID serverID, ServerAddress serverAddress, 
			LoadQueryable lq, long queryInt) {
		this.serverID = serverID;
		this.serverAddress = serverAddress;
		this.lq = lq;
		this.queryInt = queryInt;
		this.latestRingStat = null;
		this.currentLoad = null;
		this.latestUpdate = null;
	}

	/**
	 * Get the most recent ServerUpdate given intraring statistics.
	 * The room component here will be left null.
	 * 
	 * @return a shallow copy of the update. Do not modify the attached
	 * server priority listings directly.
	 */
	public ServerUpdate currentUpdate() {

		/* We try to keep locks on crucial components for
		 * as short a time as possible in this method.
		 */

		RingStat rs;

		synchronized(this.latestRingStat) {
			rs = this.latestRingStat;
		}

		// if there is no ring stat, we can't give an update
		if(rs == null) return null;

		ArrayList<ServerStats> stats;
		ServerUpdate sup;

		synchronized(this.latestUpdate) {
			sup = this.latestUpdate;
		}

		// if no latest update, we'll have to calculate one
		if(sup == null) {
			stats = rs.getServerListing();

			// sort list and init array
			Collections.sort(stats, LOAD_SORTED_SS);
			ServerPriorityListing[] servers =
				new ServerPriorityListing[stats.size()];
			
			/*
			 * Priority listing ascends with load stats,
			 * since list is sorted, i == priority.
			 */
			int i;
			ServerStats ss;
			for(i = 0; i < stats.size(); i++) {
				ss = stats.get(i);
				servers[i] = new ServerPriorityListing(i, ss.id, 
						ss.addr);
			}

			// keep name field null
			sup = new ServerUpdate(null, this.serverID, servers);

			synchronized(this.latestUpdate) {
				latestUpdate = sup;
			}
		}
		
		// the synchronization order does make it possible
		// that this object will be slightly out of date
		try {
			// shallow copy ok here
			return (ServerUpdate) sup.clone();
		} catch (CloneNotSupportedException e) {
			return null; // shouldn't make it here
		}
	}

	/**
	 * This method will return the rooms currently reported by 
	 * the ring. Keep in mind that rooms hosted exclusively
	 * "down-ring" will not be listed here. This method is 
	 * accurate for the ring only at the head node.
	 * 
	 * @return
	 */
	public String[] getRingRooms() {
		RingStat rs;
		synchronized(this.latestRingStat) {
			rs = this.latestRingStat;
		}
		
		return rs.getCurrentRoomListing();
	}
	
	/**
	 * Get a ServerUpdate object with the room set to the proper
	 * room name.
	 * 
	 * @param room room name
	 * @return
	 */
	public ServerUpdate currentUpdate(String room) {
		ServerUpdate su = this.currentUpdate();
		su.setRoom(room);
		return su;
	}


	/**
	 * A hook for other program components to report the latest
	 * RingStat object.
	 * 
	 * @param rs
	 */
	public void updateRingStat(RingStat rs) {
		synchronized(this.latestRingStat) {
			this.latestRingStat = rs;
		}
		// there is a possibility that in this brief interim,
		// a client may receive an out of date serverupdate
		synchronized(this.latestUpdate) {
			this.latestUpdate = null;
		}
	}
	
	public ServerLoad getCurrentLoad() {
		synchronized(this.currentLoad) {
			return this.currentLoad;
		}
	}

	@Override
	public void run() {

		ServerLoad load;

		/*
		 * query the loadqueryable at the specified interval
		 */
		while(true) {
			load = lq.queryLoad();

			synchronized(currentLoad){
				currentLoad = load;
			}
			
			try {
				Thread.sleep(queryInt);
			} catch (InterruptedException e) {}
		}
	}

}
