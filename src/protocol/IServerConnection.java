package protocol;

import java.io.IOException;

public interface IServerConnection<_ATTACHMENT>
{
	// Sends a packet to the other side of the connection. On the other end, it'll arrive and trigger a
	// IServerHandler.onPacket event.
    void sendPacket(ISendable packet)  throws IOException;
    // Sends a replyable packet to the other side of the connection. On the other end, it'll arrive and trigger
    // a IServerHandler.onPacket event. If the other ends responds, it should send a ReplyPacket subclass packet
    // back with the reply code included in the original replyable packet on this end. If the reply code matches,
    // the connection will intercept the event and call the IServerReplyHandler handler's .onReply instead of the 
    // normal IServerHandler handler. 
    // Three different outcomes can happen:
    // 1. if there's no response within the timeout specified (long milliseconds), the reply handler's onTimeout
    //    method will be called.
    // 2. if the connection gets closed before the timeout expires, the reply handler's onClosed method will be called.
    //    the IServerHandler handler's onClosed method will be called AFTER all of the reply handlers' onClosed methods
    //    are called.
    // 3. if there's a response within the timeout period, then the reply handler's onReply will be called. 
    void sendReplyable(IReplyable replyable, IServerReplyHandler<_ATTACHMENT> handler, long milliseconds) throws IOException;
    // Use this helper function to retrieve a unique reply code for use with replyable packets. 
    long getUniqueReplyCode();
    // Lets you set the attachment for this particular connection. Since there's one connection per client/other server,
    // this attachment is essentially a session state object. It's strongly typed for convenience reasons. The template
    // argument is specified through the ProtocolServer template class, so each ProtocolServer supports one session object
    // type paired with one port number per service type. You should call this mainly in the IServerHandler.onConnect event.
    void setAttachment(_ATTACHMENT attachment);
    // Retreives the attachment for this particular connection.
    _ATTACHMENT getAttachment();
    // Closes the connection. This will trigger the onClosed event for any reply handlers still waiting for replies.
    void close();
}