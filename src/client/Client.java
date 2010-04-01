package client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayDeque;
import java.util.Calendar;
import java.util.PriorityQueue;

import protocol.ClientConnection;
import protocol.IClientHandler;
import protocol.ISendable;
import protocol.PacketType;
import protocol.data.ClientID;
import protocol.data.ServerID;
import protocol.data.ServerPriorityListing;
import protocol.packets.ClientConnect;
import protocol.packets.ClientReconnect;
import protocol.packets.ConnectAck;
import protocol.packets.FindRoom;
import protocol.packets.MessageData;
import protocol.packets.RoomFound;
import protocol.packets.SendAck;
import protocol.packets.ServerUpdate;

public class Client
{
	private InetAddress host;
	private int hostPort;
	private String room;
	private String alias;
	
	private ClientConnection auth;
	private ClientConnection server;
	
	private IClientListener listener;
	
	private ClientID clientID;
	private ServerID serverID;
	private PriorityQueue<ServerPriorityListing> serverListings;
	
	private long lastAcked;
	private long lastReceived;
	
	public Client(String authHost, int authPort, String room, String alias, IClientListener listener) throws UnknownHostException
	{
		this.host = InetAddress.getByName(authHost);
		this.hostPort = authPort;
		this.room = room;
		this.alias = alias;
		this.listener = listener;
		
		this.auth = null;
		this.server = null;
		
		this.clientID = null;
		this.serverID = null;
		this.serverListings = new PriorityQueue<ServerPriorityListing>();
	}

	public synchronized void connect() throws IOException
	{
		auth = new ClientConnection(host, hostPort, new AuthHandler(this));
		auth.sendPacket(new FindRoom(room));
	}
	
	public synchronized void send(String message) throws IOException
	{
		if (server == null)
			throw new IOException("Client hasn't connected to the room yet.");
	}
	
	public synchronized void disconnect()
	{
		if (this.auth != null)
		{
			this.auth.close();
			this.auth = null;
		}
		
		if (this.server != null)
		{
			this.server.close();
			this.server = null;
			this.serverID = null;
		}
	}
	
	private synchronized void connectToNextServer() throws IOException
	{
		if (serverListings.isEmpty())
			throw new IOException("No more available servers.");
		
		ServerPriorityListing listing = serverListings.remove();
		if (serverID == null)
		{
			server = new ClientConnection(listing.getAddress().getHostAddress(), listing.getAddress().getPort(), new ClientHandler(this));
			server.sendPacket(new ClientConnect(clientID, room));
		}
		else
		{
			server = new ClientConnection(listing.getAddress().getHostAddress(), listing.getAddress().getPort(), new ClientHandler(this));
			server.sendPacket(new ClientReconnect(clientID, room, lastAcked, lastReceived));
		}
		
		serverID = null;
	}
	
	private synchronized void connectAcknowledged(ConnectAck ack)
	{
		
	}
	
	private synchronized void sendAcknowledged(SendAck ack)
	{
		
	}
	
	private synchronized void receivedMessage(MessageData message)
	{
		
	}
	
	private synchronized void serverUpdate(ServerUpdate update)
	{
		ServerPriorityListing[] listings = update.getServers();
		serverListings.clear();
		for (ServerPriorityListing i : listings)
		{
			// Exclude current server from the priority queue listing.
			if (!serverID.equals(i.getId()))
				serverListings.add(i);
		}
	}
	
	private class AuthHandler implements IClientHandler
	{
		private Client parent;
		
		public AuthHandler(Client parent)
		{
			this.parent = parent;
		}
		
		@Override
		public void onConnectionClosed(ClientConnection caller)
		{
			synchronized(parent)
			{
				auth = null;
			}
		}

		@Override
		public void onPacket(ClientConnection caller, ISendable packet)
		{
			synchronized(parent)
			{
				if (packet.getPacketType() == PacketType.ROOM_FOUND)
				{
					RoomFound rf = (RoomFound)packet;
					clientID = rf.getClientID();
					
					serverUpdate(rf.getServerData());
					
					try
					{
						connectToNextServer();
					}
					catch (IOException e)
					{
						e.printStackTrace();
						disconnect();
					}
				}
				else
				{
					System.err.println("Error: invalid protocol packet: " + packet.getPacketType().toString());
				}
			}
		}
	}
	
	private class ClientHandler implements IClientHandler
	{
		private Client parent;
		
		public ClientHandler(Client parent)
		{
			this.parent = parent;
		}
		
		@Override
		public void onConnectionClosed(ClientConnection caller)
		{
			synchronized(parent)
			{
				disconnect();
			}
		}

		@Override
		public void onPacket(ClientConnection caller, ISendable packet)
		{
			synchronized(parent)
			{
				switch (packet.getPacketType())
				{
				case CONNECT_ACK:
					connectAcknowledged((ConnectAck)packet);
					break;
				case SEND_ACK:
					sendAcknowledged((SendAck)packet);
					break;
				case MESSAGE_DATA:
					receivedMessage((MessageData)packet);
					break;
				case SERVER_UPDATE:
					serverUpdate((ServerUpdate)packet);
					break;
				default:
					System.err.println("Error: invalid protocol packet: " + packet.getPacketType().toString());
				}
			}
		}
	}
	
	public static long getTimestamp()
	{
		return Calendar.getInstance().getTimeInMillis();
	}
}
