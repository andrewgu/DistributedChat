package client;

@SuppressWarnings("serial")
public class ClientStateException extends Exception
{
	public ClientStateException()
	{
		super();
	}

	public ClientStateException(String arg0, Throwable arg1)
	{
		super(arg0, arg1);
	}

	public ClientStateException(String arg0)
	{
		super(arg0);
	}

	public ClientStateException(Throwable arg0)
	{
		super(arg0);
	}
}
