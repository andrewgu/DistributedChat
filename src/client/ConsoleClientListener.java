package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;

public class ConsoleClientListener
{
    public static void main(String[] args) throws UnknownHostException, IOException
    {
        Socket sock = new Socket("localhost", ConsoleClient.BROADCAST_PORT);
        BufferedReader br = new BufferedReader(new InputStreamReader(sock.getInputStream()));
        
        try
        {
            while (sock.isConnected())
            {
                String line = br.readLine();
                if (line == null)
                    break;
                else
                    System.out.println(line);
            }
        }
        catch (IOException e)
        {
            System.out.println("Disconnected.");
        }
    }

}
