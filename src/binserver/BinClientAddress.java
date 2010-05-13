package binserver;

import protocol.ISendable;
import protocol.PacketType;

public class BinClientAddress implements ISendable
{
    private static final long serialVersionUID = 1L;
    private String name;
    
    public BinClientAddress(String name)
    {
        this.name = name;
    }
    
    public String getName()
    {
        return this.name;
    }

    @Override
    public PacketType getPacketType()
    {
        // TODO Auto-generated method stub
        return PacketType.BIN_CLIENT_ADDRESS;
    }

}
