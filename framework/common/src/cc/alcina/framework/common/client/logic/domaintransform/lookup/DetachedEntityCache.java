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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.domain.MemoryStat;
import cc.alcina.framework.common.client.domain.MemoryStat.MemoryStatProvider;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.EntityLocator;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;

/**
 *
 * @author Nick Reddel
 *
 *         Thread safety. For server-side implementations, modifications of
 *         per-class maps is thread-safe, but modification of the top-level maps
 *         is not. Server-side, the top-level maps should be fully populated
 *         before their contents are populated.
 *
 */
public class DetachedEntityCache implements Serializable, MemoryStatProvider {
	protected Map<Class, Map<Long, Entity>> domain;

	protected Map<Class, Map<Long, Entity>> local;

	// TODO - mvcc.4 - we need these because entries in local can *totally* be
	// vacuumed
	protected Map<Long, Entity> createdLocals;

	private boolean throwOnExisting;

	public DetachedEntityCache() {
		createTopMaps();
	}

	@Override
	public MemoryStat addMemoryStats(MemoryStat parent) {
		MemoryStat self = new MemoryStat(this);
		parent.addChild(self);
		domain.keySet().stream().map(key -> new DomainClassStatWrapper(key,
				domain.get(key), local.get(key)))
				.forEach(w -> w.addMemoryStats(self));
		self.objectMemory.walkStats(this, self.counter, o -> o == this
				|| !self.objectMemory.isMemoryStatProvider(o.getClass()));
		return self;
	}

	public Set<Entity> allValues() {
		Set<Entity> result = new LinkedHashSet<Entity>();
		for (Class clazz : domain.keySet()) {
			result.addAll(domain.get(clazz).values());
			result.addAll(local(clazz, false).values());
		}
		return result;
	}

	protected void checkNegativeIdPut(Entity entity) {
	}

	public void clear() {
		domain.clear();
		local.clear();
		createdLocals.clear();
	}

	public <T extends Entity> boolean contains(Class<T> clazz, long id) {
		ensureMap(clazz);
		return domain.get(clazz).containsKey(id);
	}

	public boolean contains(Entity entity) {
		if (entity == null) {
			return false;
		}
		Class<? extends Entity> clazz = entity.entityClass();
		if (!domain.containsKey(clazz)) {
			return false;
		}
		if (entity.getId() != 0) {
			return domain.get(clazz).containsKey(entity.getId());
		} else {
			return local(clazz, false).containsKey(entity.getLocalId());
		}
	}

	protected Map<Class, Map<Long, Entity>> createClientInstanceClassMap() {
		return new LinkedHashMap<>();
	}

	protected Map<Long, Entity> createIdEntityMap(Class clazz) {
		return new TreeMap<>();
	}

	protected void createTopMaps() {
		domain = new LinkedHashMap<>();
		local = new LinkedHashMap<>();
		createdLocals = new LinkedHashMap<>();
	}

	public void debugNotFound(EntityLocator objectLocator) {
		throw new UnsupportedOperationException();
	}

	public Set<Entry<Class, Map<Long, Entity>>> domainClassEntries() {
		return domain.entrySet();
	}

	protected void ensureMap(Class clazz) {
		if (!domain.containsKey(clazz)) {
			synchronized (domain) {
				if (!domain.containsKey(clazz)) {
					domain.put(clazz, createIdEntityMap(clazz));
					local(clazz, true);
				}
			}
		}
	}

	public void evictCreatedLocal(Entity e) {
		createdLocals.remove(e.getLocalId());
	}

	public <T> List<T> fieldValues(Class<? extends Entity> clazz,
			String propertyName) {
		throw new UnsupportedOperationException();
	}

	public <T> T get(Class<T> clazz, long id) {
		return getUnboxed(clazz, id);
	}

	public <T> T get(Class<T> clazz, long id, long localId) {
		return (T) (id != 0 ? get(clazz, id) : getLocal(clazz, localId));
	}

	public <T> T get(Class<T> clazz, Long id) {
		if (id == null) {
			return null;
		}
		return getUnboxed(clazz, id.longValue());
	}

	public <T> T get(EntityLocator locator) {
		return (T) (locator.id != 0 ? get(locator.getClazz(), locator.getId())
				: getLocal(locator.clazz, locator.localId));
	}

	public Map<Long, Entity> getCreatedLocals() {
		return createdLocals;
	}

	public Map<Class, Map<Long, Entity>> getDomain() {
		return this.domain;
	}

	protected <T> T getLocal(Class<T> clazz, long localId) {
		if (!domain.containsKey(clazz)) {
			return null;
		}
		T t = (T) local(clazz, false).get(localId);
		return t;
	}

	public Map<Long, Entity> getMap(Class clazz) {
		ensureMap(clazz);
		return this.domain.get(clazz);
	}

	private <T> T getUnboxed(Class<T> clazz, long id) {
		Map<Long, Entity> map = domain.get(clazz);
		if (map == null) {
			return null;
		}
		if (map instanceof UnboxedLongMap) {
			return (T) ((UnboxedLongMap) map).get(id);
		} else {
			return (T) map.get(id);
		}
	}

	public boolean hasLocals(Class<?> clazz) {
		return local(clazz, false).values().stream().count() > 0;
	}

	public void invalidate(Class clazz) {
		ensureMap(clazz);
		domain.put(clazz, createIdEntityMap(clazz));
	}

	public boolean isEmpty(Class clazz) {
		ensureMap(clazz);
		return stream(clazz).count() == 0;
	}

	protected boolean isExternalCreate() {
		return false;
	}

	public boolean isThrowOnExisting() {
		return this.throwOnExisting;
	}

	public Set<Long> keys(Class clazz) {
		ensureMap(clazz);
		return domain.get(clazz).keySet();
	}

	private Map<Long, Entity> local(Class clazz, boolean ensure) {
		Map<Class, Map<Long, Entity>> perClass = local;
		if (!perClass.containsKey(clazz)) {
			if (!ensure) {
				return Collections.emptyMap();
			}
			if (!perClass.containsKey(clazz)) {
				perClass.put(clazz, createIdEntityMap(clazz));
			}
		}
		return perClass.get(clazz);
	}

	public List<Long> notContained(Collection<Long> ids, Class clazz) {
		List<Long> result = new ArrayList<Long>();
		ensureMap(clazz);
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
		put0(entity, false);
	}

	protected void put0(Entity entity, boolean external) {
		Class<? extends Entity> clazz = entity.entityClass();
		ensureMap(clazz);
		long id = entity.getId();
		long localId = entity.getLocalId();
		if (id == 0 && localId == 0) {
			throw new RuntimeException("indexing entity with zero id/localid");
		}
		if (id < 0) {
			checkNegativeIdPut(entity);
		}
		if (id != 0) {
			if (throwOnExisting) {
				if (domain.get(clazz).containsKey(id)) {
					throw Ax.runtimeException("Double-put: %s", entity);
				}
			}
			domain.get(clazz).put(id, entity);
		} else {
			Map<Long, Entity> localPerClass = local(clazz, true);
			if (throwOnExisting) {
				if (localPerClass.containsKey(id)) {
					throw Ax.runtimeException("Double-put: %s", entity);
				}
			}
			localPerClass.put(localId, entity);
			external |= isExternalCreate();
			if (!external) {
				Entity existing = createdLocals.get(localId);
				if (existing != null && existing != entity) {
					RuntimeException exception = Ax.runtimeException(
							"DEVEX::1 - Created local collision (!!) - %s %s - existing %s",
							localId, entity, existing);
					if (existing == entity) {
						Ax.out(exception);
						exception.printStackTrace();
					} else {
						throw exception;
					}
				}
				createdLocals.put(localId, entity);
			}
		}
	}

	public void putAll(Class clazz, Collection<? extends Entity> values) {
		values.forEach(this::put);
	}

	public void putForSuperClass(Class clazz, Entity entity) {
		ensureMap(clazz);
		long id = entity.getId();
		Preconditions.checkArgument(id > 0);
		domain.get(clazz).put(id, entity);
	}

	public void remove(Entity entity) {
		Class<? extends Entity> clazz = entity.entityClass();
		ensureMap(clazz);
		long id = entity.getId();
		long localId = entity.getLocalId();
		if (id == 0 && localId == 0) {
			throw new RuntimeException("indexing entity with zero id/localid");
		}
		if (id != 0) {
			domain.get(clazz).remove(id);
		} else {
			local(clazz, true).remove(localId, entity);
			createdLocals.remove(localId, entity);
		}
	}

	public void removeLocal(Entity entity) {
		Class<? extends Entity> clazz = entity.entityClass();
		long localId = entity.getLocalId();
		Preconditions.checkArgument(localId > 0);
		local(clazz, true).remove(localId, entity);
		createdLocals.remove(localId, entity);
	}

	public void setThrowOnExisting(boolean throwOnExisting) {
		this.throwOnExisting = throwOnExisting;
	}

	public int size(Class clazz) {
		ensureMap(clazz);
		return domain.get(clazz).size() + local(clazz, false).size();
	}

	public String sizes() {
		List<String> lines = new ArrayList<String>();
		for (Class clazz : domain.keySet()) {
			lines.add(CommonUtils.simpleClassName(clazz) + ": " + size(clazz));
		}
		return CommonUtils.join(lines, "\n");
	}

	public <T> Stream<T> stream(Class<T> clazz) {
		ensureMap(clazz);
		return (Stream<T>) Stream.concat(domain.get(clazz).values().stream(),
				local(clazz, false).values().stream());
	}

	@Override
	public String toString() {
		return Ax.format("Cache [%s domain classes]", domain.size());
	}

	public static interface CreatedLocalDebug {
		void debugCreation(long localId, Entity entity);
	}

	static class DomainClassStatWrapper implements MemoryStatProvider {
		private Class key;

		private Map<Long, Entity> domain;

		private Map<Long, Entity> local;

		public DomainClassStatWrapper(Class key, Map<Long, Entity> domain,
				Map<Long, Entity> local) {
			this.key = key;
			this.domain = domain;
			this.local = local;
		}

		@Override
		public MemoryStat addMemoryStats(MemoryStat parent) {
			MemoryStat self = new MemoryStat(this);
			parent.addChild(self);
			Stream.concat(domain.values().stream(), local.values().stream())
					.forEach(e -> {
						self.objectMemory.walkStats(e, self.counter, o -> o == e
								|| !Reflections.isAssignableFrom(Entity.class,
										o.getClass()));
					});
			return self;
		}

		@Override
		public String toString() {
			return Ax.format("DomainClassStat: %s", key.getName());
		}
	}
}
