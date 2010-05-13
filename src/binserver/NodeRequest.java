package binserver;

import protocol.IReplyable;
import protocol.PacketType;

public class NodeRequest implements IReplyable
{
    private static final long serialVersionUID = 1L;
    
    private long replyCode;
    
    public NodeRequest(long replyCode)
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
        return PacketType.NODE_REQUEST;
    }

}
