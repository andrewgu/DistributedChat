package protocol.packets;

import protocol.IReplyable;
import protocol.PacketType;

public class FindRoom implements IReplyable
{
	private static final long serialVersionUID = 1L;
	private long replyCode;

	@Override
	public PacketType getPacketType()
	{
		return PacketType.FIND_ROOM;
	}
	
	private String room;

	public FindRoom(String room, long replyCode)
	{
		this.replyCode = replyCode;
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

	@Override
	public long getReplyCode()
	{
		return this.replyCode;
	}

	public void setReplyCode(int replyCode)
	{
		this.replyCode = replyCode;
	}
}
