package cc.alcina.framework.common.client.log;

import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.common.client.util.CommonUtils;

public class TaggedLogger {
	public static final transient String METRIC = "metric";

	public static final transient String DEBUG = "debug";

	public static final transient String WARN = "warn";

	public static final transient String INFO = "info";

	int registrationCounter=0;

	 List<TaggedLoggerRegistration> registrations=new ArrayList<TaggedLoggerRegistration>();

	private TaggedLoggers taggedLoggers;

	Class clazz;

	Object[] tags;

	public TaggedLogger(TaggedLoggers taggedLoggers, Class clazz, Object[] tags) {
		this.taggedLoggers = taggedLoggers;
		this.clazz = clazz;
		this.tags = tags;
	}

	public boolean hasRegistrations(){
		taggedLoggers.updateRegistrations(this);
		return !registrations.isEmpty();
	}
	public synchronized void log(String message) {
		taggedLoggers.updateRegistrations(this);
		for (TaggedLoggerRegistration registration : registrations) {
			registration.handler.log(message);
		}
	}


	public static interface TaggedLoggerHandler {
		void log(String message);
	}

	public void format(String string, Object...args) {
		log(CommonUtils.formatJ(string,args));

	}
}