package cc.alcina.framework.common.client.domain;

public interface DomainListener<E> {
	abstract Class<? extends E> getListenedClass();

	abstract void insert(E o);

	boolean isEnabled();

	default void onAddValues(boolean post) {
		// debugging for specific metrics, rather than handling
	}

	abstract void remove(E o);

	void setEnabled(boolean enabled);
}
