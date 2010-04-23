package protocol;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import protocol.data.AuthRingData;
import protocol.data.ClientID;
import protocol.packets.RingAuthUpdate;
import protocol.packets.ServerUpdate;

public class AuthDB {
	private Map<String, Integer> roomToRing;
	private Map<Integer, AuthRingData> ringToRingData;
	private AtomicInteger clientCounter;

	public AuthDB() {
		this.roomToRing = new HashMap<String, Integer>();
		this.ringToRingData = new HashMap<Integer, AuthRingData>();
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

		int i;

		// update live room mappings
		for(i = 0; i < up.liveRooms.length; i++) {
			this.roomToRing.put(up.liveRooms[i], up.serverID.getRing());
		}

		// remove dead rooms
		for(i = 0; i < up.deadRooms.length; i++) {
			this.roomToRing.remove(up.deadRooms[i]);
		}

		AuthRingData ard = new AuthRingData(up.serverID.getRing(),
				up.state, up.latestUpdate);
		// update ring mappings
		this.ringToRingData.put(ard.ring, ard);
	}

	/**
	 * When a ring dies, its values must be removed
	 * 
	 * @param sid
	 */
	public synchronized void processRingDeath(Integer ring) {
		// remove server from room mappings
		Iterator<Map.Entry<String,Integer>> it =
			roomToRing.entrySet().iterator();
		Map.Entry<String,Integer> ent;

		while(it.hasNext()) {
			ent = it.next();
			if(ent.getValue().equals(ring)) it.remove();
		}

		// remove from update mapping
		this.ringToRingData.remove(ring);
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
	 * Get the latest ServerUpdate for a given ring, if it exists
	 *
	 * @param ring
	 * @return the update, or null if dne
	 */
	public synchronized ServerUpdate getUpdate(Integer ring) {
		AuthRingData ard = this.ringToRingData.get(ring);
		
		if(ard == null) return null;
		
		return ard.latestUpdate;
	}
	
	/**
	 * Get the ServerUpdate representing the most fit ring for
	 * clients to attempt a connection.
	 * 
	 * @return
	 */
	public synchronized ServerUpdate mostFitRing() {
		Iterator<AuthRingData> it = 
			this.ringToRingData.values().iterator();
		AuthRingData ard, normal, spawning;
		ard = normal = spawning = null;
		
		while(it.hasNext()) {
			ard = it.next();
			
			switch(ard.ringState) {
			case NORMAL:
				normal = ard;
				break;
				
			case SPAWNING:
				spawning = ard;
				break;
			}
			
			if(normal != null) break;
		}
		
		// for now just return a normal or spawning server, or nothing
		if(normal != null) return normal.latestUpdate;
		else if(spawning != null) return spawning.latestUpdate;
		else return null;
	}
}
