package test;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import protocol.ISendable;
import protocol.PacketReader;
import protocol.PacketWriter;
import protocol.packets.FindRoom;

public class TestSerialization
{
    /**
     * @param args
     * @throws IOException 
     * @throws ClassNotFoundException 
     */
    public static void main(String[] args) throws IOException, ClassNotFoundException
    {
    	System.out.println("Starting basic serialization test.");
    	
        FindRoom p = new FindRoom("room!", 1);
        PipedOutputStream out = new PipedOutputStream();
        PipedInputStream in = new PipedInputStream(out);
        ObjectOutputStream oout = new ObjectOutputStream(out);
        ObjectInputStream oin = new ObjectInputStream(in);
        
        oout.writeObject(p);
        Object o = oin.readObject();
        
        if (o instanceof ISendable)
        	System.out.println("is ISendable");
        
        if (o instanceof FindRoom)
        	System.out.println("is FindRoom");
        
        FindRoom f = (FindRoom) o;
        if (f == p)
        	System.out.println("Same reference!");
        
        System.out.println (f.getPacketType().toString());
        System.out.println (f.getRoom());
        
        out.close();
        in.close();
        
        System.out.println("Starting PacketReader and PacketWriter tests.");
        
        p = new FindRoom("room!", 2);
        out = new PipedOutputStream();
        in = new PipedInputStream(out);
        DataOutputStream dout = new DataOutputStream(out);
        DataInputStream din = new DataInputStream(in);
        
        PacketReader reader = new PacketReader();
        PacketWriter writer = new PacketWriter();
        
        byte[] header = writer.getSerializationHeader();
        reader.setSerializationHeader(header, 0, header.length);
        
        byte[] data = writer.getSerializedData(p);
        dout.writeInt(data.length);
        dout.write(data, 0, data.length);
        
        int len = din.readInt();
        byte[] buf = new byte[len];
        int read = 0;
        while (read < len)
        {
        	read += din.read(buf, read, len - read);
        }
        
        reader.setBytes(buf, 0, len);
        ISendable s = reader.readObject();
        
        if (s instanceof FindRoom)
        	System.out.println("is FindRoom");
    
	    f = (FindRoom) s;
	    if (f == p)
	    	System.out.println("Same reference!");
	    
	    System.out.println (f.getPacketType().toString());
	    System.out.println (f.getRoom());
	    
	    out.close();
	    in.close();
	    reader.close();
	    writer.close();
    }
}
