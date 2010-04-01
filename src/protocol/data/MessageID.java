package protocol.data;

import java.io.Serializable;

public class MessageID implements Serializable
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
}
