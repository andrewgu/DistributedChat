package binserver;

import protocol.IReplyable;
import protocol.PacketType;

public class AllocateReply implements IReplyable
{
    private static final long serialVersionUID = 1L;
    
    private long replyCode;
    
    public AllocateReply(long replyCode)
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
        return PacketType.BIN_ALLOCATE_REQUEST;
    }
}
