package protocol;
import protocol.packets.RingStat;
import protocol.packets.CoreMessage;

public class RingProtocolHandler implements IServerHandler<RingProtocolSession> {
	
	
	@Override
	public void onConnect(IServerConnection<RingProtocolSession> connection) {
		// TODO Auto-generated method stub
		
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
		/*
		 * Steps
		 * 
		 * 1. update vital information for this server in object
		 * 		-update room counts
		 * 		-update server statistics
		 * 2. make updated information available to other program components
		 * 3. forward (and wait for ack) to other servers
		 *
		 */
	}
	
	private void handleCoreMessage(CoreMessage cm) {

	}
	
	public void forwardCoreMessage(CoreMessage cm) {
		
	}

	@Override
	public void onClose(IServerConnection<RingProtocolSession> connection) {
		// TODO Auto-generated method stub
		
	}
}
