package server;

import java.io.IOException;

import protocol.ISendable;
import protocol.IServerConnection;
import protocol.IServerHandler;
import protocol.packets.FindRoom;
import protocol.packets.RingDeath;
import protocol.packets.RoomFound;
import protocol.packets.ServerUpdate;


/**
 * Handles Auth server duties. Handles packets from clients and Servers.
 * 
 * @author dew47
 */

public class AuthProtocolHandler implements IServerHandler<AuthSession> {

	//AuthDB auth;

	@Override
	public void onClose(IServerConnection<AuthSession> connection) {
		// stateless protocol
	}

	@Override
	public void onConnect(IServerConnection<AuthSession> connection) {

		// stateless protocol
		connection.setAttachment(null);
	}

	@Override
	public void onPacket(IServerConnection<AuthSession> connection,
			ISendable packet) {

		switch(packet.getPacketType()) {

		case FIND_ROOM:
			handleFindRoom(connection, (FindRoom) packet);
			break;

		case RING_AUTH_UPDATE:
			handleRingUpdate(connection, (RingAuthUpdate) packet);
			break;

		case RING_DEATH:
			handleRingDeath(connection, (RingDeath) packet);
			break;

		default:
			// we just drop these
			connection.close();
			break;
		}
	}

	private void handleFindRoom(IServerConnection<AuthSession> connection,
			FindRoom fr) {
		ServerUpdate sup;
		//Integer ring = auth.getRing(fr.getRoom());
		if(ring == null) {
			sup = null;
		} else {
		//	sup = auth.getUpdate(ring);
		}

		RoomFound response;
		
		/* Either we've found the ring that hosts this room, or we
		 * will assign the chat to a ring.
		 */
		if(sup == null) {
			// NO ROOM FOUND
			sup = auth.mostFitRing();
		}
		
		response = new RoomFound(
				auth.newClientID(fr.getRoom()), sup, fr.getReplyCode());

		try {
			connection.sendPacket(response);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// client is done with this server
		connection.close();
	}

	private void handleRingUpdate(IServerConnection<AuthSession> connection, RingAuthUpdate up) {
		// pretty simple. keep the connection open.
		auth.processUpdate(up);
	}

	private void handleRingDeath(IServerConnection<AuthSession> connection, RingDeath death) {
		// we stop talking after this
		auth.processRingDeath(death.serverID.getRing());
		connection.close();
	}
}
