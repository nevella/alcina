package cc.alcina.framework.common.client.log;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.util.CommonUtils;

public class TaggedLogger {
	public static final transient TaggedLoggerTag METRIC = TaggedLoggerTagStandard.METRIC;

	public static final transient TaggedLoggerTag DEBUG = TaggedLoggerTagStandard.DEBUG;

	public static final transient TaggedLoggerTag WARN = TaggedLoggerTagStandard.WARN;

	public static final transient TaggedLoggerTag INFO = TaggedLoggerTagStandard.INFO;

	int registrationCounter = 0;

	List<TaggedLoggerRegistration> registrations = new ArrayList<TaggedLoggerRegistration>();

	private TaggedLoggers taggedLoggers;

	Class clazz;

	TaggedLoggerTag[] tags;

	Map<String, Long> starts;

	private TaggedLogger infoLogger;

	private TaggedLogger debugLogger;

	public TaggedLogger(TaggedLoggers taggedLoggers, Class clazz,
			TaggedLoggerTag[] tags) {
		this.taggedLoggers = taggedLoggers;
		this.clazz = clazz;
		this.tags = tags;
	}

	public TaggedLogger debug() {
		if (debugLogger == null) {
			Preconditions.checkState(tags.length == 0);
			synchronized (this) {
				if (debugLogger == null) {
					debugLogger = taggedLoggers.getLogger(clazz, DEBUG);
				}
			}
		}
		return debugLogger;
	}

	public boolean hasRegistrations() {
		taggedLoggers.updateRegistrations(this);
		return !registrations.isEmpty();
	}

	public TaggedLogger info() {
		if (infoLogger == null) {
			Preconditions.checkState(tags.length == 0);
			synchronized (this) {
				if (infoLogger == null) {
					infoLogger = taggedLoggers.getLogger(clazz, INFO);
				}
			}
		}
		return infoLogger;
	}

	public synchronized void log(String message) {
		taggedLoggers.updateRegistrations(this);
		for (TaggedLoggerRegistration registration : registrations) {
			registration.handler.log(message);
		}
	}

	public void message(String string, Object... args) {
		taggedLoggers.updateRegistrations(this);
		if (registrations.size() > 0) {
			log(CommonUtils.formatJ(string, args));
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