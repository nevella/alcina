package cc.alcina.framework.common.client.util;

import cc.alcina.framework.common.client.WrappedRuntimeException;

public interface ThrowingSupplier<T> {
	public static <T> T wrap(ThrowingSupplier<T> supplier) {
		try {
			return supplier.get();
		} catch (Exception e) {
			throw WrappedRuntimeException.wrap(e);
		}
	}

	public T get() throws Exception;
}