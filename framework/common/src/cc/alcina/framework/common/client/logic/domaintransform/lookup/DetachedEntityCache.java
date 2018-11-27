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
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.domain.PrivateObjectCache;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.util.CommonUtils;

@SuppressWarnings("unchecked")
/**
 *
 * @author Nick Reddel
 */
public class DetachedEntityCache implements Serializable, PrivateObjectCache {
	protected Map<Class, Map<Long, HasIdAndLocalId>> detached;

	private transient Supplier<Map> classMapSupplier;

	public DetachedEntityCache() {
		this(new DefaultTopMapSupplier(), new DefaultClassMapSupplier());
	}

	public DetachedEntityCache(Supplier<Map> topMapSupplier,
			Supplier<Map> classMapSupplier) {
		this.classMapSupplier = classMapSupplier;
		this.detached = topMapSupplier.get();
	}

	public Set<HasIdAndLocalId> allValues() {
		Set<HasIdAndLocalId> result = new LinkedHashSet<HasIdAndLocalId>();
		for (Class clazz : detached.keySet()) {
			result.addAll(detached.get(clazz).values());
		}
		return result;
	}

	public Set<Entry<Class, Map<Long, HasIdAndLocalId>>> classEntries() {
		return detached.entrySet();
	}

	public void clear() {
		detached.clear();
	}

	public <T extends HasIdAndLocalId> boolean contains(Class<T> clazz,
			long id) {
		ensureMaps(clazz);
		return detached.get(clazz).containsKey(id);
	}

	public boolean contains(HasIdAndLocalId hili) {
		Class<? extends HasIdAndLocalId> clazz = hili.getClass();
		ensureMaps(clazz);
		long id = hili.getId();
		return detached.get(clazz).containsKey(id);
	}

	public <T extends HasIdAndLocalId> boolean containsMap(Class<T> clazz) {
		return detached.containsKey(clazz);
	}

	public Map<Long, HasIdAndLocalId> createMap() {
		return classMapSupplier.get();
	}

	public <T> List<T> fieldValues(Class<? extends HasIdAndLocalId> clazz,
			String propertyName) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> T get(Class<T> clazz, Long id) {
		ensureMaps(clazz);
		if (id == null) {
			return null;
		}
		T t = (T) detached.get(clazz).get(id);
		return t;
	}

	public Map<Class, Map<Long, HasIdAndLocalId>> getDetached() {
		return this.detached;
	}

	@Override
	public <T extends HasIdAndLocalId> T getExisting(T hili) {
		return (T) get(hili.getClass(), hili.getId());
	}

	public Map<Long, HasIdAndLocalId> getMap(Class clazz) {
		ensureMaps(clazz);
		return this.detached.get(clazz);
	}

	public <T> Collection<T> immutableRawValues(Class<T> clazz) {
		ensureMaps(clazz);
		return (Collection<T>) Collections
				.unmodifiableCollection(detached.get(clazz).values());
	}

	public void invalidate(Class clazz) {
		ensureMaps(clazz);
		detached.put(clazz, createMap());
	}

	public void invalidate(Class[] classes) {
		for (Class c : classes) {
			invalidate(c);
		}
	}

	public boolean isEmpty(Class clazz) {
		ensureMaps(clazz);
		return values(clazz).isEmpty();
	}

	public Set<Long> keys(Class clazz) {
		ensureMaps(clazz);
		return detached.get(clazz).keySet();
	}

	public <T> List<T> list(Class<T> clazz, Collection<Long> ids) {
		return ids.stream().map(id -> get(clazz, id))
				.collect(Collectors.toList());
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

	@Override
	public void put(HasIdAndLocalId hili) {
		Class<? extends HasIdAndLocalId> clazz = hili.getClass();
		ensureMaps(clazz);
		long id = hili.getId();
		detached.get(clazz).put(id, hili);
	}

	public void putAll(Class clazz,
			Collection<? extends HasIdAndLocalId> values) {
		ensureMaps(clazz);
		Map<Long, HasIdAndLocalId> m = detached.get(clazz);
		for (HasIdAndLocalId hili : values) {
			long id = hili.getId();
			m.put(hili.getId(), hili);
		}
	}

	@Override
	public void putForSuperClass(Class clazz, HasIdAndLocalId hili) {
		ensureMaps(clazz);
		long id = hili.getId();
		detached.get(clazz).put(id, hili);
	}

	public void remove(HasIdAndLocalId hili) {
		Class<? extends HasIdAndLocalId> clazz = hili.getClass();
		ensureMaps(clazz);
		long id = hili.getId();
		detached.get(clazz).remove(id);
	}

	public int size(Class clazz) {
		ensureMaps(clazz);
		return detached.get(clazz).size();
	}

	public String sizes() {
		List<String> lines = new ArrayList<String>();
		for (Class clazz : detached.keySet()) {
			lines.add(CommonUtils.simpleClassName(clazz) + ": " + size(clazz));
		}
		return CommonUtils.join(lines, "\n");
	}

	@Override
	public String toString() {
		return "Cache: " + detached;
	}

	public <T> Set<T> values(Class<T> clazz) {
		ensureMaps(clazz);
		return new LinkedHashSet<T>(
				(Collection<? extends T>) detached.get(clazz).values());
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

	static class DefaultClassMapSupplier implements Supplier<Map> {
		@Override
		public Map get() {
			return new TreeMap();
		}
	}

	static class DefaultTopMapSupplier implements Supplier<Map> {
		@Override
		public Map get() {
			return new HashMap<Class, Map<Long, HasIdAndLocalId>>(128);
		}
	}
}
