package cc.alcina.framework.entity.util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.ThrowingRunnable;

/*
 * For infrastructure components where blocking due to log emission > writer speed can cause feedback
 */
@RegistryLocation(registryPoint = OffThreadLogger.class, implementationType = ImplementationType.SINGLETON)
public class OffThreadLogger implements InvocationHandler {
	public static OffThreadLogger get() {
		return Registry.impl(OffThreadLogger.class);
	}

	public static Logger getLogger(Class clazz) {
		return getLogger(clazz.getName());
	}

	public static Logger getLogger(String name) {
		Logger delegate = LoggerFactory.getLogger(name);
		Logger proxy = (Logger) Proxy.newProxyInstance(
				Thread.currentThread().getContextClassLoader(),
				new Class[] { Logger.class }, OffThreadLogger.get());
		OffThreadLogger.get().register(proxy, delegate);
		return proxy;
	}

	private BlockingQueue<OffThreadLogger.Event> eventQueue = new LinkedBlockingQueue<>();

	private Map<Logger, Logger> proxyDelegate = Collections
			.synchronizedMap(new WeakHashMap<>());

	private LoggerThread thread;

	public OffThreadLogger() {
		thread = new LoggerThread();
		thread.start();
	}

	public void appShutdown() {
		eventQueue.add(new Event());
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args)
			throws Throwable {
		switch (method.getName()) {
		case "hashCode":
			return System.identityHashCode(proxy);
		case "equals":
			return proxy == args[0];
		case "toString":
			return proxy.getClass().getName() + "@"
					+ Integer.toHexString(System.identityHashCode(proxy));
		}
		Event event = new Event(Thread.currentThread(), method, args,
				proxyDelegate.get(proxy));
		eventQueue.add(event);
		return null;
	}

	private void register(Logger proxy, Logger delegate) {
		proxyDelegate.put(proxy, delegate);
	}

	private class LoggerThread extends Thread {
		@Override
		public void run() {
			while (true) {
				String name = Thread.currentThread().getName();
				try {
					Event event = eventQueue.poll(10, TimeUnit.SECONDS);
					if (event == null) {
						continue;
					}
					if (event.threadName == null) {
						return;
					}
					Thread.currentThread().setName(event.threadName);
					ThrowingRunnable runnable = () -> event.method
							.invoke(event.delegate, event.args);
					runnable.run();
					// FIXME - mvcc.5 - throw if any args are mvcc objects -
					// require the strinng be evaluated on the originatinng
					// thread
					// } catch (MvccException e) {
					// // FIXME - mvcc.5 - inject handler from mvcc
					// MethodContext.instance().withWrappingTransaction()
					// .run(runnable);
					// }
				} catch (Throwable e) {
					Ax.out("DEVEX::0 - OffThreadLogger.LoggerThread exception");
					e.printStackTrace();
				} finally {
					Thread.currentThread().setName(name);
				}
			}
		}
	}

	static class Event {
		String threadName;

		Method method;

		Object[] args;

		Logger delegate;

		public Event() {
			// Used for queue termination
		}

		public Event(Thread thread, Method method, Object[] args,
				Logger delegate) {
			this.threadName = thread.getName();
			this.method = method;
			this.args = args;
			this.delegate = delegate;
		}
	}
}
