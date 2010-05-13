package protocol.data;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class ServerAddress implements Serializable
{
	private static final long serialVersionUID = 1L;

	private String host;
	private int port;
	
	public ServerAddress(String host, int port)
	{
	    // Hack to get around serialization problems.
		this.host = new String(host);
		this.port = port;
	}

	public String getHost()
	{
		return host;
	}

	public void setHost(String host)
	{
		this.host = host;
	}

	public int getPort()
	{
		return port;
	}

	public void setPort(int port)
	{
		this.port = port;
	}
	
	public InetAddress getHostAddress() throws UnknownHostException
	{
		return InetAddress.getByName(host);
	}
}
