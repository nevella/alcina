package cc.alcina.framework.common.client.reflection;

import java.util.function.BiFunction;

public class Method<T> {
	private Object nativeMethod;

	private BiFunction<Object, Object[], T> invoker;

	private Class returnType;

	public Method(Object nativeMethod, BiFunction<Object, Object[], T> invoker,
			Class returnType) {
		this.nativeMethod = nativeMethod;
		this.invoker = invoker;
		this.returnType = returnType;
	}

	@Override
	public String toString() {
		return nativeMethod.toString();
	}

	public T invoke(Object target, Object[] args) {
		return invoker.apply(target, args);
	}

	public Class getReturnType() {
		return returnType;
	}
}
