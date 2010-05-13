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
	
	// BIN SERVER MESSAGES
	BIN_NODE_REQUEST, // client --> bin server
	BIN_NODE_REQUEST_RESPONSE, // bin server --> client in reply to NODE_REQUEST
	BIN_FREE_REQUEST, // client --> bin server
	BIN_FREE_REQUEST_RESPONSE, // bin server --> client in reply to FREE_REQUEST
	BIN_ALLOCATE_REQUEST, // bin server --> client to notify client to initialize.
	BIN_ALLOCATE_REQUEST_RESPONSE, // client --> bin server to ack.
}
