package cc.alcina.framework.common.client.process;

import cc.alcina.framework.common.client.logic.domaintransform.SequentialIdGenerator;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.util.Ax;

/**
 * No type should directly implement this - rather it should implement either
 * {@link GlobalObservable} or {@link ContextObservable}
 * 
 * <p>
 * The former are designed for logging/debugging -OR- app-wide pub-sub - the
 * latter for stack-shaped process implementation
 */
@Reflected
public interface ProcessObservable {
	public static class Id {
		static SequentialIdGenerator generator = new SequentialIdGenerator();

		public static void setGenerator(SequentialIdGenerator generator) {
			Id.generator = generator;
		}

		public static long nextId() {
			return generator.incrementAndGet();
		}
	}

	default boolean contains(String s) {
		return toString().contains(s);
	}

	default boolean containsRegex(String regex) {
		return toString().matches(Ax.format("(?is).*%s.*", regex));
	}

	/**
	 * Note - only use this when not in a tight loop (it is more elegant/shorter
	 * than a static call to ProcessObservers, but causes unnecessary object
	 * (observable) creation
	 */
	default void publish() {
		ProcessObservers.publishUntyped(getClass(), () -> this);
	}
}
