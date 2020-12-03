package cc.alcina.framework.common.client.util;

public interface ThrowingConsumer<T> {
	void accept(T t) throws Exception;
}