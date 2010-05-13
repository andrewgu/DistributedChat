package binserver;

import protocol.IReplyable;
import protocol.PacketType;
import protocol.ReplyPacket;

public class AllocateRequestReply extends ReplyPacket
{
    private static final long serialVersionUID = 1L;

    private boolean successful;
    
    protected AllocateRequestReply(IReplyable replyable, boolean successful)
    {
        super(replyable);
        
        this.successful = successful;
    }

    @Override
    public PacketType getPacketType()
    {
        // TODO Auto-generated method stub
        return PacketType.BIN_ALLOCATE_REQUEST_RESPONSE;
    }
    
    public boolean wasSuccessful()
    {
        return this.successful;
    }
}
