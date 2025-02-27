package cc.alcina.framework.common.client.context;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.common.client.util.ThrowingRunnable;
import cc.alcina.framework.common.client.util.ThrowingSupplier;
import cc.alcina.framework.entity.util.AlcinaParallel;

/**
 * Beginnings of a general approach to decoupling hints - let's see if it works
 *
 * Ahhh...yes. And the jdk sorta copied with ScopedValue
 *
 * FIXME - dirndl 1x5 - cleanup "LooseContext" vs "LooseContextInstance";
 * generalise stack-based contexts (this, permissions, etc)
 *
 * <p>
 * (WIP)The LooseContext system provides scoped configuration data to code
 * execution, accessible from any level in a thread's stack at or below the time
 * the value is set via LooseContext.set
 * <ul>
 * <li>LooseContext instances form a stack, controlled by LooseContext.push/pop
 * *
 * <li>Code *should* only populate the context in the same stack frame as the
 * push/pop - rarely it's unavoidable to do otherwise
 * <li>LooseContexts are a useful way to customise job performance - the Job
 * task will contain a field <code>String contextProperties</code>, which is
 * deserialized to a StringMap and then used to populate a LooseContextInstance
 * visible to the JobPerformer stack
 * <li>Encapsulation? Some ideas:
 * https://stackoverflow.com/questions/26330133/is-it-ever-a-good-idea-to-break-encapsulation
 * asdf
 * </ul>
 * <p>
 * Implementation is via a stack of LooseContextInstance objects, the stack
 * itself is a Threadlocal.
 * <p>
 * When threads are spawned (for instance for parallel job execution), the
 * {@link AlcinaParallel} class can be used to replicate the LooseContext of the
 * caller (along with other contexts, such as {@link PermissionsManager})
 * 
 * <h3>A simple example</h3>
 * 
 * <pre>
 * <code>
 &#64;Bean(PropertySource.FIELDS)
public static class ContextTaskExample extends PerformerTask {
	public static final String CONTEXT_LOG_LEVEL = ContextTaskExample.class
			.getName() + ".CONTEXT_LOG_LEVEL";

	public static void test() {
		ContextTaskExample example = new ContextTaskExample();
		example.contextProperties = StringMap.property(
				"au.com.barnet.jade.dev.console.JDevLocal.ContextTaskExample.CONTEXT_LOG_LEVEL",
				"WARN").toPropertyString();
		example.perform();
	}

	public String contextProperties = "";

	@Override
	public void run() throws Exception {
		// assume called from ContextTaskExample.test()
		try {
			LooseContext.push();
			LooseContext.getContext().addProperties(contextProperties);
			Ax.out(LooseContext.get(CONTEXT_LOG_LEVEL));
			// WARN
			LooseContext.set(CONTEXT_LOG_LEVEL, "INFO");
			methodA();
		} finally {
			LooseContext.pop();
		}
	}

	void methodA() {
		Ax.out(LooseContext.get(CONTEXT_LOG_LEVEL));
		// INFO
		try {
			LooseContext.pushWithKey(CONTEXT_LOG_LEVEL, "DEBUG");
			Ax.out(LooseContext.get(CONTEXT_LOG_LEVEL));
			// DEBUG
			methodB();
		} finally {
			LooseContext.pop();
		}
		Ax.out(LooseContext.get(CONTEXT_LOG_LEVEL));
		// INFO
	}

	void methodB() {
		Ax.out(LooseContext.get(CONTEXT_LOG_LEVEL));
		// DEBUG
	}
}
 
 * </code>
 * </pre>
 * 
 *
 */
public abstract class LooseContext {
	private static LooseContext factoryInstance;

	public static void allowUnbalancedFrameRemoval(Class clazz,
			String pushMethodName) {
		getContext().allowUnbalancedFrameRemoval(clazz, pushMethodName);
	}

	// FIXME - context - in general replace with copy/restore snapshot (which
	// recovers from exceptions). *Possibly* not in methodcontext, for
	// performance
	public static void confirmDepth(int depth) {
		if (depth != depth()) {
			getContext().clearStack();
			throw new LooseContextStackException();
		}
	}

	public static boolean containsKey(String key) {
		return getContext().containsKey(key);
	}

	public static int depth() {
		return getContext().depth();
	}

	public static <T> T ensure(String key, Supplier<T> supplier) {
		T t = get(key);
		if (t == null) {
			t = supplier.get();
			set(key, t);
		}
		return t;
	}

	public static <T> T get(String key) {
		return getContext().get(key);
	}

	public static boolean getBoolean(String key) {
		return getContext().getBoolean(key);
	}

	public static LooseContextInstance getContext() {
		return getInstance().getContext0();
	}

	/*
	 * Named 'getInstance' rather than 'get' because we want get to be Map.get
	 */
	protected static LooseContext getInstance() {
		if (factoryInstance == null) {
			factoryInstance = new ClientLooseContextProvider();
		}
		LooseContext perThreadInstance = factoryInstance.getT();
		if (perThreadInstance != null) {
			return perThreadInstance;
		}
		return factoryInstance;
	}

	public static Integer getInteger(String key) {
		return getContext().getInteger(key);
	}

	public static Long getLong(String key) {
		return getContext().getLong(key);
	}

	public static String getString(String key) {
		return getContext().get(key);
	}

	public static boolean has(String key) {
		return containsKey(key);
	}

	public static boolean is(String key) {
		return getBoolean(key);
	}

	public static <T> Optional<T> optional(String key) {
		return Optional.ofNullable(get(key));
	}

	public static void pop() {
		getContext().pop();
	}

	public static Key key(Class clazz, String keyPart) {
		return new Key(clazz, keyPart);
	}

	public static class Key<T> implements ScopeKey<T> {
		Class clazz;

		String keyPart;

		String contextKey;

		public Key(Class clazz, String keyPart) {
			this.clazz = clazz;
			this.keyPart = keyPart;
			this.contextKey = Ax.format("%s.%s",
					clazz.getName().replace("$", "."), keyPart);
		}

		public String get() {
			return LooseContext.getString(contextKey);
		}

		public void set(T t) {
			LooseContext.set(contextKey, t);
		}

		public T getTyped() {
			return LooseContext.get(contextKey);
		}

		public int intValue() {
			return LooseContext.getInteger(contextKey);
		}

		public boolean is() {
			return LooseContext.is(contextKey);
		}

		public boolean has() {
			return LooseContext.has(contextKey);
		}

		@Override
		public long longValue() {
			return LooseContext.getLong(contextKey);
		}

		@Override
		public String getPath() {
			return contextKey;
		}

		public void runWithTrue(Runnable runnable) {
			runWith(runnable, true);
		}

		public void runWith(Runnable runnable, boolean b) {
			try {
				LooseContext.pushWithBoolean(getPath(), b);
				runnable.run();
			} finally {
				LooseContext.pop();
			}
		}

		public void runWithValue(ThrowingRunnable runnable, T t) {
			try {
				LooseContext.pushWithKey(getPath(), t);
				runnable.run();
			} catch (Exception e) {
				throw WrappedRuntimeException.wrap(e);
			} finally {
				LooseContext.pop();
			}
		}

		public <T> T callWithTrue(Callable<T> callable) {
			return callWith(callable, true);
		}

		public <T> T callWith(Callable<T> callable, boolean b) {
			try {
				LooseContext.pushWithBoolean(getPath(), b);
				return callable.call();
			} catch (Exception e) {
				throw WrappedRuntimeException.wrap(e);
			} finally {
				LooseContext.pop();
			}
		}

		public void setTrue() {
			LooseContext.setTrue(getPath());
		}

		public Optional<T> optional() {
			return Optional.ofNullable(getTyped());
		}
	}

	public static void push() {
		getContext().push();
	}

	public static void pushWithBoolean(String key, boolean value) {
		getContext().pushWithKey(key, value);
	}

	public static void pushWithFalse(String key) {
		getContext().pushWithKey(key, Boolean.FALSE);
	}

	/*
	 * Don't evaluate possibly throwing expressions in a call to this!
	 */
	public static void pushWithKey(String key, Object value) {
		getContext().pushWithKey(key, value);
	}

	public static void pushWithTrue(String key) {
		getContext().pushWithKey(key, Boolean.TRUE);
	}

	/**
	 * For when copying from a launcher thread - note, no checks are made (since
	 * the executor might be a thread pool)
	 */
	public static void putSnapshotProperties(LooseContextInstance snapshot) {
		getContext().putSnapshotProperties(snapshot);
	}

	public static void register(LooseContext tm) {
		factoryInstance = tm;
	}

	public static <T> T remove(String key) {
		return getContext().remove(key);
	}

	public static void removePerThreadContext() {
		if (factoryInstance == null) {
			return;
		}
		factoryInstance.removePerThreadContext0();
	}

	public static <T> T run(ThrowingSupplier<T> supplier) {
		return runWithKeyValue(null, Boolean.TRUE, supplier);
	}

	public static <T> T runWithKeyValue(String key, Object value,
			ThrowingSupplier<T> supplier) {
		try {
			pushWithKey(key, value);
			return supplier.get();
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			pop();
		}
	}

	public static void runWithTrue(String key, ThrowingRunnable runnable) {
		runWithKeyValue(key, Boolean.TRUE, runnable.asSupplier());
	}

	public static <T> T runWithTrue(String key, ThrowingSupplier<T> supplier) {
		return runWithKeyValue(key, Boolean.TRUE, supplier);
	}

	public static void set(String key, Object value) {
		getContext().set(key, value);
	}

	public static void setBoolean(String key, Boolean value) {
		getContext().setBoolean(key, value);
	}

	public static void setIfMissing(String key, Object object) {
		if (!has(key)) {
			set(key, object);
		}
	}

	public static void setTrue(String key) {
		getContext().setBoolean(key);
	}

	protected LooseContextInstance context;

	protected LooseContextInstance getContext0() {
		if (context == null) {
			context = new LooseContextInstance();
		}
		return context;
	}

	public abstract LooseContext getT();

	protected void removePerThreadContext0() {
	}

	public static class ClientLooseContextProvider extends LooseContext {
		@Override
		public LooseContext getT() {
			return this;
		}
	}

	public interface HasContextProperties {
		public StringMap provideContextProperties();
	}

	public static class LooseContextStackException extends RuntimeException {
	}

	/*
	 * For framework calls where it's possible that a stack exception will
	 * occur, but imperative that it not propagate, restore the context snapshot
	 * in a finally block
	 */
	public static void restore(LooseContextInstance contextSnapshot) {
		contextSnapshot.cloneFieldsTo(getContext());
	}

	static Set<String> snapshotExclusions = null;

	/**
	 * <p>
	 * Define a key as execution-state specific (such as a lock), rather than
	 * execution-context specific. Fuzzy, I know
	 * <p>
	 * Sync :: manual copy-on-write
	 */
	public static synchronized void excludeFromSnapshot(String key) {
		Set<String> newExclusions = new LinkedHashSet<>();
		if (snapshotExclusions != null) {
			newExclusions.addAll(snapshotExclusions);
		}
		newExclusions.add(key);
		snapshotExclusions = newExclusions;
	}
}
