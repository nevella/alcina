package cc.alcina.extras.webdriver.api;

import java.io.IOException;
import java.io.PrintStream;
import java.io.StringWriter;
import java.io.Writer;

import javax.servlet.http.HttpServletResponse;

import cc.alcina.framework.entity.ResourceUtilities;

public class WDWriter {
	private Writer storageWriter = new StringWriter();

	private PrintStream consoleWriter = System.out;

	private HttpServletResponse resp;

	private boolean statsOnly;

	public Writer getStorageWriter() {
		return this.storageWriter;
	}

	public void setStorageWriter(Writer storageWriter) {
		this.storageWriter = storageWriter;
	}

	public PrintStream getConsoleWriter() {
		return this.consoleWriter;
	}

	public void setConsoleWriter(PrintStream consoleWriter) {
		this.consoleWriter = consoleWriter;
	}

	public HttpServletResponse getResp() {
		return this.resp;
	}

	public void setResp(HttpServletResponse resp) {
		this.resp = resp;
	}

	public WDWriter() {
	}

	public void write(String s, int level) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < level; i++) {
			sb.append("-- ");
		}
		sb.append(s);
		s = sb.toString();
		if (isStatsOnly()) {
			if (ResourceUtilities.getBoolean(WDWriter.class,
					"logStatsToSysOut")) {
				System.out.println(s);
			}
			return;
		}
		try {
			if (resp != null) {
				resp.getWriter().write(s.replace("\n", "<br />\n"));
				resp.getWriter().flush();
				resp.flushBuffer();
			} else {
				consoleWriter.print(s);
			}
			storageWriter.write(s);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void setStatsOnly(boolean statsOnly) {
		this.statsOnly = statsOnly;
	}

	public boolean isStatsOnly() {
		return statsOnly;
	}
}
