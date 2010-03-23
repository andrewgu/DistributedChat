package protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

public class ClientConnection
{
    private static final int INITIAL_PACKET_BUFFER_SIZE = 256;
    
    private Socket socket;
    private InputStream readStream;
    private OutputStream writeStream;
    private IClientHandler handler;
    private boolean closed;
    
    private DataInputStream dis;
    private DataOutputStream dos;
    
    private byte[] buffer;

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
        
        this.buffer = new byte[INITIAL_PACKET_BUFFER_SIZE];
        
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
    
    public void sendPacket(Packet p) throws IOException
    {
        if (closed)
            throw new IOException("Connection is closed.");
        
        try
        {
            dos.write(p.toString().getBytes("US-ASCII"));
        }
        catch (IOException e)
        {
            close();
            throw e;
        }
    }
    
    private void readPacket() throws IOException
    {
        int length = dis.readInt();
        
        // Dynamically grow buffer, but only if necessary.
        if (length > buffer.length)
            buffer = new byte[buffer.length * 2];
        
        // keep reading until we get the entire packet.
        int read = 0;
        while (read < length)
        {
            read += dis.read(buffer, read, length - read);
        }
        
        Packet p = Packet.parsePacket(new String(buffer, 0, length, "US-ASCII"));
        handler.onPacket(this, p);
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
