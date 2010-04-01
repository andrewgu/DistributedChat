package protocol.packets;

import protocol.ISendable;
import protocol.PacketType;
import protocol.data.ClientID;
import protocol.data.MessageID;

public class SendMessage implements ISendable
{
	private static final long serialVersionUID = 1L;

	@Override
	public PacketType getPacketType()
	{
		return PacketType.SEND_MESSAGE;
	}
	
	private String room;
	private String alias;
	private ClientID clientID;
	private MessageID messageID;
	private String message;

	public SendMessage(String room, String alias, ClientID clientID,
			MessageID messageID, String message)
	{
		this.room = room;
		this.alias = alias;
		this.clientID = clientID;
		this.messageID = messageID;
		this.message = message;
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
		return clientID;
	}

	public void setClientID(ClientID clientID)
	{
		this.clientID = clientID;
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
}
