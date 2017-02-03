package cc.alcina.framework.entity.registry;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.ClearOnAppRestartLoc;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry.RegistryProvider;
import cc.alcina.framework.common.client.util.CommonUtils;

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

	private ClassLoader servletLayerClassLoader;

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

	public <T> void ensureSingletonRegistered(Class<? super T> clazz, T t) {
		for (Registry registryInstance : getPerClassLoader().values()) {
			registryInstance.ensureSingletonRegistered(clazz, t);
		}
	}

	public void forAllRegistries(Class<?> clazz) {
		Registry sourceInstance = null;
		for (Registry registryInstance : getPerClassLoader().values()) {
			if (registryInstance.lookup(false, clazz, void.class,
					false) != null) {
				sourceInstance = registryInstance;
				break;
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
				System.out.println("Created registry for classloader "
						+ contextClassLoader);
			} else {
				throw new RuntimeException(
						String.format("Too many registies: \n%s\n%s\n",
								contextClassLoader, perClassLoader.keySet()));
			}
		}
		lastClassLoader = contextClassLoader;
		lastRegistry = registry;
		return registry;
	}

	public void registerServletLayerClassloader(ClassLoader classLoader) {
		this.servletLayerClassLoader = classLoader;
	}

	public static Registry servletLayerRegistry() {
		if (Registry
				.getProvider() instanceof ClassLoaderAwareRegistryProvider) {
			ClassLoaderAwareRegistryProvider clRegistry = (ClassLoaderAwareRegistryProvider) Registry
					.getProvider();
			return clRegistry.perClassLoader
					.get(clRegistry.servletLayerClassLoader);
		} else {
			return Registry.get();
		}
	}

	public void dumpRegistries() {
		perClassLoader.values().forEach(reg -> {
			reg.getRegistry().keySet().forEach(k -> System.out
					.format("%-12s %s\n", k.hashCode(), ((Class) k).getName()));
		});
	}

	public static <T> Class<T> servletLayerClass(Class<T> clazz) {
		if (Registry
				.getProvider() instanceof ClassLoaderAwareRegistryProvider) {
			ClassLoaderAwareRegistryProvider clRegistry = (ClassLoaderAwareRegistryProvider) Registry
					.getProvider();
			if (clazz.getClassLoader() == clRegistry.servletLayerClassLoader) {
				return clazz;
			} else {
				try {
					return (Class<T>) clRegistry.servletLayerClassLoader
							.loadClass(clazz.getName());
				} catch (Exception e) {
					throw new WrappedRuntimeException(e);
				}
			}
		}
		return clazz;
	}

	private static ClassLoader pushedContextClassLoader;

	public static void pushServletLayerRegistry() {
		if (Registry
				.getProvider() instanceof ClassLoaderAwareRegistryProvider) {
			pushedContextClassLoader = Thread.currentThread()
					.getContextClassLoader();
			Thread.currentThread().setContextClassLoader(
					((ClassLoaderAwareRegistryProvider) Registry
							.getProvider()).servletLayerClassLoader);
		}
	}

	public static void popServletLayerRegistry() {
		if (Registry
				.getProvider() instanceof ClassLoaderAwareRegistryProvider) {
			Thread.currentThread()
					.setContextClassLoader(pushedContextClassLoader);
		}
	}
}
