package server;

import java.io.IOException;
import java.net.UnknownHostException;

import protocol.ProtocolServer;
import protocol.data.ServerAddress;
import protocol.data.ServerID;
import binserver.BinClient;

/*
 * Singleton class that contains globals for the node. This makes a central point
 * of management and accessibility for all server components. 
 * Mainly, this cleans up the amount of data that has to be thrown around.
 */
public class RingServer
{
    public static final int AUTH_PORT = 13000;
    public static final int CLIENT_PORT = 13001;
    public static final int RING_PORT = 13002;
    
    public static final int AUTH_THREADS = 1;
    public static final int RING_THREADS = 1;
    public static final int CLIENT_THREADS = 1;
    
    public static final int DEFAULT_RING = 0;
    
    private static ProtocolServer<ClientSession> clientService = null;
    private static ProtocolServer<AuthSession> authService = null;
    private static ProtocolServer<RingProtocolSession> ringService = null;
    
    private static ClientProtocolHandler clientHandler = null;
    private static RingProtocolHandler ringHandler = null;
    private static AuthProtocolHandler authHandler = null;
    
    private static StatCenter statCenter = null;
    
    public static void startBase() throws UnknownHostException, IOException
    {
        clientHandler = new ClientProtocolHandler();
        clientService = new ProtocolServer<ClientSession>(CLIENT_PORT, CLIENT_THREADS, 
                clientHandler);
        ringHandler = new RingProtocolHandler();
        ringService = new ProtocolServer<RingProtocolSession>(RING_PORT, RING_THREADS,
                ringHandler);
        
        statCenter = new StatCenter();
        
        clientService.start();
        ringService.start();
        
        System.out.println("Started base services.");
    }
    
    public static void initHead(String localName) throws UnknownHostException, IOException
    {
        statCenter.initNode(new ServerID(DEFAULT_RING, Integer.MAX_VALUE), 
                new ServerAddress(localName, RING_PORT));
        
        startHead();
        
        ringHandler.startRingStat();
        
        System.out.println("Started initial head node.");
    }

    public static void startHead() throws UnknownHostException, IOException
    {   
        authHandler = new AuthProtocolHandler();
        authService = new ProtocolServer<AuthSession>(AUTH_PORT, AUTH_THREADS,
                authHandler);
        authService.start();
        
        System.out.println("Upgraded to head node.");
    }
    
    public static void stop()
    {
        System.out.println("Stopping node.");
        
        if (authService != null)
            authService.stop();
        if (clientService != null)
            clientService.stop();
        if (ringService != null)
            ringService.stop();
        
        clientHandler = null;
        clientService = null;
        ringHandler = null;
        ringService = null;
        authHandler = null;
        authService = null;
        statCenter = null;
        
        try
        {
            BinClient.free();
        }
        catch (IOException e)
        {
            // Abort if it can't free properly.
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    public static void panic()
    {
        System.exit(2);
    }
    
    public static ProtocolServer<ClientSession> ClientService()
    {
        if (clientService == null)
            throw new NullPointerException("BaseService not initialized.");
        return clientService;
    }
    
    public static ProtocolServer<AuthSession> AuthService()
    {
        if (authService == null)
            throw new NullPointerException("AuthService not initialized.");
        return authService;
    }
    
    public static ProtocolServer<RingProtocolSession> RingService()
    {
        if (ringService == null)
            throw new NullPointerException("BaseService not initialized.");
        return ringService;
    }
    
    public static RingProtocolHandler RingHandler()
    {
        if (ringHandler == null)
            throw new NullPointerException("BaseService not initialized.");
        return ringHandler;
    }
    
    public static StatCenter Stats()
    {
        if (statCenter == null)
            throw new NullPointerException("BaseService not initialized.");
        return statCenter;
    }
    
    public static boolean isHeadNode()
    {
        return authService != null;
    }
}
