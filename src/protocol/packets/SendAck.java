package protocol.packets;

import protocol.ISendable;
import protocol.PacketType;

public class SendAck implements ISendable
{
	private static final long serialVersionUID = 1L;

	@Override
	public PacketType getPacketType()
	{
		return PacketType.SEND_ACK;
	}

	private ServerUpdate serverUpdate;
	private long timestamp;

	public SendAck(ServerUpdate serverUpdate, long timestamp)
	{
		this.serverUpdate = serverUpdate;
		this.timestamp = timestamp;
	}

	public ServerUpdate getServerUpdate()
	{
		return serverUpdate;
	}

	public void setServerUpdate(ServerUpdate serverUpdate)
	{
		this.serverUpdate = serverUpdate;
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
