package java.io;

import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectStreamConstants;

public class InputStream 
{ 
	 /** The next position to read. */
    private int position;

    /** The text to stream. */
    protected String text;

    public InputStream() {
        super();
    }

    /**
     * Constructor.
     * 
     * @param text
     */
    public InputStream(String text) {
        super();
        this.position = 0;
        this.text = text;
    }

    /**
     * 
     * @return
     * @throws IOException
     */
    public int available() throws IOException {
        if (text != null) {
            return text.length();
        }

        return 0;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void close() throws IOException {

    }

    /**
     * Reads the next character in the source text.
     * 
     * @return The next character or -1 if end of text is reached.
     * @throws IOException
     */
    public int read() throws IOException {
        return (this.position == this.text.length()) ? -1 : this.text
                .charAt(this.position++);
    }

    /**
     * 
     * @param cbuf
     * @return
     * @throws IOException
     */
    public int read(char[] cbuf) throws IOException {
        return read(cbuf, 0, cbuf.length);
    }

    /**
     * 
     * @param cbuf
     * @param off
     * @param len
     * @return
     * @throws IOException
     */
    public int read(char[] cbuf, int off, int len) throws IOException {
        if (position >= text.length())
            return -1;
        int n = Math.min(text.length() - position, len);
        text.getChars(position, position + n, cbuf, off);
        position += n;
        return n;
    }
	
}