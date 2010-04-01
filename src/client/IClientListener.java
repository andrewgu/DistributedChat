package client;

import protocol.packets.MessageData;

public interface IClientListener
{
	// Called when client successfully connects to the room.
	void onConnected();
	// Called if the client gets dropped from the room.
	void onDropped();
	
	// Called if the client receives a message update.
	void onMessage(MessageData message);
}
