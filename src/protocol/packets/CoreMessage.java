package protocol.packets;

import protocol.ISendable;
import protocol.PacketType;
import protocol.data.ClientID;
import protocol.data.MessageID;

/**
 * A CoreMessage exists on the server side and is used to represent
 * the basic data about a message that needs to be passsed between
 * servers. All other data can be sent to clients from elsewhere.
 * 
 * @author dew47
 */

public class CoreMessage implements ISendable {

	private static final long serialVersionUID = 1L;
	
	public final MessageID messageID;
	public final ClientID sender;
	public final String alias;
	public final String room;
	public final long timestamp;
	public final String message;

	public CoreMessage(String room, String message, MessageID messageID,
			ClientID sender, String alias, long timestamp) {

		this.messageID = messageID;
		this.sender = sender;
		this.alias = alias;
		this.room = room;
		this.timestamp = timestamp;
		this.message = message;
	}

	@Override
	public PacketType getPacketType() {
		// TODO Auto-generated method stub
		return PacketType.CORE_MESSAGE;
	}
}
