package protocol;

public class ServerID
{
	private int ring;
	private int server;
	
	public ServerID(String data) throws ParseException
	{
		String[] parts = data.split(".");
		if (parts.length != 2)
			throw new ParseException("Input string is not a valid ServerID.");
		
		try
		{
			this.ring = Integer.parseInt(parts[0]);
			this.server = Integer.parseInt(parts[1]);
		}
		catch (NumberFormatException e)
		{
			throw new ParseException("Input string is not a valid ServerID.", e);
		}
	}
	
	public ServerID(int ring, int server)
	{
		this.setRing(ring);
		this.setServer(server);
	}

	public void setRing(int ring)
	{
		this.ring = ring;
	}

	public int getRing()
	{
		return ring;
	}

	public void setServer(int server)
	{
		this.server = server;
	}

	public int getServer()
	{
		return server;
	}

	@Override
	public String toString()
	{
		return ring + "." + server;
	}
}
