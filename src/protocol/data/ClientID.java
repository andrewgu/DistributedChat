package protocol.data;

import java.io.Serializable;

public class ClientID implements Serializable, Comparable<ClientID>
{	
	private static final long serialVersionUID = 3L;
	
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

	@Override
	public boolean equals(Object obj)
	{
		return (obj instanceof ClientID)
			&& room.equals(((ClientID)obj).getRoom())
			&& client == ((ClientID)obj).getClient();
	}

	@Override
	public int compareTo(ClientID o)
	{
		int rm = room.compareTo(o.getRoom());
		if (rm == 0)
		{
			return this.client - o.getClient();
		}
		else
		{
			return rm;
		}
	}
}
