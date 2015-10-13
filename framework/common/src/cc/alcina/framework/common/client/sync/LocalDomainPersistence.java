package cc.alcina.framework.common.client.sync;

public interface LocalDomainPersistence<T> {
	public T findLocalEquivalent(T object);

	public T ensureLocalEquivalent(T object);

	public void deleteLocalEquivalent(T object);
}
