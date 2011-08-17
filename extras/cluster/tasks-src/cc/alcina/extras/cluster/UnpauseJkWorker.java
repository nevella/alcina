package cc.alcina.extras.cluster;

import java.io.BufferedReader;
import java.io.File;
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
	
	private int timeout=30;
	
	private String redeployedMarkerFile;

	@Override
	public void execute() throws BuildException {
		File marker = new File(redeployedMarkerFile);
		if(marker.exists()){
			marker.delete();
		}
		log(redeployedMarkerFile);
		while (!marker.exists()&&timeout-->0){
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
			log("..."+timeout);
		}
		unpause();
	}

	protected void unpause() {
		String url = String.format("%s?cmd=update&from=list&w=%s&sw=%s&vwa=0",
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

	public int getTimeout() {
		return this.timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public String getRedeployedMarkerFile() {
		return this.redeployedMarkerFile;
	}

	public void setRedeployedMarkerFile(String redeployedMarkerFile) {
		this.redeployedMarkerFile = redeployedMarkerFile;
	}
}
