package protocol.data;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Part of the RingStat object
 * 
 * ServerStats amalgamates vital statistics about servers into one object.
 * 
 * @author dew47
 *
 */
public class ServerStats implements Serializable {

	private static final long serialVersionUID = 1L;

	public final ServerID id;
	public final ServerAddress addr;
	public float load;
	public long lastUpdate;
	
	public ServerStats(ServerID id, ServerAddress addr, float load, long lastUpdate) {
		this.id = id;
		this.addr = addr;
		this.load = load;
		this.lastUpdate = lastUpdate;
	}
	
	public static final Comparator<ServerStats> PRIORITY_COMPARATOR = new ServerStatsComparator();
	
    private static class ServerStatsComparator implements Comparator<ServerStats>
    {
        @Override
        public int compare(ServerStats o1, ServerStats o2)
        {
            return new Float(o1.load).compareTo(new Float(o2.load));
        }
    }
}