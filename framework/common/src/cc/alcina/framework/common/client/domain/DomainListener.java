package cc.alcina.framework.common.client.domain;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;

public interface DomainListener<H extends HasIdAndLocalId> {
	public abstract Class<H> getListenedClass();

	public abstract void insert(H o);

	public boolean isEnabled();

	public abstract void remove(H o);

	public void setEnabled(boolean enabled);

	boolean matches(H h, Object[] keys);
}
