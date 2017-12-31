package cc.alcina.framework.common.client.log;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cc.alcina.framework.common.client.util.CommonUtils;

public class TaggedLogger {
	public static final transient String METRIC = "metric";

	public static final transient String DEBUG = "debug";

	public static final transient String WARN = "warn";

	public static final transient String INFO = "info";

	int registrationCounter = 0;

	List<TaggedLoggerRegistration> registrations = new ArrayList<TaggedLoggerRegistration>();

	private TaggedLoggers taggedLoggers;

	Class clazz;

	Object[] tags;

	Map<String, Long> starts;

	public TaggedLogger(TaggedLoggers taggedLoggers, Class clazz,
			Object[] tags) {
		this.taggedLoggers = taggedLoggers;
		this.clazz = clazz;
		this.tags = tags;
	}

	public void format(String string, Object... args) {
		log(CommonUtils.formatJ(string, args));
	}

	public boolean hasRegistrations() {
		taggedLoggers.updateRegistrations(this);
		return !registrations.isEmpty();
	}

	public synchronized void log(String message) {
		taggedLoggers.updateRegistrations(this);
		for (TaggedLoggerRegistration registration : registrations) {
			registration.handler.log(message);
		}
	}

	public synchronized void metric(String key) {
		if (starts == null) {
			starts = new LinkedHashMap<>();
		}
		starts.put(key, System.currentTimeMillis());
	}

	public synchronized void metricEnd(String key) {
		if (!starts.containsKey(key)) {
			throw new RuntimeException("metric end without start - " + key);
		}
		long duration = System.currentTimeMillis() - starts.get(key);
		log(CommonUtils.formatJ("Metric - %s - %s: %s ms",
				clazz.getSimpleName(), key, duration));
	}

	public static interface TaggedLoggerHandler {
		void log(String message);
	}
}