package protocol;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import protocol.data.ClientID;
import protocol.data.ClientSession;

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
}
