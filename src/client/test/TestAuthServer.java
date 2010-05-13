package client.test;

import java.io.IOException;
import java.net.UnknownHostException;

import protocol.ISendable;
import protocol.IServerConnection;
import protocol.IServerHandler;
import protocol.PacketType;
import protocol.ProtocolServer;
import protocol.data.ClientID;
import protocol.data.ServerID;
import protocol.packets.FindRoom;
import protocol.packets.RoomFound;

public class TestAuthServer
{
    public static final int PORT = 13000;
    public static final String ROOM = "test";
    
    private static int counter = 0;
    
    public static void main(String[] args) throws UnknownHostException, IOException
    {
        ProtocolServer<TestAuthSession> server = new ProtocolServer<TestAuthSession>(
                PORT, 1, new TestAuthHandler());
        server.start();
    }
    
    private static ClientID getNewClientID(String room)
    {
        counter++;
        return new ClientID(room, counter);
    }
    
    public static ServerID getServerID()
    {
        return new ServerID(0, 0);
    }
    
    private static void println(String message)
    {
        synchronized(System.out)
        {
            System.out.println("AUTH:\t" + message);
        }
    }
    
    private static class TestAuthSession
    {
    }
    
    private static class TestAuthHandler implements IServerHandler<TestAuthSession>
    {
        @Override
        public void onClose(IServerConnection<TestAuthSession> connection)
        {
            println("Client connection closed.");
        }

        @Override
        public void onConnect(IServerConnection<TestAuthSession> connection)
        {
            println("Client connected.");
        }

        @Override
        public void onPacket(IServerConnection<TestAuthSession> connection,
                ISendable packet) throws IOException
        {
            if (packet.getPacketType() == PacketType.FIND_ROOM)
            {
                FindRoom p = (FindRoom)packet;
                println("Received FIND_ROOM packet.");
                
                if (p.getRoom().equals(ROOM))
                {
                    connection.sendPacket(new RoomFound(getNewClientID(p.getRoom()), 
                            TestChatServer.getServerUpdate(), 
                            p.getReplyCode()));
                    println("Sent ROOM_FOUND packet.");
                }
                else
                {
                    println("Room not found.");
                }
            }
            else
            {
                println("Received unrecognized packet.");
            }
        }
    }
}
