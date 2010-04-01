package protocol.packets;

import protocol.ISendable;
import protocol.PacketType;

public class StatusUpdate implements ISendable
{

	@Override
	public PacketType getPacketType()
	{
		// TODO Auto-generated method stub
		return PacketType.STATUS_UPDATE;
	}

}
