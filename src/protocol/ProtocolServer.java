package protocol;

import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.channels.ClosedChannelException;

import org.xsocket.MaxReadSizeExceededException;
import org.xsocket.connection.ConnectionUtils;
import org.xsocket.connection.IConnectHandler;
import org.xsocket.connection.IDataHandler;
import org.xsocket.connection.IDisconnectHandler;
import org.xsocket.connection.INonBlockingConnection;
import org.xsocket.connection.Server;

public class ProtocolServer<_ATTACHMENT> implements 
    IConnectHandler, IDataHandler, IDisconnectHandler
{   
    private IServerHandler<_ATTACHMENT> handler;
    private Server server;

    public ProtocolServer(int port, int threads, IServerHandler<_ATTACHMENT> handler) throws UnknownHostException, IOException
    {
        this.handler = handler;
        this.server = new Server(port, this, 1, threads);
    }
    
    public void run()
    {
    	server.run();
    }
    
    public void start() throws IOException
    {
    	server.start();
    }
    
    public void stop()
    {
    	server.close();
    }
    
    @Override
    public boolean onConnect(INonBlockingConnection connection) throws IOException,
            BufferUnderflowException, MaxReadSizeExceededException
    {
        INonBlockingConnection sconn = ConnectionUtils.synchronizedConnection(connection);
        ServerConnection sc = new ServerConnection(sconn, this.handler);
        sconn.setAttachment(sc);
        sc.notifyConnected();
        return true;
    }

    @Override
    public boolean onData(INonBlockingConnection connection) throws IOException,
            BufferUnderflowException, ClosedChannelException,
            MaxReadSizeExceededException
    {
        INonBlockingConnection sconn = ConnectionUtils.synchronizedConnection(connection);
        Object attachment = sconn.getAttachment();
        if (attachment == null)
            throw new IOException("Error: ServerConnection object wasn't attached to xSocket connection.");
        ServerConnection sc = (ServerConnection)attachment;
        
        sc.notifyData();
        return true;
    }

    @Override
    public boolean onDisconnect(INonBlockingConnection connection) throws IOException
    {
        INonBlockingConnection sconn = ConnectionUtils.synchronizedConnection(connection);
        Object attachment = sconn.getAttachment();
        if (attachment == null)
            throw new IOException("Error: ServerConnection object wasn't attached to xSocket connection.");
        ServerConnection sc = (ServerConnection)attachment;
        
        sc.close();
        return true;
    }
    
    private class ServerConnection implements IServerConnection<_ATTACHMENT>
    {
        private INonBlockingConnection sconn;
        private _ATTACHMENT attachment;
        private IServerHandler<_ATTACHMENT> handler;
        private boolean closed;
        
        private PacketWriter writer;
        private PacketReader reader;
        
        //private static final int INITIAL_BUFFER_SIZE = 1024;
        private static final int MAX_PACKET_SIZE = 1048576;
        //private byte[] dataBuffer;
        
        private ServerConnection(INonBlockingConnection syncedConnection, IServerHandler<_ATTACHMENT> handler) throws IOException
        {
            this.sconn = syncedConnection;
            //this.sconn.setAutoflush(false);
            
            this.attachment = null;
            this.handler = handler;
            
            this.writer = new PacketWriter();
            this.reader = new PacketReader();
            
            this.closed = false;
            
            // Exchange serialization headers for writer.
            // Reader is lazy-initialized on first packet read.
            prepare();
        }
        
        private void prepare() throws IOException
        {
        	// Exchange serialization headers for writer	
           	try
            {
           		byte[] data = this.writer.getSerializationHeader();
                this.sconn.write(data.length);
                this.sconn.write(data, 0, data.length);
                //this.sconn.flush();
            }
            catch (BufferOverflowException e)
            {
                // Make sure to note when BufferOverflowException happens, but we're not sure if it will.
                assert(false);
            }
            catch (IOException e)
            {
                close();
                throw e;
            }
        }
        
        public void sendPacket(ISendable packet) throws IOException
        {
            try
            {
                byte[] data = writer.getSerializedData(packet);
                this.sconn.write(data.length);
                this.sconn.write(data, 0, data.length);
                //this.sconn.flush();
            }
            catch (BufferOverflowException e)
            {
                // Make sure to note when BufferOverflowException happens, but we're not sure if it will.
                assert(false);
            }
            catch (IOException e)
            {
                close();
                throw e;
            }
        }
        
        public synchronized void setAttachment(_ATTACHMENT attachment)
        {
            this.attachment = attachment;
        }
        
        public synchronized _ATTACHMENT getAttachment()
        {
            return this.attachment;
        }
        
        public void close()
        {
        	if (!closed)
        	{
        		closed = true;
	        	try
				{
					sconn.close();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
				
				reader.close();
				writer.close();
				
	            handler.onClose(this);
        	}
        }
        
        private void notifyData()
        {
            try
            {
                // Not the most elegant way to say "read until no more packets", but it works.
                while (true)
                {
                    this.sconn.markReadPosition();
                    readPacket();
                }
            }
            catch (BufferUnderflowException e)
            {
            	// Jump here when out of packets.
                this.sconn.resetToReadMark();
            }
            catch (IOException e)
            {
                // Unexpected IO error.
                close();
            }
        }

        private void readPacket() throws BufferUnderflowException, IOException
        {
            int length = this.sconn.readInt();
            if (length > MAX_PACKET_SIZE)
            	throw new IOException("Packet size exceeds maximum allowed packet size.");
            
            byte[] data = this.sconn.readBytesByLength(length);
            assert(data.length == length);
            
            if (reader.isReady())
            {
	            reader.setBytes(data, 0, length);
				handler.onPacket(this, reader.readObject());
            }
            else
            {
            	// Initialize the reader by setting the serialization header from the writer on the other end.
            	reader.setSerializationHeader(data, 0, length);
            	readPacket();
            }
        }

		private void notifyConnected()
		{
			handler.onConnect(this);
		}
    }
}
