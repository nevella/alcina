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
package cc.alcina.framework.entity.entityaccess;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;

@SuppressWarnings("unchecked")
/**
 *
 * @author Nick Reddel
 */
public class DetachedEntityCache {
	private Map<Class, Map<Long, HasIdAndLocalId>> detached = new HashMap<Class, Map<Long, HasIdAndLocalId>>();

	private Map<Class, Map<Long, HasIdAndLocalId>> entities = new HashMap<Class, Map<Long, HasIdAndLocalId>>();

	public DetachedEntityCache() {
	}

	private static DetachedEntityCache commonInstance;

	public static DetachedEntityCache get() {
		if (commonInstance == null) {
			commonInstance = new DetachedEntityCache();
		}
		return commonInstance;
	}

	public void appShutdown() {
		commonInstance = null;
	}

	public <T> T get(Class<T> clazz, Long id) {
		ensureMaps(clazz);
		T t = (T) detached.get(clazz).get(id);
		if (t == null) {
			t = (T) entities.get(clazz).get(id);
		}
		return t;
	}

	public <T> Set<T> values(Class<T> clazz) {
		ensureMaps(clazz);
		if (detached.get(clazz).isEmpty()) {
			return new LinkedHashSet<T>((Collection<? extends T>) entities.get(
					clazz).values());
		} else {
			return new LinkedHashSet<T>((Collection<? extends T>) detached.get(
					clazz).values());
		}
	}

	public Set<Long> keys(Class clazz) {
		ensureMaps(clazz);
		if (detached.get(clazz).isEmpty()) {
			return entities.get(clazz).keySet();
		} else {
			return detached.get(clazz).keySet();
		}
	}

	public void put(HasIdAndLocalId hili) {
		Class<? extends HasIdAndLocalId> clazz = hili.getClass();
		ensureMaps(clazz);
		long id = hili.getId();
		detached.get(clazz).put(id, hili);
	}

	public void putAll(Class clazz, Collection<? extends HasIdAndLocalId> values) {
		ensureMaps(clazz);
		Map<Long, HasIdAndLocalId> m = entities.get(clazz);
		for (HasIdAndLocalId hili : values) {
			long id = hili.getId();
			m.put(hili.getId(), hili);
		}
	}

	private void ensureMaps(Class clazz) {
		if (!detached.containsKey(clazz)) {
			detached.put(clazz, new TreeMap<Long, HasIdAndLocalId>());
		}
		if (!entities.containsKey(clazz)) {
			entities.put(clazz, new TreeMap<Long, HasIdAndLocalId>());
		}
	}

	public boolean isEmpty(Class clazz) {
		ensureMaps(clazz);
		return values(clazz).isEmpty();
	}

	public void invalidate(Class clazz) {
		ensureMaps(clazz);
		detached.put(clazz, new TreeMap<Long, HasIdAndLocalId>());
		entities.put(clazz, new TreeMap<Long, HasIdAndLocalId>());
	}

	public boolean clearEntities() {
		boolean hasValues = false;
		for (Entry<Class, Map<Long, HasIdAndLocalId>> entry : entities
				.entrySet()) {
			hasValues |= !entry.getValue().isEmpty();
			entry.getValue().clear();
		}
		return hasValues;
	}

	public void clear() {
		detached.clear();
		entities.clear();
	}

	public DetachedEntityCache clone() {
		DetachedEntityCache c = new DetachedEntityCache();
		c.detached = (Map) ((HashMap<Class, Map<Long, HasIdAndLocalId>>) detached)
				.clone();
		return c;
	}

	public void invalidate(Class[] classes) {
		for (Class c : classes) {
			invalidate(c);
		}
	}
}
