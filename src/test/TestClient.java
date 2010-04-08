package test;

import java.io.IOException;
import java.net.UnknownHostException;

import protocol.data.MessageID;
import protocol.packets.MessageData;
import protocol.packets.SendMessage;
import protocol.packets.ServerUpdate;
import client.Client;
import client.IChatClientHandler;

public class TestClient
{
	private static final int AUTH_PORT = 9999;
	private static final int[] CHAT_PORT = {9001, 9002, 9003};
	
	public static void main(String[] args) throws UnknownHostException
	{
		new TestClient().runTest();
	}

	private Client client;
	
	public TestClient()
	{
		this.client = null;
	}
	
	public void runTest()
	{
		ServerHandler sh = new ServerHandler();
		Thread r = new Thread(sh);
		r.start();
		
		try
		{
			client = new Client("localhost", AUTH_PORT, "room", "client", new ClientHandler());
			Thread.sleep(3000);
			client.send("test message 1");
			Thread.sleep(1000);
			client.send("test message 2");
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
			System.exit(1);
		}
		
		try
		{
			r.join();
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
		
		if (sh.hadError())
		{
			System.err.println("Server had error.");
		}
	}
	
	private class ServerHandler implements Runnable
	{
		private boolean err;
		
		@Override
		public void run()
		{
			err = false;
			// TODO Auto-generated method stub
			error();
		}
		
		private synchronized void error()
		{
			err = true;
			throw new RuntimeException("ServerHandler.error fired.");
		}
		
		public synchronized boolean hadError()
		{
			return err;
		}
	}
	
	private class ClientHandler implements IChatClientHandler
	{

		@Override
		public void onAuthenticateFailed(Client caller)
		{
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onAuthenticated(Client caller)
		{
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onConnectFailed(Client caller)
		{
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onConnected(Client caller)
		{
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onCurrentServerDropped(Client caller)
		{
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onDisconnected(Client caller)
		{
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onDropped(Client caller)
		{
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onFallbackUpdate(Client caller, ServerUpdate update)
		{
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onMessageReceived(Client caller, MessageData message)
		{
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onReconnectFailed(Client client)
		{
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onReconnected(Client client)
		{
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onSendAcknowledged(Client caller, MessageID messageID)
		{
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onSendAttempted(Client caller, SendMessage msg)
		{
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onSendFailed(Client caller, SendMessage msg)
		{
			// TODO Auto-generated method stub
			
		}
	}
}
