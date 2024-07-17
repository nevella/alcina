package cc.alcina.framework.common.client.util;

import java.util.function.Consumer;
import java.util.function.Function;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;

@Bean(PropertySource.FIELDS)
public class Result<T, E> {
	T t;

	E e;

	boolean ok;

	public static <T, E> Result<T, E> ok(T t) {
		Result<T, E> result = new Result<>();
		result.ok = true;
		result.t = t;
		return result;
	}

	public static <T, E> Result<T, E> error(E e) {
		Result<T, E> result = new Result<>();
		result.ok = false;
		result.e = e;
		return result;
	}

	Result() {
	}

	public <T2> Result<T2, E> mapSuccess(Function<T, T2> successMap) {
		if (ok) {
			return ok(successMap.apply(t));
		} else {
			return (Result<T2, E>) this;
		}
	}

	public void onOk(Consumer<T> handler) {
		Preconditions.checkState(ok);
		handler.accept(t);
	}

	public void onError(Consumer<E> handler) {
		Preconditions.checkState(!ok);
		handler.accept(e);
	}

	public void throwOnError() {
		if (!ok) {
			throw new RuntimeException(String.valueOf(e));
		}
	}

	public T getOk() {
		Preconditions.checkState(ok);
		return t;
	}
}
