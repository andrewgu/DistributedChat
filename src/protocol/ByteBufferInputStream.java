package protocol;

import java.io.IOException;
import java.io.InputStream;

public class ByteBufferInputStream extends InputStream
{
	private byte[] dataBuffer;
	private int offset;
	private int length;
	private int mark;
	
	public ByteBufferInputStream()
	{
		dataBuffer = null;
		offset = 0;
		length = 0;
		mark = 0;
	}
	
	public void setBytes(byte[] data, int offset, int length)
	{
		this.dataBuffer = data;
		this.offset = offset;
		this.length = length;
		this.mark = 0;
	}
	
	public void repeat()
	{
		this.mark = 0;
	}

	@Override
	public int read() throws IOException
	{
		if (mark < length)
		{
			byte data = dataBuffer[offset + mark];
			mark++;
			return data;
		}
		else
		{
			return -1;
		}
	}

}
