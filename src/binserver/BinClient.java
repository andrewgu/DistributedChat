package binserver;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import protocol.ClientConnection;
import protocol.IClientHandler;
import protocol.IReplyHandler;
import protocol.ISendable;
import protocol.PacketType;
import protocol.ReplyPacket;
import server.RingServer;

public class BinClient
{
    private static InetAddress binServerAddr = null;
    private static ClientConnection conn = null;
    //private static Thread readLoop = null;
    
    // in milliseconds
    public static final int REQUEST_POLL_INTERVAL = 100;
    public static final int NODE_REQUEST_TIMEOUT = 300000;
    public static final int FREE_REQUEST_TIMEOUT = 300000;
    
    /**
     * @param args The array of command line arguments for the nodes.
     * Arguments are:
     * [0] = address of bin server
     * [1] = defined as "head" if head node
     * @throws IOException 
     */
    public static void main(String[] args) throws IOException
    {
        boolean isHeadNode = args.length > 1 && args[1].equals("head");
        initBinClient(args[0]);
        
        if (isHeadNode)
        {
            RingServer.startBase();
            RingServer.initHead(args[0]);    
        }
        else
        {
            free();
        }
    }
    
    // Call this to request a new node from the Bin server. Returns the host name for use
    // with InetAddress.getByName.
    public static String request() throws IOException, NoFreeNodesException
    {
        NodeRequestReplyHandler handler = new NodeRequestReplyHandler(); 
        
        synchronized(conn)
        {
            conn.sendReplyable(new NodeRequest(conn.getUniqueReplyCode()), 
                    handler, NODE_REQUEST_TIMEOUT);
            System.err.println("Requested node.");
        }
        
        try
        {
            while (!handler.isReady())
                Thread.sleep(REQUEST_POLL_INTERVAL);
        }
        catch (InterruptedException e)
        {
            System.err.println("BinClient.request interrupted in poll loop.");
            throw new IOException(e);
        }
        
        if (handler.isError())
        {
            throw new IOException("BinClient: connection error to BinServer.");
        }
        else
        {
            String value = handler.getValue();
            if (value == null)
                throw new NoFreeNodesException(
                        "Bin server responded, no free nodes.");
            else
                System.err.println("Got node " + value);
            return value;
        }
    }
    
    // Tells the Bin server to put this node on the free node queue.
    public static void free() throws IOException
    {
        FreeRequestReplyHandler handler = new FreeRequestReplyHandler();
        synchronized(conn)
        {
            
            conn.sendReplyable(new FreeRequest(conn.getUniqueReplyCode()), 
                    handler, FREE_REQUEST_TIMEOUT);
            System.err.println("Sent free request.");
        }
        
        try
        {
            while (!handler.isReady())
                Thread.sleep(REQUEST_POLL_INTERVAL);
        }
        catch (InterruptedException e)
        {
            System.err.println("BinClient.free interrupted in poll loop.");
            throw new IOException(e);
        }
        
        if (handler.isError() || !handler.isConfirmed())
        {
            throw new IOException("BinClient: BinServer connection error.");
        }

        System.err.println("Got free confirmation");
    }
    
    private static void allocateServer() throws UnknownHostException, IOException
    {
        RingServer.startBase();
    }
    
    private static void initBinClient(String binServerAddress) throws IOException
    {   
        binServerAddr = InetAddress.getByName(binServerAddress);
        conn = new ClientConnection(binServerAddr, BinServer.BIN_SERVER_PORT, new BinClientHandler());
        conn.sendPacket(new BinClientAddress(binServerAddress));
        //readLoop = conn.startReadLoop();
        conn.startReadLoop();
    }
    
    private static class FreeRequestReplyHandler implements IReplyHandler
    {
        private boolean ready;
        private boolean confirmed;
        private boolean error;
        
        public FreeRequestReplyHandler()
        {
            this.ready = false;
            this.confirmed = false;
            this.error = false;
        }
        
        public synchronized boolean isReady()
        {
            return this.ready;
        }
        
        public synchronized boolean isConfirmed()
        {
            return this.confirmed;
        }
        
        public synchronized boolean isError()
        {
            return this.error;
        }

        @Override
        public synchronized void onRejected(ClientConnection caller)
        {
            this.ready = true;
            this.error = true;
        }

        @Override
        public synchronized void onReply(ClientConnection caller, ReplyPacket reply)
        {
            this.ready = true;
            this.confirmed = true;
        }

        @Override
        public synchronized void onTimeout(ClientConnection caller)
        {
            this.ready = true;
            this.error = true;
        }
    }
    
    private static class NodeRequestReplyHandler implements IReplyHandler
    {
        private boolean ready;
        private boolean error;
        private String value;
        
        public NodeRequestReplyHandler()
        {
            this.ready = false;
            this.error = false;
            this.value = null;
        }
        
        public synchronized boolean isReady()
        {
            return this.ready;
        }
        
        public synchronized boolean isError()
        {
            return this.error;
        }
        
        // Null if error.
        public synchronized String getValue()
        {
            return this.value;
        }
        
        @Override
        public synchronized void onReply(ClientConnection caller, ReplyPacket reply)
        {
            NodeRequestReply rp = (NodeRequestReply)reply;
            this.value = rp.getNodeAddress();
            this.ready = true;
        }

        @Override
        public synchronized void onTimeout(ClientConnection caller)
        {
            //this.value = null;
            this.ready = true;
            this.error = true;
        }
        
        @Override
        public synchronized void onRejected(ClientConnection caller)
        {
            // this.value = null;
            this.ready = true;
            this.error = true;
        }
    }
    
    // Dummy handler. All of the actual work happens in the reply handlers.
    private static class BinClientHandler implements IClientHandler
    {
        @Override
        public void onConnectionClosed(ClientConnection caller)
        {
            // Not used.
        }

        @Override
        public void onPacket(ClientConnection caller, ISendable packet)
        {
            // Can receive one type of packet: AllocateRequest.
            if (packet.getPacketType() == PacketType.BIN_ALLOCATE_REQUEST)
            {
                try
                {
                    allocateServer();
                }
                catch (Exception e)
                {
                    try
                    {
                        conn.sendPacket(new AllocateRequestReply((AllocateReply)packet, false));
                    }
                    catch (IOException e1)
                    {
                        e1.printStackTrace();
                    }
                    
                    return;
                }
                
                try
                {
                    conn.sendPacket(new AllocateRequestReply((AllocateReply)packet, true));
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }
}
