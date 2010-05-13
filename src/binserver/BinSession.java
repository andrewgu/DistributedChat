package binserver;

import protocol.IServerConnection;

public class BinSession
{
    public String identifier;
    public String nodeAddress;
    public boolean isActive;
    public boolean isConnected;
    public IServerConnection<BinSession> connection;
    
    public BinSession(IServerConnection<BinSession> connection)
    {
        this.connection = connection;
        this.nodeAddress = connection.getRemoteAddress().getHostName();
        // Not freed by default, will be freed at initialization if it's not a head node.
        this.isActive = true;
        this.isConnected = true;
        
        this.identifier = this.nodeAddress + "." + Integer.toString(connection.getRemotePort());
    }
}
