package cc.alcina.framework.entity.control;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.entity.ResourceUtilities;

public class ClusterStateProviderFile implements ClusterStateProvider {
	public static final String SYS_PROP_CONFIG_FILE_PATH = ClusterStateProviderFile.class
			.getName() + ".SYS_PROP_CONFIG_FILE_PATH";

	private String configFilePath;

	public ClusterStateProviderFile() {
		configFilePath = System.getProperty(SYS_PROP_CONFIG_FILE_PATH);
	}

	@Override
	public ClusterState getClusterState(String clusterId) throws Exception {
		StringMap props = StringMap.fromPropertyString(
				ResourceUtilities.readFileToString(configFilePath), true);
		ClusterState state = new ClusterState();
		state.setClusterId(clusterId);
		state.setCurrentWriterHost(props.get(clusterId + "_currentWriterHost"));
		state.setPreferredWriterHost(props.get(clusterId
				+ "_preferredWriterHost"));
		String hostString = props.get(clusterId + "_allHosts");
		List<String> hosts = new ArrayList<String>(Arrays.asList(hostString
				.split(",\\s*")));
		state.setAllHosts(hosts);
		return state;
	}

	@Override
	public void persistClusterState(ClusterState state) {
		StringMap props =new StringMap();
		String clusterId = state.getClusterId();
		props.put(clusterId+"_clusterId", clusterId);
		props.put(clusterId+"_currentWriterHost", state.getCurrentWriterHost());
		props.put(clusterId+"_preferredWriterHost", state.getPreferredWriterHost());
		props.put(clusterId+"_clusterId", clusterId);
	}
}
