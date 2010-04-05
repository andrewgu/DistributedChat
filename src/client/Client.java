package client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.concurrent.LinkedBlockingQueue;

import protocol.ClientConnection;
import protocol.IClientHandler;
import protocol.ISendable;
import protocol.PacketType;
import protocol.data.ClientID;
import protocol.data.MessageID;
import protocol.data.ServerID;
import protocol.data.ServerPriorityListing;
import protocol.packets.ClientConnect;
import protocol.packets.ClientReconnect;
import protocol.packets.ConnectAck;
import protocol.packets.FindRoom;
import protocol.packets.MessageData;
import protocol.packets.RoomFound;
import protocol.packets.SendAck;
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
	private int sendTimeout;
	
	private int maxReconnectRetries;
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
	private Map<MessageID, ClientMessage> messages;
	// List of messages not yet acknowledged.
	private Map<Integer, SendMessage> sendList;
	private Queue<SendMessage> delayedSends;
	// Handler for chat client events.
	private IChatClientHandler handler;
	// Counts up from 0 for subsequent message sent by the client.
	private int sendCounter;
	
	public Client(String authHost, int authPort, String room, String alias, IChatClientHandler handler)
	{
		this(authHost, authPort, room, alias, 2, 20000, 4, 3000, handler);
	}
	
	public Client(String authHost, int authPort, String room, String alias, 
			int sendRetries, int sendTimeout,
			int reconnectRetries, int connectTimeout,
			IChatClientHandler handler)
	{
		this.state = State.DISCONNECTED;
		
		this.authHost = authHost;
		this.authPort = authPort;
		this.room = room;
		this.alias = alias;
		this.maxSendRetries = sendRetries;
		this.sendTimeout = sendTimeout;
		this.maxReconnectRetries = reconnectRetries;
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
		this.messages = new TreeMap<MessageID, ClientMessage>();
		this.sendList = new TreeMap<Integer, SendMessage>();
		this.delayedSends = new LinkedBlockingQueue<SendMessage>();
		this.handler = handler;
		this.sendCounter = 0;
	}
	
	public synchronized Iterator<ClientMessage> messageIterator()
	{
		return this.messages.values().iterator();
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
			SendMessage msg = new SendMessage(room, alias, clientID, new MessageID(clientID, getNextMessageNumber()), message, getCurrentTimestamp());
			try
			{
				sendMessage(msg, this.maxSendRetries);
			}
			catch (IOException e)
			{
				this.delayedSends.add(msg);
				connectionError();
			}
		}
		else if (state == State.RECONNECTING)
		{
			SendMessage msg = new SendMessage(room, alias, clientID, new MessageID(clientID, getNextMessageNumber()), message, getCurrentTimestamp());
			this.delayedSends.add(msg);
		}
		else
		{
			throw new ClientStateException("Can only connect to room when disconnected.");
		}
	}

	private void sendMessage(SendMessage msg, int triesLeft) throws IOException
	{
		connection.sendPacket(msg);
		this.sendList.put(msg.getMessageID().getMessageNumber(), msg);
		this.timeoutTimer.schedule(new SendTimeout(triesLeft-1, this, msg.getMessageID().getMessageNumber()), this.sendTimeout);
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
		if (this.state != State.AUTHENTICATING)
			System.err.println("Invalid internal state: authenticationSuccess should only be called in AUTHENTICATING state.");
		
		authHandler.cancel();
		authConnection.close();
		authHandler = null;
		authConnection = null;
		
		if (!packet.getServerData().getRoom().equals(room))
			authenticationError();
		
		clientID = packet.getClientID();
		setFallbackServers(packet.getServerData());
		
		tryConnectLoop(this.maxReconnectRetries);
	}
	
	private void tryConnectLoop(int triesLeft)
	{		
		this.state = State.CONNECTING;
		
		if (!this.fallbacks.isEmpty() && triesLeft > 0)
		{
			// Send packet
			ConnectTimeout timeout = null;
			try
			{
				ServerPriorityListing listing = this.fallbacks.remove();
				this.connHandler = new ConnectionHandler(this);
				this.connection = new ClientConnection(listing.getAddress().getHostAddress(), listing.getAddress().getPort(), this.connHandler);
				
				timeout = new ConnectTimeout(triesLeft-1, this);
				this.connHandler.setConnectTimeoutHandler(timeout);
				this.connection.sendPacket(new ClientConnect(this.clientID, this.room));
				this.timeoutTimer.schedule(timeout, this.connectTimeout);
			}
			catch (IOException e)
			{
				connectAttemptFailed(triesLeft-1, timeout);
			}
		}
		else
		{
			connectFailed();
		}
	}
	
	private void connectAttemptFailed(int triesLeft, ConnectTimeout timeout)
	{
		if (timeout != null)
			timeout.cancel();
		if (this.connHandler != null)
			this.connHandler.cancel();
		if (this.connection != null)
			this.connection.close();
		
		this.connection = null;
		this.connHandler = null;
		
		tryConnectLoop(triesLeft);
	}
	
	private void connectAttemptSuccess(ConnectAck packet)
	{
		this.currentServer = packet.getServers().getSender();
		setFallbackServers(packet.getServers());
	
		this.state = State.CONNECTED;
		
		this.lastAcked = this.lastReceived = getCurrentTimestamp();
		
		synchronized(this.handler)
		{
			this.handler.onConnected(this);
		}
	}
	
	private void tryReconnectLoop(int triesLeft)
	{
		this.state = State.RECONNECTING;
		
		if (!this.fallbacks.isEmpty() && triesLeft > 0)
		{
			// Send packet
			ReconnectTimeout timeout = null;
			try
			{
				ServerPriorityListing listing = this.fallbacks.remove();
				this.connHandler = new ConnectionHandler(this);
				this.connection = new ClientConnection(listing.getAddress().getHostAddress(), listing.getAddress().getPort(), this.connHandler);
				
				timeout = new ReconnectTimeout(triesLeft-1, this);
				this.connHandler.setReconnectTimeoutHandler(timeout);
				this.connection.sendPacket(new ClientReconnect(this.clientID, this.room, this.lastAcked, this.lastReceived));
				this.timeoutTimer.schedule(timeout, this.connectTimeout);
			}
			catch (IOException e)
			{
				reconnectAttemptFailed(triesLeft-1, timeout);
			}
		}
		else
		{
			reconnectFailed();
		}
	}
	
	private void reconnectAttemptFailed(int triesLeft, ReconnectTimeout timeout)
	{
		if (timeout != null)
			timeout.cancel();
		if (this.connHandler != null)
			this.connHandler.cancel();
		if (this.connection != null)
			this.connection.close();
		
		this.connection = null;
		this.connHandler = null;
		
		tryReconnectLoop(triesLeft);
	}
	
	private void reconnectAttemptSuccess(ConnectAck packet)
	{	
		this.currentServer = packet.getServers().getSender();
		setFallbackServers(packet.getServers());
	
		this.state = State.CONNECTED;
		
		synchronized(this.handler)
		{
			this.handler.onReconnected(this);
		}
		
		try
		{
			clearDelaySendQueue();
		}
		catch (IOException e)
		{
			connectionError();
		}
	}
	
	private void connectFailed()
	{
		synchronized(this.handler)
		{
			this.handler.onConnectFailed(this);
		}
		
		disconnect();
	}
	
	private void reconnectFailed()
	{
		synchronized(this.handler)
		{
			this.handler.onReconnectFailed(this);
		}
		
		disconnect();
	}
	
	private void connectionError()
	{
		if (this.state != State.CONNECTED)
			System.err.println("Internal state error: should not call connectionError except when in CONNECTED state.");
		
		this.connHandler.cancel();
		this.connection.close();
		this.connHandler = null;
		this.connection = null;
		this.currentServer = null;
		
		tryReconnectLoop(this.maxReconnectRetries);
	}
	
	
	private void clearDelaySendQueue() throws IOException
	{
		while (!this.delayedSends.isEmpty())
		{
			SendMessage msg = this.delayedSends.peek();
			sendMessage(msg, this.maxSendRetries);
			this.delayedSends.remove();
		}
	}
	
	// Fired when no more retries
	private void sendFailed(int messageNumber)
	{
		connectionError();
		
		synchronized(this.handler)
		{
			SendMessage msg = this.sendList.get(new Integer(messageNumber));
			this.handler.onSendFailed(this, new ClientMessage(msg));
		}
	}
	
	private void sendAcknowledged(SendAck packet)
	{
		Integer messageNumber = new Integer(packet.getMessageID().getMessageNumber());
		if (this.sendList.containsKey(messageNumber))
		{
			SendMessage msg = this.sendList.get(messageNumber);
			this.sendList.remove(messageNumber);
			
			ClientMessage cmsg = new ClientMessage(msg);
			this.messages.put(msg.getMessageID(), cmsg);
			
			synchronized(this.handler)
			{
				this.handler.onSendAcknowledged(this, cmsg);
			}
		}
	}

	// Fired
	private void receivedMessage(MessageData md)
	{
		ClientMessage message = new ClientMessage(md);
		
		if (this.messages.get(md.getMessageID()) == null)
		{
			this.messages.put(md.getMessageID(), message);
			synchronized(this.handler)
			{
				this.handler.onMessageReceived(this, message);
			}
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
		
		private ConnectTimeout connectTimeout;
		private ReconnectTimeout reconnectTimeout;

		public ConnectionHandler(Client parent)
		{
			this.parent = parent;
			this.cancel = false;
			this.connectTimeout = null;
			this.reconnectTimeout = null;
		}

		private synchronized void setConnectTimeoutHandler(ConnectTimeout connectTimeout)
		{
			this.connectTimeout = connectTimeout;
		}
		
		private synchronized void setReconnectTimeoutHandler(ReconnectTimeout reconnectTimeout)
		{
			this.reconnectTimeout = reconnectTimeout;
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
				
				switch (packet.getPacketType())
				{
				case CONNECT_ACK:
					if (connectTimeout != null && reconnectTimeout == null)
					{
						connectTimeout.cancel();
						connectTimeout = null;
						connectAttemptSuccess((ConnectAck)packet);
					}
					else if (reconnectTimeout != null && connectTimeout == null)
					{
						reconnectTimeout.cancel();
						reconnectTimeout = null;
						reconnectAttemptSuccess((ConnectAck)packet);
					}
					else
					{
						System.err.println("Error... can't tell if it's a connect or reconnect ACK.");
					}
					break;
				case SEND_ACK:
					sendAcknowledged((SendAck)packet);
					break;
				case MESSAGE_DATA:
					receivedMessage((MessageData)packet);
					break;
				case SERVER_UPDATE:
					setFallbackServers((ServerUpdate)packet);
					break;
				default:
					break;
				}
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
		private boolean cancel;
		private int triesLeft;
		private Client parent;

		public ConnectTimeout(int triesLeft, Client parent)
		{
			this.triesLeft = triesLeft;
			this.parent = parent;
			this.cancel = false;
		}
		
		@Override
		public boolean cancel()
		{	
			synchronized(parent)
			{
				this.cancel = true;
				return super.cancel();
			}
		}

		@Override
		public void run()
		{
			synchronized(parent)
			{
				if (cancel) return;
				connectAttemptFailed(triesLeft, this);
			}
		}
	}
	
	private class ReconnectTimeout extends TimerTask
	{
		private boolean cancel;
		private int triesLeft;
		private Client parent;
		
		public ReconnectTimeout(int triesLeft, Client parent)
		{
			this.triesLeft = triesLeft;
			this.parent = parent;
			this.cancel = false;
		}
		
		@Override
		public boolean cancel()
		{
			synchronized(parent)
			{
				this.cancel = true;
				return super.cancel();
			}
		}

		@Override
		public void run()
		{
			synchronized(parent)
			{
				if (cancel) return;
				reconnectAttemptFailed(triesLeft, this);
			}
		}
	}
	
	private class SendTimeout extends TimerTask
	{
		private boolean cancel;
		private int triesLeft;
		private Client parent;
		private int messageNumber;
		
		public SendTimeout(int triesLeft, Client parent, int messageNumber)
		{
			this.triesLeft = triesLeft;
			this.parent = parent;
			this.cancel = false;
			this.messageNumber = messageNumber;
		}

		@Override
		public boolean cancel()
		{
			synchronized(parent)
			{
				this.cancel = true;
				return super.cancel();
			}
		}

		@Override
		public void run()
		{
			synchronized(parent)
			{
				if (cancel) return;
				
				this.cancel();
				SendMessage msg = sendList.get(messageNumber);
				if (triesLeft > 0 && msg != null)
				{
					try
					{
						sendMessage(msg, triesLeft);
					}
					catch (IOException e)
					{
						delayedSends.add(msg);
						connectionError();
					}
				}
				else if (msg != null)
				{
					sendFailed(messageNumber);
				}
			}
		}
	}
}
