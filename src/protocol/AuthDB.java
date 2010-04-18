package protocol;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import protocol.data.ClientID;
import protocol.data.RoomCount;
import protocol.packets.RingAuthUpdate;
import protocol.packets.ServerUpdate;

public class AuthDB {
	private Map<String, Integer> roomToRing;
	private Map<Integer, ServerUpdate> serverToRing;
	private AtomicInteger clientCounter;
	
	public AuthDB() {
		this.roomToRing = new HashMap<String, Integer>();
		this.serverToRing = new HashMap<Integer, ServerUpdate>();
		this.clientCounter = new AtomicInteger(0);
	}

	public ClientID newClientID(String room) {
		return new ClientID(room, clientCounter.incrementAndGet());
	}
	
	/**
	 * Take an update from a ring and process it appropriately
	 * 
	 * @param up
	 */
	public synchronized void processUpdate(RingAuthUpdate up) {

		Integer ring;

		for(RoomCount rc : up.roomCounts) {
			ring = this.roomToRing.get(rc.name);
			if(ring != null && rc.users == 0) {
				// delete the room from the listing
				this.roomToRing.remove(rc.name);
			} else {
				// update/add the listing
				this.roomToRing.put(rc.name, up.serverID.getRing());
			}
		}

		this.serverToRing.put(up.serverID.getRing(), up.latestUpdate);
	}

	/**
	 * When a ring dies, its values must be removed
	 * 
	 * @param sid
	 */
	public synchronized void processRingDeath(Integer ring) {
		// remove server from room mappings
		Iterator<Map.Entry<String,Integer>> it = roomToRing.entrySet().iterator();
		Map.Entry<String,Integer> ent;

		while(it.hasNext()) {
			ent = it.next();
			if(ent.getValue().equals(ring)) it.remove();
		}

		// remove from update mapping
		this.serverToRing.remove(ring);
	}

	/**
	 * Determine whether this room is currently hosted
	 * 
	 * @param room
	 * @return the ring number or null, if room not hosted
	 */
	public synchronized Integer getRing(String room) {
		return this.roomToRing.get(room);
	}

	/**
	 * Get the latest ServerUpdate for a given server, if it exists
	 *
	 * @param sid
	 * @return the update, or null if dne
	 */
	public synchronized ServerUpdate getUpdate(Integer ring) {
		return this.serverToRing.get(ring);
	}
}
