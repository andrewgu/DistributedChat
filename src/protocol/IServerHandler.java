package protocol;

public interface IServerHandler<_ATTACHMENT>
{
	// Called right after the connection is accepted. Use this event to attach an object of
	// type _ATTACHMENT to the connection to store session data.
    void onConnect(IServerConnection<_ATTACHMENT> connection);
    // Called whenever a packet arrives at the server.
    void onPacket(IServerConnection<_ATTACHMENT> connection, ISendable packet);
    void onClose(IServerConnection<_ATTACHMENT> connection);
}
