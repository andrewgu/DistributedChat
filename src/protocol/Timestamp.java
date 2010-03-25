package protocol;

import java.util.Calendar;

public class Timestamp
{
	public static long getCurrentTimestamp()
	{
		return Calendar.getInstance().getTimeInMillis();
	}

	private long timestamp;
	
	public Timestamp()
	{
		this.timestamp = getCurrentTimestamp();
	}
	
	public Timestamp(String data) throws ParseException
	{
		try
		{
			this.timestamp = Long.parseLong(data);
		}
		catch (NumberFormatException e)
		{
			throw new ParseException("Input string is not a valid Timestamp.", e);
		}
	}
	
	public Timestamp(long time)
	{
		this.timestamp = time;
	}

	public long getTimestamp()
	{
		return timestamp;
	}
	
	public void setTimestamp(long time)
	{
		this.timestamp = time;
	}
}
