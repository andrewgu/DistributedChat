
package protocol;

import java.util.Calendar;

import protocol.packets.ClientConnect;
import protocol.packets.ClientReconnect;
import protocol.packets.ConnectAck;
import protocol.packets.CoreMessage;
import protocol.packets.MessageData;
import protocol.packets.SendMessage;

public class ClientProtocolHandler implements IServerHandler<ClientSession> {


	private RingProtocolHandler rph;
	private ClientList clients;
	private StatCenter statc;
	private TimeBoundedMessageCache tbmc;



	@Override
	public void onClose(IServerConnection<ClientSession> connection) {
		// TODO stub
		clients.removeClient(connection.getAttachment());
	}

	@Override
	public void onConnect(IServerConnection<ClientSession> connection) {

		
		connection.setAttachment(new ClientSession(connection));


	}

	@Override
	public void onPacket(IServerConnection<ClientSession> connection,
			ISendable packet) {

		ClientSession sess = connection.getAttachment();

		// TODO what about FIND_ROOM? how does this get handled
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
		// init session and ack
		sess.onConnect(cn);
		ackConnect(sess, cn.getReplyCode());
		
		clients.addClient(sess);
	}

	/**
	 * Handle a client reconnect request
	 * 
	 * @param connection
	 * @param sess
	 * @param crn
	 */
	private void handleReconnect(ClientSession sess, ClientReconnect crn) {
		// init session and ack
		sess.onReconnect(crn);
		ackConnect(sess, crn.getReplyCode());
		
		clients.addClient(sess);
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
		ConnectAck cak = new ConnectAck(statc.currentUpdate(),
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
		// translate sent message into CoreMessage, adding timestamp
		CoreMessage cm = new CoreMessage(snd);

		// deliver message to clients on this machine
		this.deliverMessageLocally(cm);
		
		// pass on to other nodes
		rph.forwardPacket(cm);
	}

	/**
	 * Deliver a CoreMessage to all clients in the room, excluding,
	 * if necessary, the sender. Cache message for the suggested
	 * caching interval.
	 * 
	 * @param cm the message object
	 */
	public void deliverMessageLocally(CoreMessage cm) {
		ClientSession[] roomClients = clients.getRoomClients(cm.room);

		// just means there are no local clients on this machine
		if(roomClients == null) return;

		// prepare a wrapper for sending to clients
		MessageData mdata = new MessageData(statc.currentUpdate(), cm);

		int i;
		for(i = 0; i < roomClients.length; i++) {
			// deliver to all clients but the sender
			if(roomClients[i].getClientID() != cm.messageID.getClientID())
				roomClients[i].deliverToClient(mdata);
		}
		
		// put message in message cache
		tbmc.addMessage(cm);
	}

}
