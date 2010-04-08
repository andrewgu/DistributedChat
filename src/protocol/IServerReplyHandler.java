package protocol;

public interface IServerReplyHandler<_ATTACHMENT>
{
	public void onReply(IServerConnection<_ATTACHMENT> caller, ReplyPacket reply);
	public void onTimeout(IServerConnection<_ATTACHMENT> caller);
	public void onRejected(IServerConnection<_ATTACHMENT> caller);
}
