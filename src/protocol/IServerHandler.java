package protocol;

// The handler's methods should be implemented as thread-safe.
// In other words, if the handler touches any shared resources, it must synchronize, because
// the ProtocolServer makes no guarantees about when the handler will get called. However,
// the ProtocolServer does guarantee that, for a given single connection, only one event
// will be fired at a time.
public interface IServerHandler<_ATTACHMENT>
{
	// Called right after the connection is accepted. Use this event to attach an object of
	// type _ATTACHMENT to the connection to store session data.
    void onConnect(IServerConnection<_ATTACHMENT> connection);
    // Called whenever a packet arrives at the server. Use ISendable.getPacketType to determine
    // the PacketType (enum) in order to respond appropriately.
    void onPacket(IServerConnection<_ATTACHMENT> connection, ISendable packet);
    // Called when the connection is closed. This can be called in three cases:
    // 1. the connection was dropped (network error)
    // 2. the other end closed the connection
    // 3. you called connection.close().
    void onClose(IServerConnection<_ATTACHMENT> connection);
}
