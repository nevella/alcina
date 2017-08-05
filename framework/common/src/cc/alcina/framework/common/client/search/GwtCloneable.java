package cc.alcina.framework.common.client.search;

import cc.alcina.framework.common.client.WrappedRuntimeException;

public interface GwtCloneable extends Cloneable {
	public Object clone() throws CloneNotSupportedException;

	default <T> T safeClone() {
		try {
			return (T) clone();
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}
}
