package cc.alcina.framework.entity.domaintransform.event;

public interface DomainTransformPersistenceListener {
	public void onDomainTransformRequestPersistence(
			DomainTransformPersistenceEvent evt);

	/*
	 * Fire this local event outside of the sequential barrier. Ensures that it
	 * is not blocked (e.g. cache propagation)
	 */
	default boolean isPreBarrierListener() {
		return false;
	}
}