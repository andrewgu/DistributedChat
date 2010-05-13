package protocol.packets;

import protocol.ISendable;
import protocol.PacketType;
import protocol.data.MessageID;

public class MessageData implements ISendable
{
	private static final long serialVersionUID = 2L;

	@Override
	public PacketType getPacketType()
	{
		// TODO Auto-generated method stub
		return PacketType.MESSAGE_DATA;
	}
	
	private ServerUpdate serverUpdate;
	private CoreMessage coreMessage;
	
	
	public MessageData(ServerUpdate serverUpdate, CoreMessage coreMessage) {
		this.serverUpdate = serverUpdate;
		this.coreMessage = coreMessage;
		// Hack to get around serialization problems.
		this.serverUpdate.setRoom(new String(this.coreMessage.room));
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
