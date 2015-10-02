package cc.alcina.framework.common.client.util;

import java.util.function.Function;

public interface ThrowingFunction<T, R>  {
	R apply(T t) throws Exception;
}