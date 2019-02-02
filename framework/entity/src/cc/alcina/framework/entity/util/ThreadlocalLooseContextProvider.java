package cc.alcina.framework.entity.util;

import cc.alcina.framework.common.client.logic.reflection.ClearStaticFieldsOnAppShutdown;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.util.LooseContext;

@RegistryLocation(registryPoint = ClearStaticFieldsOnAppShutdown.class)
public class ThreadlocalLooseContextProvider extends LooseContext {
	private static ThreadLocal threadLocalInstance = new ThreadLocal() {
		protected synchronized Object initialValue() {
			ThreadlocalLooseContextProvider provider = new ThreadlocalLooseContextProvider();
			return provider;
		}
	};

	public static ThreadlocalLooseContextProvider cast() {
		return (ThreadlocalLooseContextProvider) LooseContext.getInstance();
	}

	/**
	 * Convenience "override" of LooseContextProvider.get()
	 */
	public static ThreadlocalLooseContextProvider get() {
		return ThreadlocalLooseContextProvider.cast();
	}

	public static LooseContext ttmInstance() {
		return new ThreadlocalLooseContextProvider();
	}

	@Override
	public LooseContext getT() {
		return (LooseContext) threadLocalInstance.get();
	}
}
