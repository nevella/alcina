package cc.alcina.framework.gwt.client.rpc;

import cc.alcina.framework.common.client.logic.domaintransform.ClientInstanceExpiredException;
import cc.alcina.framework.common.client.util.TopicPublisher.TopicSupport;

public class AlcinaRpcTopics {
	private static final String TOPIC_CLIENT_INSTANCE_EXPIRED_EXCEPTION = AlcinaRpcTopics.class
			.getName() + ".TOPIC_CLIENT_INSTANCE_EXPIRED_EXCEPTION";

	public static TopicSupport<ClientInstanceExpiredExceptionToken>
			topicClientInstanceExpiredException() {
		return new TopicSupport<>(TOPIC_CLIENT_INSTANCE_EXPIRED_EXCEPTION);
	}

	public static class ClientInstanceExpiredExceptionToken {
		public ClientInstanceExpiredException exception;

		public boolean handled;
	}
}
