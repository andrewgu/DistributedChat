
package server;

import java.util.Calendar;

import protocol.ISendable;
import protocol.IServerConnection;
import protocol.IServerHandler;
import protocol.packets.ClientConnect;
import protocol.packets.ClientReconnect;
import protocol.packets.ConnectAck;
import protocol.packets.CoreMessage;
import protocol.packets.SendAck;
import protocol.packets.SendMessage;

public class ClientProtocolHandler implements IServerHandler<ClientSession> 
{
	@Override
	public void onClose(IServerConnection<ClientSession> connection) 
	{
	    System.out.println("Client disconnected.");
	    RingServer.RingHandler().removeClient(connection.getAttachment());
	}

	@Override
	public void onConnect(IServerConnection<ClientSession> connection) 
	{	
	    System.out.println("Client connected.");
	    // Reject above a certain limit.
	    if (RingServer.Stats().getLoad() >= 1.0f)
	        connection.close();
	    else
	        connection.setAttachment(new ClientSession(connection));
	}

	@Override
	public void onPacket(IServerConnection<ClientSession> connection,
			ISendable packet) {

	    System.out.println("Client packet.");
		ClientSession sess = connection.getAttachment();

		switch(packet.getPacketType()) {
		case CLIENT_CONNECT:
			handleConnect(sess, (ClientConnect) packet);
			break;

		case CLIENT_RECONNECT:
			handleReconnect(sess, (ClientReconnect) packet);
			break;

		case SEND_MESSAGE:
			handleSendMessage(sess, (SendMessage) packet);
			break;
		}
	}

	private void handleConnect(ClientSession sess, ClientConnect cn) {
	    System.out.println("Connect request.");
		// init session and ack
		sess.onConnect(cn);
		RingServer.RingHandler().addClient(sess);
		ackConnect(sess, cn.getReplyCode());
	}

	/**
	 * Handle a client reconnect request
	 * 
	 * @param connection
	 * @param sess
	 * @param crn
	 */
	private void handleReconnect(ClientSession sess, ClientReconnect crn) {
	    System.out.println("Reconnect request.");
		// init session and ack
		sess.onReconnect(crn);
		RingServer.RingHandler().addClient(sess);
		RingServer.RingHandler().replayHistory(sess, crn.getRoom(), crn.getLastReceived());
		ackConnect(sess, crn.getReplyCode());
	}
	
	/**
	 * Acknowledge receipt of a ClientConnect or a ClientReconnect
	 * object.
	 * 
	 * @param sess
	 * @param replyCode
	 */
	private void ackConnect(ClientSession sess, long replyCode) {
		// prepare the packet
		ConnectAck cak = new ConnectAck(RingServer.Stats().getServerUpdate(sess.getRoom()),
		        Calendar.getInstance().getTimeInMillis(), replyCode);
		
		// deliver
		sess.deliverToClient(cak);
	}

	/**
	 * Internal handler for SendMessage objects from Clients.
	 * 
	 * @param sess the ClientSession in question
	 * @param snd the SendMessage
	 */
	private void handleSendMessage(ClientSession sess, SendMessage snd) {
	    System.out.println("Send request.");
		// translate sent message into CoreMessage, adding timestamp
		CoreMessage cm = new CoreMessage(snd);

		// deliver message to clients on this machine and pass on to other nodes
		RingServer.RingHandler().originateMessage(cm);
		
		// Ack immediately for now.
		sess.deliverToClient(new SendAck(RingServer.Stats().getServerUpdate(snd.getRoom()),
		        Calendar.getInstance().getTimeInMillis(), snd.getMessageID(),
		        snd.getReplyCode()));
	}
}
