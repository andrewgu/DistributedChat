package protocol;

public class MessageID
{
	private ClientID client;
	private int messageNumber;
	
	public MessageID(ClientID client, int messageNumber)
	{
		this.setClient(client);
		this.messageNumber = messageNumber;
	}
	
	public MessageID(String data) throws ParseException
	{
		String[] parts = data.split(":");
		if (parts.length != 2)
			throw new ParseException("Input string is not a valid MessageID.");
		
		try
		{
			this.client = new ClientID(parts[0]);
			this.messageNumber = Integer.parseInt(parts[1]);
		}
		catch (NumberFormatException e)
		{
			throw new ParseException("Input string is not a valid MessageID.", e);
		}
		catch (ParseException e)
		{
			throw new ParseException("Input string is not a valid MessageID.", e);
		}
	}
	
	public void setClient(ClientID client)
	{
		this.client = client;
	}
	
	public ClientID getClient()
	{
		return client;
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
