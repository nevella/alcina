package cc.alcina.framework.common.client.util;

public interface ThrowingSupplier<T> {
	public T get() throws Exception;
}