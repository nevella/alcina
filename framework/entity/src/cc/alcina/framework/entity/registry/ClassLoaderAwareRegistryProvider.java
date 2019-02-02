package cc.alcina.framework.entity.registry;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.ClearStaticFieldsOnAppShutdown;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry.RegistryProvider;
import cc.alcina.framework.common.client.logic.reflection.registry.RegistryKey;
import cc.alcina.framework.common.client.util.CommonUtils;

@RegistryLocation(registryPoint = ClearStaticFieldsOnAppShutdown.class)
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

    Map<ClassLoader, Registry> perClassLoader = new LinkedHashMap<ClassLoader, Registry>();

    ClassLoader lastClassLoader;

    Registry lastRegistry;

    private ClassLoader servletLayerClassloader;

    @Override
    public void appShutdown() {
        Logger logger = LoggerFactory.getLogger(Registry.class);
        for (Registry registry : perClassLoader.values()) {
            registry.shutdownSingletons();
        }
        List<Class> clear = Registry.get().lookup(false,
                ClearStaticFieldsOnAppShutdown.class, void.class, false);
        try {
            for (Class clazz : clear) {
                logger.debug("Clearing static fields for class\n\t{}", clazz);
                try {
                    try {
                        if (clazz.getName()
                                .contains("ThreadedPermissionsManager")) {
                            int debug = 3;
                        }
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
        Registry sourceInstance = perClassLoader.get(servletLayerClassloader);
        getPerClassLoader().entrySet().stream()
                .filter(e -> e.getKey() != servletLayerClassloader)
                .forEach(e -> {
                    e.getValue().copyFrom(sourceInstance, clazz);
                });
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
                registry.setName(contextClassLoader.toString());
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

    public ClassLoader getServletLayerClassloader() {
        return this.servletLayerClassloader;
    }

    public void setServletLayerClassloader(
            ClassLoader servletLayerClassloader) {
        this.servletLayerClassloader = servletLayerClassloader;
    }
}
