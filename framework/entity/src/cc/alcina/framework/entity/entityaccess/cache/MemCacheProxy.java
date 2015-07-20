package cc.alcina.framework.entity.entityaccess.cache;

import java.beans.PropertyChangeEvent;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;

public interface MemCacheProxy<T> extends HasIdAndLocalId{
	public T nonProxy();
	public void checkPropertyChange(PropertyChangeEvent propertyChangeEvent);
}
