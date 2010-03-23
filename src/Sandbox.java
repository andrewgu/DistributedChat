

import java.util.StringTokenizer;

public class Sandbox
{
    /**
     * @param args
     */
    public static void main(String[] args)
    {
        String body = "asdfasdf\nasdfasdf\nasdf\n\n\nbody";
        StringTokenizer tk = new StringTokenizer(body, "\n", true);
        
        while (tk.hasMoreTokens())
            System.out.println(":" + tk.nextToken());
    }
}
