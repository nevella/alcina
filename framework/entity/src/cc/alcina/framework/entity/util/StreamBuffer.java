package cc.alcina.framework.entity.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class StreamBuffer extends Thread {
	InputStream is;

	String type;
	
	StringBuilder buf=new StringBuilder();

	public StreamBuffer(InputStream is, String type) {
		this.is = is;
		this.type = type;
	}

	public void run() {
		try {
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			String line = null;
			while ((line = br.readLine()) != null){
				if(buf.length()>0){
					buf.append("\n");
				}
				buf.append(line);
				System.out.println(type + ">" + line);
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	public StringBuilder getBuf() {
		return this.buf;
	}
}