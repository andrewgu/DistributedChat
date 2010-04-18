package protocol.packets;

import protocol.ISendable;
import protocol.PacketType;
import protocol.data.ServerID;

public class RingDeath implements ISendable {

	public final ServerID serverID;
	
	public RingDeath(ServerID reportingServer) {
		this.serverID = reportingServer;
	}

	@Override
	public PacketType getPacketType() {
		// TODO Auto-generated method stub
		return PacketType.RING_DEATH;
	}
	
}
