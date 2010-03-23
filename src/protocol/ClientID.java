package protocol;

public class ClientID
{
	private String room;
	private int client;
	
	public ClientID(String room, int client)
	{
		this.room = room;
		this.client = client;
	}
	
	public ClientID(String data) throws ParseException
	{
		String[] parts = data.split(".");
		if (parts.length != 2)
			throw new ParseException("Input string is not a valid ClientID.");
		
		this.room = parts[0];
		this.client = Integer.parseInt(parts[1]);
	}

	public void setRoom(String room)
	{
		this.room = room;
	}

	public String getRoom()
	{
		return room;
	}

	public int getClient()
	{
		return client;
	}

	public void setClient(int client)
	{
		this.client = client;
	}

	@Override
	public String toString()
	{
		return room + "." + client;
	}
	
	public static final IParser<ClientID> PARSER = new Parser();
	
	private static class Parser implements IParser<ClientID>
	{
		@Override
		public ClientID parse(String value) throws ParseException
		{
			return new ClientID(value);
		}
	}
}
