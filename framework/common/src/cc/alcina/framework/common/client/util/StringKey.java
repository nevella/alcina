package cc.alcina.framework.common.client.util;

import cc.alcina.framework.common.client.log.TaggedLoggerTag;

public class StringKey implements TaggedLoggerTag {
	private String key;

	public StringKey(String key) {
		this.key = key;
	}

	@Override
	public String toString() {
		return "Key:" + key;
	}
}
