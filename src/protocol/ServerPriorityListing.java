package protocol;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class ServerPriorityListing
{
	private ServerID id;
	private InetAddress address;
	private int port;
	private int priority;
	
	public ServerPriorityListing(String data) throws UnknownHostException, ParseException
	{
		String[] parts = data.split(" ");
		if (parts.length != 4)
			throw new ParseException("Input string is not a valid ServerListing.");
		
		this.priority = Integer.parseInt(parts[0]);
		this.id = new ServerID(parts[1]);
		this.address = InetAddress.getByName(parts[2]);
		this.port = Integer.parseInt(parts[3]);
	}

	public ServerPriorityListing(ServerID id, InetAddress address, int port,
			int priority)
	{
		this.id = id;
		this.address = address;
		this.port = port;
		this.priority = priority;
	}

	public ServerID getId()
	{
		return id;
	}

	public void setId(ServerID id)
	{
		this.id = id;
	}

	public InetAddress getAddress()
	{
		return address;
	}

	public void setAddress(InetAddress address)
	{
		this.address = address;
	}

	public int getPort()
	{
		return port;
	}

	public void setPort(int port)
	{
		this.port = port;
	}

	public int getPriority()
	{
		return priority;
	}

	public void setPriority(int priority)
	{
		this.priority = priority;
	}

	/*public static final IParser<ServerPriorityListing[]> MULTI_PARSER = new MultiParser();
	public static final IParser<ServerPriorityListing> PARSER = new Parser();
	
	private static class MultiParser implements IParser<ServerPriorityListing[]>
	{
		@Override
		public ServerPriorityListing[] parse(String value)
		{
			String[] entries = value.split("|");
			ArrayList<ServerPriorityListing> listings = new ArrayList<ServerPriorityListing>();
			
			for (int i = 0; i < entries.length; i++)
			{
				try
				{
					listings.add(new ServerPriorityListing(entries[i]));
				}
				catch (Exception e)
				{
					// Do nothing.
				}
			}
			
			return (ServerPriorityListing[])listings.toArray();
		}	
	}
	
	private static class Parser implements IParser<ServerPriorityListing>
	{
		@Override
		public ServerPriorityListing parse(String value) throws IOException
		{
			return new ServerPriorityListing(value);
		}	
	}*/
}
