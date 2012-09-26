package cc.alcina.framework.common.client.util;

/**
 * Beginnings of a general approach to decoupling hints - let's see if it works
 * 
 * @author nick@alcina.cc
 * 
 */
public abstract class LooseContextProvider {
	private static LooseContextProvider factoryInstance;

	public static LooseContextProvider get() {
		if (factoryInstance == null) {
			factoryInstance = new ClientLooseContextProvider();
		}
		LooseContextProvider tm = factoryInstance.getT();
		if (tm != null) {
			return tm;
		}
		return factoryInstance;
	}

	public static void register(LooseContextProvider tm) {
		factoryInstance = tm;
	}

	private LooseContext context;

	public static LooseContext getContext() {
		return get().getContext0();
	}

	private LooseContext getContext0() {
		if (context == null) {
			context = new LooseContext();
		}
		return context;
	}

	public abstract LooseContextProvider getT();

	public static class ClientLooseContextProvider extends LooseContextProvider {
		@Override
		public LooseContextProvider getT() {
			return this;
		}
	}

	public static boolean getBoolean(String key) {
		return getContext().getBoolean(key);
	}
}
