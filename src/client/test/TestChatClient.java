package client.test;

public class TestChatClient
{
    // Two args: room alias
    public static void main(String[] args) throws Exception
    {
        client.DebugConsoleChatClient.main(new String[] {"localhost", 
                Integer.toString(TestAuthServer.PORT), args[0], args[1]});
    }
}
