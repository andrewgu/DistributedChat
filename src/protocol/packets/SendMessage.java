package protocol.packets;

import protocol.ISendable;
import protocol.PacketType;

public class SendMessage implements ISendable
{

	@Override
	public PacketType getPacketType()
	{
		return PacketType.SEND_MESSAGE;
	}

}
