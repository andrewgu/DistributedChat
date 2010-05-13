package client.test;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.TreeMap;

import protocol.ISendable;
import protocol.IServerConnection;
import protocol.IServerHandler;
import protocol.ProtocolServer;
import protocol.data.ClientID;
import protocol.data.ServerID;

public class TestChatServer
{
    public static final int PORT = 13001;
    public static final String ROOM = "test";
    
    public static final int FORCE_CONNECT_RETRY = 1;
    public static final int FORCE_RECONNECT_RETRY = 1;
    public static final int FORCE_SEND_RETRY = 1;
    
    public static void main(String[] args) throws UnknownHostException, IOException
    {
        ProtocolServer<TestChatSession> server = new ProtocolServer<TestChatSession>(
                PORT, 1, new TestChatHandler());
        server.start();
    }
    
    private static void println(String message)
    {
        synchronized(System.out)
        {
            System.out.println("CHAT:\t" + message);
        }
    }
    
    public static ServerID getServerID()
    {
        return new ServerID(0, 1);
    }
    
    private static class TestChatSession
    {
        private IServerConnection<TestChatSession> connection;
        private ClientID clientId;

        public TestChatSession(IServerConnection<TestChatSession> connection)
        {
            this.connection = connection;
            this.clientId = null;
        }
        
        public void setClientID(ClientID id)
        {
            this.clientId = id;
        }
        
        public ClientID getClientID()
        {
            return this.clientId;
        }
    }
    
    private static class TestChatHandler implements IServerHandler<TestChatSession>
    {
        // never cleaned up: client ID stays constant for retry attempts.
        private TreeMap<ClientID, TestChatSession> clients;
        
        public TestChatHandler()
        {
            this.clients = new TreeMap<ClientID, TestChatSession>();
        }
        
        @Override
        public void onClose(IServerConnection<TestChatSession> connection)
        {
            println("Client connection closed.");
        }

        @Override
        public void onConnect(IServerConnection<TestChatSession> connection)
        {
            println("Client connection opened.");
            connection.setAttachment(new TestChatSession(connection));
        }

        @Override
        public void onPacket(IServerConnection<TestChatSession> connection,
                ISendable packet) throws IOException
        {
            switch (packet.getPacketType())
            {
            case CLIENT_CONNECT:
                clientConnect(connection, packet);
                break;
            case CLIENT_RECONNECT:
                clientReconnect(connection, packet);
                break;
            case SEND_MESSAGE:
                sendMessage(connection, packet);
                break;
            default:
                println("Unrecognized packet.");
            }
        }

        private void sendMessage(IServerConnection<TestChatSession> connection,
                ISendable packet)
        {
            // TODO Auto-generated method stub
            
        }

        private void clientReconnect(
                IServerConnection<TestChatSession> connection, ISendable packet)
        {
            // TODO Auto-generated method stub
            
        }

        private void clientConnect(
                IServerConnection<TestChatSession> connection, ISendable packet)
        {
            // TODO `Auto-generated method stub
            
        }
    }
}
