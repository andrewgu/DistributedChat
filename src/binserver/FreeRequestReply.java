package binserver;

import protocol.IReplyable;
import protocol.PacketType;
import protocol.ReplyPacket;

public class FreeRequestReply extends ReplyPacket
{
    private static final long serialVersionUID = 1L;
    
    private boolean success;
    
    protected FreeRequestReply(IReplyable replyable, boolean success)
    {
        super(replyable);
        this.success = success;
    }

    @Override
    public PacketType getPacketType()
    {
        // TODO Auto-generated method stub
        return PacketType.BIN_FREE_REQUEST_REPLY;
    }
    
    public boolean wasSuccessful()
    {
        return success;
    }
}
