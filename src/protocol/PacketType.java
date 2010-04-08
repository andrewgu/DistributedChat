package protocol;

// Every unique packet type should have a corresponding entry in this enum.
public enum PacketType
{
	MESSAGE_DATA, SEND_MESSAGE, SERVER_UPDATE, SEND_ACK, FIND_ROOM, ROOM_FOUND, CLIENT_CONNECT, CLIENT_RECONNECT, CONNECT_ACK
}
