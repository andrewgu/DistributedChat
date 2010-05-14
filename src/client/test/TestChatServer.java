package client.test;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.TreeMap;

import protocol.ISendable;
import protocol.IServerConnection;
import protocol.IServerHandler;
import protocol.ProtocolServer;
import protocol.data.ClientID;
import protocol.data.ServerAddress;
import protocol.data.ServerID;
import protocol.data.ServerPriorityListing;
import protocol.packets.ClientConnect;
import protocol.packets.ClientReconnect;
import protocol.packets.ConnectAck;
import protocol.packets.CoreMessage;
import protocol.packets.MessageData;
import protocol.packets.SendAck;
import protocol.packets.SendMessage;
import protocol.packets.ServerUpdate;

public class TestChatServer
{
    public static final int PORT = 13001;
    public static final String ROOM = "test";
    
    public static final int FORCE_CONNECT_RETRY = 0;
    public static final int FORCE_RECONNECT_RETRY = 0;
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
        //private ClientID clientId;
        private int connectAttempts;
        private int reconnectAttempts;
        private int sendAttempts;

        public TestChatSession(IServerConnection<TestChatSession> connection)
        {
            this.connection = connection;
            //this.clientId = null;
            
            this.connectAttempts = 0;
            this.reconnectAttempts = 0;
            this.sendAttempts = 0;
        }
        
        public IServerConnection<TestChatSession> getConnection()
        {
            return this.connection;
        }
        
        public void setConnection(IServerConnection<TestChatSession> connection)
        {
            this.connection = connection;
        }
        
        public void setClientID(ClientID id)
        {
            //this.clientId = id;
        }
        
        /*public ClientID getClientID()
        {
            return this.clientId;
        }*/

        public int getConnectAttempts()
        {
            return connectAttempts;
        }

        public void setConnectAttempts(int connectAttempts)
        {
            this.connectAttempts = connectAttempts;
        }

        public int getReconnectAttempts()
        {
            return reconnectAttempts;
        }

        public void setReconnectAttempts(int reconnectAttempts)
        {
            this.reconnectAttempts = reconnectAttempts;
        }

        public int getSendAttempts()
        {
            return sendAttempts;
        }

        public void setSendAttempts(int sendAttempts)
        {
            this.sendAttempts = sendAttempts;
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
            connection.getAttachment().setConnection(null);
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
                clientConnect(connection, (ClientConnect)packet);
                break;
            case CLIENT_RECONNECT:
                clientReconnect(connection, (ClientReconnect)packet);
                break;
            case SEND_MESSAGE:
                sendMessage(connection, (SendMessage)packet);
                break;
            default:
                println("Unrecognized packet.");
            }
        }

        private void sendMessage(IServerConnection<TestChatSession> connection,
                SendMessage packet) throws IOException
        {
            println("SEND request");
            //ClientID id = packet.getClientID();
            TestChatSession session = connection.getAttachment();
            
            if (session.getSendAttempts() < FORCE_SEND_RETRY)
            {
                println("Forced ignore send.");
                session.setSendAttempts(session.getSendAttempts()+1);
            }
            else
            {
                println("Executing send.");
                // reset
                session.setSendAttempts(0);
                
                for (TestChatSession s : clients.values())
                {
                    IServerConnection<TestChatSession> c = s.getConnection();
                    if (c != null)
                    {
                        c.sendPacket(new MessageData(getServerUpdate(), new CoreMessage(packet)));
                    }
                }
                
                connection.sendPacket( new SendAck(
                        getServerUpdate(), getTimestamp(), packet.getMessageID(), packet.getReplyCode()));
            }
        }

        private void clientReconnect(
                IServerConnection<TestChatSession> connection, ClientReconnect packet) throws IOException
        {
            println("Client RECONNECT request...");
            ClientID id = packet.getClient();
            TestChatSession session;
            
            if (clients.containsKey(id))
            {
                session = clients.get(id);
                session.setConnection(connection);
                connection.setAttachment(session);
            }
            else
            {
                session = connection.getAttachment();
                session.setClientID(id);
                clients.put(id, session);
            }
            
            if (session.getReconnectAttempts() < FORCE_RECONNECT_RETRY)
            {
                println("Force close.");
                session.setReconnectAttempts(session.getReconnectAttempts()+1);
                connection.close();
            }
            else
            {
                println("Accepted connect request.");
                // reset
                session.setReconnectAttempts(0);
                connection.sendPacket(new ConnectAck(getServerUpdate(), getTimestamp(), packet.getReplyCode()));
            }
        }

        private void clientConnect(
                IServerConnection<TestChatSession> connection, ClientConnect packet) throws IOException
        {
            println("Client connect request...");
            ClientID id = packet.getClient();
            TestChatSession session;
            
            if (clients.containsKey(id))
            {
                session = clients.get(id);
                session.setConnection(connection);
                connection.setAttachment(session);
            }
            else
            {
                session = connection.getAttachment();
                session.setClientID(id);
                clients.put(id, session);
            }
            
            if (session.getConnectAttempts() < FORCE_CONNECT_RETRY)
            {
                println("Force close.");
                session.setConnectAttempts(session.getConnectAttempts()+1);
                connection.close();
            }
            else
            {
                println("Accepted connect request.");
                // reset
                session.setConnectAttempts(0);
                connection.sendPacket(new ConnectAck(getServerUpdate(), getTimestamp(), packet.getReplyCode()));
            }
        }
    }
    
    public static long getTimestamp()
    {
        return Calendar.getInstance().getTimeInMillis();
    }
    
    public static ServerUpdate getServerUpdate()
    {
        return new ServerUpdate(ROOM, getServerID(), 
                new ServerPriorityListing[] { new ServerPriorityListing(0, TestChatServer.getServerID(), 
                        new ServerAddress("localhost", TestChatServer.PORT))});
    }
}
