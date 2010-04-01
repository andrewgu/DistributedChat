package protocol;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;

public class Packet
{
	private byte[] packet;
	
	public Packet(ISendable data) throws IOException
	{
		ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
		ObjectOutputStream objectOut = new ObjectOutputStream(byteOut);
		objectOut.writeObject(data);
		objectOut.close();
		packet = byteOut.toByteArray();
	}
	
	public Packet(byte[] data, int offset, int length)
	{
		packet = new byte[length];
		for (int i = 0; i < length; i++)
			packet[i] = data[offset+i];
	}
	
	public int getLength()
	{
		return packet.length;
	}
	
	public byte[] getByteArray()
	{
		return packet;
	}
	
	public ByteBuffer getNewByteBuffer()
	{
		return ByteBuffer.wrap(packet);
	}
	
	public ISendable getObject() throws IOException
	{
		ByteArrayInputStream byteIn = new ByteArrayInputStream(packet);
		ObjectInputStream objectIn = new ObjectInputStream(byteIn);
		
		try
		{
			return (ISendable)objectIn.readObject();
		}
		catch (ClassNotFoundException e)
		{
			throw new IOException("ClassNotFoundException.", e);
		}
	}
}
