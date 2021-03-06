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
        this.serverList = new ArrayList<ServerPriorityListing>();
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
                // Switch port to CLIENT_PORT because that's what the clients connect to.
                serverList.add(new ServerPriorityListing(priorityCounter, stat.id, 
                        new ServerAddress(stat.addr.getHost(), RingServer.CLIENT_PORT)));
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
    
    public synchronized long getPreviousUpdateCounter()
    {
        return this.previousUpdateCounter;
    }
}
