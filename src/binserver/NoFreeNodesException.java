package binserver;

public class NoFreeNodesException extends RuntimeException
{
    private static final long serialVersionUID = 1L;

    public NoFreeNodesException()
    {
    }

    public NoFreeNodesException(String arg0)
    {
        super(arg0);
    }

    public NoFreeNodesException(Throwable arg0)
    {
        super(arg0);
    }

    public NoFreeNodesException(String arg0, Throwable arg1)
    {
        super(arg0, arg1);
    }
}
