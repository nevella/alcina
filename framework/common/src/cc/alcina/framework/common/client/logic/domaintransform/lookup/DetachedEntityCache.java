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
package cc.alcina.framework.common.client.logic.domaintransform.lookup;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import cc.alcina.framework.common.client.cache.PrivateObjectCache;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.util.CommonUtils;

@SuppressWarnings("unchecked")
/**
 *
 * @author Nick Reddel
 */
public class DetachedEntityCache implements Serializable,PrivateObjectCache {
	// have it distributed
	protected Map<Class, Map<Long, HasIdAndLocalId>> detached = new HashMap<Class, Map<Long, HasIdAndLocalId>>(
			128);

	public DetachedEntityCache() {
	}

	public <T> T get(Class<T> clazz, Long id) {
		ensureMaps(clazz);
		if (id == null) {
			return null;
		}
		T t = (T) detached.get(clazz).get(id);
		return t;
	}

	public <T extends HasIdAndLocalId> T getExisting(T hili) {
		return (T) get(hili.getClass(), hili.getId());
	}

	public <T> Set<T> values(Class<T> clazz) {
		ensureMaps(clazz);
		return new LinkedHashSet<T>((Collection<? extends T>) detached.get(
				clazz).values());
	}

	public <T> Collection<T> immutableRawValues(Class<T> clazz) {
		ensureMaps(clazz);
		return (Collection<T>) Collections.unmodifiableCollection(detached.get(
				clazz).values());
	}

	public Set<HasIdAndLocalId> allValues() {
		Set<HasIdAndLocalId> result = new LinkedHashSet<HasIdAndLocalId>();
		for (Class clazz : detached.keySet()) {
			result.addAll(detached.get(clazz).values());
		}
		return result;
	}

	public Set<Long> keys(Class clazz) {
		ensureMaps(clazz);
		return detached.get(clazz).keySet();
	}

	public void put(HasIdAndLocalId hili) {
		Class<? extends HasIdAndLocalId> clazz = hili.getClass();
		ensureMaps(clazz);
		long id = hili.getId();
		detached.get(clazz).put(id, hili);
	}

	public void putForSuperClass(Class clazz, HasIdAndLocalId hili) {
		ensureMaps(clazz);
		long id = hili.getId();
		detached.get(clazz).put(id, hili);
	}

	public void putAll(Class clazz, Collection<? extends HasIdAndLocalId> values) {
		ensureMaps(clazz);
		Map<Long, HasIdAndLocalId> m = detached.get(clazz);
		for (HasIdAndLocalId hili : values) {
			long id = hili.getId();
			m.put(hili.getId(), hili);
		}
	}

	protected void ensureMaps(Class clazz) {
		if (!detached.containsKey(clazz)) {
			synchronized (this) {
				if (!detached.containsKey(clazz)) {
					detached.put(clazz, createMap());
				}
			}
		}
	}

	public Map<Long, HasIdAndLocalId> createMap() {
		return new TreeMap<Long, HasIdAndLocalId>();
	}

	public boolean isEmpty(Class clazz) {
		ensureMaps(clazz);
		return values(clazz).isEmpty();
	}

	public void invalidate(Class clazz) {
		ensureMaps(clazz);
		detached.put(clazz, createMap());
	}

	public void clear() {
		detached.clear();
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

	public Map<Class, Map<Long, HasIdAndLocalId>> getDetached() {
		return this.detached;
	}

	public Map<Long, HasIdAndLocalId> getMap(Class clazz) {
		ensureMaps(clazz);
		return this.detached.get(clazz);
	}

	public int size(Class clazz) {
		ensureMaps(clazz);
		return detached.get(clazz).size();
	}

	public void remove(HasIdAndLocalId hili) {
		Class<? extends HasIdAndLocalId> clazz = hili.getClass();
		ensureMaps(clazz);
		long id = hili.getId();
		detached.get(clazz).remove(id);
	}

	public boolean contains(HasIdAndLocalId hili) {
		Class<? extends HasIdAndLocalId> clazz = hili.getClass();
		ensureMaps(clazz);
		long id = hili.getId();
		return detached.get(clazz).containsKey(id);
	}

	@Override
	public String toString() {
		return "Cache: " + detached;
	}

	public List<Long> notContained(Collection<Long> ids, Class clazz) {
		List<Long> result = new ArrayList<Long>();
		ensureMaps(clazz);
		Set<Long> existing = detached.get(clazz).keySet();
		// can be reasonably confident size(existing)>size(ids)
		for (Long id : ids) {
			if (!existing.contains(id)) {
				result.add(id);
			}
		}
		return result;
	}

	public String sizes() {
		List<String> lines = new ArrayList<String>();
		for (Class clazz : detached.keySet()) {
			lines.add(CommonUtils.simpleClassName(clazz) + ": " + size(clazz));
		}
		return CommonUtils.join(lines, "\n");
	}

	
}
