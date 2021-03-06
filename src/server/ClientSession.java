package server;

import java.io.IOException;

import protocol.ISendable;
import protocol.IServerConnection;
import protocol.data.ClientID;
import protocol.packets.ClientConnect;
import protocol.packets.ClientReconnect;

public class ClientSession {

	private IServerConnection<ClientSession> conn;
	private ClientID id;
	private String room;
	
	public ClientSession(IServerConnection<ClientSession> connection) {
		this.conn = connection;
		this.id = null;
		this.room = null;
	}
	
	public void kickClient()
	{
	    IServerConnection<ClientSession> temp = conn;
	    conn = null;
	    temp.close();
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
	
	public void deliverToClient(ISendable pkt) {
		try {
			conn.sendPacket(pkt);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
