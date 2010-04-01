package protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

public class ClientConnection
{
    private Socket socket;
    private InputStream readStream;
    private OutputStream writeStream;
    private IClientHandler handler;
    private boolean closed;
    
    private DataInputStream dis;
    private DataOutputStream dos;
    private ObjectInputStream ois;
    
    private PacketWriter writer;
    
    public ClientConnection(InetAddress host, int port, 
            IClientHandler handler) throws IOException
    {
        this.socket = new Socket(host, port);
        this.readStream = socket.getInputStream();
        this.writeStream = socket.getOutputStream();
        this.handler = handler;
        this.closed = false;
        
        this.dis = new DataInputStream(readStream);
        this.dos = new DataOutputStream(writeStream);
        this.ois = new ObjectInputStream(this.dis);
        
        this.writer = new PacketWriter();
        
        // Spawn a worker thread for reading.
        new Thread(new ReadWorker(this)).start();
    }
    
    public synchronized boolean isOpen()
    {
        return !closed;
    }
    
    public synchronized void close()
    {
        // Tells worker thread to cancel.
        if (!closed)
        {
            closed = true;
            // Try to clean up.
            try
            {
                socket.close();
            }
            catch (IOException e)
            {   
            }
        }
    }
    
    public void sendPacket(ISendable sendable) throws IOException
    {
        if (closed)
            throw new IOException("Connection is closed.");
        
        try
        {
        	byte[] data = writer.getSerializedData(sendable);
        	dos.writeInt(data.length);
            dos.write(data);
        }
        catch (IOException e)
        {
            close();
            throw e;
        }
    }
    
    private void readPacket() throws IOException
    {
    	// Has side effects.
        dis.readInt();
        
        try
		{
			handler.onPacket(this, (ISendable)ois.readObject());
		}
		catch (ClassNotFoundException e)
		{
			throw new IOException("Class not found.", e);
		}
    }
    
    private class ReadWorker implements Runnable
    {
        private ClientConnection parent;
        
        public ReadWorker(ClientConnection parent)
        {
            this.parent = parent;
        }
        
        @Override
        public void run()
        {
            try
            {
                while (!closed)
                {
                    readPacket();
                }
            }
            catch (IOException e)
            {
                close();
            }
            
            handler.onConnectionClosed(parent);
        }
    }
}
