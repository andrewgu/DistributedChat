package protocol;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.channels.ClosedChannelException;

import org.xsocket.MaxReadSizeExceededException;
import org.xsocket.connection.ConnectionUtils;
import org.xsocket.connection.IConnectHandler;
import org.xsocket.connection.IDataHandler;
import org.xsocket.connection.IDisconnectHandler;
import org.xsocket.connection.INonBlockingConnection;

public class ProtocolServer<_ATTACHMENT> implements 
    IConnectHandler, IDataHandler, IDisconnectHandler
{   
    private IServerHandler<_ATTACHMENT> handler;

    public ProtocolServer(int port, int threads, IServerHandler<_ATTACHMENT> handler)
    {
        this.handler = handler;
    }
    
    private class ServerConnection implements IServerConnection<_ATTACHMENT>
    {
        private INonBlockingConnection sconn;
        private _ATTACHMENT attachment;
        private IServerHandler<_ATTACHMENT> handler;
        private PacketWriter writer;
        
        //private static final int INITIAL_READ_BUFFER_SIZE = 256;
        //private byte[] buffer;
        
        private ServerConnection(INonBlockingConnection syncedConnection, IServerHandler<_ATTACHMENT> handler) throws IOException
        {
            this.sconn = syncedConnection;
            //this.sconn.setAutoflush(false);
            
            this.attachment = null;
            this.handler = handler;
            
            this.writer = new PacketWriter();
            
            //this.buffer = new byte[INITIAL_READ_BUFFER_SIZE];
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
            handler.onClose(this);
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
            byte[] data = this.sconn.readBytesByLength(length);
            
            assert(data.length == length);
            
            ByteArrayInputStream bis = new ByteArrayInputStream(data);
            ObjectInputStream ois = new ObjectInputStream(bis);
            
            try
			{
				handler.onPacket(this, (ISendable)ois.readObject());
			}
			catch (ClassNotFoundException e)
			{
				throw new IOException("Class not found.", e);
			}
        }
    }

    @Override
    public boolean onConnect(INonBlockingConnection connection) throws IOException,
            BufferUnderflowException, MaxReadSizeExceededException
    {
        INonBlockingConnection sconn = ConnectionUtils.synchronizedConnection(connection);
        ServerConnection sc = new ServerConnection(sconn, this.handler);
        sconn.setAttachment(sc);
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
}
