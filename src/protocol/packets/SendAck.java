package protocol.packets;

import protocol.PacketType;
import protocol.ReplyPacket;
import protocol.data.MessageID;

public class SendAck extends ReplyPacket
{
	private static final long serialVersionUID = 1L;

	@Override
	public PacketType getPacketType()
	{
		return PacketType.SEND_ACK;
	}

	private ServerUpdate serverUpdate;
	private long timestamp;
	private MessageID messageID;

	public SendAck(ServerUpdate serverUpdate, long timestamp, MessageID messageID, int replyCode)
	{
		super(replyCode);
		this.serverUpdate = serverUpdate;
		this.timestamp = timestamp;
		this.messageID = messageID;
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

	public MessageID getMessageID()
	{
		return messageID;
	}

	public void setMessageID(MessageID messageID)
	{
		this.messageID = messageID;
	}
}
