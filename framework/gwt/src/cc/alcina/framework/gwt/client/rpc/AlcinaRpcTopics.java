package cc.alcina.framework.gwt.client.rpc;

import cc.alcina.framework.common.client.logic.domaintransform.ClientInstanceExpiredException;
import cc.alcina.framework.common.client.util.Topic;

public class AlcinaRpcTopics {
	public static final Topic<ClientInstanceExpiredExceptionToken> topicClientInstanceExpiredException = Topic
			.create();

	public static class ClientInstanceExpiredExceptionToken {
		public ClientInstanceExpiredException exception;

		public boolean handled;
	}
}
