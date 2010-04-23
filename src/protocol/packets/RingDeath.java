package protocol.packets;

import protocol.ISendable;
import protocol.PacketType;
import protocol.data.ServerID;

public class RingDeath implements ISendable {

	private static final long serialVersionUID = 1L;
	public final ServerID serverID;
	
	public RingDeath(ServerID reportingServer) {
		this.serverID = reportingServer;
	}

	@Override
	public PacketType getPacketType() {
		return PacketType.RING_DEATH;
	}
	
}
