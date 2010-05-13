package binserver;

import protocol.IServerConnection;

public class BinSession
{
    public String nodeAddress;
    public boolean isActive;
    public boolean isConnected;
    public IServerConnection<BinSession> connection;
    
    public BinSession(IServerConnection<BinSession> connection)
    {
        this.connection = connection;
        nodeAddress = connection.getRemoteAddress().getHostName();
        // Not freed by default, will be freed at initialization if it's not a head node.
        isActive = true;
        isConnected = true;
    }
}
