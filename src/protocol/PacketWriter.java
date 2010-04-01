package protocol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

public class PacketWriter
{
	private ByteArrayOutputStream bout;
	private ObjectOutputStream oout;
	
	public PacketWriter()
	{
		bout = new ByteArrayOutputStream();
		oout = null;
	}
	
	public boolean isReady()
	{
		return oout != null;
	}
	
	public byte[] getSerializationHeader() throws IOException
	{
		oout = new ObjectOutputStream(bout);
		return bout.toByteArray();
	}
	
	public byte[] getSerializedData(ISendable sendable) throws IOException
	{
		oout.reset();
		bout.reset();
		oout.writeObject(sendable);
		return bout.toByteArray();
	}
	
	public void close()
	{
		try
		{
			oout.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
}
