package protocol;

public interface IClientHandler
{
    // Called when a packet arrives.
    void onPacket(ClientConnection caller, ISendable packet);
 // Called when the registered connection is closed.
    void onConnectionClosed(ClientConnection caller);
}
