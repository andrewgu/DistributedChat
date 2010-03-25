package protocol;

public class Attribute
{
    private String name;
    private String value;
    
    public Attribute(String name, String value)
    {
        this.name = name;
        this.value = value;
    }
    
    public Attribute(String name, int value)
    {
        this.name = name;
        this.value = Integer.toString(value);
    }
    
    public Attribute(String name, long value)
    {
        this.name = name;
        this.value = Long.toString(value);
    }
    
    public Attribute(String name, float value)
    {
        this.name = name;
        this.value = Float.toString(value);
    }
    
    public Attribute(String name, double value)
    {
        this.name = name;
        this.value = Double.toString(value);
    }
    
    public <T> Attribute(String name, T obj)
    {
    	this.name = name;
    	this.value = obj.toString();
    }
    
    @Override
    public String toString()
    {
        String escapedName = this.name.replace(":", "\\#").replace("\n", "\\n");
        String escapedValue = this.value.replace(":", "\\#").replace("\n", "\\n");
        return escapedName + ":" + escapedValue;
    }
    
    public static Attribute parseAttribute(String line)
    {
        String[] parts = line.split(":");

        if (parts.length != 2)
        {
            return null;
        }
        else 
        {
            String unescapedName = parts[0].replace("\\#", ":").replace("\\n", "\n");
            String unescapedValue = parts[1].replace("\\#", ":").replace("\\n", "\n");
            return new Attribute(unescapedName, unescapedValue);
        }
    }

    public String getName()
    {
        return name;
    }

    public String getString()
    {
        return value;
    }
    
    public String[] getSplit(String splitRegex)
    {
    	return value.split(splitRegex);
    }
    
    public int getInt()
    {
        return Integer.parseInt(value);
    }
    
    public long getLong()
    {
        return Long.parseLong(value);
    }
    
    public float getFloat()
    {
        return Float.parseFloat(value);    
    }
    
    public double getDouble()
    {
        return Double.parseDouble(value);
    }
}
