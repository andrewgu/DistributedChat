package protocol.packets;

import protocol.ISendable;
import protocol.PacketType;

public class ConnectAck implements ISendable
{
	private static final long serialVersionUID = 1L;

	@Override
	public PacketType getPacketType()
	{
		return PacketType.CONNECT_ACK;
	}

	private ServerUpdate servers;
	private long timestamp;

	public ConnectAck(ServerUpdate servers, long timestamp)
	{
		this.servers = servers;
		this.timestamp = timestamp;
	}

	public ServerUpdate getServers()
	{
		return servers;
	}

	public void setServers(ServerUpdate servers)
	{
		this.servers = servers;
	}

	public long getTimestamp()
	{
		return timestamp;
	}

	public void setTimestamp(long timestamp)
	{
		this.timestamp = timestamp;
	}
}
