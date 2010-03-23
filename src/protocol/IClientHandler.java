package protocol;

public interface IClientHandler
{
    // Called when a packet arrives.
    void onPacket(ClientConnection caller, Packet p);
 // Called when the registered connection is closed.
    void onConnectionClosed(ClientConnection caller);
}
