package protocol.data;

import java.io.Serializable;

/**
 * Representation of load on a server.
 * 
 * @author dew47
 *
 */
public class ServerLoad implements Serializable, Comparable<ServerLoad> {

	private static final long serialVersionUID = 1L;

	@Override

	/**
	 * Lower loads are placed before higher loads using this comparator
	 */
	public int compareTo(ServerLoad o) {
		// TODO Auto-generated method stub
		return 0;
	}

	
}
