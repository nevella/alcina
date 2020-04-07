package cc.alcina.framework.entity.control;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

public interface ClusterStateProvider {
	static ClusterStateProvider get() {
		return Registry.impl(ClusterStateProvider.class);
	}

	String getClusterLeaderState();

	default String getMemberClusterState() {
		throw new UnsupportedOperationException();
	}

	default String getVmHealth() {
		throw new UnsupportedOperationException();
	}

	long getVmInstancedId(String launcherName);

	boolean isRestarting(String owner);

	default boolean isVmHealthyCached() {
		return true;
	}
}
