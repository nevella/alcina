package cc.alcina.framework.servlet.servlet.control;

import java.io.IOException;

import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.entity.ResourceUtilities;

public class ClusterStateProviderFile implements ClusterStateProvider {
	public static final String CONTEXT_CONFIG_FILE_PATH = ClusterStateProviderFile.class
			.getName() + ".CONTEXT_CONFIG_FILE_PATH";

	private String configFilePath;

	public ClusterStateProviderFile() {
		configFilePath = LooseContext.getString(CONTEXT_CONFIG_FILE_PATH);
	}

	@Override
	public ClusterState getClusterState(String clusterId) throws Exception {
		StringMap props = StringMap.fromPropertyString(
				ResourceUtilities.readFileToString(configFilePath), true);
		ClusterState state =new ClusterState();
		//TODO
		return state;
	}

	@Override
	public void persistClusterState(ClusterState state) {
		// TODO Auto-generated method stub
	}
}
