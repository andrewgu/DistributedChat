package protocol.packets;

import protocol.ISendable;
import protocol.PacketType;
import protocol.data.ServerAddress;
import protocol.data.ServerID;

public class RingInitPacket implements ISendable
{
    private static final long serialVersionUID = 1L;
 
    private int ring;
    private int server;
    private int port;
    private String host;
    
    
    public RingInitPacket(int ring, int server, int port, String host)
    {
        this.ring = ring;
        this.server = server;
        this.port = port;
        this.host = host;
    }
    
    public ServerID getServerID()
    {
        return new ServerID(this.ring, this.server);
    }
    
    public ServerAddress getServerAddress()
    {
        return new ServerAddress(host, port);
    }

    @Override
    public PacketType getPacketType()
    {
        return PacketType.RING_INIT;
    }
}
