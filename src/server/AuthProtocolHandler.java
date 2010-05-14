package server;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import protocol.ISendable;
import protocol.IServerConnection;
import protocol.IServerHandler;
import protocol.data.ClientID;
import protocol.packets.FindRoom;
import protocol.packets.RoomFound;

public class AuthProtocolHandler implements IServerHandler<AuthSession> {

    private Map<String, Integer> clientInts = new HashMap<String, Integer>();
   
    @Override
    public void onClose(IServerConnection<AuthSession> connection) {
        // stateless
    }

    @Override
    public void onConnect(IServerConnection<AuthSession> connection) {
        // stateless   
    }

    @Override
    public void onPacket(IServerConnection<AuthSession> connection,
            ISendable packet) throws IOException {

        switch(packet.getPacketType()) {
        case FIND_ROOM:
            handleFindRoom(connection, (FindRoom) packet);
            break;
           
        default:
            connection.close();
        }
    }

    private void handleFindRoom(IServerConnection<AuthSession> connection,
            FindRoom fr)
    {
        Integer i;
        synchronized (this.clientInts)
        {
            i = clientInts.get(fr.getRoom());
            if (i == null)
                i = 0;
            else
                i++;

            this.clientInts.put(fr.getRoom(), i);
        }
        ClientID client = new ClientID(fr.getRoom(), i);
        try
        {
            connection.sendPacket(new RoomFound(client, RingServer.Stats().getServerUpdate(fr.getRoom()),
                    fr.getReplyCode()));
        }
        catch (IOException e)
        {
            e.printStackTrace();
            connection.close();
        }

    }
}
