package client;

import protocol.data.MessageID;
import protocol.packets.MessageData;
import protocol.packets.SendMessage;
import protocol.packets.ServerUpdate;

public interface IChatClientHandler
{
	// Called if authentication fails.
	void onAuthenticateFailed(Client caller);
	// Called if authentication succeeds.
	void onAuthenticated(Client caller);
	
	// Called whenever a connect attempt fails. This can happen even if the connection eventually succeeds.
	void onConnectFailed(Client caller);
	// Called when a connection attempt succeeds.
	void onConnected(Client caller);
	
	// Called whenever a reconnect attempt fails. This can happen even if the reconnection eventually succeeds.
	void onReconnectFailed(Client client);
	// Called when a reconnect attempt succeeds
	void onReconnected(Client client);
	
	// Called when the current chat server connection is dropped. Immediately after calling, the client will
	// attempt to reconnect.
	void onCurrentServerDropped(Client caller);
	// Called when a connect or reconnect attempt completely fails, i.e. out of retries or out of fallback servers.
	void onDropped(Client caller);
	// Called when the connection is disconnected. This happens either if the client calls disconnect or if the 
	// client is dropped (see onDropped).
	void onDisconnected(Client caller);
	
	// Called when a send is attempted. This can happen multiple times, potentially for different servers,
	// before a send is successful.
	void onSendAttempted(Client caller, SendMessage msg);
	// Called when a send is acknowledged by the server, i.e. the message has been forwarded to all clients.
	void onSendAcknowledged(Client caller, MessageID messageID);
	// Called when a send fails, i.e. out of retries. A failed send will trigger a reconnect attempt.
	void onSendFailed(Client caller, SendMessage msg);
	
	// Called whenever the client receives a message.
	void onMessageReceived(Client caller, MessageData message);
	// Called whenever the client's fall-back list is updated.
	void onFallbackUpdate(Client caller, ServerUpdate update);
}
