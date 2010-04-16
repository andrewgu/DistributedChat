package protocol;

import java.util.HashMap;
import protocol.data.ClientID;
import protocol.data.ClientSession;
import protocol.data.MessageID;
import protocol.packets.ClientConnect;
import protocol.packets.ClientReconnect;
import protocol.packets.ConnectAck;
import protocol.packets.CoreMessage;
import protocol.packets.MessageData;
import protocol.packets.SendMessage;
import protocol.packets.ServerUpdate;

public class ClientProtocolHandler implements IServerHandler<ClientSession> {


	private RingProtocolHandler rph;
	private ClientList clients;
	private StatCenter statc;



	@Override
	public void onClose(IServerConnection<ClientSession> connection) {
		// TODO stub
		clients.removeClient(connection.getAttachment());
	}

	@Override
	public void onConnect(IServerConnection<ClientSession> connection) {


		connection.setAttachment(null);


	}

	@Override
	public void onPacket(IServerConnection<ClientSession> connection,
			ISendable packet) {

		ClientSession sess = connection.getAttachment();

		// TODO what about FIND_ROOM? how does this get handled
		switch(packet.getPacketType()) {
		case CLIENT_CONNECT:
			handleConnect(connection, sess, (ClientConnect) packet);
			break;

		case CLIENT_RECONNECT:
			handleReconnect(connection, sess, (ClientReconnect) packet);
			break;

		case SEND_MESSAGE:
			handleSendMessage(sess, (SendMessage) packet);
			break;

		}
	}

	private void handleConnect(IServerConnection<ClientSession> connection, 
			ClientSession sess, ClientConnect cn) {
		// init session and ack
		sess.onConnect(cn);
		ackConnect(sess, cn.getReplyCode());
		
		clients.addClient(sess);
	}

	private void handleReconnect(IServerConnection<ClientSession> connection,
			ClientSession sess, ClientReconnect crn) {
		// init session and ack
		sess.onReconnect(crn);
		ackConnect(sess, crn.getReplyCode());
		
		clients.addClient(sess);
	}
	
	private void ackConnect(ClientSession sess, long replyCode) {
		// TODO proc server update
		ConnectAck cak = new ConnectAck(statc.currentUpdate(),
				System.currentTimeMillis(), replyCode);
		
		sess.deliverPacketToClient(cak);
	}

	private void handleSendMessage(ClientSession sess, SendMessage snd) {
		// translate sent message into CoreMessage, adding timestamp
		CoreMessage cm = 
			new CoreMessage(snd.getRoom(), snd.getMessage(),
					snd.getMessageID(), snd.getClientID(),
					snd.getAlias(), System.currentTimeMillis(),
					snd.getReplyCode());

		this.deliverMessageLocally(cm);
		rph.forwardCoreMessage(cm);
	}

	/**
	 * Deliver a message to all clients on this server that should
	 * receive it.
	 * 
	 * @param cm the message object
	 */
	public void deliverMessageLocally(CoreMessage cm) {
		ClientSession[] roomClients = clients.getRoomClients(cm.room);

		// just means there are no local clients on this machine
		if(roomClients == null) return;

		//TODO fill in these values
		MessageData mdata = new MessageData(statc.currentUpdate(), cm);

		int i;
		for(i = 0; i < roomClients.length; i++) {
			// deliver to all clients but the sender
			if(roomClients[i].getClientID() != cm.sender)
				roomClients[i].deliverPacketToClient(mdata);
		}
	}

}
