package cc.alcina.framework.common.client.log;

import java.util.List;

public class TaggedLogger {
	private List<TaggedLoggerRegistration> registrations;

	public TaggedLogger(List<TaggedLoggerRegistration> registrations) {
		this.registrations = registrations;
	}

	public void log(String message) {
		for (TaggedLoggerRegistration registration : registrations) {
			registration.handler.log(message);
		}
	}

	public static interface TaggedLoggerHandler {
		void log(String message);
	}
}