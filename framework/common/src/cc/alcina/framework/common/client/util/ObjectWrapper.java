package cc.alcina.framework.common.client.util;

public class ObjectWrapper<T> {
	public static <T> ObjectWrapper<T> of(T t) {
		ObjectWrapper<T> wrapper = new ObjectWrapper<>();
		wrapper.set(t);
		return wrapper;
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
		return Ax.format("Object Wrapper: [%s]", t);
	}
}
