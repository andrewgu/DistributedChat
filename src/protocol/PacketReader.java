package protocol;

import java.io.IOException;
import java.io.ObjectInputStream;

public class PacketReader
{
	private ObjectInputStream ois;
	private ByteBufferInputStream bis;
	
	public PacketReader()
	{
		bis = new ByteBufferInputStream();
		ois = null;
	}
	
	public boolean isReady()
	{
		return ois != null;
	}
	
	public void setSerializationHeader(byte[] data, int offset, int length) throws IOException
	{
		bis.setBytes(data, offset, length);
		ois = new ObjectInputStream(bis);
	}
	
	public void setBytes(byte[] data, int offset, int length)
	{
		bis.setBytes(data, offset, length);
	}
	
	public ISendable readObject() throws IOException
	{
		try
		{
			bis.repeat();
			return (ISendable)ois.readObject();
			//return (ISendable)ois.readUnshared();
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
			if (ois != null)
				ois.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
}
