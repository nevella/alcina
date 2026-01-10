package cc.alcina.framework.common.client.process;

public interface ContextObservable extends ProcessObservable {
	default void publish() {
		ProcessObservers.context().publish(this);
	}

	/**
	 * These observables will be routed to the base observer if one exists
	 */
	public interface Base extends ContextObservable {
	}
}