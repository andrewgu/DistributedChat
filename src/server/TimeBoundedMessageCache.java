package server;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.PriorityQueue;

import protocol.packets.CoreMessage;


/**
 * A TimeBoundedMessageCache is a synchronized class allowing addition
 * of CoreMessage objects. These objects will be cached until a
 * certain retentionPeriod has passed.
 * 
 * @author dew47
 *
 */
public class TimeBoundedMessageCache 
{
    private static final Comparator<CoreMessage> OLDEST_FIRST =
        new Comparator<CoreMessage>() {
        @Override
        public int compare(CoreMessage o1, CoreMessage o2) {
            return ((Long) o1.timestamp).compareTo(o2.timestamp);
        }
    };
    
    private PriorityQueue<CoreMessage> messages;
    private final long retentionPeriod;
    private long newestTimestamp;
    
    /**
     * 
     * @param retentionPeriod - history length in milliseconds
     */
    public TimeBoundedMessageCache(long retentionPeriod)
    {
        this.messages = new PriorityQueue<CoreMessage>(1024, OLDEST_FIRST);
        this.retentionPeriod = retentionPeriod;
        this.newestTimestamp = Long.MIN_VALUE;
    }
    
    public synchronized void addMessage(CoreMessage msg)
    {
        if (msg.timestamp > this.newestTimestamp)
            this.newestTimestamp = msg.timestamp;
        long cutoff = this.newestTimestamp - this.retentionPeriod;
        
        messages.add(msg);
        
        while (!messages.isEmpty() && messages.peek().timestamp < cutoff)
            messages.remove();
    }
    
    public synchronized ArrayList<CoreMessage> getHistory()
    {
        ArrayList<CoreMessage> lst = new ArrayList<CoreMessage>(messages.size());
        PriorityQueue<CoreMessage> clone = new PriorityQueue<CoreMessage>(messages.size() + 128, OLDEST_FIRST);
        
        while (!this.messages.isEmpty())
        {
            CoreMessage cm = this.messages.remove();
            lst.add(cm);
            clone.add(cm);
        }
        
        // Restore the clone
        this.messages = clone;
        
        return lst;
    }

    public synchronized int getHistoryLength()
    {
        return this.messages.size();
    }
}
