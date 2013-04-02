package cc.alcina.framework.common.client.util;

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

	public static boolean getBoolean(String key) {
		return getContext().getBoolean(key);
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
}
