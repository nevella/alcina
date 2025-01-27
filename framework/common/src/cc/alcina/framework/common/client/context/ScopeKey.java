package cc.alcina.framework.common.client.context;

import cc.alcina.framework.common.client.util.Ax;

import cc.alcina.framework.common.client.util.Ax;

import cc.alcina.framework.common.client.util.Ax;

import cc.alcina.framework.common.client.util.Ax;

import cc.alcina.framework.common.client.util.Ax;

/**
 * <p>
 * Specifies a scope key for accessing a context value - either LooseContext or
 * (entity layer subclass) Configuration
 * <p>
 * Instances should always be static final fields
 */
public class ScopeKey {
	Class clazz;

	String key;

	String contextKey;

	public ScopeKey(Class clazz, String key) {
		this.clazz = clazz;
		this.key = key;
		this.contextKey = Ax.format("%s.%s", clazz.getName().replace("$", "."),
				key);
	}

	public String get() {
		return LooseContext.getString(contextKey);
	}
}
