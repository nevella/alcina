package cc.alcina.framework.common.client.process;

import cc.alcina.framework.common.client.util.Ax;

public interface ProcessObservable {
	default boolean contains(String s) {
		return toString().contains(s);
	}

	default boolean containsRegex(String regex) {
		return toString().matches(Ax.format("(?is).*%s.*", regex));
	}
}
