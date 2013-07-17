package cc.alcina.framework.entity.entityaccess.cache;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;

public interface CacheListener<T extends HasIdAndLocalId> {

	public abstract Class<T> getListenedClass();

	public abstract void insert(T o);
	
	public abstract void remove(T o);
}
