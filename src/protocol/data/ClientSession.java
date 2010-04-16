package protocol.data;

import protocol.packets.ClientConnect;
import protocol.packets.ClientReconnect;

public class ClientSession {

	private ClientID id;
	private String room;
	
	public ClientSession() {		
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
}
