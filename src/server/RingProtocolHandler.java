package server;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import protocol.ClientConnection;
import protocol.ISendable;
import protocol.IServerConnection;
import protocol.IServerHandler;
import protocol.packets.CoreMessage;
import protocol.packets.RingInitPacket;
import protocol.packets.RingStat;

public class RingProtocolHandler implements IServerHandler<RingProtocolSession> 
{	
    public static int MAX_CLIENT_COUNT = 150;
    public static float THRESHOLD_CLIENT_LOAD = 0.7f;
    public static float IDLE_CLIENT_LOAD = 0.3f;
    
	private IServerConnection<RingProtocolSession> inLink;
	private ClientConnection outLink;
    
    // Map of clients by room name
    private Map<String,Room> rooms;
    private LinkedList<ISendable> queuedOutgoing;
    private ArrayList<String> emptyRooms;
	
	public RingProtocolHandler()
	{
	    inLink = null;
	    outLink = null;
	    rooms = new HashMap<String,Room>();
	    queuedOutgoing = new LinkedList<ISendable>();
	    emptyRooms = new ArrayList<String>();
	}
	
	@Override
	public void onConnect(IServerConnection<RingProtocolSession> connection) 
	{
		connection.setAttachment(new RingProtocolSession());
		this.inLink = connection;
	}

	@Override
	public void onPacket(IServerConnection<RingProtocolSession> connection,
			ISendable packet) {
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
		case RING_STAT:
			handleRingStat((RingStat) packet);
			break;
		case CORE_MESSAGE:
			handleCoreMessage((CoreMessage) packet);
			break;
		}
		
	}
	
	private void handleRingInit(RingInitPacket packet)
    {
	    RingServer.Stats().initNode(packet.getServerID(), packet.getServerAddress());
    }

    private void handleRingStat(RingStat rs) 
	{
        if (RingServer.isHeadNode())
        {
            // TODO: Special head node behavior.
        }
        
        // Update individual room counts on the existing RingStat.
        float load = updateLoads(rs);
        // Update server load.
        rs.updateLoad(RingServer.Stats().getServerID(), load);
        // Push the stats to the StatCenter
        RingServer.Stats().updateLoad(load);
        RingServer.Stats().updateRingStat(rs);
        // Forward the RingStat to the next guy.
    	this.forwardPacket(rs);
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
		// deliver locally
		//cph.deliverMessageLocally(cm);
		// and forward
		this.forwardPacket(cm);
	}
	
	public void forwardPacket(ISendable pkt) 
	{
	    if(this.outLink != null) 
	    {
			try 
			{
				this.outLink.sendPacket(pkt);
			} 
			catch (IOException e) 
			{
			    // TODO: Handle failed connection?
				e.printStackTrace();
			}
		} 
	    else 
	    {
	        // TODO: Handle failed connection? 
			this.queuedOutgoing.add(pkt);
	    }
	}

	@Override
	public void onClose(IServerConnection<RingProtocolSession> connection) 
	{
		// TODO Auto-generated method stub
		
	}
}
