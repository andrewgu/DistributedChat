package protocol.data;

import java.io.Serializable;

public class ServerPriorityListing implements Serializable, Comparable<ServerPriorityListing>
{
	private static final long serialVersionUID = 1L;
	
	private int priority;
	private ServerID id;
	private ServerAddress address;
	
	public ServerPriorityListing(int priority, ServerID id,
			ServerAddress address)
	{
		this.priority = priority;
		// Hack for serialization.
		this.id = new ServerID(id.getRing(), id.getServerNumber());
		this.address = address;
	}

	public int getPriority()
	{
		return priority;
	}

	public void setPriority(int priority)
	{
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

	public ServerAddress getAddress()
	{
		return address;
	}

	public void setAddress(ServerAddress address)
	{
		this.address = address;
	}

	@Override
	public int compareTo(ServerPriorityListing o)
	{
		return this.getPriority() - o.getPriority();
	}
}
