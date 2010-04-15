package protocol;

import java.util.HashMap;
import protocol.data.ClientID;
import protocol.data.ClientSession;
import protocol.packets.ClientConnect;
import protocol.packets.ClientReconnect;

public class ClientProtocolHandler implements IServerHandler<ClientSession> {


	private RingProtocolHandler rph;
	private ClientList clients;
	
	
	
	
	@Override
	public void onClose(IServerConnection<ClientSession> connection) {
		// TODO stub
		clients.removeClient(connection.getAttachment());
	}

	@Override
	public void onConnect(IServerConnection<ClientSession> connection) {
		
		
		
	}

	@Override
	public void onPacket(IServerConnection<ClientSession> connection,
			ISendable packet) {

		ClientSession sess = connection.getAttachment();
		
		// TODO what about FIND_ROOM? how does this get handled
		switch(packet.getPacketType()) {
		case CLIENT_CONNECT:
			handleConnect(sess, (ClientConnect) packet);
			break;
			
		case CLIENT_RECONNECT:
			handleReconnect(sess, (ClientReconnect) packet);
			break;
			
		case SEND_MESSAGE:
			
			break;
		
		}
	}
	
	private void handleConnect(ClientSession sess, ClientConnect cn) {
		// init session
		sess.onConnect(cn);

		clients.addClient(sess);
		
		
	}

	private void handleReconnect(ClientSession sess, ClientReconnect crn) {
		// init session
		sess.onReconnect(crn);
		
		clients.addClient(sess);

	}
	
}
