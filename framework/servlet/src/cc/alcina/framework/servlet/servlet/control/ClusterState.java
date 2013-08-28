package cc.alcina.framework.servlet.servlet.control;

import java.util.List;

public class ClusterState {
	private String clusterId;

	private String currentWriterHost;

	private List<String> allHosts;

	private String preferredWriterHost;

	public String getCurrentWriterHost() {
		return this.currentWriterHost;
	}

	public void setCurrentWriterHost(String currentWriterHost) {
		this.currentWriterHost = currentWriterHost;
	}

	public List<String> getAllHosts() {
		return this.allHosts;
	}

	public void setAllHosts(List<String> allHosts) {
		this.allHosts = allHosts;
	}

	public String getPreferredWriterHost() {
		return this.preferredWriterHost;
	}

	public void setPreferredWriterHost(String preferredWriterHost) {
		this.preferredWriterHost = preferredWriterHost;
	}

	public String getClusterId() {
		return this.clusterId;
	}

	public void setClusterId(String clusterId) {
		this.clusterId = clusterId;
	}
}
