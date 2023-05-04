package cc.alcina.framework.entity.transform.event;

public interface DomainTransformPersistenceListener {
	public void onDomainTransformRequestPersistence(
			DomainTransformPersistenceEvent event);

	default boolean isAllVmEventsListener() {
		return false;
	}

	/*
	 * Fire this local event outside of the sequential barrier. Ensures that it
	 * is not blocked (e.g. cache propagation)
	 */
	default boolean isPreBarrierListener() {
		return false;
	}

	public interface Has {
		void addDomainTransformPersistenceListener(
				DomainTransformPersistenceListener listener);
	}
}