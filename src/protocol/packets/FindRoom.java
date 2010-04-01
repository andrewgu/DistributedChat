package protocol.packets;

import protocol.ISendable;
import protocol.PacketType;

public class FindRoom implements ISendable
{
	private static final long serialVersionUID = 1L;

	@Override
	public PacketType getPacketType()
	{
		return PacketType.FIND_ROOM;
	}
	
	private String room;

	public FindRoom(String room)
	{
		this.room = room;
	}

	public String getRoom()
	{
		return room;
	}

	public void setRoom(String room)
	{
		this.room = room;
	}
}
