package cc.alcina.framework.common.client.domain;

import cc.alcina.framework.common.client.logic.domain.Entity;

public interface DomainListener<E extends Entity> {
	public abstract Class<E> getListenedClass();

	public abstract void insert(E o);

	public boolean isEnabled();

	public abstract void remove(E o);

	public void setEnabled(boolean enabled);

	boolean matches(E h, Object[] keys);
}
