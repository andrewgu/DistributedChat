package protocol.packets;

import protocol.ISendable;
import protocol.PacketType;
import protocol.data.RingState;
import protocol.data.ServerID;

public class RingAuthUpdate implements ISendable {

	private static final long serialVersionUID = 1L;
	public final ServerID serverID;
	public final RingState state;
	public final ServerUpdate latestUpdate;
	public final String[] liveRooms;
	public final String[] deadRooms;

	public RingAuthUpdate(ServerID serverID, RingState state,
			ServerUpdate latestUpdate, String[] liveRooms,
			String[] deadRooms) {
		this.serverID = serverID;
		this.state = state;
		this.latestUpdate = latestUpdate;
		this.liveRooms = liveRooms;
		this.deadRooms = deadRooms;
	}

	@Override
	public PacketType getPacketType() {
		return PacketType.RING_AUTH_UPDATE;
	}
}
