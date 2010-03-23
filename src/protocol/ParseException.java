package protocol;

import java.io.IOException;

public class ParseException extends IOException
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1743939386552709483L;

	public ParseException()
	{
		super();
	}

	public ParseException(String arg0, Throwable arg1)
	{
		super(arg0, arg1);
	}

	public ParseException(String arg0)
	{
		super(arg0);
	}

	public ParseException(Throwable arg0)
	{
		super(arg0);
	}
}
