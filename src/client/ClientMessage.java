package client;

import java.util.Comparator;

import protocol.packets.MessageData;

public class ClientMessage
{	
	public static final Comparator<ClientMessage> TIMESTAMP_COMPARATOR = new TimestampComparer();
	
	private Type type;
	private String alias;
	private long timestamp;
	private String message;
	private MessageData getReceiptData;
	
	public ClientMessage(Type type, String alias, long timestamp,
			String message, MessageData getReceiptData)
	{
		this.type = type;
		this.alias = alias;
		this.timestamp = timestamp;
		this.message = message;
		this.getReceiptData = getReceiptData;
	}

	public ClientMessage (MessageData mdata)
	{
		this(Type.RECEIVED, mdata.getAlias(), mdata.getTimestamp(), mdata.getMessage(), mdata);
	}
	
	public ClientMessage (String alias, long timestamp, String message)
	{
		this(Type.SENT, alias, timestamp, message, null);
	}
	
	public ClientMessage (long timestamp, String notification)
	{
		this(Type.NOTIFICATION, null, timestamp, notification, null);
	}

	public Type getType()
	{
		return type;
	}

	public void setType(Type type)
	{
		this.type = type;
	}

	public String getAlias()
	{
		return alias;
	}

	public void setAlias(String alias)
	{
		this.alias = alias;
	}

	public long getTimestamp()
	{
		return timestamp;
	}

	public void setTimestamp(long timestamp)
	{
		this.timestamp = timestamp;
	}

	public String getMessage()
	{
		return message;
	}

	public void setMessage(String message)
	{
		this.message = message;
	}
	
	public MessageData getGetReceiptData()
	{
		return getReceiptData;
	}

	public void setGetReceiptData(MessageData getReceiptData)
	{
		this.getReceiptData = getReceiptData;
	}

	public enum Type
	{
		RECEIVED,
		SENT,
		NOTIFICATION
	}
	
	private static class TimestampComparer implements Comparator<ClientMessage>
	{
		@Override
		public int compare(ClientMessage o1, ClientMessage o2)
		{
			return (int)(o1.getTimestamp() - o2.getTimestamp());
		}
	}
}
