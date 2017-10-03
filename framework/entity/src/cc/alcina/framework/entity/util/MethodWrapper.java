package cc.alcina.framework.entity.util;

import com.totsp.gwittir.client.beans.Method;

public class MethodWrapper implements Method {
	private final java.lang.reflect.Method inner;

	public MethodWrapper(java.lang.reflect.Method inner) {
		assert inner != null;
		this.inner = inner;
	}

	// @Override
	// For JDK1.5 compatibility, don't override methods inherited from an
	// interface
	public String getName() {
		return ((java.lang.reflect.Method) inner).toString();
	}

	// @Override
	public Object invoke(Object target, Object[] args) throws Exception {
		return inner.invoke(target, args);
	}

	@Override
	public String toString() {
		return inner.toString();
	}
}