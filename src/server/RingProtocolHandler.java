package server;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import protocol.ClientConnection;
import protocol.IClientHandler;
import protocol.ISendable;
import protocol.IServerConnection;
import protocol.IServerHandler;
import protocol.data.ServerID;
import protocol.data.ServerStats;
import protocol.packets.CoreMessage;
import protocol.packets.MessageData;
import protocol.packets.RingInitPacket;
import protocol.packets.RingStat;
import protocol.packets.ServerUpdate;
import binserver.BinClient;
import binserver.NoFreeNodesException;

public class RingProtocolHandler implements IServerHandler<RingProtocolSession> 
{	
    public static final int MAX_CLIENT_COUNT = 4;
    public static final float THRESHOLD_CLIENT_LOAD = 0.5f;
    public static final float RELIEF_LOAD = 0.3f;
    public static final float IDLE_CLIENT_LOAD = 0.1f;
    // 5 minutes
    public static final long ROOM_RETENTION_PERIOD = 300000;
    public static final long HEADNODE_RINGSTAT_FORWARD_DELAY = 2000;
    
    // Nodes stick around for at least 15 mintues before they let themselves die.
    public static final long MINIMUM_NODE_LONGEVITY = 15000;
    
	private ClientConnection outLink;
    
    // Map of clients by room name
    private Map<String,Room> rooms;
    private LinkedList<ISendable> queuedOutgoing;
    private ArrayList<String> emptyRooms;
    private long initializationAge;
	
	public RingProtocolHandler()
	{
	    outLink = null;
	    rooms = new HashMap<String,Room>();
	    queuedOutgoing = new LinkedList<ISendable>();
	    emptyRooms = new ArrayList<String>();
	    
	    initializationAge = Calendar.getInstance().getTimeInMillis();
	}
	
	public synchronized void addClient(ClientSession client)
	{
	    Room r = this.rooms.get(client.getRoom());
	    if (r == null)
	    {
	        r = new Room(client.getRoom(), ROOM_RETENTION_PERIOD);
	        this.rooms.put(client.getRoom(), r);
	    }
	    
	    r.addClient(client);
	}
	
	// Don't kill the room until the culling moment that happens in sync with the
	// ring stat.
	public synchronized void removeClient(ClientSession client)
	{
	    Room r = this.rooms.get(client.getRoom());
        if (r != null)
        {
            r.removeClient(client);
        }
	}
	
	// Originates a message from this node.
	public synchronized void originateMessage(CoreMessage cm)
	{
	    // 1. Delivers locally.
	    Room r = this.rooms.get(cm.room);
	    if (r == null)
        {
            r = new Room(cm.room, ROOM_RETENTION_PERIOD);
            this.rooms.put(cm.room, r);
        }
	    
	    //if (r.addMessage(cm))
	    //{
    	    // 2. Forwards.
    	    this.forwardPacket(cm);
	    //}
	    //else
	    //{
	    //    System.err.println("Originating node for message does not contain the sender.");
	    //}
	}
	
	@Override
	public synchronized void onConnect(IServerConnection<RingProtocolSession> connection) 
	{
	    System.out.println("Predecessor connected.");
		connection.setAttachment(new RingProtocolSession());
	}

	@Override
	public synchronized void onPacket(IServerConnection<RingProtocolSession> connection,
			ISendable packet) {
		
	    System.out.print(">");
	    //System.out.println("Packet from predecessor.");
	    /*
		 * Ring sends around different types of packets
		 * 
		 * 1. Message forwards: keep on forwarding them/deliver to 
		 *    appropriate clients here in the ring.
		 *    
		 * 2. RingStat status messages: keep on forwarding/add our own
		 */
		
		switch(packet.getPacketType()) 
		{
		case RING_INIT:
		    handleRingInit((RingInitPacket) packet);
		    break;
		case RING_STAT:
			handleRingStat((RingStat) packet);
			break;
		case CORE_MESSAGE:
			handleCoreMessage((CoreMessage) packet);
			break;
		default:
		    System.out.println("Unhandled packet.");
		}	
	}
	
	@Override
    public synchronized void onClose(IServerConnection<RingProtocolSession> connection) 
    {
	    //Thread.dumpStack();
	    System.out.println("Predecessor disconnected.");
    }
	
	private void handleRingInit(RingInitPacket packet)
    {
	    System.out.println("Ring init packet server number: " + packet.getServerID().getServerNumber());
	    RingServer.Stats().initNode(packet.getServerID(), packet.getServerAddress());
    }

    private void handleRingStat(RingStat rs) 
	{
        System.out.print("#" + rs.getCurrentUpdateCounter() + "#");
        
        if (RingServer.isHeadNode())
        {
            headNodeUpdate(rs);
            // Delay the RingStat so that it's not instant.
            try
            {
                Thread.sleep(HEADNODE_RINGSTAT_FORWARD_DELAY);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
        
        if (this.outLink == null)
        {
            findSuccessor(rs);
        }
        
        // Clear out the dead entries.
        rs.cullDeadNodes(RingServer.Stats().getPreviousUpdateCounter());
        rs.cullEmptyRooms();
        
        // Update individual room counts on the existing RingStat.
        float load = updateLoads(rs);
        System.out.print("L" + load + ";");
        // Update server load.
        ServerID self = RingServer.Stats().getServerID();
        rs.updateLoad(self, load);
        // Push the stats to the StatCenter
        RingServer.Stats().updateLoad(load);
        RingServer.Stats().updateRingStat(rs);
        
        // Forward the RingStat to the next guy, but only if the connection exists
        System.out.println("Forwarding Ring Stat.");
        this.forwardPacket(rs);
        
        dynamicLoadBalance(self, load, rs);
	}

    private void findSuccessor(RingStat rs)
    {
        System.out.println("Looking for successor.");
        
        // No outlink, and this RingStat is hot off the press. The previous node has
        // removed the dead nodes for you, so really you're just looking for the lowest
        // counter in the list.
        ServerStats successor = rs.getOldestNode();
        try
        {
            if (successor.id.equals(RingServer.Stats().getServerID()))
            {
                System.out.println("Localhost.");
                this.outLink = new ClientConnection(InetAddress.getLocalHost(),
                        successor.addr.getPort(), new RingClientHandler(this));
                this.outLink.startReadLoop();
            }
            else
            {
                System.out.println("External successor.");
                this.outLink = new ClientConnection(InetAddress.getByName(successor.addr.getHost()), 
                        successor.addr.getPort(), new RingClientHandler(this));
                this.outLink.startReadLoop();
            }
            
            // Empty the queue.
            while (!this.queuedOutgoing.isEmpty())
                this.outLink.sendPacket(this.queuedOutgoing.remove());
        }
        catch (IOException e)
        {
            System.out.println("Failed to connect to successor.");
            // Failed, try again next time.
            this.outLink = null;
        }
    }

    private float updateLoads(RingStat rs)
    {
        int sum = 0;
        emptyRooms.clear();
        for (String rName : this.rooms.keySet())
        {
            Room r = this.rooms.get(rName);
            int num = r.numClients();
            sum += num;
            
            // Mark for removal if necessary.
            if (num == 0 && r.getHistoryLength() == 0)
                emptyRooms.add(rName);
            else
                rs.incrementRoomCount(rName, num);
        }
        
        // Cull dead rooms. (rooms with no history and no clients)
        for (String rName : emptyRooms)
            this.rooms.remove(rName);
        
        return (float)sum / (float)MAX_CLIENT_COUNT;
    }

    private void handleCoreMessage(CoreMessage cm) {
        System.out.println("Core message sent by " + cm.alias);
        
        String room = cm.room;
        Room rm = this.rooms.get(room);
        if (rm == null)
        {
            rm = new Room(room, ROOM_RETENTION_PERIOD);
            this.rooms.put(room, rm);
        }
        
        // Forward if this was not the originating node.
        // addMessage returns whether this was the originating node.
        if (!rm.addMessage(cm))
        {
            System.out.println("Forwarded.");
            this.forwardPacket(cm);
        }
	}
	
	private void forwardPacket(ISendable pkt) 
	{
	    //System.out.println("Attempting to forward packet.");
	    
	    if(this.outLink != null) 
	    {
	        //System.out.println("Non-null outlink.");
			try 
			{
			    System.out.println(".");
				this.outLink.sendPacket(pkt);
				System.out.println(":");
			} 
			catch (IOException e) 
			{
			    System.out.println("Forwarding error.");
			    // Will try to find a successor on next RingStat.
			    this.outLink = null;
			    this.queuedOutgoing.add(pkt);
			}
		} 
	    else 
	    { 
	        System.out.println("Null outlink.");
			this.queuedOutgoing.add(pkt);
	    }
	}
	
	private class RingClientHandler implements IClientHandler
	{   
	    //private RingProtocolHandler parent;
	    
	    public RingClientHandler(RingProtocolHandler parent)
	    {
	        //this.parent = parent;
	    }
	    
        @Override
        public void onConnectionClosed(ClientConnection caller)
        {
            //synchronized(parent)
            //{
                System.out.println("Successor dropped.");
                // Try to grab the right successor from the last RingStat to pass by.
                outLink = null;
                //findSuccessor(RingServer.Stats().getLatestRingStat());
            //}
        }

        @Override
        public void onPacket(ClientConnection caller, ISendable packet)
        {
            /*synchronized(parent)
            {
                // Shouldn't be getting any packets.
            }
            */
        }
	}

    public void startRingStat()
    {
        RingStat starter = new RingStat(RingServer.Stats().getServerID(),
                RingServer.Stats().getServerAddress());
        
        this.handleRingStat(starter);
    }
    
    private boolean hasReliefLoad(ServerID self, RingStat rs)
    {
        for (ServerStats s : rs.getGlobalStats())
        {
            if (!s.id.equals(self) && s.load < RELIEF_LOAD)
                return true;
        }
        return false;
    }
    
    private void dynamicLoadBalance(ServerID self, float load, RingStat rs)
    {
        boolean hasRelief = hasReliefLoad(self, rs);
        if (load >= THRESHOLD_CLIENT_LOAD && hasRelief)
        {
            // If above THRESHOLD and there is at least one below RELIEF, then offload excess.
            System.out.println("Kicking clients to other nodes.");
            kickExcessNodes(load);
        }
        else if ( !RingServer.isHeadNode()
                && load <= IDLE_CLIENT_LOAD && 
                (Calendar.getInstance().getTimeInMillis() - initializationAge > MINIMUM_NODE_LONGEVITY)
                && hasRelief)
        {
            // If below IDLE and there is at least one below RELIEF, then kick all clients
            // and shrink the ring.
            System.out.println("Idle. Dropping.");
            dropNode();
        }
    }

    private void dropNode()
    {
        for (Room r : this.rooms.values())
            r.kickAll();
        
        // Most drastic possible measure, just kicks everyone and everything.
        RingServer.stop();
    }

    private void kickExcessNodes(float currentLoad)
    {
        int kickAmount = Math.round((currentLoad - THRESHOLD_CLIENT_LOAD)*MAX_CLIENT_COUNT);
        
        outer:
        for (Room r : this.rooms.values())
        {
            while (r.numClients() > 0)
            {
                r.kickOne();
                kickAmount--;
                if (kickAmount <= 0)
                    break outer;
            }
        }
    }
    
    private void headNodeUpdate(RingStat rs)
    {
        // Be aware of the possibility that the head node is the only node in the ring.
        
        // If all nodes are above THRESHOLD, then add a node.
        if (allAboveThreshold(rs))
        {
            System.out.println("All are above threshold - adding a node.");
            
            ServerID lowest = rs.getLowestServerID();
            try
            {
                String nodeAddress = BinClient.request();
                this.outLink.close();
                this.outLink = new ClientConnection(InetAddress.getByName(nodeAddress), 
                        RingServer.RING_PORT, new RingClientHandler(this));
                this.outLink.startReadLoop();
                this.outLink.sendPacket(new RingInitPacket(lowest.getRing(), 
                        lowest.getServerNumber()-1, RingServer.RING_PORT, nodeAddress));
            }
            catch (NoFreeNodesException e)
            {
                // Give up.
            }
            catch (IOException e)
            {
                e.printStackTrace();
                throw new RuntimeException(e);
                // throw and pray.
            }
        }
    }

    private boolean allAboveThreshold(RingStat rs)
    {
        for (ServerStats s : rs.getGlobalStats())
        {
            if (s.load < THRESHOLD_CLIENT_LOAD)
                return false;
        }
        return true;
    }

    public void replayHistory(ClientSession sess, String room, long lastReceived)
    {
        Room r = this.rooms.get(room);
        if (r != null)
        {
            ServerUpdate upd = RingServer.Stats().getServerUpdate(room);
            for (CoreMessage msg : r.getHistory())
            {
                if (msg.timestamp > lastReceived)
                    sess.deliverToClient(new MessageData(upd, msg));
            }
        }
    }
}
