package protocol.packets;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import protocol.ISendable;
import protocol.PacketType;
import protocol.data.RoomCount;
import protocol.data.ServerID;
import protocol.data.ServerStats;


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
	
	public final ServerID headNode;
	private ArrayList<ServerStats> globalStats;
	private HashMap<String, RoomCount> globalRoomCounts;
	
	@Override
	public PacketType getPacketType() {
		return PacketType.RING_STAT;
	}

	public RingStat(ServerID headNode, ArrayList<ServerStats> lastRoundGlobalStats) {
		this.headNode = headNode;
		this.globalStats = lastRoundGlobalStats;
		this.globalRoomCounts = new HashMap<String, RoomCount>();
	}
	
	/**
	 * Take counts for rooms on this server and add them to the counts
	 * from previously visited nodes this round.
	 * 
	 * @param localRoomCounts a collection of RoomCount objects
	 */
	public void updateRoomCounts(Collection<RoomCount> localRoomCounts) {
		RoomCount globalRC;
		
		for(RoomCount localRC : localRoomCounts) {
			globalRC = globalRoomCounts.get(localRC.name);
			if(globalRC == null) {
				// if room not present, add it
				globalRoomCounts.put(localRC.name, localRC);
			} else {
				// otherwise, update count
				globalRC.users += localRC.users;
			}
		}
		
	}
	
	/**
	 * Update the set of statistics for a given server in the 
	 * list of statistics.
	 * 
	 * @param update the update
	 * @return true if update performed, false if server not in list
	 */
	public boolean updateServerStats(ServerStats update){
		int i;
		ServerStats ss;
		
		for(i = 0; i < globalStats.size(); i++) {
			ss = globalStats.get(i);
			if(ss.id.equals(update.id)){
				globalStats.set(i, update);
				break;
			}
		}
		
		return i != globalStats.size();
	}
	
	/**
	 * Remove a given server from the list of servers in the RingStat
	 * @param remove the server to remove
	 * @return true if server removed, false if does not exist in list
	 */
	public boolean removeServer(ServerID remove) {
		int i;
		ServerStats ss;
		
		for(i = 0; i < globalStats.size(); i++) {
			ss = globalStats.get(i);
			if(ss.id.equals(remove)) {
				globalStats.remove(i);
				break;
			}
		}
		
		return i != globalStats.size();
	}
	
	/**
	 * Get the ServerStats for the successor to the given node
	 * @param cur the node for whose successors we are searching
	 * @return null if cur is not in list, else successor
	 */
	public ServerStats getSuccessor(ServerID cur) {
		ServerStats ss = null;
		
		for(int i = 0; i < globalStats.size(); i++) {
			ss = globalStats.get(i);
			if(ss.id.equals(cur)) {
				i++;
				i %= globalStats.size(); // wrap around to front of list
				ss = globalStats.get(i);
				break;
			}
			ss = null; // want to return null if never find
		}
		
		return ss;
	}
	
	public ArrayList<ServerStats> getServerListing() {
		return new ArrayList<ServerStats>(globalStats);
	}

	/**
	 * Get an array list of the servers with the most available
	 * server first, followed by less available servers
	 * 
	 * @return
	 */
	public ArrayList<ServerStats> getPriorityListing() {
		// must sort these here
		// (need to flesh out load class and define comparator)
		return null; // for now
	}
}
