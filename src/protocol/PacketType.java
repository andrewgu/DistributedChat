package protocol;

// Every unique packet type should have a corresponding entry in this enum.
public enum PacketType
{
	
	// CLIENT-->SERVER MESSAGES
	CLIENT_CONNECT,
	CLIENT_RECONNECT,
	SEND_MESSAGE,

	// SERVER-->CLIENT MESSAGES
	// ack types
	CONNECT_ACK,
	SEND_ACK,
	// message components
	MESSAGE_DATA,
	SERVER_UPDATE,

	// INTRA RING MESSAGES
	RING_STAT, // communicate server status
	CORE_MESSAGE, // pass a message along

	// AUTH PROTOCOL MESSAGES
	FIND_ROOM, // client --> authserver
	ROOM_FOUND, // authserver --> client
	RING_AUTH_UPDATE, // ring --> authserver
	RING_DEATH, // ring --> authserver
}
