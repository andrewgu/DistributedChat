package client;

public interface IChatClientHandler
{
	void onAuthenticated(Client caller);
	void onConnected(Client caller);
	void onConnectFailed(Client caller);
	
	void onDropped(Client caller);
	void onReconnected(Client caller);
	
	void onDisconnected(Client caller);
	
	void onSendAttempted(Client caller, ClientMessage message);
	void onSendAcknowledged(Client caller, ClientMessage message);
	void onSendFailed(Client caller, ClientMessage message);
	
	void onMessageReceived(Client caller, ClientMessage message);
	void onFallbackUpdate(Client caller, ClientMessage message);
}
