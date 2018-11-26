package cc.alcina.framework.entity.entityaccess.cache;

import java.util.Set;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;

interface MultiplexableCache {
	public <T> T get(Class<T> clazz, Long id);

	public Set<Long> keys(Class clazz);

	public void put(HasIdAndLocalId hili);

	public void remove(HasIdAndLocalId hili);
}