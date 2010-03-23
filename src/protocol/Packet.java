package protocol;

import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;

public class Packet
{
    public static final String DELIMITER = "\n";
    private String type;
    private Map<String,Attribute> attributes;
    private String body;
    
    // empty packet
    public Packet(String type)
    {
        this.type = type;
        this.attributes = new TreeMap<String, Attribute>();
        this.body = null;
    }

	public static Packet parsePacket(String data)
    {
        StringTokenizer t = new StringTokenizer(data, DELIMITER, true);
        
        if (!t.hasMoreTokens())
            return null;
        
        String typeEscaped = t.nextToken();
        Packet p = new Packet(typeEscaped.replace("\\n", "\n"));
        
        if (!t.hasMoreTokens() || t.nextToken() != DELIMITER)
            return null;
        
        while (t.hasMoreTokens())
        {
            // Extra \n indicates either end of packet or body, so break attribute parsing loop.
            String token = t.nextToken();
            if (token == DELIMITER)
                break;
            
            // Not a \n, so parse the attribute.
            Attribute a = Attribute.parseAttribute(token);
            
            // But only add if the line is terminated properly and attribute is valid.
            if (a == null || !t.hasMoreTokens() || t.nextToken() != DELIMITER)
                return null;
            else
                p.set(a);
        }
        
        if (t.hasMoreTokens())
        {
            // If there's a body to the message
            StringBuilder body = new StringBuilder();
            do
            {
                body.append(t.nextToken());
            } while (t.hasMoreTokens());
            p.setBody(body.toString());
        }
        
        return p;
    }
    
    @Override
    public String toString()
    {
        StringBuilder bldr = new StringBuilder();
        
        // Type
        String typeEscaped = type.replace("\n", "\\n");
        bldr.append(typeEscaped);
        bldr.append('\n');
        
        // Attributes
        for (Attribute a : this.attributes.values())
        {
            bldr.append(a.toString());
            bldr.append('\n');
        }
        
        // Extra newline to mark end of header
        bldr.append('\n');
        
        // Body is optional
        if (this.body != null)
            bldr.append(this.body);
        
        return bldr.toString();
    }
    
    public String getType()
    {
        return this.type;
    }
    
    public Set<String> getAttributeNames()
    {
        return this.attributes.keySet();
    }
    
    public Attribute get(String attributeName)
    {
        return this.attributes.get(attributeName);
    }
    
    public String getBody()
    {
        return this.body;
    }
    
    public void setType(String type)
    {
        this.type = type;
    }
    
    public void set(Attribute attribute)
    {
        this.attributes.put(attribute.getName(), attribute);
    }
    
    public void setBody(String body)
    {
        this.body = body;
    }
}
