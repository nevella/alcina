package cc.alcina.framework.common.client.context;

import cc.alcina.framework.common.client.util.Ax;

import cc.alcina.framework.common.client.util.Ax;

import cc.alcina.framework.common.client.util.Ax;

import cc.alcina.framework.common.client.util.Ax;

import cc.alcina.framework.common.client.util.Ax;

/**
 * <p>
 * The API for a scope key, used for accessing a scope such as LooseContext or
 * Configuration
 * <p>
 * Instances should always be static final fields
 */
public interface ScopeKey<T> {
	T getTyped();

	void set(T t);

	String get();

	boolean has();

	int intValue();

	boolean is();

	long longValue();

	String getPath();
}
