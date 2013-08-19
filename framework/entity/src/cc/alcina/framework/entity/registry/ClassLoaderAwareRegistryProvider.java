package cc.alcina.framework.entity.registry;

import java.util.HashMap;
import java.util.Map;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry.RegistryProvider;

public class ClassLoaderAwareRegistryProvider implements RegistryProvider {
	Map<ClassLoader, Registry> perClassLoader = new HashMap<ClassLoader, Registry>();

	@Override
	public Registry getRegistry() {
		ClassLoader contextClassLoader = Thread.currentThread()
				.getContextClassLoader();
		Registry registry = perClassLoader.get(contextClassLoader);
		if (registry == null) {
			if (perClassLoader.size() < 2) {
				registry = new Registry();
				perClassLoader.put(contextClassLoader, registry);
				System.out.println("Created registry for classloader "
						+ contextClassLoader);
			} else {
				throw new RuntimeException(String.format(
						"Too many registies: \n%s\n%s\n", contextClassLoader,
						perClassLoader.keySet()));
			}
		}
		return registry;
	}

	@Override
	public void appShutdown() {
		for (Registry registry : perClassLoader.values()) {
			registry.shutdownSingletons();
		}
	}
}
