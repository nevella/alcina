package cc.alcina.framework.common.client.sync;

public interface TypedLocalDomainPersistence<T> {
	default void adjustUpdateContext() {
	}

	public void deleteLocalEquivalent(T object);

	public T ensureLocalEquivalent(T object);

	public T findLocalEquivalent(T object);
}
