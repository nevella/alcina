package cc.alcina.extras.cluster;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

public class PauseJkWorker extends Task {
	private String workerName;

	private String jkStatusUrl;

	private String balancedWorkerName;

	@Override
	public void execute() throws BuildException {
		String url = String.format("%s?cmd=update&from=list&w=%s&sw=%s&vwa=1",
				getJkStatusUrl(), getBalancedWorkerName(), getWorkerName());
		try {
			log("reading " + url);
			Utils.readUrlAsString(url);
		} catch (Exception e) {
			log(e.getMessage() + " reading " + url, Project.MSG_WARN);
		}
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
