package protocol.data;

import java.io.Serializable;

public class ServerID implements Serializable, Comparable<ServerID>
{
	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof ServerID)
		{
			ServerID other = (ServerID)obj;
			return this.getRing() == other.getRing()
				&& this.getServerNumber() == other.getServerNumber();
		}
		else
		{
			return false;
		}
	}

	private static final long serialVersionUID = 1L;

	private int ring;
	private int serverNumber;
	
	public ServerID(int ring, int serverNumber)
	{
		this.ring = ring;
		this.serverNumber = serverNumber;
	}
	
	public int getRing()
	{
		return ring;
	}
	
	public void setRing(int ring)
	{
		this.ring = ring;
	}
	
	public int getServerNumber()
	{
		return serverNumber;
	}
	
	public void setServerNumber(int serverNumber)
	{
		this.serverNumber = serverNumber;
	}

	@Override
	public int compareTo(ServerID o)
	{
		int r = this.ring - o.getRing();
		if (r == 0)
			return this.serverNumber - o.serverNumber;
		else
			return r;
	}
}
