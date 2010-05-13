package protocol.packets;

import java.util.ArrayList;
import java.util.PriorityQueue;

import protocol.ISendable;
import protocol.PacketType;
import protocol.data.RoomCount;
import protocol.data.ServerID;
import protocol.data.ServerStats;
import server.RingServer;


/**
 * This class implements the RingStat status ping that circulates
 * between servers in a ring.
 * 
 * RingStat contains the following information
 * 1. the head node from which it originated
 * 2. a list of the most up to date status available for each server
 * 3. counts for clients in every room
 * 
 * RingStats are to be acked and in the event of no timely ack,
 * the missing server will be removed from the RingStat object and
 * (as a result) from the ring. Every server should add their room
 * population counts to the RingStat object.
 * 
 * @author dew47
 *
 */
public class RingStat implements ISendable {

	private static final long serialVersionUID = 1L;
	
	private ServerID headNode;
	// Arrays for serializability
	private ServerStats[] globalStats;
	private RoomCount[] globalRoomCounts;
	private long updateCounter;
	
	@Override
	public PacketType getPacketType() {
		return PacketType.RING_STAT;
	}

	public RingStat(ServerID headNode) {
		this.headNode = headNode;
		this.globalStats = new ServerStats[0];
		this.globalRoomCounts = new RoomCount[0];
		this.updateCounter = 0;
	}
	
	public ServerID getHeadNode()
    {
        return headNode;
    }

    public void setHeadNode(ServerID headNode)
    {
        this.headNode = headNode;
    }

    public ServerStats[] getGlobalStats()
    {
        return globalStats;
    }

    public RoomCount[] getGlobalRoomCounts()
    {
        return globalRoomCounts;
    }

    public void updateLoad(ServerID serverID, float load)
    {
        // Increment the update counter for each server touched.
        updateCounter++;
        
        for (int i = 0; i < globalStats.length; i++)
        {
            ServerStats s = globalStats[i];
            if (s.id.getServerNumber() == serverID.getServerNumber())
            {
                s.load = load;
                s.lastUpdate = updateCounter;
            }
            else if (s.id.getServerNumber() > serverID.getServerNumber())
            {
                ServerStats[] newList = new ServerStats[globalStats.length+1];
                for (int j = 0; j < i; j++)
                    newList[j] = globalStats[j];
                
                newList[i] = new ServerStats(RingServer.Stats().getServerID(), 
                        RingServer.Stats().getServerAddress(), load, updateCounter);
                
                for (int j = i; j < globalStats.length; j++)
                    newList[j+1] = globalStats[j];
                this.globalStats = newList;
                break;
            }
        }
    }

    public void incrementRoomCount(String rName, int num)
    {
        for (int i = 0; i < globalRoomCounts.length; i++)
        {
            RoomCount s = globalRoomCounts[i];
            int comparison = s.name.compareTo(rName); 
            if ( comparison == 0)
            {
                s.users += num;
            }
            else if (comparison > 0)
            {
                RoomCount[] newList = new RoomCount[globalRoomCounts.length+1];
                for (int j = 0; j < i; j++)
                    newList[j] = globalRoomCounts[j];
                newList[i] = new RoomCount(rName, num);
                for (int j = i; j < globalRoomCounts.length; j++)
                    newList[j+1] = globalRoomCounts[j];
                this.globalRoomCounts = newList;
                break;
            }
        }
    }
    
    public void cullEmptyRooms()
    {
        ArrayList<RoomCount> rooms = new ArrayList<RoomCount>();
        for (int i = 0; i < globalRoomCounts.length; i++)
        {
            if (globalRoomCounts[i].users > 0)
                rooms.add(globalRoomCounts[i]);
        }
        this.globalRoomCounts = new RoomCount[rooms.size()];
        rooms.toArray(this.globalRoomCounts);
    }
    
    public PriorityQueue<ServerStats> getPriorityListing()
    {
        PriorityQueue<ServerStats> queue = new PriorityQueue<ServerStats>(
                this.globalStats.length, ServerStats.PRIORITY_COMPARATOR);
        for (ServerStats s : this.globalStats)
            queue.add(s);
        return queue;
    }

    public long getCurrentUpdateCounter()
    {
        return this.updateCounter;
    }

    public ServerStats getOldestNode()
    {
        if (this.globalStats.length == 0)
            return null;
        
        ServerStats oldest = this.globalStats[0];
        for (int i = 1; i < this.globalStats.length; i++)
        {
            if (this.globalStats[i].lastUpdate < oldest.lastUpdate)
            {
                oldest = this.globalStats[i];
            }
        }
        return oldest;
    }
    
//  /**
//  * Take counts for rooms on this server and add them to the counts
//  * from previously visited nodes this round.
//  * 
//  * @param localRoomCounts a collection of RoomCount objects
//  */
// public void updateRoomCounts(Collection<RoomCount> localRoomCounts) {
//     RoomCount globalRC;
//     
//     for(RoomCount localRC : localRoomCounts) {
//         globalRC = globalRoomCounts.get(localRC.name);
//         if(globalRC == null) {
//             // if room not present, add it
//             globalRoomCounts.put(localRC.name, localRC);
//         } else {
//             // otherwise, update count
//             globalRC.users += localRC.users;
//         }
//     }
// }
// 
// /**
//  * Get all of the room names we currently know about
//  * 
//  * @return
//  */
// public String[] getCurrentRoomListing() {
//     String[] ret = new String[0];
//     ret = this.globalRoomCounts.keySet().toArray(ret);
//     return ret;
// }
// 
// /**
//  * Get all of the room count objects in their most up to date
//  * state. This is only really useful after the RingStat has 
//  * made the rounds.
//  * 
//  * @return
//  */
// public Collection<RoomCount> getCurrentRoomCounts() {
//     return this.globalRoomCounts.values();
// }
// 
// /**
//  * Update the set of statistics for a given server in the 
//  * list of statistics.
//  * 
//  * @param update the update
//  * @return true if update performed, false if server not in list
//  */
// public boolean updateServerStats(ServerStats update){
//     int i;
//     ServerStats ss;
//     
//     for(i = 0; i < globalStats.size(); i++) {
//         ss = globalStats.get(i);
//         if(ss.id.equals(update.id)){
//             globalStats.set(i, update);
//             break;
//         }
//     }
//     
//     return i != globalStats.size();
// }
//     
// 
// public ArrayList<ServerStats> getServerListing() {
//     return new ArrayList<ServerStats>(globalStats);
// }
//
// /**
//  * Get an array list of the servers with the most available
//  * server first, followed by less available servers
//  * 
//  * @return
//  */
// public ArrayList<ServerStats> getPriorityListing() {
//     // must sort these here
//     // (need to flesh out load class and define comparator)
//     return null; // for now
// }
}
