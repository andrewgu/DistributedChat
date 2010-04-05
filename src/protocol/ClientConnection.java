package protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Timer;

public class ClientConnection
{
    private Socket socket;
    private DataInputStream dis;
    private DataOutputStream dos;
    
    private IClientHandler handler;
    private boolean closed;
    
    private static final int INITIAL_BUFFER_SIZE = 1024;
    private static final int MAX_BUFFER_SIZE = 1048576;
    private byte[] dataBuffer;
    
    private PacketReader reader;
    private PacketWriter writer;
    
    private Timer timer;
    
    public ClientConnection(InetAddress host, int port, 
            IClientHandler handler) throws IOException
    {
    	this.socket = new Socket(host, port);
    	
        this.handler = handler;
        this.closed = false;
     
        this.dis = new DataInputStream(socket.getInputStream());
	    this.dos = new DataOutputStream(socket.getOutputStream());

		this.dataBuffer = new byte[INITIAL_BUFFER_SIZE];

		this.reader = new PacketReader();
		this.writer = new PacketWriter();

		// Push serialization headers for writer.
		// Reader will be lazy-initialized on first read.
		byte[] data = this.writer.getSerializationHeader();
		this.dos.writeInt(data.length);
		this.dos.write(data);
		
		this.timer = new Timer();
    }
    
    public synchronized boolean isOpen()
    {
        return !closed;
    }
    
    public synchronized void close()
    {
    	if (!closed)
    	{
    		closed = true;
    		this.timer.cancel();
    		this.reader.close();
    		this.writer.close();
    		
    		try
			{
				this.socket.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			
			synchronized(handler)
			{
				handler.onConnectionClosed(this);
			}
    	}
    }
    
    public synchronized void sendPacket(ISendable sendable) throws IOException
    {
        this.sendPacket(sendable, null, -1);
    }
    
    // timeoutCallback is ALWAYS called unless the callback is explicitly cancelled by the caller to sendPacket.
    public synchronized void sendPacket(ISendable sendable, TimeoutCallback timeoutCallback, 
    		long delayMilliseconds) throws IOException
    {
    	if (closed)
            throw new IOException("Connection is closed.");
        
        try
        {
        	byte[] data = writer.getSerializedData(sendable);
        	dos.writeInt(data.length);
            dos.write(data);
            
            if (timeoutCallback != null && delayMilliseconds > 0)
            	timer.schedule(timeoutCallback, delayMilliseconds);
        }
        catch (IOException e)
        {
            close();
            throw e;
        }
    }
    
    private void readPacket() throws IOException
    {
    	synchronized(reader)
    	{
	    	// Has side effects.
	    	int len = dis.readInt();
	        while (dataBuffer.length < len)
	        {
	        	if (dataBuffer.length >= MAX_BUFFER_SIZE)
	        		throw new IOException("ClientConnection's internal data buffer size limit has been exceeded.");
	        	dataBuffer = new byte[dataBuffer.length * 2];
	        }
	        
	        int read = 0;
	        while (read < len)
	        	read += dis.read(dataBuffer, read, len - read);
	        
	        if (reader.isReady())
	        {
	        	reader.setBytes(dataBuffer, 0, len);
	        	synchronized(handler)
				{
	        		handler.onPacket(this, reader.readObject());
				}
	        }
	        else
	        {
	        	reader.setSerializationHeader(dataBuffer, 0, len);
	        	// Retry the read now that the reader is ready.
	        	readPacket();
	        }
    	}
    }
    
    private void readLoop()
    {
    	while (!closed)
    	{
    		try
			{
				readPacket();
			}
			catch (IOException e)
			{
				close();
			}
    	}
    }
    
    public Thread startReadLoop()
    {
    	Thread loop = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				readLoop();
			}
		});
    	loop.start();
    	return loop;
    }
}
