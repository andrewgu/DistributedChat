package protocol;
import java.io.IOException;
import java.util.LinkedList;

import protocol.packets.RingStat;
import protocol.packets.CoreMessage;

public class RingProtocolHandler implements IServerHandler<RingProtocolSession> {
	
	private ClientProtocolHandler cph;
	private ClientList clients;
	private StatCenter stats;
	
	// we are connected to one link before us
	private IServerConnection<RingProtocolSession> inLink;
	private IServerConnection<RingProtocolSession> outLink;
	private LinkedList<ISendable> queuedOutgoing;
	
	public RingProtocolHandler(ClientProtocolHandler cph, ClientList clients,
			StatCenter stats) {
		this.cph = cph;
		this.clients = clients;
		this.stats = stats;
		
		this.inLink = null;
		this.outLink = null;
		this.queuedOutgoing = new LinkedList<ISendable>();
	}
	
	@Override
	public void onConnect(IServerConnection<RingProtocolSession> connection) {
		connection.setAttachment(new RingProtocolSession());		
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
		
		switch(packet.getPacketType()) {
		case RING_STAT:
			handleRingStat((RingStat) packet);
		case CORE_MESSAGE:
			handleCoreMessage((CoreMessage) packet);
		}
		
	}
	
	private void handleRingStat(RingStat rs) {
	
		/* Steps
		 * 
		 * 1. update vital information for this server in object
		 * 		-update room counts
		 * 		-update server statistics
		*/
		rs.updateRoomCounts(clients.countRooms());
		rs.updateServerStats(stats.getCurrentServerStats());

		// 2. make updated information available to other program components
		stats.registerRingStat(rs);
		
		// 3. forward (and wait for ack) to other servers
		this.forwardPacket(rs);
	}
	
	private void handleCoreMessage(CoreMessage cm) {
		// deliver locally
		cph.deliverMessageLocally(cm);
		// and forward
		this.forwardPacket(cm);
	}
	
	public void forwardPacket(ISendable pkt) {
		if(this.outLink != null) {
			try {
				this.outLink.sendPacket(pkt);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			// TODO reconnect protocol?
			this.queuedOutgoing.add(pkt);
			
		}
	}

	@Override
	public void onClose(IServerConnection<RingProtocolSession> connection) {
		// TODO Auto-generated method stub
		
	}
}
