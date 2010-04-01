package protocol.packets;

import protocol.ISendable;
import protocol.PacketType;
import protocol.data.ClientID;

public class RoomFound implements ISendable
{
	private static final long serialVersionUID = 1L;

	@Override
	public PacketType getPacketType()
	{
		return PacketType.ROOM_FOUND;
	}
	
	private ClientID clientID;
	private ServerUpdate serverData;

	public RoomFound(ClientID clientID, ServerUpdate serverData)
	{
		this.clientID = clientID;
		this.serverData = serverData;
	}

	public ClientID getClientID()
	{
		return clientID;
	}

	public void setClientID(ClientID clientID)
	{
		this.clientID = clientID;
	}

	public ServerUpdate getServerData()
	{
		return serverData;
	}

	public void setServerData(ServerUpdate serverData)
	{
		this.serverData = serverData;
	}
}
