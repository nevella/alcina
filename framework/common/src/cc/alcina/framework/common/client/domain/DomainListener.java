package cc.alcina.framework.common.client.domain;

public interface DomainListener<E> {
	public abstract Class<? extends E> getListenedClass();

	public abstract void insert(E o);

	public boolean isEnabled();

	public abstract void remove(E o);

	public void setEnabled(boolean enabled);
}
