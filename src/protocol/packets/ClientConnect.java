package protocol.packets;

import protocol.ISendable;
import protocol.PacketType;
import protocol.data.ClientID;

public class ClientConnect implements ISendable
{
	private static final long serialVersionUID = 1L;

	@Override
	public PacketType getPacketType()
	{
		return PacketType.CLIENT_CONNECT;
	}
	
	private ClientID client;
	private String room;

	public ClientConnect(ClientID client, String room)
	{
		this.client = client;
		this.room = room;
	}

	public ClientID getClient()
	{
		return client;
	}

	public void setClient(ClientID client)
	{
		this.client = client;
	}

	public String getRoom()
	{
		return room;
	}

	public void setRoom(String room)
	{
		this.room = room;
	}
}
