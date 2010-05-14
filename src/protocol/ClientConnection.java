package protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.InvalidParameterException;
import java.util.HashMap;
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
    
    public class ReplyableRecord
    {
    	public IReplyHandler replyHandler;
    	public TimeoutCallback timeoutCallback;
    	public ReplyableRecord(IReplyHandler handler, TimeoutCallback cb)
    	{
    		this.replyHandler = handler;
    		this.timeoutCallback = cb;
    	}
    }
    private HashMap<Long, ReplyableRecord> replyables;
    private long replyCodeCounter;
    
    private PacketReader reader;
    private PacketWriter writer;
    
    private Timer timer;
    
    public ClientConnection(InetAddress host, int port, 
            IClientHandler handler) throws IOException
    {
        System.out.println("Connection opened on port " + port);
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
		int len = data.length;
		this.dos.writeInt(len);
		this.dos.write(data);
		
		this.replyables = new HashMap<Long, ReplyableRecord>();
		this.replyCodeCounter = 0;
		
		this.timer = new Timer(true);
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
			
			// Clear out the reply settings.
			for (ReplyableRecord record : this.replyables.values())
			{
				record.timeoutCallback.cancel();
				record.replyHandler.onRejected(this);
			}
			this.replyables.clear();
			
			synchronized(handler)
			{
				handler.onConnectionClosed(this);
			}
    	}
    }
    
    public synchronized long getUniqueReplyCode()
    {
    	long r = this.replyCodeCounter;
    	this.replyCodeCounter++;
    	return r;
    }
    
    public synchronized void sendPacket(ISendable sendable) throws IOException
    {
    	if (closed)
    	{
    	    //System.out.println("Closed.");
            throw new IOException("Connection is closed.");
    	}
        
        try
        {
            byte[] data = writer.getSerializedData(sendable);
        	int len = data.length;
        	//System.out.println("Begin write");
            dos.writeInt(len);
            dos.write(data);
            //System.out.println("End write");
        }
        catch (IOException e)
        {
            System.out.println("Caught error.");
            close();
            throw e;
        }
    }
    
    public synchronized void sendReplyable(IReplyable replyable, IReplyHandler replyHandler,
    	long delayMilliseconds) throws IOException
    {
     	if (closed)
     		throw new IOException("Connection is closed.");
     	
     	if (this.replyables.containsKey(replyable.getReplyCode()))
     		throw new InvalidParameterException("Cannot have duplicate reply code.");
     	
     	if (delayMilliseconds <= 0)
     	   throw new InvalidParameterException("delayMilliseconds must be positive.");
     	
     	try
     	{
     		byte[] data = writer.getSerializedData(replyable);
     		int len = data.length;
        	dos.writeInt(len);
            dos.write(data);
     	}
     	catch (IOException e)
     	{
     		close();
     		throw e;
     	}
     	
     	final ClientConnection cbCaller = this;
        final IReplyHandler cbTimeoutHandler = replyHandler;
        final long cbReplyCode = replyable.getReplyCode();
        TimeoutCallback timeoutCallback = new TimeoutCallback(new Runnable()
			{
				@Override
				public void run()
				{
					synchronized(cbCaller)
					{
						if (cbCaller.replyables.containsKey(cbReplyCode))
						{
							cbCaller.replyables.remove(cbReplyCode);
							cbTimeoutHandler.onTimeout(cbCaller);
						}
					}
				}
			});
        
        this.replyables.put(replyable.getReplyCode(), new ReplyableRecord(replyHandler, timeoutCallback));
        this.timer.schedule(timeoutCallback, delayMilliseconds);
    }
    
    private void readPacket() throws IOException
    {
    	synchronized(reader)
    	{
	    	// Get length of packet
	    	int len = dis.readInt();
	    	
	    	// Grow data buffer if necessary
	        while (dataBuffer.length < len)
	        {
	        	if (dataBuffer.length >= MAX_BUFFER_SIZE)
	        		throw new IOException("ClientConnection's internal data buffer size limit has been exceeded.");
	        	dataBuffer = new byte[dataBuffer.length * 2];
	        }
	        
	        // Read entire packet
	        int read = 0;
	        while (read < len)
	        	read += dis.read(dataBuffer, read, len - read);
	        
	        // Initialize reader if it's not read (see else case)
	        if (reader.isReady())
	        {
	        	// Push data into the packet converter
	        	reader.setBytes(dataBuffer, 0, len);
	        	ISendable packet = reader.readObject();
	        	
	        	if (packet instanceof ReplyPacket)
	        	{
		        	// If it's a reply packet and it has a valid reply code
	        		ReplyPacket rp = (ReplyPacket)packet;
	        		ReplyableRecord record = this.replyables.get(rp.getReplyPacketCode());
	        		if (record != null)
	        		{
	        			this.replyables.remove(rp.getReplyPacketCode());
	        			record.timeoutCallback.cancel();
	        			record.replyHandler.onReply(this, rp);
	        		}
	        		else
	        		{
	        			synchronized(handler)
						{
			        		handler.onPacket(this, reader.readObject());
						}
	        		}
	        	}
	        	else
	        	{
		        	synchronized(handler)
					{
		        		handler.onPacket(this, reader.readObject());
					}
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
