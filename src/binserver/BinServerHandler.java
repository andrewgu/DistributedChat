package binserver;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import protocol.ISendable;
import protocol.IServerConnection;
import protocol.IServerHandler;
import protocol.IServerReplyHandler;
import protocol.ReplyPacket;

public class BinServerHandler implements IServerHandler<BinSession>
{
    public static int ALLOCATE_REQUEST_REPLY_TIMEOUT = 300000;
    
    private Queue<BinSession> freeNodeQueue;
    private Map<String, BinSession> nodes;
    
    public BinServerHandler()
    {
        this.freeNodeQueue = new LinkedBlockingQueue<BinSession>();
        this.nodes = new HashMap<String, BinSession>();
    }
    
    @Override
    public void onClose(IServerConnection<BinSession> connection)
    {
        BinSession session = connection.getAttachment();
        session.isConnected = false;
        nodes.remove(session.identifier);
    }

    @Override
    public void onConnect(IServerConnection<BinSession> connection)
    {
        BinSession session = new BinSession(connection);
        connection.setAttachment(session);
        nodes.put(session.identifier, session);
    }

    @Override
    public void onPacket(IServerConnection<BinSession> connection,
            ISendable packet) throws IOException
    {
        switch (packet.getPacketType())
        {
        case BIN_NODE_REQUEST:
            this.nodeRequest((NodeRequest)packet, connection);
            break;
        case BIN_FREE_REQUEST:
            System.err.println("Got free request.");
            this.freeRequest((FreeRequest)packet, connection);
            break;
        default:
            System.err.println(packet.getPacketType());
            throw new RuntimeException("Error: Invalid packet type in BinServerHandler.onPacket.");
        }
    }

    private void freeRequest(FreeRequest packet,
            IServerConnection<BinSession> connection) throws IOException
    {
        BinSession session = connection.getAttachment();
        session.isActive = false;
        this.freeNodeQueue.add(session);
        connection.sendPacket(new FreeRequestReply(packet, true));
        System.err.println("Node freed.");
    }

    private void nodeRequest(NodeRequest packet,
            IServerConnection<BinSession> connection) throws IOException
    {
        BinSession popped;
        
        do
        {
            popped = this.freeNodeQueue.poll();
        } while (popped != null && !popped.isConnected);
        
        if (popped != null)
        {
            popped.connection.sendReplyable(new AllocateReply(popped.connection.getUniqueReplyCode()), 
                    new AllocateRequestReplyHandler(popped, packet, connection), ALLOCATE_REQUEST_REPLY_TIMEOUT);
        }
        else
        {
            connection.sendPacket(new NodeRequestReply(packet, null));
        }
    }
    
    private class AllocateRequestReplyHandler implements IServerReplyHandler<BinSession>
    {
        private BinSession popped;
        private IServerConnection<BinSession> connection;
        private NodeRequest packet;

        public AllocateRequestReplyHandler(BinSession popped, NodeRequest packet, IServerConnection<BinSession> connection)
        {
            this.popped = popped;
            this.packet = packet;
            this.connection = connection;
        }

        @Override
        public void onRejected(IServerConnection<BinSession> caller)
        {
            try
            {
                connection.sendPacket(new NodeRequestReply(packet, null));
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        @Override
        public void onReply(IServerConnection<BinSession> caller,
                ReplyPacket reply)
        {
            AllocateRequestReply response = (AllocateRequestReply)reply;
            if (response.wasSuccessful())
            {
                try
                {
                    connection.sendPacket(new NodeRequestReply(this.packet, this.popped.nodeAddress));
                    System.err.println("Node request fulfilled.");
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
            else
            {
                // Retry until there are no more left.
                try
                {
                    nodeRequest(this.packet, this.connection);
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onTimeout(IServerConnection<BinSession> caller)
        {
            //caller.close();
            try
            {
                connection.sendPacket(new NodeRequestReply(packet, null));
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }
}
