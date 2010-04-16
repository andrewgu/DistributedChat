package protocol;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.ListIterator;

import protocol.packets.CoreMessage;


/**
 * A TimeBoundedMessageCache is a synchronized class allowing addition
 * of CoreMessage objects. These objects will be cached until a
 * certain retentionPeriod has passed.
 * 
 * @author dew47
 *
 */
public class TimeBoundedMessageCache implements Runnable {

	public static final long NO_WAIT = -1;

	private static final Comparator<CoreMessage> OLDEST_FIRST =
		new Comparator<CoreMessage>() {
		@Override
		public int compare(CoreMessage o1, CoreMessage o2) {
			return ((Long) o1.timestamp).compareTo(o2.timestamp);
		}
	};
	private static final long SPIN_INT = 10;

	private LinkedList<CoreMessage> cache;
	private final long retentionPeriod;
	private final long minWait;
	private long oldestMessage;

	/**
	 * 
	 * 
	 * @param retentionPeriod the time after which a message should be discarded
	 * @param minWait a variable to throttle this thread, determines amount
	 * of time to sleep before discarding again
	 */
	public TimeBoundedMessageCache(long retentionPeriod, long minWait) {
		this.retentionPeriod = retentionPeriod;
		this.minWait = minWait;

		this.cache = new LinkedList<CoreMessage>();
		this.oldestMessage = Long.MAX_VALUE; // so will decrease with addMessage()
	}

	/**
	 * Given the current time, find out the timestamp of the oldest
	 * message that should stay in the cache.
	 * @return the timestamp of that message
	 */
	public long discardHorizon() {
		return System.currentTimeMillis() - this.retentionPeriod;
	}

	/**
	 * Add a message to the cache
	 * @param cm the message (will be discarded if already too old)
	 */
	public synchronized void addMessage(CoreMessage cm) {
		if(cm.timestamp <= discardHorizon()) return;
		if(cm.timestamp < this.oldestMessage)
			this.oldestMessage = cm.timestamp;

		cache.add(cm);
	}

	/**
	 * Remove all of the messages that are timestamped before the
	 * discard horizon.
	 * 
	 * @return the number of messages removed
	 */
	private synchronized int cullMessages() {
		orderCache();		

		int i = 0;
		long disc = discardHorizon();

		while(cachesz() > 0){
			if(cache.getFirst().timestamp > disc) break;

			cache.removeFirst();
			i++;
		}

		this.oldestMessage = cache.getFirst().timestamp;

		return i;
	}

	/**
	 * Get all message from a certain time onwards
	 * 
	 * @param from the certain time
	 * @return the messages, in a linked list
	 */
	public synchronized LinkedList<CoreMessage>
	getAllMessagesFrom(long from) {
		
		orderCache();		
		LinkedList<CoreMessage> results = new LinkedList<CoreMessage>();
		ListIterator<CoreMessage> lit = cache.listIterator();
		CoreMessage cm;

		while(lit.hasNext()) {
			cm = lit.next();
			if(cm.timestamp < from) continue;
			results.add(cm);
		}

		return results;
	}

	/**
	 * Order the cache, oldest first
	 */
	private synchronized void orderCache() {
		Collections.sort(cache, OLDEST_FIRST);
	}

	private synchronized int cachesz() {
		return cache.size();
	}


	@Override
	public void run() {
		long untilNextRemoval;

		while(true) {

			// don't even bother while cache has nothing
			// in it (also, this will ensure that oldestMessage
			// has a non-bs value)
			while(cachesz() == 0) {
				try {
					Thread.sleep(SPIN_INT);
				} catch (InterruptedException e) {}
			}

			// determine whether this thread should sleep before
			// culling entries
			untilNextRemoval = this.oldestMessage - discardHorizon();
			untilNextRemoval = Math.max(untilNextRemoval, minWait);

			// sleep, else cull
			if(untilNextRemoval > 0) {
				try {
					Thread.sleep(untilNextRemoval);
				} catch (InterruptedException e) {}
			} else {
				cullMessages();
			}
		}
	}

}
