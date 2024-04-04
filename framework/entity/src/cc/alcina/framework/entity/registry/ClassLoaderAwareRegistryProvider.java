package cc.alcina.framework.entity.registry;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.ClearStaticFieldsOnAppShutdown;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry.RegistryProvider;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.logic.EntityLayerObjects;

@Registration(ClearStaticFieldsOnAppShutdown.class)
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

	public static void clearThreadLocalsForAllThreads(Class clazz) {
		Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
		try {
			Field _Thread_threadLocals = Thread.class
					.getDeclaredField("threadLocals");
			_Thread_threadLocals.setAccessible(true);
			Method _ThreadLocalMap_remove = null;
			while (clazz != null) {
				for (Field f : clazz.getDeclaredFields()) {
					if (ThreadLocal.class.isAssignableFrom(f.getType())
							&& Modifier.isStatic(f.getModifiers())) {
						f.setAccessible(true);
						ThreadLocal tl = (ThreadLocal) f.get(null);
						if (tl != null) {
							for (Thread thread : threadSet) {
								Object threadLocalMap = _Thread_threadLocals
										.get(thread);
								if (threadLocalMap != null) {
									if (_ThreadLocalMap_remove == null) {
										_ThreadLocalMap_remove = threadLocalMap
												.getClass().getDeclaredMethod(
														"remove", new Class[] {
																ThreadLocal.class });
										_ThreadLocalMap_remove
												.setAccessible(true);
									}
									_ThreadLocalMap_remove.invoke(
											threadLocalMap,
											new Object[] { tl });
								}
							}
						}
					}
				}
				clazz = clazz.getSuperclass();
			}
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	private static ClassLoaderAwareRegistryProvider instance;

	public static ClassLoaderAwareRegistryProvider get() {
		if (instance == null) {
			// no need to worry about threading, this happens early in servlet
			// init
			instance = new ClassLoaderAwareRegistryProvider();
		}
		return instance;
	}

	Map<ClassLoader, Registry> perClassLoader = new LinkedHashMap<ClassLoader, Registry>();

	ClassLoader lastClassLoader;

	Registry lastRegistry;

	private ClassLoader servletLayerClassloader;

	@Override
	public void appShutdown() {
		Logger logger = LoggerFactory.getLogger(Registry.class);
		Stream<Class<?>> clear = Registry.query()
				.addKeys(ClearStaticFieldsOnAppShutdown.class)
				.untypedRegistrations();
		try {
			clear.forEach(clazz -> {
				logger.debug("Clearing static fields for class\n\t{}", clazz);
				try {
					try {
						clearThreadLocalsForAllThreads(clazz);
					} catch (Exception e) {
						logger.debug("Thread local clear issue", e);
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
			});
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public void dumpRegistries() {
		perClassLoader.values().forEach(reg -> {
			reg.instanceInternals().dump();
		});
	}

	public ClassLoader getEntityLayerClassloader() {
		Preconditions.checkArgument(
				perClassLoader.size() == 2 && servletLayerClassloader != null);
		return perClassLoader.entrySet().stream()
				.filter(e -> e.getKey() != servletLayerClassloader).findFirst()
				.get().getKey();
	}

	public Map<ClassLoader, Registry> getPerClassLoader() {
		return this.perClassLoader;
	}

	@Override
	public Registry getRegistry() {
		ClassLoader classLoader = Thread.currentThread()
				.getContextClassLoader();
		if (classLoader == null || classLoader.getClass().getName()
				.equals("jdk.internal.loader.ClassLoaders$AppClassLoader")) {
			if (getClass().getClassLoader() != classLoader) {
				throw new RuntimeException("Context classloader not set");
			}
		}
		if (classLoader == lastClassLoader) {
			return lastRegistry;
		}
		Registry registry = perClassLoader.get(classLoader);
		if (registry == null) {
			synchronized (this) {
				if (perClassLoader.get(classLoader) == null) {
					if (perClassLoader.isEmpty()) {
						servletLayerClassloader = classLoader;
					}
					if (perClassLoader.size() < 2) {
						Registry existing = CommonUtils
								.first(perClassLoader.values());
						registry = new Registry();
						registry.instanceInternals()
								.setName(classLoader.toString());
						if (existing != null) {
							existing.instanceInternals()
									.shareImplementationsTo(registry);
						}
						perClassLoader.put(classLoader, registry);
						System.out.format(
								"Created registry for classloader %s - %s\n",
								classLoader, classLoader.hashCode());
					} else {
						throw new RuntimeException(String.format(
								"Too many registries: \n%s\n%s\n%s\n",
								classLoader, classLoader.hashCode(),
								perClassLoader.keySet()));
					}
				}
			}
		}
		lastClassLoader = classLoader;
		lastRegistry = registry;
		return registry;
	}

	public ClassLoader getServletLayerClassloader() {
		return this.servletLayerClassloader;
	}

	public void
			setServletLayerClassloader(ClassLoader servletLayerClassloader) {
		this.servletLayerClassloader = servletLayerClassloader;
		EntityLayerObjects.get()
				.setServletLayerClassLoader(getServletLayerClassloader());
	}
}
