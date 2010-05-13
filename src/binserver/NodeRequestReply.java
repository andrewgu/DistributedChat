package binserver;

import protocol.IReplyable;
import protocol.PacketType;
import protocol.ReplyPacket;

public class NodeRequestReply extends ReplyPacket
{
    private static final long serialVersionUID = 1L;
    private String nodeAddress;

    protected NodeRequestReply(IReplyable replyable, String nodeAddress)
    {
        super(replyable);
        
        this.nodeAddress = nodeAddress;
    }

    @Override
    public PacketType getPacketType()
    {
        return PacketType.BIN_NODE_REQUEST_REPLY;
    }

    public String getNodeAddress()
    {
        return this.nodeAddress;
    }
}
