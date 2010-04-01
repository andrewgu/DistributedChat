package protocol.packets;

import protocol.ISendable;
import protocol.PacketType;
import protocol.data.ClientID;

public class ClientReconnect implements ISendable
{
	private static final long serialVersionUID = 1L;

	@Override
	public PacketType getPacketType()
	{
		return PacketType.CLIENT_RECONNECT;
	}

	private ClientID client;
	private String room;
	private long lastAcked;
	private long lastReceived;

	public ClientReconnect(ClientID client, String room, long lastAcked,
			long lastReceived)
	{
		this.client = client;
		this.room = room;
		this.lastAcked = lastAcked;
		this.lastReceived = lastReceived;
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

	public long getLastAcked()
	{
		return lastAcked;
	}

	public void setLastAcked(long lastAcked)
	{
		this.lastAcked = lastAcked;
	}

	public long getLastReceived()
	{
		return lastReceived;
	}

	public void setLastReceived(long lastReceived)
	{
		this.lastReceived = lastReceived;
	}
}
