package server;

import java.util.ArrayList;
import java.util.PriorityQueue;

import protocol.data.ServerAddress;
import protocol.data.ServerID;
import protocol.data.ServerPriorityListing;
import protocol.data.ServerStats;
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
public class StatCenter {

	/*
	 * Sort a group of ServerStats based on their load component.
	 * Sort from least to greatest load.
	 */
	//private static final Comparator<ServerStats> LOAD_SORTED_SS = ServerStats.PRIORITY_COMPARATOR;

	/*
     * The objects pointed to here may be changed, but they 
     * should not be directly modified. It will interfere
     * with the synchronization methods put in place.
     */
	private ServerID serverID;
	private ServerAddress serverAddress;
	private RingStat latestRingStat;
	private long previousUpdateCounter;
	private float load;
	private ArrayList<ServerPriorityListing> serverList;

	public StatCenter()
    {
        this.serverID = null;
        this.serverAddress = null;
        this.load = 0.0f;
        this.latestRingStat = null;
        this.serverList = null;
        this.previousUpdateCounter = Long.MIN_VALUE;
    }
	
	public synchronized void initNode(ServerID serverID, ServerAddress serverAddress)
    {
        this.serverID = serverID;
        this.serverAddress = serverAddress;
    }
	
    public synchronized ServerUpdate getServerUpdate(String room)
    {
        ServerPriorityListing[] listing = new ServerPriorityListing[this.serverList.size()];
        this.serverList.toArray(listing);
        return new ServerUpdate(room, serverID, listing);
    }

    public synchronized ServerID getServerID()
    {
        return this.serverID;
    }

    public synchronized ServerAddress getServerAddress()
    {
        return this.serverAddress;
    }
    
    public synchronized void updateLoad(float load)
    {
        this.load = load;
    }

    public synchronized void updateRingStat(RingStat rs)
    {
        this.latestRingStat = rs;
        
        serverList.clear();
        PriorityQueue<ServerStats> stats = rs.getPriorityListing();
        int priorityCounter = 0;
        while (!stats.isEmpty())
        {
            ServerStats stat = stats.remove();
            // Any servers that dropped from the ring won't update their entry.
            if (stat.lastUpdate > this.previousUpdateCounter)
            {
                serverList.add(new ServerPriorityListing(priorityCounter, stat.id, stat.addr));
                priorityCounter++;
            }
        }
        
        // Catch up to the current update counter.
        this.previousUpdateCounter = rs.getCurrentUpdateCounter();
    }
    
    public synchronized float getLoad()
    {
        return load;
    }
    
    public synchronized RingStat getLatestRingStat()
    {
        return this.latestRingStat;
    }
    
//    /**
//    * Get the successor for this node.
//    * 
//    * @return
//    */
//    public synchronized ServerAddress getSuccessor() 
//    {
//        return this.successor;
//    }

//    /**
//	 * Get the most recent ServerUpdate given intraring statistics.
//	 * The room component here will be left null.
//	 * 
//	 * @return a shallow copy of the update. Do not modify the attached
//	 * server priority listings directly.
//	 */
//	public ServerUpdate currentUpdate() {
//
//		/* We try to keep locks on crucial components for
//		 * as short a time as possible in this method.
//		 */
//
//		RingStat rs;
//
//		synchronized(this.latestRingStat) {
//			rs = this.latestRingStat;
//		}
//
//		// if there is no ring stat, we can't give an update
//		if(rs == null) return null;
//
//		ArrayList<ServerStats> stats;
//		ServerUpdate sup;
//
//		synchronized(this.latestUpdate) {
//			sup = this.latestUpdate;
//		}
//
//		// if no latest update, we'll have to calculate one
//		if(sup == null) {
//			stats = rs.getServerListing();
//
//			// sort list and init array
//			Collections.sort(stats, LOAD_SORTED_SS);
//			ServerPriorityListing[] servers =
//				new ServerPriorityListing[stats.size()];
//
//			/*
//			 * Priority listing ascends with load stats,
//			 * since list is sorted, i == priority.
//			 */
//			int i;
//			ServerStats ss;
//			for(i = 0; i < stats.size(); i++) {
//				ss = stats.get(i);
//				servers[i] = new ServerPriorityListing(i, ss.id, 
//						ss.addr);
//			}
//
//			// keep name field null
//			sup = new ServerUpdate(null, this.serverID, servers);
//
//			synchronized(this.latestUpdate) {
//				latestUpdate = sup;
//			}
//		}
//
//		// the synchronization order does make it possible
//		// that this object will be slightly out of date
//		try {
//			// shallow copy ok here
//			return (ServerUpdate) sup.clone();
//		} catch (CloneNotSupportedException e) {
//			return null; // shouldn't make it here
//		}
//	}
//
//	
//
//	/**
//	 * Remove the successor for this node, provided it is possible
//	 * to do so.
//	 * @return
//	 */
//	public boolean removeSuccessor() {
//		ServerID remove = this.getSuccessor().id;
//		return this.removeServer(remove);
//	}
//
//	/**
//	 * Get the ServerStats for the successor to the given node
//	 * @param cur the node for whose successors we are searching
//	 * @return null if cur is not in list, else successor
//	 */
//	public ServerStats getSuccessor(ServerID cur) {
//		ServerStats ss = null;
//
//		synchronized(this.serverList) {
//
//			for(int i = 0; i < serverList.size(); i++) {
//				ss = serverList.get(i);
//				if(ss.id.equals(cur)) {
//					i++;
//					i %= serverList.size(); // wrap around to front of list
//					ss = serverList.get(i);
//					break;
//				}
//				ss = null; // want to return null if never find
//			}
//		}
//		return ss;
//	}
//
//	/**
//	 * This method will return the rooms currently reported by 
//	 * the ring. Keep in mind that rooms hosted exclusively
//	 * "down-ring" will not be listed here. This method is 
//	 * accurate for the ring only at the head node.
//	 * 
//	 * @return
//	 */
//	public String[] getRingRooms() {
//		RingStat rs;
//		synchronized(this.latestRingStat) {
//			rs = this.latestRingStat;
//		}
//
//		return rs.getCurrentRoomListing();
//	}
//
//	/**
//	 * Get a ServerUpdate object with the room set to the proper
//	 * room name.
//	 * 
//	 * @param room room name
//	 * @return
//	 */
//	public ServerUpdate currentUpdate(String room) {
//		ServerUpdate su = this.currentUpdate();
//		su.setRoom(room);
//		return su;
//	}
//
//	/**
//	 * Get the most recently updated ServerLoad
//	 * 
//	 * @return
//	 */
//	public float getCurrentLoad() {
//		return this.load;
//	}
//
//
//	/**
//	 * Get the most up to date ServerStats object for this
//	 * object.
//	 * 
//	 * @return
//	 */
//	public ServerStats getCurrentServerStats() {
//		return new ServerStats(this.serverID,
//				this.serverAddress, this.getCurrentLoad());		
//	}
}
