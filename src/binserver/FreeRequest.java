package binserver;

import protocol.IReplyable;
import protocol.PacketType;

public class FreeRequest implements IReplyable
{
    private static final long serialVersionUID = 1L;
    
    private long replyCode;
    
    public FreeRequest(long replyCode)
    {
        this.replyCode = replyCode;
    }

    @Override
    public long getReplyCode()
    {
        // TODO Auto-generated method stub
        return this.replyCode;
    }

    @Override
    public PacketType getPacketType()
    {
        // TODO Auto-generated method stub
        return PacketType.FREE_REQUEST;
    }

}
