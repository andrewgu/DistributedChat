package protocol;

import java.io.IOException;
import java.io.ObjectInputStream;

public class PacketReader
{
	private ObjectInputStream ois;
	private ByteBufferInputStream bis;
	
	public PacketReader() throws IOException
	{
		bis = new ByteBufferInputStream();
		ois = new ObjectInputStream(bis);
	}
	
	public void setBytes(byte[] data, int offset, int length)
	{
		bis.setBytes(data, offset, length);
	}
	
	public ISendable readPacket() throws IOException
	{
		try
		{
			return (ISendable)ois.readObject();
		}
		catch (ClassNotFoundException e)
		{
			throw new IOException("Class not found.", e);
		}
	}
	
	public void close()
	{
		try
		{
			ois.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
}
