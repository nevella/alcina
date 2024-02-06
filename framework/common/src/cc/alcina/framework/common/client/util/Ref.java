package cc.alcina.framework.common.client.util;

public class Ref<T> {
	public static <T> Ref<T> empty() {
		return of(null);
	}

	public static <T> Ref<T> of(T t) {
		Ref<T> ref = new Ref<>();
		ref.set(t);
		return ref;
	}

	private T t;

	public T get() {
		return this.t;
	}

	public boolean isEmpty() {
		return t == null;
	}

	public boolean isPresent() {
		return t != null;
	}

	public void set(T t) {
		this.t = t;
	}

	@Override
	public String toString() {
		return Ax.format("Ref<%s>", t);
	}
}
