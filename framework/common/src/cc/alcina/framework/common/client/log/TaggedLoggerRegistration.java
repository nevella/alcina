package cc.alcina.framework.common.client.log;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import cc.alcina.framework.common.client.log.TaggedLogger.TaggedLoggerHandler;

public class TaggedLoggerRegistration {
	Class clazz;

	Set<Object> tags;

	TaggedLoggerHandler handler;

	private TaggedLoggers taggedLoggers;

	public TaggedLoggerRegistration(Class clazz, Object[] tags,
			TaggedLoggerHandler handler, TaggedLoggers taggedLoggers) {
		this.clazz = clazz;
		this.taggedLoggers = taggedLoggers;
		this.tags = new LinkedHashSet(Arrays.asList(tags));
		this.handler = handler;
	}

	public boolean subscribesTo(Class clazz, Object[] tags) {
		if (clazz != null && clazz != this.clazz) {
			return false;
		}
		if(this.tags.isEmpty()||tags.length==0){
			return true;
		}
		for (Object tag : tags) {
			if (this.tags.contains(tag)) {
				return true;
			}
		}
		return false;
	}

	public void unsubscribe() {
		taggedLoggers.unsubscribe(this);
	}
}