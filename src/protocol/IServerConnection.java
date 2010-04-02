package protocol;

import java.io.IOException;

public interface IServerConnection<_ATTACHMENT>
{
    void sendPacket(ISendable packet)  throws IOException;
    void setAttachment(_ATTACHMENT attachment);
    _ATTACHMENT getAttachment();
    void close();
}