package protocol;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import protocol.data.ClientID;
import protocol.data.RoomCount;
import server.ClientSession;

/**
 * Organizes clients by ClientID and Room, providing functions for
 * easy access and manipulation.
 * 
 * @author dew47
 *
 */
public class ClientList {

	private Map<ClientID, ClientSession> clientsByID;
	private Map<String, Set<ClientSession>> clientsByRoom;
	
	/**
	 * Add the given client to the list
	 * @param sess the client
	 */
	public synchronized void addClient(ClientSession sess) {
		Set<ClientSession> roomSet;
		
		// make clients accessible by ClientID
		clientsByID.put(sess.getClientID(), sess); 
		
		// make clients accessible by RoomID
		roomSet = clientsByRoom.get(sess.getRoom());
		if(roomSet == null) {
			// add room
			roomSet = new HashSet<ClientSession>();
			clientsByRoom.put(sess.getRoom(), roomSet);
		}
		roomSet.add(sess);
	}
	
	/**
	 * Remove the given client from the list
	 * @param sess the client
	 */
	public synchronized void removeClient(ClientSession sess) {
		Set<ClientSession> roomSet;
		
		// remove from room listing
		roomSet = clientsByRoom.get(sess.getRoom());
		assert(roomSet != null);
		roomSet.remove(sess);
		// delete room listing entirely if size == 0
		if(roomSet.size() == 0) {
			clientsByRoom.remove(sess.getRoom());
		}
		
		// remove from clientid listing
		clientsByID.remove(sess.getClientID());
	}
	
	/**
	 * Remove the given client from the list
	 * @param cid the client ID of the client
	 */
	public synchronized void removeClient(ClientID cid) {
		ClientSession sess;
		sess = clientsByID.get(cid);
		// TODO decide what to do when null
		if(sess != null) {
			removeClient(sess);
		}
	}
	
	/**
	 * Return an array of all the clients in a given chat room
	 * 
	 * @param roomName
	 * @return
	 */
	public synchronized ClientSession[] getRoomClients(String roomName) {
		Set<ClientSession> roomSet = this.clientsByRoom.get(roomName);
		ClientSession[] ret = new ClientSession[0];
		
		if(roomSet == null) return null;
		
		return roomSet.toArray(ret);
	}
	
	/**
	 * Get a collection of counts for all the rooms currently hosted
	 * on this server
	 * 
	 * @return
	 */
	public synchronized Collection<RoomCount> countRooms() {
		RoomCount rc;
		Entry<String, Set<ClientSession>> ent;
		LinkedList<RoomCount> roomCounts = new LinkedList<RoomCount>();
		Iterator<Entry<String, Set<ClientSession>>> it =
			clientsByRoom.entrySet().iterator();

		while(it.hasNext()) {
			ent = it.next();
			rc = new RoomCount(ent.getKey(), ent.getValue().size());
			roomCounts.add(rc);
		}
		
		return roomCounts;
	}
}
