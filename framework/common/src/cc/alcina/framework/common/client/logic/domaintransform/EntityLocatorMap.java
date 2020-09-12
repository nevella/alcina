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
import java.util.HashMap;

import com.totsp.gwittir.client.beans.Converter;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.util.UnsortedMultikeyMap;

/**
 * The key is the (client's) localid of the Entity Most methods are "local to
 * persistent" - except where marked
 * 
 * @author nick@alcina.cc
 * 
 */
public class EntityLocatorMap implements Serializable {
	static final transient long serialVersionUID = 1;

	private HashMap<Long, EntityLocator> localToPersistent = new HashMap<>();

	private UnsortedMultikeyMap<EntityLocator> persistentToLocal = new UnsortedMultikeyMap<>(
			2);

	public EntityLocatorMap() {
	}

	public synchronized void clear() {
		localToPersistent.clear();
		persistentToLocal.clear();
	}

	public synchronized boolean containsKey(Long localId) {
		return localToPersistent.containsKey(localId);
	}

	public synchronized EntityLocatorMap copy() {
		EntityLocatorMap clone = new EntityLocatorMap();
		clone.localToPersistent = (HashMap<Long, EntityLocator>) localToPersistent
				.clone();
		clone.persistentToLocal = persistentToLocal.clone();
		return clone;
	}

	public synchronized EntityLocator getFor(Entity entity) {
		return localToPersistent.get(entity.getLocalId());
	}

	public synchronized EntityLocator getFor(ObjectRef ref) {
		long id = ref.getId();
		if (id != 0) {
			return new EntityLocator(ref.getClassRef().getRefClass(), id,
					ref.getLocalId());
		}
		return localToPersistent.get(ref.getLocalId());
	}

	public synchronized EntityLocator getForLocalId(Long localId) {
		return localToPersistent.get(localId);
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

	public synchronized boolean isEmpty() {
		return localToPersistent.isEmpty();
	}

	public synchronized void merge(EntityLocatorMap locatorMap) {
		putAll(locatorMap);
	}

	public synchronized void putAll(EntityLocatorMap other) {
		localToPersistent.putAll(other.localToPersistent);
		persistentToLocal.putMulti(other.persistentToLocal);
	}

	public synchronized void putToLookups(EntityLocator entityLocator) {
		localToPersistent.put(entityLocator.localId, entityLocator);
		persistentToLocal.put(entityLocator.clazz, entityLocator.id,
				entityLocator);
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