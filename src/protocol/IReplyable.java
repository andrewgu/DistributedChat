package protocol;

// Each replyable packet will have to implement the reply code storing mechanism independently.
// The only guarantee necessary is that the value returned from getReplyCode is unique between all
// packets send on a single connection. To guarantee that, use the getUniqueReplyCode methods
// on ClientConnection and IServerConnection.
public interface IReplyable extends ISendable
{
	public long getReplyCode();
}
