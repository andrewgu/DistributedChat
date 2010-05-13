package protocol;

import java.util.TimerTask;

// Used internally by the ProtocolServer and ClientConnection for managing the reply
// mechanism.
public class TimeoutCallback extends TimerTask
{
	private Runnable callback;
	private boolean cancelled;
	
	public TimeoutCallback(Runnable callback)
	{
		this.callback = callback;
		this.cancelled = false;
	}
	
	public synchronized void run()
	{
		if (!cancelled)
		{
		    this.cancel();
			callback.run();
		}
	}
	
	public synchronized boolean cancel()
	{
		cancelled = true;
		return super.cancel();
	}
}
