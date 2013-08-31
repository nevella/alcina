package cc.alcina.framework.entity.control;

public interface ClusterStateProvider {
	ClusterState getClusterState(String clusterId) throws Exception;
	void persistClusterState(ClusterState state) throws Exception;
}
