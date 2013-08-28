package cc.alcina.framework.servlet.servlet.control;

public interface ClusterStateProvider {
	ClusterState getClusterState(String clusterId) throws Exception;
	void persistClusterState(ClusterState state) throws Exception;
}
