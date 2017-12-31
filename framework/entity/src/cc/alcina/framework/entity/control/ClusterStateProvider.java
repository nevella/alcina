package cc.alcina.framework.entity.control;

public interface ClusterStateProvider {
	ClusterState getClusterState(String clusterId) throws Exception;

	default String getMemberClusterState() {
		throw new UnsupportedOperationException();
	}

	default String getVmHealth() {
		throw new UnsupportedOperationException();
	}

	void persistClusterState(ClusterState state) throws Exception;
}
