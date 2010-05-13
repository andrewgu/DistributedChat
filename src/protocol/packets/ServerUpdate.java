package protocol.packets;

import protocol.ISendable;
import protocol.PacketType;
import protocol.data.ServerID;
import protocol.data.ServerPriorityListing;

public class ServerUpdate implements ISendable, Cloneable
{
	private static final long serialVersionUID = 2L;

	@Override
	public PacketType getPacketType()
	{
		return PacketType.SERVER_UPDATE;
	}

	private String room;
	private ServerID sender;
	private ServerPriorityListing[] servers;

	public ServerUpdate(String room, ServerID sender, ServerPriorityListing[] servers)
	{
		this.room = new String(room);
		this.sender = new ServerID(sender.getRing(), sender.getServerNumber());
		this.servers = servers;
	}

	public String getRoom()
	{
		return room;
	}


	public void setRoom(String room)
	{
		this.room = room;
	}

	public ServerID getSender()
	{
		return sender;
	}

	public void setSender(ServerID sender)
	{
		this.sender = sender;
	}

	public ServerPriorityListing[] getServers()
	{
		return servers;
	}

	public void setServers(ServerPriorityListing[] servers)
	{
		this.servers = servers;
	}
	
	@Override
	public Object clone() throws CloneNotSupportedException {
		// for now, a shallow copy is sufficient for our purposes
		return super.clone();
	}

}
