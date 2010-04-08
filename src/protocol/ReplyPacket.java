package protocol;

// ReplyPacket is an abstract base class to simplify the implementation of
// a packet specifically designed to reply to another packet. Call the hidden
// constructor from the implementing class's constructor using "super(long replyPacketCode)"
// The protocol framework takes care of the rest. Alternately, you use the other constructor,
// which just extracts the reply code from the original packet you're replying to. Same idea:
// super(originalPacket).
public abstract class ReplyPacket implements ISendable
{
	private static final long serialVersionUID = 1L;
	private long replyPacketCode;
	
	protected ReplyPacket(long replyPacketCode)
	{
		this.replyPacketCode = replyPacketCode;
	}
	
	protected ReplyPacket(IReplyable replyable)
	{
		this.replyPacketCode = replyable.getReplyCode();
	}

	public long getReplyPacketCode()
	{
		return replyPacketCode;
	}
	
	public void setReplyPacketCode(int replyCode)
	{
		this.replyPacketCode = replyCode;
	}
}
