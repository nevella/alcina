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
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;

@SuppressWarnings("unchecked")
/**
 *
 * @author Nick Reddel
 */
public class DetachedEntityCache implements Serializable {
	protected Map<Class, Map<Long, HasIdAndLocalId>> detached = new HashMap<Class, Map<Long, HasIdAndLocalId>>();

	public DetachedEntityCache() {
	}

	private static transient DetachedEntityCache commonInstance;

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
		if (id == null) {
			return null;
		}
		T t = (T) detached.get(clazz).get(id);
		return t;
	}

	public <T> Set<T> values(Class<T> clazz) {
		ensureMaps(clazz);
		return new LinkedHashSet<T>((Collection<? extends T>) detached.get(
				clazz).values());
	}

	public <T> Collection<T> rawValues(Class<T> clazz) {
		ensureMaps(clazz);
		return (Collection<T>) detached.get(clazz).values();
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
			detached.put(clazz, new TreeMap<Long, HasIdAndLocalId>());
		}
	}

	public boolean isEmpty(Class clazz) {
		ensureMaps(clazz);
		return values(clazz).isEmpty();
	}

	public void invalidate(Class clazz) {
		ensureMaps(clazz);
		detached.put(clazz, new TreeMap<Long, HasIdAndLocalId>());
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
}
