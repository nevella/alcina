package cc.alcina.framework.common.client.util;

public class InnerAccess<T> {
	public static <T> InnerAccess<T> of(T t) {
		InnerAccess<T> innerAccess = new InnerAccess<>();
		innerAccess.set(t);
		return innerAccess;
	}

	private T t;

	public T get() {
		return this.t;
	}

	public void set(T t) {
		this.t = t;
	}

	@Override
	public String toString() {
		return Ax.format("Inner Access: [%s]", t);
	}
}
