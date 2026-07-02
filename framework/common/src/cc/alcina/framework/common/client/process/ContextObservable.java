package cc.alcina.framework.common.client.process;

public interface ContextObservable extends ProcessObservable {
	default void publish() {
		ProcessObservers.context().publish(this);
	}

	/*
	 * The observer must be registered in the application context (root for
	 * single-threaded applicatoins). For example, when the StatusModule UI
	 * component is registered, there must not be a containing LooseContext
	 * frame (i.e. LooseContext.push() in the stack)
	 */
	public interface Base extends ContextObservable {
	}

	/*
	 * An analogue to log.warn() - but for the context procss. It supercedes
	 * such primitve(s) as AlcinaTopics.devWarning
	 */
	public static class ProcessWarning implements ContextObservable {
		public String message;

		public ProcessWarning(String message) {
			this.message = message;
		}
	}

	/*
	 * An analogue to log.exception() - but for the context procss
	 */
	public static class ProcessException implements ContextObservable {
		public String message;

		public ProcessException(String message) {
			this.message = message;
		}
	}
}