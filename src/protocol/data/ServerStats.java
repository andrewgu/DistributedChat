package protocol.data;

import java.io.Serializable;

/**
 * Part of the RingStat object
 * 
 * ServerStats amalgamates vital statistics about servers into one object.
 * 
 * @author dew47
 *
 */
public class ServerStats implements Serializable, Comparable<ServerStats> {

	private static final long serialVersionUID = 1L;

	public final ServerID id;
	public final ServerAddress addr;
	public ServerLoad load;
	
	public ServerStats(ServerID id, ServerAddress addr, ServerLoad load) {
		this.id = id;
		this.addr = addr;
		this.load = load;
	}

	@Override
	/**
	 * This should sort such that the ServerStats are ordered
	 * from most desirable to least desirable
	 */
	public int compareTo(ServerStats o) {
		// TODO Auto-generated method stub
		return 0;
	}
}