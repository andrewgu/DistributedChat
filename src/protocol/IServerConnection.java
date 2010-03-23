package protocol;

import java.io.IOException;

interface IServerConnection<_ATTACHMENT>
{
    void sendPacket(Packet p)  throws IOException;
    void setAttachment(_ATTACHMENT attachment);
    _ATTACHMENT getAttachment();
    void close();
}