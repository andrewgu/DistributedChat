package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;

import protocol.data.MessageID;
import protocol.packets.MessageData;
import protocol.packets.SendMessage;
import protocol.packets.ServerUpdate;

public class DebugConsoleChatClient implements IChatClientHandler
{
    // Try to authenticate to arg[0] at port arg[1]
    // to room arg[2] as alias arg[3].
    public static void main(String[] args) throws Exception
    {
        if (args.length < 4)
        {
            System.err.println("Error: not enough args.");
            return;
        }
        
        InetAddress.getByName(args[0]);
        int port = Integer.parseInt(args[1]);
        String room = args[2];
        String alias = args[3];
        
        new DebugConsoleChatClient(args[0], port, room, alias).run();
    }
    
    private Client client;
    
    public DebugConsoleChatClient(String host, int port, String room, String alias) throws UnknownHostException
    {
        client = new Client(host, port, room, alias, this);
    }    

    private void run() throws IOException
    {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        
        boolean close = false;
        while (!close)
        {
            String line;
            synchronized(System.out)
            {
                System.out.print("> ");
            }
            line = reader.readLine();
            close = !handleInput(line);
        }
    }
    
    private void debugMessage(String message)
    {
        synchronized(System.err)
        {
            System.err.println("\t### " + message);
        }
    }
    
    private void receivedMessage(String sender, String message)
    {
        synchronized(System.out)
        {
            System.out.println(sender + " says: " + message);
        }
    }

    // Return whether to keep connection open.
    private boolean handleInput(String line) throws IOException
    {
        if (line.startsWith("$"))
        {
            return handleCommand(line.substring(1));
        }
        else
        {
            sendMessage(line);
            return true;
        }
    }

    private void sendMessage(String line) throws IOException
    {
        client.send(line);
    }

    private boolean handleCommand(String line) throws UnknownHostException, IOException
    {
        String[] splits = line.split(" ");
        String cmd = splits[0];
        
        if (cmd.equals("exit"))
            return false;
        else if (cmd.equals("connect"))
            client.connect();
        else if (cmd.equals("disconnect"))
            client.disconnect();
        return true;
    }

    @Override
    public void onAuthenticateFailed(Client caller)
    {
        debugMessage("Authentication failed.");
    }

    @Override
    public void onAuthenticated(Client caller)
    {
        debugMessage("Authenticated.");
    }

    @Override
    public void onConnectFailed(Client caller)
    {
        debugMessage("Connect failed.");
    }

    @Override
    public void onConnected(Client caller)
    {
        debugMessage("Connected.");
    }

    @Override
    public void onCurrentServerDropped(Client caller)
    {
        debugMessage("Current server dropped. Attempting reconnect.");
    }

    @Override
    public void onDisconnected(Client caller)
    {
        debugMessage("Disconnected.");
    }

    @Override
    public void onDropped(Client caller)
    {
        debugMessage("Failed to reconnect.");
    }

    @Override
    public void onFallbackUpdate(Client caller, ServerUpdate update)
    {
        debugMessage("New fallback list.");
    }

    @Override
    public void onMessageReceived(Client caller, MessageData message)
    {
        receivedMessage(message.getCoreMessage().alias, message.getCoreMessage().message);
    }

    @Override
    public void onReconnectFailed(Client client)
    {
        debugMessage("Failed reconnect attempt.");
    }

    @Override
    public void onReconnected(Client client)
    {
        debugMessage("Reconnected.");
    }

    @Override
    public void onSendAcknowledged(Client caller, MessageID messageID)
    {
        debugMessage("Sent.");
    }

    @Override
    public void onSendAttempted(Client caller, SendMessage msg)
    {
        debugMessage("Attempting to send.");
    }

    @Override
    public void onSendFailed(Client caller, SendMessage msg)
    {
        debugMessage("Failed to send.");
    }
}
