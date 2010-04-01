package protocol.data;

import java.io.Serializable;

public class ClientID implements Serializable
{	
	private static final long serialVersionUID = 1L;
	
	private String room;
	private int client;
	
	public ClientID(String room, int client)
	{
		this.room = room;
		this.client = client;
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
}
