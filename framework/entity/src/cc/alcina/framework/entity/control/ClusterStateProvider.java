package cc.alcina.framework.entity.control;

public interface ClusterStateProvider {
    default String getMemberClusterState() {
        throw new UnsupportedOperationException();
    }

    default String getVmHealth() {
        throw new UnsupportedOperationException();
    }

    default boolean isVmHealthyCached() {
        return true;
    }
}
