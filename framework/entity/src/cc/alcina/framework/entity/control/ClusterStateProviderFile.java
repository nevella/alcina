package cc.alcina.framework.entity.control;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.entity.ResourceUtilities;

public class ClusterStateProviderFile implements ClusterStateProvider {
	public static final String SYS_PROP_CONFIG_FILE_PATHS = ClusterStateProviderFile.class
			.getName() + ".SYS_PROP_CONFIG_FILE_PATHS";

	private String configFilePaths;

	public ClusterStateProviderFile() {
		configFilePaths = System.getProperty(SYS_PROP_CONFIG_FILE_PATHS);
	}

	@Override
	public ClusterState getClusterState(String forClusterId) throws Exception {
		for (String path : configFilePaths.split(",")) {
			StringMap props = StringMap.fromPropertyString(
					ResourceUtilities.readFileToString(path), true);
			String clusterId = props.get("clusterId");
			if (clusterId.equals(forClusterId)) {
				ClusterState state = new ClusterState();
				state.setClusterId(clusterId);
				state.setCurrentWriterHost(props.get("currentWriterHost"));
				state.setPreferredWriterHost(props.get("preferredWriterHost"));
				String hostString = props.get("allHosts");
				List<String> hosts = new ArrayList<String>(
						Arrays.asList(hostString.split(",\\s*")));
				state.setAllHosts(hosts);
				state.setHttpProxyBalancerUrl(props.get("httpProxyBalancerUrl"));
				state.setHttpsProxyBalancerUrl(props.get("httpsProxyBalancerUrl"));
				state.setProxyToHttpPort(props.get("proxyToHttpPort"));
				state.setProxyToHttpsPort(props.get("proxyToHttpsPort"));
				state.setTestUrl(props.get("testUrl"));
				return state;
			}
		}
		throw new Exception(String.format("Cluster %s not found", forClusterId));
	}

	@Override
	public void persistClusterState(ClusterState state) {
		StringMap props = new StringMap();
		String clusterId = state.getClusterId();
		props.put(clusterId + "_clusterId", clusterId);
		props.put(clusterId + "_currentWriterHost",
				state.getCurrentWriterHost());
		props.put(clusterId + "_preferredWriterHost",
				state.getPreferredWriterHost());
		props.put(clusterId + "_clusterId", clusterId);
	}
}
