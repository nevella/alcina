package cc.alcina.framework.common.client.flight;

import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer.ReflectiveSerializable;

public interface HasSessionId extends ReflectiveSerializable {
	String getSessionId();

	@Bean(PropertySource.FIELDS)
	public static class FlightExceptionMessage implements HasSessionId {
		public FlightExceptionMessage() {
		}

		public FlightExceptionMessage(String sessionId, String body) {
			this.sessionId = sessionId;
			this.body = body;
		}

		public String sessionId;

		public String getSessionId() {
			return sessionId;
		}

		public String body;
	}
}
