package protocol;

import java.util.regex.Pattern;

public class ClientID
{
	private static final Pattern ROOM_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9\\_]+$");
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
		if (parts.length != 2 || !ROOM_NAME_PATTERN.matcher(parts[0]).matches())
			throw new ParseException("Input string is not a valid ClientID.");
		
		this.room = parts[0];
		try
		{
			this.client = Integer.parseInt(parts[1]);
		}
		catch (NumberFormatException e)
		{
			throw new ParseException("Input string is not a valid ClientID.", e);
		}
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
}
