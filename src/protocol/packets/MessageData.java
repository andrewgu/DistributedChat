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
	private CoreMessage coreMessage;
	
	public MessageData(ServerUpdate serverUpdate, String room, String message,
			MessageID messageID, ClientID sender, String alias, long timestamp)
	{
		this.serverUpdate = serverUpdate;
		this.serverUpdate.setRoom(room);
		this.coreMessage = new CoreMessage(room, message, messageID, sender,
				alias, timestamp);
	}
	
	public MessageData(ServerUpdate serverUpdate, CoreMessage coreMessage) {
		this.serverUpdate = serverUpdate;
		this.coreMessage = coreMessage;
		this.serverUpdate.setRoom(this.coreMessage.room);
	}

	public ServerUpdate getServerUpdate()
	{
		return serverUpdate;
	}

	public void setServerUpdate(ServerUpdate serverUpdate)
	{
		this.serverUpdate = serverUpdate;
	}

	public CoreMessage getCoreMessage()
	{
		return this.coreMessage;
	}
	
	public MessageID getMessageID() {
		return coreMessage.messageID;
	}
	
	public long getTimestamp() {
		return coreMessage.timestamp;
	}
}
