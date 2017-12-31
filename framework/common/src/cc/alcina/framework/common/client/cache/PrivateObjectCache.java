package cc.alcina.framework.common.client.cache;

import java.util.Map;

import cc.alcina.framework.common.client.cache.CacheCreators.CacheIdMapCreator;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

/**
 * Objects that are not part of the main domain, but used similarly (e.g. sync)
 * 
 * @author nick@alcina.cc
 *
 */
public interface PrivateObjectCache {
	public <T> T get(Class<T> clazz, Long id);

	public <T extends HasIdAndLocalId> T getExisting(T hili);

	void put(HasIdAndLocalId hili);

	void putForSuperClass(Class clazz, HasIdAndLocalId hili);

	public static class PrivateObjectCacheSingleClass
			implements PrivateObjectCache {
		Map map = Registry.impl(CacheIdMapCreator.class).get();

		@Override
		public <T> T get(Class<T> clazz, Long id) {
			return (T) map.get(id);
		}

		@Override
		public <T extends HasIdAndLocalId> T getExisting(T hili) {
			return (T) map.get(hili.getId());
		}

		@Override
		public void put(HasIdAndLocalId hili) {
			map.put(hili.getId(), hili);
		}

		@Override
		public void putForSuperClass(Class clazz, HasIdAndLocalId hili) {
			put(hili);
		}
	}
}
