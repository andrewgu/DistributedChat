package client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayDeque;
import java.util.Calendar;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.TreeMap;
import java.util.TreeSet;

import protocol.ClientConnection;
import protocol.IClientHandler;
import protocol.IReplyHandler;
import protocol.ISendable;
import protocol.ReplyPacket;
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
		DISCONNECTED(1),
		AUTHENTICATING(2),
		AUTHENTICATED(4),
		CONNECTING(8),
		RECONNECTING(16),
		CONNECTED(32),
		DROPPED(64);
		
		public int v;
		private State(int val)
		{
			this.v = val;
		}
	}
	
	private InetAddress authHost;
	private int authPort;
	private String room;
	private String alias;
	private int maxSendRetries;
	private int sendTimeout;
	private int maxReconnectRetries;
	private int connectTimeout;
	// Handler for chat client events.
	private IChatClientHandler handler;
	
	//Current state of the client.
	private State state;
	// Last timestamp when a message was received.
	private long lastReceived;
	// Last timestamp when a message was acknowledged.
	private long lastAcked;
	
	private ClientConnection authConnection;
	private ClientConnection chatConnection;
	
	// ClientID given by authentication
	private ClientID clientID;
	// ID of curent connected server
	private ServerID serverID;
	
	// List of fallbacks, ordered from most desirable fallback to least desirable.
	private PriorityQueue<ServerPriorityListing> fallbacks;
	
	private TreeSet<MessageID> receivedMessages;
	// List of messages not yet acknowledged.
	private TreeMap<Integer, SendMessage> sendList;
	// List of messages to send once connected.
	private Queue<SendMessage> delayedSends;
	
	public Client(String authHost, int authPort, String room, String alias, IChatClientHandler handler) throws UnknownHostException
	{
		this(authHost, authPort, room, alias, 2, 20000, 4, 3000, handler);
	}
	
	public Client(String authHost, int authPort, String room, String alias, 
			int sendRetries, int sendTimeout,
			int reconnectRetries, int connectTimeout,
			IChatClientHandler handler) throws UnknownHostException
	{
		this.state = State.DISCONNECTED;
		
		this.authHost = InetAddress.getByName(authHost);
		this.authPort = authPort;
		this.room = room;
		this.alias = alias;
		this.maxSendRetries = sendRetries;
		this.sendTimeout = sendTimeout;
		this.maxReconnectRetries = reconnectRetries;
		this.connectTimeout = connectTimeout;
		
		this.handler = handler;
		
		this.lastAcked = this.lastReceived = getCurrentTimestamp();
		
		this.authConnection = null;
		this.chatConnection = null;
		
		this.clientID = null;
		this.serverID = null;
		
		this.fallbacks = new PriorityQueue<ServerPriorityListing>(); 
		
		this.receivedMessages = new TreeSet<MessageID>();
		this.sendList = new TreeMap<Integer, SendMessage>();
		this.delayedSends = new ArrayDeque<SendMessage>();
	}
	
	public synchronized void connect() throws UnknownHostException, IOException
	{
		authenticate();
	}
	
	public synchronized void disconnect()
	{
		this.state = State.DISCONNECTED;
		this.lastReceived = -1;
		this.lastAcked = -1;
		
		if (this.authConnection != null)
		{
			this.authConnection.close();
			this.authConnection = null;
		}
		
		if (this.chatConnection != null)
		{
			this.chatConnection.close();
			this.chatConnection = null;
		}
		
		this.clientID = null;
		this.serverID = null;
		
		this.fallbacks.clear();
		this.receivedMessages.clear();
		this.sendList.clear();
		this.delayedSends.clear();
		
		synchronized(this.handler)
		{
			this.handler.onDisconnected(this);
		}
	}
	
	public synchronized void send(String message) throws IOException
	{
		//assertState(State.CONNECTED.v | State.RECONNECTING.v);
		
		if (this.state == State.CONNECTED)
		{
			trySend(message, this.maxSendRetries);
		}
		else if (this.state == State.RECONNECTING)
		{
			this.delayedSends.add(
				new SendMessage(this.room, this.alias,  
					new MessageID(this.clientID, getNextMessageNumber()), message, getCurrentTimestamp()));
		}
		else
		{
			throw new IOException("Can only send messages while connected to chatroom.");
		}
	}

	private void receivedMessage(MessageData message)
	{
		if (!this.receivedMessages.contains(message.getMessageID()))
		{
			this.lastReceived = message.getTimestamp();
			this.receivedMessages.add(message.getMessageID());
			synchronized(this.handler)
			{
				this.handler.onMessageReceived(this, message);
			}
		}
	}
	
	private void receivedServerUpdate(ServerUpdate update)
	{
		updateServerList(update);
	}
	
	private void authenticate() throws UnknownHostException, IOException
	{
		assertState(State.DISCONNECTED);
		this.state = State.AUTHENTICATING;
		
		try
		{
			this.authConnection = new ClientConnection(this.authHost, this.authPort, new AuthenticationHandler(this));
			this.authConnection.startReadLoop();
			
			this.authConnection.sendReplyable(new FindRoom(this.room, this.authConnection.getUniqueReplyCode()), 
					new AuthReplyHandler(this), this.connectTimeout);
		}
		catch (IOException e)
		{
			// No need to close stuff, authConnection closes itself on the IOException.
			this.authConnection = null;
			throw e;
		}
	}
	
	private void tryConnect(int triesLeft)
	{
		assertState(State.AUTHENTICATED);
		this.state = State.CONNECTING;
		
		ServerPriorityListing listing = getNextListing();
		System.err.println("Attempting to connect to " + listing.getAddress().getHost());
		if (triesLeft > 0 && listing != null )
		{	
			try
			{
				this.chatConnection = new ClientConnection(listing.getAddress().getHostAddress(), listing.getAddress().getPort(), 
						new ConnectHandler(this));
				this.chatConnection.startReadLoop();
				
				this.chatConnection.sendReplyable(new ClientConnect(this.clientID, this.room, this.chatConnection.getUniqueReplyCode()), 
						new ConnectReplyHandler(this, triesLeft-1), this.connectTimeout);
			}
			catch (IOException e)
			{
				e.printStackTrace();
				// connection closed automatically or never opened in the first place.
				//connectRejected(triesLeft - 1);
			}
		}
		else
		{
			this.chatConnection = null;
			this.clientID = null;
			
			// Failed.
			synchronized(this.handler)
			{
				this.handler.onDropped(this);
			}
			
			this.disconnect();
		}
	}
	
	private void connectTimedOut(int triesLeft)
	{
		assertState(State.CONNECTING);
		// Indirectly calls connectRejected.
		this.chatConnection.close();
	}
	
	private void connectRejected(int triesLeft)
	{
		// Only relevant when trying to connect.
		assertState(State.CONNECTING);
		
		this.chatConnection = null;
		this.state = State.AUTHENTICATED;
		
		synchronized(this.handler)
		{
			this.handler.onConnectFailed(this);
		}
		
		tryConnect(triesLeft);
	}
	
	private void connectSuccess(ConnectAck ack)
	{
		assertState(State.CONNECTING);	
		
		this.serverID = ack.getServers().getSender();
		this.lastAcked = this.lastReceived = ack.getTimestamp();
		updateServerList(ack.getServers());
		
		this.state = State.CONNECTED;
		
		synchronized(this.handler)
		{
			this.handler.onConnected(this);
		}
	}
	
	private void connectDropped()
	{
		assertState(State.CONNECTED);
		// Happens if connection gets dropped while in active chat state.
		
		// Store failed sends for reconnect.
		this.chatConnection = null;
		for (SendMessage msg : this.sendList.values())
			this.delayedSends.add(msg);
		this.sendList.clear();
		
		this.state = State.DROPPED;
		
		synchronized(this.handler)
		{
			this.handler.onCurrentServerDropped(this);
		}
		
		this.tryReconnect(this.maxReconnectRetries);
	}
	
	private void connectClosedUnknownState()
	{
	    System.err.println("Connection was dropped in an unsupported state: not CONNECTING, CONNECTED, or DISCONNECTED:");
		System.err.println(this.state);
		this.disconnect();
	}
	
	private void tryClearSendBacklog()
	{
		assertState(State.CONNECTED);
		
		try
		{
			while (!this.delayedSends.isEmpty())
			{
				SendMessage msg = this.delayedSends.peek();
				this.trySend(msg, this.maxSendRetries);
				this.delayedSends.remove();
			}
		}
		catch (IOException e)
		{
			System.err.println("Failed to resend backlog.");
		}
	}
	
	private void trySend(String message, int triesLeft) throws IOException
	{
		this.trySend(
			new SendMessage(this.room, this.alias, 
				new MessageID(this.clientID, getNextMessageNumber()), message, getCurrentTimestamp()), triesLeft);
	}
	
	private void trySend(SendMessage msg, int triesLeft) throws IOException
	{
		assertState(State.CONNECTED);
		
		if (triesLeft > 0)
		{
			msg.setReplyCode(this.chatConnection.getUniqueReplyCode());
			this.sendList.put(msg.getMessageID().getMessageNumber(), msg);

			try
			{
				this.chatConnection.sendReplyable(msg, new SendReplyHandler(this, triesLeft-1, msg), this.sendTimeout);
				synchronized(this.handler)
				{
					this.handler.onSendAttempted(this, msg);
				}
			}
			catch (IOException e)
			{
				throw e;
			}
		}
		else
		{
			// Send failed.
			synchronized(this.handler)
			{
				this.handler.onSendFailed(this, msg);
			}
			
			//this.chatConnection.close();
		}
	}
	
	private void sendTimedOut(SendMessage packet, int triesLeft)
	{
		// If connection gets dropped, will still trigger, so test that message is still in sendList.
		// This also catches the case in which the client times out after the connection is dropped,
		// because sendList gets emptied into delayedSend.
		// Same mechanism also prevents resent attempts when the packet is ACKed.
		if (this.sendList.containsKey(packet.getMessageID().getMessageNumber()))
		{
			try
			{
				trySend(packet, triesLeft);
			}
			catch (IOException e)
			{
			}
		}
	}
	
	private void sendAcknowledged(SendAck packet)
	{
		if (this.sendList.containsKey(packet.getMessageID().getMessageNumber()))
		{
			this.lastAcked = packet.getTimestamp();
			this.sendList.remove(packet.getMessageID().getMessageNumber());
			synchronized(this.handler)
			{
				this.handler.onSendAcknowledged(this, packet.getMessageID());
			}
		}
	}
	
	private void tryReconnect(int triesLeft)
	{
		assertState(State.DROPPED);
		this.state = State.RECONNECTING;
		
		ServerPriorityListing listing = getNextListing();
		if (listing != null)
		    System.err.println("Attempting to reconnect to " + listing.getAddress().getHost());
		else
		    System.err.println("Ran out of alternate servers!");
		
		if (triesLeft > 0 && listing != null )
		{
			try
			{
				this.chatConnection = new ClientConnection(listing.getAddress().getHostAddress(), listing.getAddress().getPort(), new ConnectHandler(this));
				this.chatConnection.startReadLoop();
				
				this.chatConnection.sendReplyable(new ClientReconnect(this.clientID, this.room, this.lastAcked, this.lastReceived, this.chatConnection.getUniqueReplyCode()), 
						new ReconnectReplyHandler(this, triesLeft - 1), this.connectTimeout);
			}
			catch (IOException e)
			{
				// connection closed automatically or never opened in the first place.
				//reconnCb.cancel();
				//connectRejected(triesLeft - 1);
				e.printStackTrace();
			}
		}
		else
		{
			this.chatConnection = null;
			this.clientID = null;
			
			// Failed.
			synchronized(this.handler)
			{
				this.handler.onDropped(this);
			}
			
			this.disconnect();
		}
	}
	
	private void reconnectTimedOut(int triesLeft)
	{
		assertState(State.RECONNECTING);
		// Indirectly calls connectRejected.
		this.chatConnection.close();
	}
	
	private void reconnectRejected(int triesLeft)
	{
		// Only relevant when trying to connect.
		assertState(State.RECONNECTING);
		
		this.chatConnection = null;
		this.state = State.DROPPED;
		
		synchronized(this.handler)
		{
			this.handler.onReconnectFailed(this);
		}
		
		tryReconnect(triesLeft);
	}
	
	private void reconnectSuccess(ConnectAck ack)
	{
		assertState(State.RECONNECTING);	
		
		this.serverID = ack.getServers().getSender();
		updateServerList(ack.getServers());
		
		this.state = State.CONNECTED;
		
		synchronized(this.handler)
		{
			this.handler.onReconnected(this);
		}
		
		tryClearSendBacklog();
	}

	private void updateServerList(ServerUpdate serverData)
	{
		this.fallbacks.clear();
		for (ServerPriorityListing listing : serverData.getServers())
		{
			// Don't add the currently connected server.
			if (this.serverID == null || !this.serverID.equals(listing.getId()))
				this.fallbacks.add(listing);
		}
		
		synchronized(this.handler)
		{
			this.handler.onFallbackUpdate(this, serverData);
		}
	}
	
	private ServerPriorityListing getNextListing()
	{
		if (!this.fallbacks.isEmpty())
		{
			return this.fallbacks.remove();
		}
		else
		{
			return null;
		}
	}
	
	private int messageCounter = 0;
	private int getNextMessageNumber()
	{
		int ret = messageCounter;
		messageCounter++;
		return ret;
	}

	private static long getCurrentTimestamp()
	{
		return Calendar.getInstance().getTimeInMillis();
	}
	
	private void assertState(State s)
	{
		this.assertState(s.v);
	}
	
	private void assertState(int s)
	{
		if ((s & this.state.v) == 0)
			throw new RuntimeException("Client state error: inconsistent state.");
	}
	
	private class AuthReplyHandler implements IReplyHandler
	{
		private Client parent;
		
		public AuthReplyHandler(Client parent)
		{
			this.parent = parent;
		}
		
		@Override
		public void onRejected(ClientConnection caller)
		{
			// Do nothing, will get reported to the AuthenticationHandler instead.
		}

		@Override
		public void onReply(ClientConnection caller, ReplyPacket reply)
		{
			synchronized(parent)
			{
				assertState(State.AUTHENTICATING);
				parent.state = State.AUTHENTICATED;
				
				RoomFound packet = (RoomFound)reply;
				parent.clientID = packet.getClientID();
				updateServerList(packet.getServerData());
				
				parent.authConnection.close();
				parent.authConnection = null;
				
				synchronized(parent.handler)
				{
					parent.handler.onAuthenticated(parent);
				}
				
				tryConnect(parent.maxReconnectRetries);
			}
		}

		@Override
		public void onTimeout(ClientConnection caller)
		{
			synchronized(parent)
			{
				assertState(State.AUTHENTICATING);
				// .close will trigger the connection handler's onConnectionClosed.
				parent.authConnection.close();
			}
		}
	}

	private class AuthenticationHandler implements IClientHandler
	{
		private Client parent;
		
		public AuthenticationHandler(Client parent)
		{
			this.parent = parent;
		}
		
		@Override
		public void onConnectionClosed(ClientConnection caller)
		{
			synchronized(parent)
			{
				if (state == State.AUTHENTICATING)
				{
					//assertState(State.AUTHENTICATING);
					
					synchronized(parent.handler)
					{
						parent.handler.onAuthenticateFailed(parent);
					}
					
					parent.disconnect();
				}
			}
		}

		@Override
		public void onPacket(ClientConnection caller, ISendable packet)
		{
			synchronized(parent)
			{
				System.err.println("Detected server error: Authentication server responded with packet that isn't a proper reply.");
				//authConnection.close();
			}
		}
	}
	
	private class SendReplyHandler implements IReplyHandler
	{
		private Client parent;
		private int triesLeft;
		private SendMessage packet;
		
		public SendReplyHandler(Client parent, int triesLeft, SendMessage packet) 
		{
			this.parent = parent;
			this.triesLeft = triesLeft;
			this.packet = packet;
		}

		@Override
		public void onRejected(ClientConnection caller) 
		{
			// Let the ConnectHandler take care of it.
		}

		@Override
		public void onReply(ClientConnection caller, ReplyPacket reply) 
		{
			synchronized(parent)
			{
				sendAcknowledged((SendAck)reply);
			}
		}

		@Override
		public void onTimeout(ClientConnection caller) 
		{
			synchronized(parent)
			{
				sendTimedOut(packet, triesLeft);
			}
		}
	}
	
	private class ReconnectReplyHandler implements IReplyHandler
	{
		private Client parent;
		private int triesLeft;

		public ReconnectReplyHandler(Client parent, int triesLeft) 
		{
			this.parent = parent;
			this.triesLeft = triesLeft;
		}

		@Override
		public void onRejected(ClientConnection caller) 
		{
			synchronized(parent)
			{
				reconnectRejected(triesLeft);
			}
			
		}

		@Override
		public void onReply(ClientConnection caller, ReplyPacket reply) 
		{
			synchronized(parent)
			{
				reconnectSuccess((ConnectAck)reply);
			}
		}

		@Override
		public void onTimeout(ClientConnection caller) 
		{
			synchronized(parent)
			{
				reconnectTimedOut(triesLeft);
			}
		}
	}
	
	private class ConnectReplyHandler implements IReplyHandler
	{
		private Client parent;
		private int triesLeft;
		
		public ConnectReplyHandler(Client parent, int triesLeft) 
		{
			this.parent = parent;
			this.triesLeft = triesLeft;
		}

		@Override
		public void onRejected(ClientConnection caller) 
		{
			synchronized(parent)
			{
				connectRejected(triesLeft);
			}
		}

		@Override
		public void onReply(ClientConnection caller, ReplyPacket reply) 
		{
			synchronized(parent)
			{
				connectSuccess((ConnectAck)reply);
			}
		}

		@Override
		public void onTimeout(ClientConnection caller) 
		{
			synchronized(parent)
			{
				connectTimedOut(triesLeft);
			}
		}
	}
	
	private class ConnectHandler implements IClientHandler
	{
		private Client parent;
		
		public ConnectHandler(Client parent)
		{
			this.parent = parent;
		}

		@Override
		public synchronized void onConnectionClosed(ClientConnection caller)
		{
			synchronized (parent)
			{	
				if (state == State.CONNECTED)
					connectDropped();
				// Failed initial connection attempt is handled by the reply handler.
				else if (state != State.CONNECTING && state != State.DISCONNECTED)
					connectClosedUnknownState();
			}
		}

		@Override
		public synchronized void onPacket(ClientConnection caller, ISendable packet)
		{
			synchronized (parent)
			{
				switch (packet.getPacketType())
				{
				// Successful CONNECT_ACK will be intercepted by the reply handler.
				// Successfull SEND_ACK will be intercepted by the send reply handler.
				case MESSAGE_DATA:
					receivedMessage((MessageData)packet);
					break;
				case SERVER_UPDATE:
					receivedServerUpdate((ServerUpdate)packet);
				default:
					System.err.println("Detected server error: Unsupported packet type in chat connection.");
					// Calls onConnectionClosed indirectly.
					chatConnection.close();
					break;
				}
			}
		}
	}
	
//	private class ReconnectHandler implements IClientHandler
//	{
//		private Client parent;
//		
//		public ReconnectHandler(Client parent)
//		{
//			this.parent = parent;
//		}
//		
//		@Override
//		public void onConnectionClosed(ClientConnection caller)
//		{
//			synchronized (parent)
//			{
//				synchronized (parent)
//				{
//					// Failed reconnect will be handled by reply handler.
//					if (state == State.CONNECTED)
//						connectDropped(); // Name is intentional, not reconnectDropped.
//					else if (state != State.CONNECTING)
//						connectClosedUnknownState(); // Name is intentional, not reconnectClosedUnknownState.
//				}
//			}
//		}
//
//		@Override
//		public void onPacket(ClientConnection caller, ISendable packet)
//		{
//			synchronized (parent)
//			{
//				switch (packet.getPacketType())
//				{
//				// Reconnect success handled by the reply handler.
//				case MESSAGE_DATA:
//					receivedMessage((MessageData)packet);
//					break;
//				case SERVER_UPDATE:
//					receivedServerUpdate((ServerUpdate)packet);
//				case SEND_ACK:
//					sendAcknowledged((SendAck)packet);
//					break;
//				default:
//					System.err.println("Detected server error: Unsupported packet type in chat connection.");
//					// Calls onConnectionClosed indirectly.
//					chatConnection.close();
//					break;
//				}
//			}
//		}
//	}
}
