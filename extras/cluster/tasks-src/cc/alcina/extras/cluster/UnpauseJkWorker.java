package cc.alcina.extras.cluster;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URL;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

public class UnpauseJkWorker extends Task {
	private String workerName;

	private String jkStatusUrl;

	private String balancedWorkerName;

	@Override
	public void execute() throws BuildException {
		String url = String.format("%s?cmd=update&from=list&w=%s&sw=%s&vwa=1",
				getJkStatusUrl(), getBalancedWorkerName(), getWorkerName());
		try {
			log("reading "+url);
			readUrlAsString(url);
		} catch (Exception e) {
			throw new BuildException(e);
		}
	}

	public static String readUrlAsString(String strUrl) throws Exception {
		URL url = new URL(strUrl);
		InputStream is = null;
		is = url.openConnection().getInputStream();
		String input = readStreamToString(is, "UTF-8");
		return input;
	}

	public static String readStreamToString(InputStream is, String charsetName)
			throws IOException {
		charsetName = charsetName == null ? "UTF-8" : charsetName;
		BufferedReader in = new BufferedReader(new InputStreamReader(is,
				charsetName));
		StringWriter sw = new StringWriter();
		char[] cb = new char[4096];
		int len = -1;
		while ((len = in.read(cb, 0, 4096)) != -1) {
			sw.write(cb, 0, len);
		}
		is.close();
		return sw.toString();
	}

	public String getJkStatusUrl() {
		return this.jkStatusUrl;
	}

	public void setJkStatusUrl(String jkStatusUrl) {
		this.jkStatusUrl = jkStatusUrl;
	}

	public String getWorkerName() {
		return this.workerName;
	}

	public void setWorkerName(String workerName) {
		this.workerName = workerName;
	}

	public void setBalancedWorkerName(String balancedWorkerName) {
		this.balancedWorkerName = balancedWorkerName;
	}

	public String getBalancedWorkerName() {
		return balancedWorkerName;
	}
}
