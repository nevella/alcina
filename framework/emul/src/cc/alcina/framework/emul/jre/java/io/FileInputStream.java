package java.io;

import java.io.IOException;
import java.io.InputStream;

public class FileInputStream 
    extends InputStream 
{ 
	public FileInputStream(File file)  {
		throw new UnsupportedOperationException();
	}
	@Override
    public int read() throws IOException {
        throw new UnsupportedOperationException();
    }
}