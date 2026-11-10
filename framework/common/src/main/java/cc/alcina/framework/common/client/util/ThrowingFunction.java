package cc.alcina.framework.common.client.util;

public interface ThrowingFunction<T, R> {
	R apply(T t) throws Exception;
}