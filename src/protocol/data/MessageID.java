package protocol.data;

import java.io.Serializable;

public class MessageID implements Serializable, Comparable<MessageID>
{
	private static final long serialVersionUID = 1L;
	
	private ClientID clientId;
	private int messageNumber;
	
	public MessageID (ClientID clientId, int messageNumber)
	{
		this.clientId = clientId;
		this.messageNumber = messageNumber;
	}

	public ClientID getClientID()
	{
		return clientId;
	}

	public void setClientID(ClientID clientId)
	{
		this.clientId = clientId;
	}

	public int getMessageNumber()
	{
		return messageNumber;
	}

	public void setMessageNumber(int messageNumber)
	{
		this.messageNumber = messageNumber;
	}

	@Override
	public boolean equals(Object obj)
	{
		return (obj instanceof MessageID) 
			&& clientId.equals(obj) 
			&& messageNumber == ((MessageID)obj).getMessageNumber(); 
	}

	@Override
	public int compareTo(MessageID other)
	{
		int c = clientId.compareTo(other.getClientID());
		
		if (c == 0)
			return messageNumber - other.getMessageNumber();
		else
			return c;
	}
}
