package cc.alcina.framework.entity.registry;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.ClearOnAppRestartLoc;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry.RegistryProvider;
import cc.alcina.framework.common.client.logic.reflection.registry.RegistryKey;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.SEUtilities;

@RegistryLocation(registryPoint = ClearOnAppRestartLoc.class)
public class ClassLoaderAwareRegistryProvider implements RegistryProvider {
	public static void clearThreadLocals(Class... clear) {
		try {
			for (Class clazz : clear) {
				while (clazz != null) {
					for (Field f : clazz.getDeclaredFields()) {
						if (ThreadLocal.class.isAssignableFrom(f.getType())
								&& Modifier.isStatic(f.getModifiers())) {
							f.setAccessible(true);
							ThreadLocal tl = (ThreadLocal) f.get(null);
							if (tl != null) {
								tl.remove();
							}
						}
					}
					clazz = clazz.getSuperclass();
				}
			}
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	Map<ClassLoader, Registry> perClassLoader = new HashMap<ClassLoader, Registry>();

	ClassLoader lastClassLoader;

	Registry lastRegistry;

	@Override
	public void appShutdown() {
		for (Registry registry : perClassLoader.values()) {
			registry.shutdownSingletons();
		}
		List<Class> clear = Registry.get().lookup(false,
				ClearOnAppRestartLoc.class, void.class, false);
		try {
			for (Class clazz : clear) {
				try {
					try {
						clearThreadLocals(clazz);
					} catch (Exception e) {
						// ignore
					}
					while (clazz != null) {
						for (Field f : clazz.getDeclaredFields()) {
							if (Modifier.isStatic(f.getModifiers())
									&& !Modifier.isFinal(f.getModifiers())
									&& !f.getType().isPrimitive()) {
								f.setAccessible(true);
								f.set(null, null);
							}
						}
						clazz = clazz.getSuperclass();
					}
				} catch (Throwable e) {
					if (e instanceof java.lang.NoClassDefFoundError) {
						// classloader issues
					} else {
						e.printStackTrace();
					}
				}
			}
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public void dumpRegistries() {
		perClassLoader.values().forEach(reg -> {
			reg.getRegistry().keySet()
					.forEach(k -> System.out.format("%-12s %s\n", k.hashCode(),
							((RegistryKey) k).name()));
		});
	}

	public void forAllRegistries(Class<?> clazz) {
		Registry sourceInstance = null;
		for (Registry registryInstance : getPerClassLoader().values()) {
			try {
				if (registryInstance.lookup(false, clazz, void.class,
						false) != null) {
					sourceInstance = registryInstance;
					break;
				}
			} catch (RuntimeException e) {
				if (CommonUtils.extractCauseOfClass(e,
						ClassNotFoundException.class) != null) {
					// squelch - not in this module (i.e. this is an ejb
					// classloader)
				} else {
					throw e;
				}
			}
		}
		for (Registry registryInstance : getPerClassLoader().values()) {
			if (registryInstance == sourceInstance) {
				continue;
			}
			registryInstance.copyFrom(sourceInstance, clazz);
		}
	}

	public Map<ClassLoader, Registry> getPerClassLoader() {
		return this.perClassLoader;
	}

	@Override
	public Registry getRegistry() {
		ClassLoader contextClassLoader = Thread.currentThread()
				.getContextClassLoader();
		if (contextClassLoader == lastClassLoader) {
			return lastRegistry;
		}
		Registry registry = perClassLoader.get(contextClassLoader);
		if (registry == null) {
			if (perClassLoader.size() < 2) {
				Registry existing = CommonUtils.first(perClassLoader.values());
				registry = new Registry();
				if (existing != null) {
					existing.shareSingletonMapTo(registry);
				}
				perClassLoader.put(contextClassLoader, registry);
				System.out.format("Created registry for classloader %s - %s\n",
						contextClassLoader, contextClassLoader.hashCode());
			} else {
				throw new RuntimeException(String.format(
						"Too many registries: \n%s\n%s\n%s\n",
						contextClassLoader, contextClassLoader.hashCode(),
						perClassLoader.keySet()));
			}
		}
		lastClassLoader = contextClassLoader;
		lastRegistry = registry;
		return registry;
	}
}
