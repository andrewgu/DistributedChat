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
public class ServerStats implements Serializable {

	private static final long serialVersionUID = 1L;

	public final ServerID id;
	public final ServerAddress addr;
	public ServerLoad load;
	
	public ServerStats(ServerID id, ServerAddress addr, ServerLoad load) {
		this.id = id;
		this.addr = addr;
		this.load = load;
	}

}