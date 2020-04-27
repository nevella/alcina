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
import java.util.stream.Stream;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.domain.MemoryStat;
import cc.alcina.framework.common.client.domain.MemoryStat.MemoryStatProvider;
import cc.alcina.framework.common.client.domain.MemoryStat.StatType;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.EntityLocator;
import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;

/**
 *
 * @author Nick Reddel
 */
public class DetachedEntityCache implements Serializable, MemoryStatProvider {
	protected Map<Class, Map<Long, Entity>> domain;

	protected Map<Class, Map<Long, Entity>> local;

	private transient Supplier<Map> classMapSupplier;

	private boolean throwOnExisting;

	public DetachedEntityCache() {
		this(new DefaultTopMapSupplier(), new DefaultClassMapSupplier());
	}

	public DetachedEntityCache(Supplier<Map> topMapSupplier,
			Supplier<Map> classMapSupplier) {
		this.classMapSupplier = classMapSupplier;
		this.domain = topMapSupplier.get();
		this.local = topMapSupplier.get();
	}

	@Override
	public MemoryStat addMemoryStats(MemoryStat parent, StatType type) {
		MemoryStat self = new MemoryStat(this);
		parent.addChild(self);
		self.objectMemory.walkStats(this, self.counter, o -> o == this
				|| !self.objectMemory.isMemoryStatProvider(o.getClass()));
		return self;
	}

	public Set<Entity> allValues() {
		Set<Entity> result = new LinkedHashSet<Entity>();
		for (Class clazz : domain.keySet()) {
			result.addAll(domain.get(clazz).values());
			result.addAll(local.get(clazz).values());
		}
		return result;
	}

	public void clear() {
		domain.clear();
		local.clear();
	}

	public <T extends Entity> boolean contains(Class<T> clazz, long id) {
		ensureMaps(clazz);
		return domain.get(clazz).containsKey(id);
	}

	public boolean contains(Entity entity) {
		Class<? extends Entity> clazz = entity.provideEntityClass();
		ensureMaps(clazz);
		long id = entity.getId();
		Preconditions.checkArgument(id > 0);
		return domain.get(clazz).containsKey(id);
	}

	public Set<Entry<Class, Map<Long, Entity>>> domainClassEntries() {
		return domain.entrySet();
	}

	public <T> List<T> fieldValues(Class<? extends Entity> clazz,
			String propertyName) {
		throw new UnsupportedOperationException();
	}

	public <T> T get(Class<T> clazz, Long id) {
		ensureMaps(clazz);
		if (id == null) {
			return null;
		}
		T t = (T) domain.get(clazz).get(id);
		return t;
	}

	public <T> T get(EntityLocator locator) {
		return (T) get(locator.getClazz(), locator.getId());
	}

	public Map<Long, Entity> getCreatedLocalsSnapshot() {
		Map<Long, Entity> result = new HashMap<>();
		local.values().forEach(result::putAll);
		return result;
	}

	public Map<Class, Map<Long, Entity>> getDomain() {
		return this.domain;
	}

	public Map<Long, Entity> getMap(Class clazz) {
		ensureMaps(clazz);
		return this.domain.get(clazz);
	}

	public <T> Collection<T> immutableRawValues(Class<T> clazz) {
		ensureMaps(clazz);
		return (Collection<T>) Collections
				.unmodifiableCollection(domain.get(clazz).values());
	}

	public void invalidate(Class clazz) {
		ensureMaps(clazz);
		domain.put(clazz, createMap());
	}

	public boolean isEmpty(Class clazz) {
		ensureMaps(clazz);
		return values(clazz).isEmpty();
	}

	public boolean isThrowOnExisting() {
		return this.throwOnExisting;
	}

	public Set<Long> keys(Class clazz) {
		ensureMaps(clazz);
		return domain.get(clazz).keySet();
	}

	public <T> List<T> list(Class<T> clazz, Collection<Long> ids) {
		return ids.stream().map(id -> get(clazz, id))
				.collect(Collectors.toList());
	}

	public List<Long> notContained(Collection<Long> ids, Class clazz) {
		List<Long> result = new ArrayList<Long>();
		ensureMaps(clazz);
		Set<Long> existing = domain.get(clazz).keySet();
		// can be reasonably confident size(existing)>size(ids)
		for (Long id : ids) {
			if (!existing.contains(id)) {
				result.add(id);
			}
		}
		return result;
	}

	public void put(Entity entity) {
		Class<? extends Entity> clazz = entity.provideEntityClass();
		ensureMaps(clazz);
		long id = entity.getId();
		long localId = entity.getLocalId();
		if (id == 0 && localId == 0) {
			throw new RuntimeException("indexing entity with zero id/localid");
		}
		if (id < 0) {
			throw new RuntimeException("indexing entity with negative id");
		}
		if (id != 0) {
			if (throwOnExisting) {
				if (domain.get(clazz).containsKey(id)) {
					throw Ax.runtimeException("Double-put: %s", entity);
				}
			}
			domain.get(clazz).put(id, entity);
		} else {
			if (throwOnExisting) {
				if (local.get(clazz).containsKey(id)) {
					throw Ax.runtimeException("Double-put: %s", entity);
				}
			}
			local.get(clazz).put(localId, entity);
		}
	}

	public void putAll(Class clazz, Collection<? extends Entity> values) {
		values.forEach(this::put);
	}

	public void putForSuperClass(Class clazz, Entity entity) {
		ensureMaps(clazz);
		long id = entity.getId();
		Preconditions.checkArgument(id > 0);
		domain.get(clazz).put(id, entity);
	}

	public void remove(Entity entity) {
		Class<? extends Entity> clazz = entity.provideEntityClass();
		ensureMaps(clazz);
		long id = entity.getId();
		long localId = entity.getLocalId();
		if (id == 0 && localId == 0) {
			throw new RuntimeException("indexing entity with zero id/localid");
		}
		if (id < 0) {
			throw new RuntimeException("indexing entity with negative id");
		}
		if (id != 0) {
			domain.get(clazz).remove(id);
		} else {
			local.get(clazz).remove(localId, entity);
		}
	}

	public void setThrowOnExisting(boolean throwOnExisting) {
		this.throwOnExisting = throwOnExisting;
	}

	public int size(Class clazz) {
		ensureMaps(clazz);
		return domain.get(clazz).size() + local.get(clazz).size();
	}

	public String sizes() {
		List<String> lines = new ArrayList<String>();
		for (Class clazz : domain.keySet()) {
			lines.add(CommonUtils.simpleClassName(clazz) + ": " + size(clazz));
		}
		return CommonUtils.join(lines, "\n");
	}

	public <T> Stream<T> stream(Class<T> clazz) {
		ensureMaps(clazz);
		return (Stream<T>) Stream.concat(domain.get(clazz).values().stream(),
				local.get(clazz).values().stream());
	}

	@Override
	public String toString() {
		return "Cache: " + domain;
	}

	public <T> Set<T> values(Class<T> clazz) {
		ensureMaps(clazz);
		return stream(clazz).collect(AlcinaCollectors.toLinkedHashSet());
	}

	protected Map<Long, Entity> createMap() {
		return classMapSupplier.get();
	}

	protected void ensureMaps(Class clazz) {
		if (!domain.containsKey(clazz)) {
			synchronized (this) {
				if (!domain.containsKey(clazz)) {
					domain.put(clazz, createMap());
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
			return new HashMap<Class, Map<Long, Entity>>(128);
		}
	}
}
