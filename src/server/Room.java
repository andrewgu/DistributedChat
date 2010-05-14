package server;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import protocol.data.ClientID;
import protocol.packets.CoreMessage;
import protocol.packets.MessageData;

public class Room
{
    private String name;
    private Map<ClientID, ClientSession> clients;
    private TimeBoundedMessageCache messages;
    
    public Room(String name, long retentionPeriod)
    {
        this.name = name;
        this.clients = new HashMap<ClientID, ClientSession>();
        this.messages = new TimeBoundedMessageCache(retentionPeriod);
    }
    
    public synchronized String getName()
    {
        return name;
    }
    
    // Adds and culls messages.
    public synchronized boolean addMessage(CoreMessage msg)
    {
        System.out.println("Room " + name + " message added.");
        this.messages.addMessage(msg);
        // return whether this was the originating node, i.e. if the original sender is here. 
        return broadcastMessage(msg);
    }

    public synchronized List<CoreMessage> getHistory()
    {
        return this.messages.getHistory();
    }
    
    public synchronized int getHistoryLength()
    {
        return this.messages.getHistoryLength();
    }
    
    public synchronized void addClient(ClientSession client)
    {
        System.out.println("Room " + name + " client added.");
        this.clients.put(client.getClientID(), client);
    }
    
    public synchronized void removeClient(ClientSession client)
    {
        System.out.println("Room " + name + " client removed.");
        this.clients.remove(client.getClientID());
    }
    
    public synchronized boolean hasClient(ClientID clientID)
    {
        return this.clients.containsKey(clientID);
    }
    
    public synchronized int numClients()
    {
        return clients.size();
    }
    
    private boolean broadcastMessage(CoreMessage msg)
    {
        boolean clientFound = false;
        MessageData md = new MessageData(RingServer.Stats().getServerUpdate(name), msg);
        for (ClientSession s : this.clients.values())
        {
            if (s.getClientID().equals(msg.messageID.getClientID()))
            {
                clientFound = true;
            }
            s.deliverToClient(md);
        }
        return clientFound;
    }

    public void kickOne()
    {
        System.out.println("Room " + name + " client kicked.");
        ClientSession s = clients.values().iterator().next();
        s.kickClient();
    }
}
