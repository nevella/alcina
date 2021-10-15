package cc.alcina.framework.entity.control;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import cc.alcina.framework.common.client.util.Ax;

public class ClusterDefinition {
	public String clusterId;

	public String balancerName;

	public String balancerUrl;

	public String zkHostPort;

	public String testName;

	public String bpxGroup;

	public boolean supportsClusterHealth;

	public boolean publicCluster;

	public String zkNamespace;

	public List<ClusterMember> clusterMembers = new ArrayList<>();

	public String buildArtifactPath;

	public String testSshUserName;

	public String testSshHost;

	public String testCommand;

	public ClusterMember memberByName(String hostName) {
		Optional<ClusterMember> member = clusterMembers.stream()
				.filter(m -> m.hostName.equals(hostName)).findFirst();
		if (!member.isPresent()) {
			throw Ax.runtimeException("Host not in cluster definition: %s",
					hostName);
		}
		return member.get();
	}

	public ClusterMember provideCompilaationMember() {
		return clusterMembers.stream().filter(m -> m.compilationMember).findFirst()
				.get();
	}

	public static class ClusterMember {
		public String containerName;

		public String dockerHostName;

		public String comment;

		public String sshUserName = "root";

		public String deploymentPath;

		public String restartMarker;

		public String hostName;

		public boolean compilationMember;

		public boolean proxiedTo;

		public String tunnelAliasTo;

		public String getBpxConfigurationFilePath() {
			return Ax.format("/g/barpub/provisioning/dk/container/%s/%s.json",
					containerName.replace(".", "/"), containerName);
		}

		public String getBpxPropertiesFilePath() {
			String suffix = containerName.contains("portal") ? ".template" : "";
			return Ax.format(
					"/g/barpub/provisioning/dk/container/%s/%s.properties%s",
					containerName.replace(".", "/"), containerName, suffix);
		}

		@Override
		public String toString() {
			return hostName;
		}
	}
}
