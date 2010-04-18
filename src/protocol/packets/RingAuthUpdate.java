package protocol.packets;

import java.util.Collection;

import protocol.ISendable;
import protocol.PacketType;
import protocol.data.RoomCount;
import protocol.data.ServerID;

public class RingAuthUpdate implements ISendable {

	private static final long serialVersionUID = 1L;
	public final ServerID serverID;
	public final ServerUpdate latestUpdate;
	public final Collection<RoomCount> roomCounts;

	public RingAuthUpdate(ServerID serverID, ServerUpdate latestUpdate,
			Collection<RoomCount> roomCounts) {
		this.serverID = serverID;
		this.latestUpdate = latestUpdate;
		this.roomCounts = roomCounts;
	}

	@Override
	public PacketType getPacketType() {
		return PacketType.RING_AUTH_UPDATE;
	}
}
