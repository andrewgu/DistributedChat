package test;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import protocol.ClientConnection;
import protocol.IClientHandler;
import protocol.ISendable;
import protocol.PacketReader;
import protocol.PacketWriter;
import protocol.data.ClientID;
import protocol.data.ServerID;
import protocol.data.ServerPriorityListing;
import protocol.packets.FindRoom;
import protocol.packets.RoomFound;
import protocol.packets.ServerUpdate;

public class TestClientConnection
{

	/**
	 * @param args
	 * @throws IOException 
	 * @throws UnknownHostException 
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws UnknownHostException, IOException, InterruptedException
	{
		Thread testServerThread = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					ServerSocket ssock = new ServerSocket(9998);
					Socket sock = ssock.accept();
						
					DataInputStream dis = new DataInputStream(sock
							.getInputStream());
					DataOutputStream dos = new DataOutputStream(sock
							.getOutputStream());
					PacketReader reader = new PacketReader();
					PacketWriter writer = new PacketWriter();

					// Initialize readers and writers
					byte[] winit = writer.getSerializationHeader();
					dos.writeInt(winit.length);
					dos.write(winit, 0, winit.length);

					// Initialize readers and writers
					int len = dis.readInt();
					byte[] data = new byte[len];
					int read = 0;
					while (read < len)
						read += dis.read(data, read, len - read);
					reader.setSerializationHeader(data, 0, len);

					// Wait for a FIND_ROOM packet
					len = dis.readInt();
					data = new byte[len];
					read = 0;
					while (read < len)
						read += dis.read(data, read, len - read);
					reader.setBytes(data, 0, len);

					FindRoom packet = (FindRoom) reader.readObject();
					RoomFound response = new RoomFound(
							new ClientID(packet.getRoom(), 0), 
							new ServerUpdate(packet.getRoom(), 
								new ServerID(0, 0), 
								new ServerPriorityListing[0]), 
							packet.getReplyCode());

					// Got FIND_ROOM packet, respond with a ROOM_FOUND packet.
					byte[] responseBytes = writer.getSerializedData(response);
					dos.writeInt(responseBytes.length);
					dos.write(responseBytes, 0, responseBytes.length);

					sock.close();
					ssock.close();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		});
		
		testServerThread.start();
		Thread.sleep(500);
		
		// Opens the connection automatically, so the test server has to be ready.
		ClientConnection client = new ClientConnection(InetAddress.getByName("localhost"), 9998, new TestClientHandler());
		// Starts listening for packets.
		client.startReadLoop();
		
		client.sendPacket(new FindRoom("room!", client.getUniqueReplyCode()));
		
		client.close();
		
		//BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		//reader.readLine();
		Thread.sleep(1000);
		System.exit(0);
	}

	public static class TestClientHandler implements IClientHandler
	{

		@Override
		public void onConnectionClosed(ClientConnection caller)
		{
			System.out.println("Client: connection was closed.");
		}

		@Override
		public void onPacket(ClientConnection caller, ISendable packet)
		{
			System.out.println("Client: " + packet.getPacketType());
			if (packet instanceof RoomFound)
			{
				System.out.println("Client: is RoomFound!");
				RoomFound f = (RoomFound)packet;
				System.out.println("Client: " + f.getClientID().getRoom());
			}
		}

	}
}
