package cc.alcina.framework.common.client.util;

public class InnerAccess<T> {
	private T t;

	public T get() {
		return this.t;
	}

	public void set(T t) {
		this.t = t;
	}
}
