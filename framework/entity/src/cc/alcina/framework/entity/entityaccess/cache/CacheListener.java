package cc.alcina.framework.entity.entityaccess.cache;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;

public interface CacheListener<H extends HasIdAndLocalId> {

	public abstract Class<H> getListenedClass();

	public abstract void insert(H o);
	
	public abstract void remove(H o);
	
	public boolean isEnabled();
	
	public void setEnabled(boolean enabled);

	boolean matches(H h, Object[] keys);

}
