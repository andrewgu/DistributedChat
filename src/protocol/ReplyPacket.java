package protocol;

public abstract class ReplyPacket implements ISendable
{
	private static final long serialVersionUID = 1L;
	private int replyPacketCode;
	
	protected ReplyPacket(int replyPacketCode)
	{
		this.replyPacketCode = replyPacketCode;
	}

	public int getReplyPacketCode()
	{
		return replyPacketCode;
	}
	
	public void setReplyPacketCode(int replyCode)
	{
		this.replyPacketCode = replyCode;
	}
}
