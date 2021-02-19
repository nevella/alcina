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
import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LooseContext;

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
	public static final transient String CONTEXT_CREATED_LOCAL_DEBUG = DetachedEntityCache.class
			.getName() + ".CONTEXT_CREATED_LOCAL_DEBUG";

	protected Map<Class, Map<Long, Entity>> domain;

	protected Map<Class, Map<Long, Entity>> local;

	protected Map<Long, Entity> createdLocals;

	private boolean throwOnExisting;

	public DetachedEntityCache() {
		createTopMaps();
	}

	@Override
	public MemoryStat addMemoryStats(MemoryStat parent) {
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
			result.addAll(local(clazz, false).values());
		}
		return result;
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

	public void debugNotFound(EntityLocator objectLocator) {
		throw new UnsupportedOperationException();
	}

	public Set<Entry<Class, Map<Long, Entity>>> domainClassEntries() {
		return domain.entrySet();
	}

	public <T> List<T> fieldValues(Class<? extends Entity> clazz,
			String propertyName) {
		throw new UnsupportedOperationException();
	}

	public <T> T get(Class<T> clazz, Long id) {
		if (!domain.containsKey(clazz)) {
			return null;
		}
		if (id == null) {
			return null;
		}
		T t = (T) domain.get(clazz).get(id);
		return t;
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

	public Map<Long, Entity> getMap(Class clazz) {
		ensureMap(clazz);
		return this.domain.get(clazz);
	}

	public void invalidate(Class clazz) {
		ensureMap(clazz);
		domain.put(clazz, createIdEntityMap(clazz));
	}

	public boolean isEmpty(Class clazz) {
		ensureMap(clazz);
		return stream(clazz).count() == 0;
	}

	public boolean isThrowOnExisting() {
		return this.throwOnExisting;
	}

	public Set<Long> keys(Class clazz) {
		ensureMap(clazz);
		return domain.get(clazz).keySet();
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
		return "Cache: " + domain;
	}

	public <T> Set<T> values(Class<T> clazz) {
		ensureMap(clazz);
		return stream(clazz).collect(AlcinaCollectors.toLinkedHashSet());
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

	protected void ensureMap(Class clazz) {
		if (!domain.containsKey(clazz)) {
			if (!domain.containsKey(clazz)) {
				domain.put(clazz, createIdEntityMap(clazz));
				local(clazz, true);
			}
		}
	}

	protected <T> T getLocal(Class<T> clazz, long localId) {
		if (!domain.containsKey(clazz)) {
			return null;
		}
		T t = (T) local(clazz, false).get(localId);
		return t;
	}

	protected boolean isExternalCreate() {
		return false;
	}

	protected void put0(Entity entity, boolean external) {
		Class<? extends Entity> clazz = entity.entityClass();
		ensureMap(clazz);
		long id = entity.getId();
		long localId = entity.getLocalId();
		if (id == 0 && localId == 0) {
			throw new RuntimeException("indexing entity with zero id/localid");
		}
		// these will not be put in to-domain phase transactions, so are
		// harmless
		// if (id < 0) {
		// throw new RuntimeException("indexing entity with negative id");
		// }
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
				if (LooseContext.has(CONTEXT_CREATED_LOCAL_DEBUG)) {
					((CreatedLocalDebug) LooseContext
							.get(CONTEXT_CREATED_LOCAL_DEBUG))
									.debugCreation(localId, entity);
				}
				if (createdLocals.containsKey(localId)) {
					throw Ax.runtimeException(
							"DEVEX::1 - Created local collision (!!) - %s %s - existing %s",
							localId, entity, createdLocals.get(localId));
				}
				createdLocals.put(localId, entity);
			}
		}
	}

	public static interface CreatedLocalDebug {
		void debugCreation(long localId, Entity entity);
	}
}
