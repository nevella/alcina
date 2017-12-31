package cc.alcina.framework.common.client.sync;

public interface LocalDomainPersistence<T> {
	public void deleteLocalEquivalent(T object);

	public T ensureLocalEquivalent(T object);

	public T findLocalEquivalent(T object);

	default void adjustUpdateContext() {
	}
}
