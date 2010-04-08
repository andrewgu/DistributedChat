package protocol.packets;

import protocol.IReplyable;
import protocol.PacketType;
import protocol.data.ClientID;

public class ClientConnect implements IReplyable
{
	private static final long serialVersionUID = 1L;

	@Override
	public PacketType getPacketType()
	{
		return PacketType.CLIENT_CONNECT;
	}
	
	private ClientID client;
	private String room;
	private long replyCode;

	public ClientConnect(ClientID client, String room, long replyCode)
	{
		this.client = client;
		this.room = room;
		this.replyCode = replyCode;
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

	@Override
	public long getReplyCode()
	{
		return this.replyCode;
	}
}
