package cc.alcina.framework.entity.domaintransform.event;

public interface DomainTransformPersistenceListener {
	public void onDomainTransformRequestPersistence(
			DomainTransformPersistenceEvent evt);

	// fired outside synchronized block; handles monitors
	default boolean isSequencingListener() {
		return false;
	}
}