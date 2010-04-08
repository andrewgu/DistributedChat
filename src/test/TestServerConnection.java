package test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import protocol.ClientConnection;
import protocol.ISendable;
import protocol.IServerConnection;
import protocol.IServerHandler;
import protocol.PacketType;
import protocol.ProtocolServer;
import protocol.data.ClientID;
import protocol.data.ServerID;
import protocol.data.ServerPriorityListing;
import protocol.packets.FindRoom;
import protocol.packets.RoomFound;
import protocol.packets.ServerUpdate;
import test.TestClientConnection.TestClientHandler;

public class TestServerConnection
{
	public static void main(String[] args) throws UnknownHostException, IOException, InterruptedException
	{
		ProtocolServer<ServerState> server = new ProtocolServer<ServerState>(9998, 1, new ServerHandler());
		server.start();
		Thread.sleep(1000);
		
		// Opens the connection automatically, so the test server has to be ready.
		ClientConnection client = new ClientConnection(InetAddress.getByName("localhost"), 9998, new TestClientHandler());
		// Starts listening for packets.
		client.startReadLoop();
		client.sendPacket(new FindRoom("room!", client.getUnusedReplyCode()));
		//Thread.sleep(1000);
		//client.close();
	
		//Thread.sleep(1000);
		//server.stop();
	}
	
	public static class ServerState
	{
		// Intentionally nothing to actually store.
	}
	
	private static class ServerHandler implements IServerHandler<ServerState>
	{

		@Override
		public void onClose(IServerConnection<ServerState> connection)
		{
			System.out.println("Server: Connection closed.");
		}

		@Override
		public void onConnect(IServerConnection<ServerState> connection)
		{
			System.out.println("Server: Connection opened.");
			connection.setAttachment(new ServerState());
		}

		@Override
		public void onPacket(IServerConnection<ServerState> connection,
				ISendable packet)
		{
			if (packet.getPacketType() == PacketType.FIND_ROOM)
			{
				System.out.println("Server: FIND_ROOM:");
				FindRoom fr = (FindRoom)packet;
				System.out.println("Server: " + fr.getRoom());
				
				RoomFound response = new RoomFound(
						new ClientID(fr.getRoom(), 0), 
						new ServerUpdate(fr.getRoom(),
							new ServerID(0, 0), 
							new ServerPriorityListing[0]),
						fr.getReplyCode());
				
				try
				{
					connection.sendPacket(response);
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
				
				connection.close();
			}
			else
			{
				System.out.println("Unrecognized packet.");
			}
		}
	}
}
