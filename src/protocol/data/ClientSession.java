package protocol.data;

import java.io.IOException;

import protocol.ISendable;
import protocol.IServerConnection;
import protocol.packets.ClientConnect;
import protocol.packets.ClientReconnect;
import protocol.packets.CoreMessage;
import protocol.packets.MessageData;

public class ClientSession {

	private final IServerConnection<ClientSession> conn;
	private ClientID id;
	private String room;
	
	public ClientSession(IServerConnection<ClientSession> connection) {
		this.conn = connection;
	}

	public void sessInit(ClientID client, String room) {
		this.id = client;
		this.room = room;
	}
	
	public void onConnect(ClientConnect cn) {
		sessInit(cn.getClient(), cn.getRoom());
	}
	
	public void onReconnect(ClientReconnect crn) {
		sessInit(crn.getClient(), crn.getRoom());
	}
	
	public ClientID getClientID() {
		return id;
	}
	
	public String getRoom() {
		return room;
	}
	
	public void deliverPacketToClient(ISendable pkt) {
		try {
			conn.sendPacket(pkt);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
