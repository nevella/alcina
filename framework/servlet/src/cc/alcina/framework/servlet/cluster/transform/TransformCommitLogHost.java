package cc.alcina.framework.servlet.cluster.transform;

import java.util.Properties;

public interface TransformCommitLogHost {
	Properties createConsumerProperties(String groupId);

	Properties
			createProducerProperties(Class<? extends TransformCommitLog> clazz);

	long getPollTimeout();

	String getTopic();
}
