package protocol;

public interface IReplyHandler
{
	public void onReply(ClientConnection caller, ReplyPacket reply);
	public void onTimeout(ClientConnection caller);
	public void onRejected(ClientConnection caller);
}
