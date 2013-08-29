package cc.alcina.framework.entity.domaintransform.event;

public interface DomainTransformPersistenceListener {
	public void onDomainTransformRequestPersistence(
			DomainTransformPersistenceEvent evt);
}