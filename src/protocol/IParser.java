package protocol;

import java.io.IOException;

public interface IParser<T>
{
	T parse(String value) throws IOException;
}
