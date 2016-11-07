package cc.alcina.framework.common.client.util;

import java.util.function.Supplier;

/**
 * Beginnings of a general approach to decoupling hints - let's see if it works
 * 
 * @author nick@alcina.cc
 * 
 */
public abstract class LooseContext {
	private static LooseContext factoryInstance;

	public static <T> T get(String key) {
		return getContext().get(key);
	}

	public static <T> T runWithBoolean(String key,
			ThrowingSupplier<T> supplier) {
		return runWithKeyValue(key, Boolean.TRUE, supplier);
	}

	public static <T> T run(ThrowingSupplier<T> supplier) {
		return runWithKeyValue(null, Boolean.TRUE, supplier);
	}

	public static <T> T runWithKeyValue(String key, Object value,
			ThrowingSupplier<T> supplier) {
		try {
			pushWithKey(key, value);
			return supplier.get();
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			pop();
		}
	}

	public static void remove(String key) {
		getContext().remove(key);
	}

	public static boolean getBoolean(String key) {
		return getContext().getBoolean(key);
	}

	public static boolean is(String key) {
		return getBoolean(key);
	}

	public static boolean containsKey(String key) {
		return getContext().containsKey(key);
	}
	public static boolean has(String key) {
		return containsKey(key);
	}

	public static LooseContextInstance getContext() {
		return getInstance().getContext0();
	}

	public static String getString(String key) {
		return getContext().get(key);
	}

	public static void pop() {
		getContext().pop();
	}

	public static void push() {
		getContext().push();
	}

	public static void pushWithKey(String key, Object value) {
		getContext().pushWithKey(key, value);
	}

	public static void pushWithBoolean(String key) {
		getContext().pushWithKey(key, Boolean.TRUE);
	}

	public static void register(LooseContext tm) {
		factoryInstance = tm;
	}

	public static void set(String key, Object value) {
		getContext().set(key, value);
	}

	public static void setBoolean(String key) {
		getContext().setBoolean(key);
	}

	/*
	 * Named 'getInstance' rather than 'get' because we want get to be Map.get
	 */
	protected static LooseContext getInstance() {
		if (factoryInstance == null) {
			factoryInstance = new ClientLooseContextProvider();
		}
		LooseContext tm = factoryInstance.getT();
		if (tm != null) {
			return tm;
		}
		return factoryInstance;
	}

	private LooseContextInstance context;

	public abstract LooseContext getT();

	private LooseContextInstance getContext0() {
		if (context == null) {
			context = new LooseContextInstance();
		}
		return context;
	}

	public static class ClientLooseContextProvider extends LooseContext {
		@Override
		public LooseContext getT() {
			return this;
		}
	}

	public static Integer getInteger(String key) {
		return getContext().getInteger(key);
	}

	public static int depth() {
		return getContext().depth();
	}

	public static void confirmDepth(int depth) {
		if (depth != depth()) {
			getContext().clearStack();
			throw new LooseContextStackException();
		}
	}

	public static class LooseContextStackException extends RuntimeException {
	}

	/**
	 * For when copying from a launcher thread - note, no checks are made (since
	 * the executor might be a thread pool)
	 */
	public static void putContext(LooseContextInstance snapshot) {
		getInstance().context = snapshot;
	}

	public static <T> T ensure(String key, Supplier<T> supplier) {
		T t = get(key);
		if (t == null) {
			t = supplier.get();
			set(key, t);
		}
		return t;
	}
}
