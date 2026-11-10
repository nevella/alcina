package cc.alcina.framework.common.client.flight;

import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer.ReflectiveSerializable;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer.SerializerOptions;
import cc.alcina.framework.common.client.util.HasStringRepresentation;

public interface FlightEventWrappable
		extends ReflectiveSerializable, HasStringRepresentation {
	String getSessionId();

	default long provideDuration() {
		return 0;
	}

	default long provideStart() {
		return 0;
	}

	/*
	 * subcategory of the event
	 */
	default String provideSubcategory() {
		return null;
	}

	/*
	 * detail of the event
	 */
	default String provideDetail() {
		return null;
	}

	/*
	 * a string representation of the event (such as its full json)
	 */
	default String provideStringRepresentation() {
		return serialize(this);
	}

	static String serialize(Object o) {
		return ReflectiveSerializer.serialize(o,
				new SerializerOptions().withPretty(true).withElideDefaults(true)
						.withDefaultCollectionTypes(true));
	}

	@Bean(PropertySource.FIELDS)
	public static class FlightExceptionMessage implements FlightEventWrappable {
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

	default byte[] provideOutputBytes() {
		return new byte[0];
	}

	default byte[] provideInputBytes() {
		return new byte[0];
	}
}
