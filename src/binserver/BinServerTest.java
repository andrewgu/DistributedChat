package binserver;

import java.io.IOException;

public class BinServerTest
{
    /**
     * @param args
     * @throws IOException 
     */
    public static void main(String[] args) throws IOException
    {
        BinClient.main(new String[] {"localhost"});
        BinClient.request();
        BinClient.free();
        System.exit(0);
    }
}
