package protocol;

import java.io.IOException;

public interface IServerConnection<_ATTACHMENT>
{
    void sendPacket(ISendable packet)  throws IOException;
    void sendReplyable(IReplyable replyable, IServerReplyHandler<_ATTACHMENT> handler, long milliseconds) throws IOException;
    int getUniqueReplyCode();
    void setAttachment(_ATTACHMENT attachment);
    _ATTACHMENT getAttachment();
    void close();
}