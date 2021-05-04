package cc.alcina.framework.common.client.domain;

import cc.alcina.framework.common.client.logic.domain.Entity;

public interface DomainListener<E extends Entity> {
	public abstract Class<? extends E> getListenedClass();

	public abstract Object insert(E o);

	public boolean isEnabled();

	public abstract Object remove(E o);

	public void setEnabled(boolean enabled);
}
