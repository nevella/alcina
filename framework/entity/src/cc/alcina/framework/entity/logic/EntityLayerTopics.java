package cc.alcina.framework.entity.logic;

import cc.alcina.framework.common.client.util.TopicPublisher.Topic;

public class EntityLayerTopics {
	public static Topic<ClusterStatistics> clusterStatistics = Topic.local();

	public static class ClusterStatistics {
		public int serverIndex;

		public int serverCount;
	}
}
