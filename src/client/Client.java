package client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.SortedSet;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.LinkedBlockingQueue;

import protocol.ClientConnection;
import protocol.IClientHandler;
import protocol.ISendable;
import protocol.PacketType;
import protocol.data.ClientID;
import protocol.data.MessageID;
import protocol.data.ServerID;
import protocol.data.ServerPriorityListing;
import protocol.packets.FindRoom;
import protocol.packets.RoomFound;
import protocol.packets.SendMessage;
import protocol.packets.ServerUpdate;

public class Client
{
	public enum State
	{
		DISCONNECTED,
		AUTHENTICATING,
		CONNECTING,
		RECONNECTING,
		CONNECTED
	}
	
	//Current state of the client.
	private State state;
	
	private String authHost;
	private int authPort;
	private String room;
	private String alias;
	
	private int maxSendRetries;
	private int resendDelay;
	private int sendTimeout;
	
	private int maxReconnectRetries;
	private int reconnectDelay;
	private int connectTimeout;
	
	// Last timestamp when a message was received.
	private long lastReceived;
	// Last timestamp when a message was acknowledged.
	private long lastAcked;
	
	// Used for implementing the send and connect timeouts.
	private Timer timeoutTimer;
	// Current connection for authentication, alive only when authenticating
	private ClientConnection authConnection;
	private AuthenticationHandler authHandler;
	// Current connection
	private ClientConnection connection;
	private ConnectionHandler connHandler;
	// ClientID given by authentication
	private ClientID clientID;
	// ID of curent connected server
	private ServerID currentServer;
	// List of fallbacks, ordered from most desirable fallback to least desirable.
	private Queue<ServerPriorityListing> fallbacks;
	// List of messages received by the client.
	private SortedSet<ClientMessage> messages;
	// List of messages not yet acknowledged.
	private Map<Integer, SendMessage> sendList;
	private Queue<SendMessage> delayedSends;
	// Handler for chat client events.
	private IChatClientHandler handler;
	// Counts up from 0 for subsequent message sent by the client.
	private int sendCounter;
	
	public Client(String authHost, int authPort, String room, String alias, IChatClientHandler handler)
	{
		this(authHost, authPort, room, alias, 2, 500, 20000, 4, 500, 3000, handler);
	}
	
	public Client(String authHost, int authPort, String room, String alias, 
			int sendRetries, int resendDelay, int sendTimeout,
			int reconnectRetries, int reconnectDelay, int connectTimeout,
			IChatClientHandler handler)
	{
		this.state = State.DISCONNECTED;
		
		this.authHost = authHost;
		this.authPort = authPort;
		this.room = room;
		this.alias = alias;
		this.maxSendRetries = sendRetries;
		this.resendDelay = resendDelay;
		this.sendTimeout = sendTimeout;
		this.maxReconnectRetries = reconnectRetries;
		this.reconnectDelay = reconnectDelay;
		this.connectTimeout = connectTimeout;
		
		this.lastAcked = this.lastReceived = getCurrentTimestamp();
		this.timeoutTimer = new Timer();
		this.authConnection = null;
		this.authHandler = null;
		this.connection = null;
		this.connHandler = null;
		this.clientID = null;
		this.currentServer = null;
		this.fallbacks = new PriorityQueue<ServerPriorityListing>();
		this.messages = new TreeSet<ClientMessage>(ClientMessage.TIMESTAMP_COMPARATOR);
		this.sendList = new TreeMap<Integer, SendMessage>();
		this.delayedSends = new LinkedBlockingQueue<SendMessage>();
		this.handler = handler;
		this.sendCounter = 0;
	}
	
	public synchronized void connect() throws ClientStateException, UnknownHostException, IOException
	{
		if (state != State.DISCONNECTED)
			throw new ClientStateException("Can only connect to room when disconnected.");
		
		try
		{
			this.authHandler = new AuthenticationHandler(this);
			this.authConnection = new ClientConnection(InetAddress.getByName(authHost), authPort, authHandler);
			this.authConnection.sendPacket(new FindRoom(this.room));
			this.timeoutTimer.schedule(authHandler, this.connectTimeout);
		}
		catch (UnknownHostException e)
		{
			this.authHandler.cancel();
			this.authHandler = null;
			throw e;
		}
		catch (IOException e)
		{
			this.authHandler.cancel();
			this.authHandler = null;
			this.authConnection = null;
			
			synchronized(handler)
			{
				handler.onConnectFailed(this);
			}
			
			throw e;
		}
		
		state = State.AUTHENTICATING;
	}
	
	public synchronized void disconnect()
	{
		switch (state)
		{
		case CONNECTED:
		case RECONNECTING:
		case CONNECTING:
			this.connHandler.cancel();
			this.connection.close();
		case AUTHENTICATING:
			if (this.authConnection != null)
			{
				this.authHandler.cancel();
				this.authConnection.close();
			}
		case DISCONNECTED:
			this.lastAcked = this.lastReceived = getCurrentTimestamp();
			this.timeoutTimer.cancel();
			this.timeoutTimer = new Timer();
			this.authConnection = null;
			this.authHandler = null;
			this.connection = null;
			this.connHandler = null;
			this.clientID = null;
			this.currentServer = null;
			this.fallbacks.clear();
			this.messages.clear();
			this.sendList.clear();
			this.delayedSends.clear();
			this.sendCounter = 0;
		}
		
		state = State.DISCONNECTED;
		
		synchronized (handler)
		{
			handler.onDisconnected(this);
		}
	}
	
	public synchronized void send(String message) throws ClientStateException
	{
		if (state == State.CONNECTED)
		{
			SendMessage msg = new SendMessage(room, alias, clientID, new MessageID(clientID, getNextMessageNumber()), message);
			try
			{
				sendMessage(msg);
			}
			catch (IOException e)
			{
				connectionError();
				this.delayedSends.add(msg);
			}
		}
		else if (state == State.RECONNECTING)
		{
			SendMessage msg = new SendMessage(room, alias, clientID, new MessageID(clientID, getNextMessageNumber()), message);
			this.delayedSends.add(msg);
		}
		else
		{
			throw new ClientStateException("Can only connect to room when disconnected.");
		}
	}

	private void sendMessage(SendMessage msg) throws IOException
	{
		connection.sendPacket(msg);
		this.sendList.put(msg.getMessageID().getMessageNumber(), msg);
		this.timeoutTimer.schedule(new SendTimeout(this.maxSendRetries-1, this), this.sendTimeout);
	}

	private int getNextMessageNumber()
	{
		int ret = this.sendCounter;
		this.sendCounter++;
		return ret;
	}

	private static long getCurrentTimestamp()
	{
		return Calendar.getInstance().getTimeInMillis();
	}
	
	private void authenticationError()
	{
		synchronized (handler)
		{
			handler.onConnectFailed(this);
		}
		disconnect();
	}
	
	private void authenticationSuccess(RoomFound packet)
	{
		authHandler.cancel();
		authConnection.close();
		authHandler = null;
		authConnection = null;
		
		if (!packet.getServerData().getRoom().equals(room))
			authenticationError();
		
		clientID = packet.getClientID();
		setFallbackServers(packet.getServerData());
		
		this.state = State.CONNECTING;
		tryConnectLoop();
	}
	
	private void tryConnectLoop()
	{
		// TODO: Implement.
	}
	
	private void tryReconnectLoop()
	{
		// TODO: Implement.
	}
	
	private void connectionError()
	{
		// TODO Auto-generated method stub
		
	}
	
	private void clearDelaySendQueue() throws IOException
	{
		while (!this.delayedSends.isEmpty())
		{
			SendMessage msg = this.delayedSends.peek();
			sendMessage(msg);
			this.delayedSends.remove();
		}
	}

	private void setFallbackServers(ServerUpdate serverData)
	{
		this.fallbacks.clear();
		for (ServerPriorityListing listing : serverData.getServers())
		{
			if (this.currentServer == null || !this.currentServer.equals(listing.getId()))
				this.fallbacks.add(listing);
		}
	}

	private class AuthenticationHandler extends TimerTask implements IClientHandler
	{
		private boolean cancel;
		private Client parent;
		
		public AuthenticationHandler(Client parent)
		{
			this.parent = parent;
			this.cancel = false;
		}
		
		@Override
		public void onConnectionClosed(ClientConnection caller)
		{
			synchronized(parent)
			{
				if (cancel) return;
				authenticationError();
			}
		}

		@Override
		public void onPacket(ClientConnection caller, ISendable packet)
		{
			synchronized(parent)
			{
				if (cancel) return;
				
				if (packet.getPacketType() == PacketType.ROOM_FOUND)
				{
					this.cancel();
					authenticationSuccess((RoomFound)packet);
				}
				else
				{
					System.err.println("Detected server error: Authentication server responded with packet that isn't ROOM_FOUND.");
					authenticationError();
				}
			}
		}

		@Override
		public boolean cancel()
		{
			synchronized(parent)
			{
				cancel = true;
				return super.cancel();
			}
		}

		// For handling a timeout.
		@Override
		public void run()
		{
			synchronized(parent)
			{
				authenticationError();
			}
		}
	}
	
	private class ConnectionHandler implements IClientHandler
	{
		private boolean cancel;
		private Client parent;
				
		public ConnectionHandler(Client parent)
		{
			this.parent = parent;
			this.cancel = false;
		}

		@Override
		public synchronized void onConnectionClosed(ClientConnection caller)
		{
			synchronized(parent)
			{
				if (cancel) return;
			}
		}

		@Override
		public synchronized void onPacket(ClientConnection caller, ISendable packet)
		{
			synchronized(parent)
			{
				if (cancel) return;
			}
		}
		
		public synchronized void cancel()
		{
			synchronized(parent)
			{
				cancel = true;
			}
		}
	}
	
	private class ConnectTimeout extends TimerTask
	{
		private int triesLeft;
		private Client parent;

		public ConnectTimeout(int triesLeft, Client parent)
		{
			this.triesLeft = triesLeft;
			this.parent = parent;
		}
		
		@Override
		public boolean cancel()
		{	
			synchronized(parent)
			{
				// TODO Auto-generated method stub
				return super.cancel();
			}
		}

		@Override
		public void run()
		{
			synchronized(parent)
			{
				// TODO Auto-generated method stub
			}
		}
	}
	
	private class ReconnectTimeout extends TimerTask
	{
		private int triesLeft;
		private Client parent;
		
		public ReconnectTimeout(int triesLeft, Client parent)
		{
			this.triesLeft = triesLeft;
			this.parent = parent;
		}
		
		@Override
		public boolean cancel()
		{
			synchronized(parent)
			{
				// TODO Auto-generated method stub
				return super.cancel();
			}
		}

		@Override
		public void run()
		{
			synchronized(parent)
			{
				// TODO Auto-generated method stub
			}
		}
	}
	
	private class SendTimeout extends TimerTask
	{
		private int triesLeft;
		private Client parent;
		
		public SendTimeout(int triesLeft, Client parent)
		{
			this.triesLeft = triesLeft;
			this.parent = parent;
		}

		@Override
		public boolean cancel()
		{
			synchronized(parent)
			{
				// TODO Auto-generated method stub
				return super.cancel();
			}
		}

		@Override
		public void run()
		{
			synchronized(parent)
			{
				// TODO Auto-generated method stub
			}
		}
	}
}
