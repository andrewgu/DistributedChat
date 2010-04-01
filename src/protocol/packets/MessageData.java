package protocol.packets;

import protocol.ISendable;
import protocol.PacketType;

public class MessageData implements ISendable
{

	@Override
	public PacketType getPacketType()
	{
		// TODO Auto-generated method stub
		return PacketType.MESSAGE_DATA;
	}

}
