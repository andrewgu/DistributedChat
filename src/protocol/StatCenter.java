package protocol;

import java.util.ArrayList;
import java.util.Collections;

import protocol.data.ServerAddress;
import protocol.data.ServerID;
import protocol.data.ServerLoad;
import protocol.data.ServerPriorityListing;
import protocol.data.ServerStats;
import protocol.packets.RingStat;
import protocol.packets.ServerUpdate;

/**
 * Central class for maintaining statistics crucial to program decision
 * making.
 * 
 * @author dew47
 *
 */
public class StatCenter implements Runnable {

	public final ServerID serverID;
	public final ServerAddress serverAddress;
	private LoadQueryable lq;
	private long queryInt;
	private RingStat latestRingStat;
	private ServerLoad currentLoad;
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

	public synchronized ServerUpdate currentUpdate() {
		if(this.latestRingStat == null) return null;
		
		ArrayList<ServerStats> stats;
		
		// TODO determine meaning of priority number
		if(this.latestUpdate == null) {
			stats = this.latestRingStat.getServerListing();
			Collections.sort(stats);
			ServerPriorityListing[] servers =
				new ServerPriorityListing[stats.size()];
			
			int i;
			ServerStats st;
			for(i = 0; i < stats.size(); i++) {
				st = stats.get(i);
				servers[i] = new ServerPriorityListing(i, this.serverID, 
						this.serverAddress);
			}
			
			this.latestUpdate = 
				new ServerUpdate(null, this.serverID, servers);
		}
		
		return latestUpdate;
	}


	public synchronized void updateRingStat(RingStat rs) {
		this.latestRingStat = rs;
		this.latestUpdate = null;
	}

	@Override
	public void run() {

		/*
		 * query the loadqueryable at the specified interval
		 */
		while(true) {
			try {
				Thread.sleep(queryInt);
			} catch (InterruptedException e) {}

			synchronized(this){
				currentLoad = lq.queryLoad();
			}
		}
	}

}
