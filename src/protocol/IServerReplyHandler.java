package protocol;

public interface IServerReplyHandler<_ATTACHMENT>
{
	// Called when a response arrives to an IReplyable packet sent via the
	// IServerConnection.sendReplyable method. 
	public void onReply(IServerConnection<_ATTACHMENT> caller, ReplyPacket reply);
	// Called if the timeout period expires on the IReplyable packet sent via the
	// IServerConnection.sendReplyable method. 
	public void onTimeout(IServerConnection<_ATTACHMENT> caller);
	// Called if the connection gets closed before the timeout period expires.
	public void onRejected(IServerConnection<_ATTACHMENT> caller);
	
	// NOTE: for each packet, exactly one of the three methods will get called eventually on the assigned reply handler.
}
