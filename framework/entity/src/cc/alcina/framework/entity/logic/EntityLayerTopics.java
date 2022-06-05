package cc.alcina.framework.entity.logic;

import cc.alcina.framework.common.client.util.Topic;

public class EntityLayerTopics {
	public static final Topic<ClusterStatistics> clusterStatistics = Topic.create();

	public static class ClusterStatistics {
		public int serverIndex;

		public int serverCount;
	}
}
