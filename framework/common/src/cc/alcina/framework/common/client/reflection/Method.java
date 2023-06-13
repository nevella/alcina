package cc.alcina.framework.common.client.reflection;

import java.util.function.BiFunction;

public class Method<T> {
	public static final transient Method EXISTS_REF = new Method(null, null,
			null);

	private Object nativeReflected;

	private BiFunction<Object, Object[], T> invoker;

	private Class returnType;

	public Method(Object nativeReflected,
			BiFunction<Object, Object[], T> invoker, Class returnType) {
		this.nativeReflected = nativeReflected;
		this.invoker = invoker;
		this.returnType = returnType;
	}

	public Class getReturnType() {
		return returnType;
	}

	public T invoke(Object target, Object[] args) {
		return invoker.apply(target, args);
	}

	@Override
	public String toString() {
		return nativeReflected.toString();
	}
}
