package cc.alcina.framework.common.client.domain;

import java.util.Map;

import cc.alcina.framework.common.client.domain.DomainStoreCreators.DomainStoreIdMapCreator;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.EntityLocator;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

/**
 * Objects that are not part of the main domain, but used similarly (e.g. sync)
 * 
 * @author nick@alcina.cc
 *
 */
public interface PrivateObjectCache {
	public <T> T get(Class<T> clazz, Long id);

	public <T extends Entity> T getExisting(T entity);

	default <T extends Entity> T get(EntityLocator locator) {
		return (T) get(locator.clazz, locator.id);
	}

	void put(Entity entity);

	void putForSuperClass(Class clazz, Entity entity);

	public static class PrivateObjectCacheSingleClass
			implements PrivateObjectCache {
		Map map = Registry.impl(DomainStoreIdMapCreator.class).get();

		@Override
		public <T> T get(Class<T> clazz, Long id) {
			return (T) map.get(id);
		}

		@Override
		public <T extends Entity> T getExisting(T entity) {
			return (T) map.get(entity.getId());
		}

		@Override
		public void put(Entity entity) {
			map.put(entity.getId(), entity);
		}

		@Override
		public void putForSuperClass(Class clazz, Entity entity) {
			put(entity);
		}
	}
}
