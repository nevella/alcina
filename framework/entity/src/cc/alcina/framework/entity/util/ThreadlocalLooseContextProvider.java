package cc.alcina.framework.entity.util;

import cc.alcina.framework.common.client.util.LooseContextProvider;

public class ThreadlocalLooseContextProvider extends LooseContextProvider {
	private static ThreadLocal threadLocalInstance = new ThreadLocal() {
		protected synchronized Object initialValue() {
			ThreadlocalLooseContextProvider provider = new ThreadlocalLooseContextProvider();
			return provider;
		}
	};

	public static ThreadlocalLooseContextProvider cast() {
		return (ThreadlocalLooseContextProvider) LooseContextProvider.get();
	}

	/**
	 * Convenience "override" of LooseContextProvider.get()
	 */
	public static ThreadlocalLooseContextProvider get() {
		return ThreadlocalLooseContextProvider.cast();
	}

	public static LooseContextProvider ttmInstance() {
		return new ThreadlocalLooseContextProvider();
	}

	@Override
	public LooseContextProvider getT() {
		return (LooseContextProvider) threadLocalInstance.get();
	}
}
