package binserver;

import java.io.IOException;
import java.net.UnknownHostException;

import protocol.ProtocolServer;

public class BinServer 
{
    public static final int BIN_SERVER_PORT = 12999;
	public static void main(String[] args) throws UnknownHostException, IOException 
	{
	    ProtocolServer<BinSession> binServer = new ProtocolServer<BinSession>(BIN_SERVER_PORT, 1, 
	            new BinServerHandler());
	    binServer.start();
	}
}
