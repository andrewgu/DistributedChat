package protocol;

// Every unique packet type should have a corresponding entry in this enum.
public enum PacketType
{
	
	// CLIENT-->SERVER MESSAGES
	CLIENT_CONNECT,
	CLIENT_RECONNECT,
	SEND_MESSAGE,
	FIND_ROOM,

	// SERVER-->CLIENT MESSAGES
	// ack types
	CONNECT_ACK,
	SEND_ACK,
	// message components
	MESSAGE_DATA,
	SERVER_UPDATE,
	ROOM_FOUND,

	// INTRA RING MESSAGES
	RING_STAT, // communicate server status
	CORE_MESSAGE // pass a message along
}
