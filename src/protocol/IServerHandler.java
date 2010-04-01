package protocol;

public interface IServerHandler<_ATTACHMENT>
{
    void onConnect(IServerConnection<_ATTACHMENT> connection);
    void onPacket(IServerConnection<_ATTACHMENT> connection, ISendable packet);
    void onClose(IServerConnection<_ATTACHMENT> connection);
}
