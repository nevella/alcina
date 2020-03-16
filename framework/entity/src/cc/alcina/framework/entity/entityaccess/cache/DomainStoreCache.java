package cc.alcina.framework.entity.entityaccess.cache;

import java.util.List;
import java.util.Set;

import cc.alcina.framework.common.client.logic.domain.Entity;

interface DomainStoreCache {
	public <T> List<T> fieldValues(Class<? extends Entity> clazz,
			String propertyName);

	public <T> T get(Class<T> clazz, Long id);

	public Set<Long> keys(Class clazz);

	public void put(Entity entity);

	public void remove(Entity entity);
}