package protocol.packets;

/**
 * A subpacket of RingStat, this data structure allows for
 * bookkeeping the number of users in a room. The number of 
 * users can be modified directly, as if by getters/setters.
 * 
 * @author dew47
 */
public class RoomCount {

	public final String name;
	public int users;
	
	public RoomCount(String name, int start) {
		this.name = name;
		this.users = start;
	}
	
	public RoomCount(String name) {
		this.name = name;
		this.users = 0;
	}
}
