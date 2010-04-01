package protocol.packets;

import protocol.ISendable;
import protocol.PacketType;
import protocol.data.ClientID;
import protocol.data.MessageID;

public class MessageData implements ISendable
{
	private static final long serialVersionUID = 1L;

	@Override
	public PacketType getPacketType()
	{
		// TODO Auto-generated method stub
		return PacketType.MESSAGE_DATA;
	}
	
	private ServerUpdate serverUpdate;
	private MessageID messageID;
	private ClientID sender;
	private String alias;
	private long timestamp;
	private String message;

	public MessageData(ServerUpdate serverUpdate, String room, String message,
			MessageID messageID, ClientID sender, String alias, long timestamp)
	{
		this.serverUpdate = serverUpdate;
		this.serverUpdate.setRoom(room);
		this.messageID = messageID;
		this.sender = sender;
		this.alias = alias;
		this.timestamp = timestamp;
		this.message = message;
	}

	public ServerUpdate getServerUpdate()
	{
		return serverUpdate;
	}

	public void setServerUpdate(ServerUpdate serverUpdate)
	{
		this.serverUpdate = serverUpdate;
	}

	public String getRoom()
	{
		return this.serverUpdate.getRoom();
	}

	public void setRoom(String room)
	{
		this.serverUpdate.getRoom();
	}

	public MessageID getMessageID()
	{
		return messageID;
	}

	public void setMessageID(MessageID messageID)
	{
		this.messageID = messageID;
	}

	public ClientID getSender()
	{
		return sender;
	}

	public void setSender(ClientID sender)
	{
		this.sender = sender;
	}

	public String getAlias()
	{
		return alias;
	}

	public void setAlias(String alias)
	{
		this.alias = alias;
	}

	public long getTimestamp()
	{
		return timestamp;
	}

	public void setTimestamp(long timestamp)
	{
		this.timestamp = timestamp;
	}

	public String getMessage()
	{
		return message;
	}

	public void setMessage(String message)
	{
		this.message = message;
	}
}
