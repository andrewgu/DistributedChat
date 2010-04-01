package protocol;

import java.io.Serializable;

public interface ISendable extends Serializable
{
	public PacketType getPacketType();
}
