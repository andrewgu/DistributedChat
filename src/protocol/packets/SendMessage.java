package protocol.packets;

import protocol.IReplyable;
import protocol.PacketType;
import protocol.data.ClientID;
import protocol.data.MessageID;

public class SendMessage implements IReplyable
{
	private static final long serialVersionUID = 4L;

	@Override
	public PacketType getPacketType()
	{
		return PacketType.SEND_MESSAGE;
	}
	
	private String room;
	private String alias;
	//private ClientID clientID;
	private MessageID messageID;
	private String message;
	private long timestamp;
	private long replyCode;

	public SendMessage(String room, String alias, //ClientID clientID,
			MessageID messageID, String message, long timestamp)
	{
		this.room = room;
		this.alias = alias;
		//this.clientID = clientID;
		this.messageID = messageID;
		this.message = message;
		this.timestamp = timestamp;
		this.replyCode = this.messageID.getMessageNumber();
	}

	public String getRoom()
	{
		return room;
	}

	public void setRoom(String room)
	{
		this.room = room;
	}

	public String getAlias()
	{
		return alias;
	}

	public void setAlias(String alias)
	{
		this.alias = alias;
	}

	public ClientID getClientID()
	{
		return this.messageID.getClientID();
	}

	public void setClientID(ClientID clientID)
	{
		this.getMessageID().setClientID(clientID);
	}

	public MessageID getMessageID()
	{
		return messageID;
	}

	public void setMessageID(MessageID messageID)
	{
		this.messageID = messageID;
	}

	public String getMessage()
	{
		return message;
	}

	public void setMessage(String message)
	{
		this.message = message;
	}

	public long getTimestamp()
	{
		return this.timestamp;
	}
	
	public void setTimestamp(long timestamp)
	{
		this.timestamp = timestamp;
	}
	
	public void setReplyCode(long replyCode)
	{
		this.replyCode = replyCode;
	}

	@Override
	public long getReplyCode()
	{
		// Message number is unique per client, so safe to use.
		return this.replyCode;
	}
}
