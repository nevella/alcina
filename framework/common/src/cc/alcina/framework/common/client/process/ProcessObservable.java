package cc.alcina.framework.common.client.process;

import cc.alcina.framework.common.client.util.Ax;

public interface ProcessObservable {
	default boolean contains(String s) {
		return toString().contains(s);
	}

	default boolean containsRegex(String regex) {
		return toString().matches(Ax.format("(?is).*%s.*", regex));
	}

	/**
	 * Note - only use this when not in a tight loop (it is more elegant/shorter
	 * than a static call to ProcessObservers, but causes unnecessary object
	 * (observable) creation
	 */
	default void publish() {
		ProcessObservers.publishUntyped(getClass(), () -> this);
	}
}
