package cc.alcina.framework.entity.control;

import java.util.ArrayList;
import java.util.List;

public class ClusterDefinition {
    public String clusterId;

    public String balancerName;

    public String balancerUrl;

    public String zkHostPort;

    public List<ClusterMember> clusterMembers = new ArrayList<>();

    public ClusterMember memberByName(String hostName) {
        return clusterMembers.stream().filter(m -> m.hostName.equals(hostName))
                .findFirst().get();
    }

    public ClusterMember provideCurrentWriterHost() {
        return clusterMembers.stream().filter(m -> m.writerHost).findFirst()
                .get();
    }

    public static class ClusterMember {
        public String containerName;

        public String dockerHostName;

        public String deploymentPath;

        public String restartMarker;

        public String hostName;

        public boolean writerHost;

        public boolean proxiedTo;

        public String tunnelAliasTo;
    }
}
