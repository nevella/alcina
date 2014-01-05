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
	Map<ClassLoader, Registry> perClassLoader = new HashMap<ClassLoader, Registry>();

	@Override
	public Registry getRegistry() {
		ClassLoader contextClassLoader = Thread.currentThread()
				.getContextClassLoader();
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

	public static void clearThreadLocals(Class... clear) {
		try {
			for (Class clazz : clear) {
				while (clazz != null) {
					for (Field f : clazz.getDeclaredFields()) {
						if (ThreadLocal.class.isAssignableFrom(f.getType())) {
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
}
