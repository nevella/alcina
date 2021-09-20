/* 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package cc.alcina.framework.common.client.logic.domaintransform;

import java.io.Serializable;
import java.util.Map;

import com.totsp.gwittir.client.beans.Converter;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.CollectionCreators;
import cc.alcina.framework.common.client.util.UnsortedMultikeyMap;

/**
 * 
 * @author nick@alcina.cc
 * 
 */
public class EntityLocatorMap implements Serializable {
	private Map<Long, EntityLocator> localToPersistent = Registry
			.impl(CollectionCreators.HashMapCreator.class).create();

	private UnsortedMultikeyMap<EntityLocator> persistentToLocal = new UnsortedMultikeyMap<>(
			2);

	public EntityLocatorMap() {
	}

	public synchronized boolean containsKey(Long localId) {
		return localToPersistent.containsKey(localId);
	}

	/*
	 * Only called by test servers
	 */
	public synchronized EntityLocatorMap copy() {
		EntityLocatorMap clone = new EntityLocatorMap();
		clone.localToPersistent = Registry
				.impl(CollectionCreators.HashMapCreator.class)
				.copy(localToPersistent);
		clone.persistentToLocal = persistentToLocal.clone();
		return clone;
	}

	public synchronized EntityLocator getFor(Entity entity) {
		return localToPersistent.get(entity.getLocalId());
	}

	public synchronized EntityLocator getForLocalId(Long localId) {
		return localToPersistent.get(localId);
	}

	public long getLocalId(Class<? extends Entity> clazz, long id) {
		EntityLocator entityLocator = persistentToLocal.get(clazz, id);
		if (entityLocator != null) {
			return entityLocator.localId;
		}
		return 0;
	}

	public synchronized <H extends Entity> long
			getLocalIdForClientInstance(H entity) {
		if (entity.getLocalId() != 0) {
			return entity.getLocalId();
		}
		EntityLocator entityLocator = persistentToLocal.get(entity.getClass(),
				entity.getId());
		if (entityLocator != null) {
			return entityLocator.localId;
		}
		return 0;
	}

	public synchronized EntityLocator getPersistentLocator(Entity entity) {
		if (entity.getId() != 0) {
			return entity.toLocator();
		} else {
			return localToPersistent.get(entity.getLocalId());
		}
	}

	public synchronized void merge(EntityLocatorMap other) {
		localToPersistent.putAll(other.localToPersistent);
		persistentToLocal.putMulti(other.persistentToLocal);
	}

	public synchronized void putToLookups(EntityLocator entityLocator) {
		localToPersistent.put(entityLocator.localId, entityLocator);
		if (entityLocator.id != 0) {
			persistentToLocal.put(entityLocator.clazz, entityLocator.id,
					entityLocator);
		}
	}

	public void remove(EntityLocator locator) {
		EntityLocator toPersistent = localToPersistent
				.remove(locator.getLocalId());
		if (toPersistent != null) {
			localToPersistent.remove(toPersistent.getId());
		}
	}

	public static class ToCreatedIdConverter<H extends Entity>
			implements Converter<H, Long> {
		private EntityLocatorMap map;

		public ToCreatedIdConverter(EntityLocatorMap map) {
			this.map = map;
		}

		@Override
		public Long convert(H original) {
			return map.containsKey(original.getLocalId())
					? map.getForLocalId(original.getLocalId()).getId()
					: null;
		}
	}
}