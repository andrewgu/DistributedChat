package server;

import java.io.IOException;
import java.net.UnknownHostException;

import binserver.BinClient;

import protocol.ProtocolServer;
import protocol.packets.ServerUpdate;

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
    
    private static ProtocolServer<ClientSession> clientService = null;
    private static ProtocolServer<AuthSession> authService = null;
    private static ProtocolServer<RingProtocolSession> ringService = null;
    private static StatCenter statCenter = null;
    
    public static void startBase() throws UnknownHostException, IOException
    {
        clientService = new ProtocolServer<ClientSession>(CLIENT_PORT, CLIENT_THREADS, 
                new ClientProtocolHandler());
        ringService = new ProtocolServer<RingProtocolSession>(RING_PORT, RING_THREADS,
                new RingProtocolHandler());
        
        statCenter = new StatCenter();
        
        clientService.start();
        ringService.start();
    }
    
    public static void startHead() throws UnknownHostException, IOException
    {
        authService = new ProtocolServer<AuthSession>(AUTH_PORT, AUTH_THREADS,
                new AuthProtocolHandler());
        authService.start();
    }
    
    public static void stop()
    {
        if (clientService != null)
        {
            clientService.stop();
            clientService = null;
        }
        
        if (ringService != null)
        {
            ringService.stop();
            ringService = null;
        }
        
        if (authService != null)
        {
            authService.stop();
            authService = null;
        }
        
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
