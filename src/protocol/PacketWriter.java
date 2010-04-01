package protocol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

public class PacketWriter
{
	private ByteArrayOutputStream bout;
	private ObjectOutputStream oout;
	
	public PacketWriter() throws IOException
	{
		bout = new ByteArrayOutputStream();
		oout = new ObjectOutputStream(bout);
	}
	
	public byte[] getSerializedData(ISendable sendable) throws IOException
	{
		oout.reset();
		bout.reset();
		oout.writeObject(sendable);
		return bout.toByteArray();
	}
}
