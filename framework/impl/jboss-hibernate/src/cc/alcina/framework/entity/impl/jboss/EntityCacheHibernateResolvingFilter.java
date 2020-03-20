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
package cc.alcina.framework.entity.impl.jboss;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;

import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedEntityCache;
import cc.alcina.framework.entity.entityaccess.cache.DomainStore;
import cc.alcina.framework.entity.entityaccess.cache.mvcc.MvccObject;
import cc.alcina.framework.entity.projection.GraphProjection;
import cc.alcina.framework.entity.projection.GraphProjection.GraphProjectionContext;
import cc.alcina.framework.entity.projection.GraphProjection.InstantiateImplCallback;
import cc.alcina.framework.entity.projection.GraphProjection.InstantiateImplCallbackWithShellObject;


/**
 *
 * @author Nick Reddel
 */
public class EntityCacheHibernateResolvingFilter extends Hibernate4CloneFilter {
	private DetachedEntityCache cache;

	private Map<? extends Entity, ? extends Entity> ensureInjected;

	private InstantiateImplCallbackWithShellObject shellInstantiator;

	private InstantiateImplCallback instantiateImplCallback;

	private boolean useRawDomainStore;

	public EntityCacheHibernateResolvingFilter() {
	}

	public EntityCacheHibernateResolvingFilter(
			InstantiateImplCallback instantiateImplCallback) {
		this();
		this.instantiateImplCallback = instantiateImplCallback;
		if (instantiateImplCallback instanceof InstantiateImplCallbackWithShellObject) {
			shellInstantiator = (InstantiateImplCallbackWithShellObject) instantiateImplCallback;
		}
	}

	public EntityCacheHibernateResolvingFilter(
			InstantiateImplCallback instantiateImplCallback,
			boolean blankCache) {
		this(instantiateImplCallback);
		setCache(new DetachedEntityCache());
	}

	@Override
	public <T> T filterData(T value, T cloned, GraphProjectionContext context,
			GraphProjection graphProjection) throws Exception {
		if (value instanceof Entity) {
			Entity entity = (Entity) value;
			if (ensureInjected != null && ensureInjected.containsKey(entity)) {
				entity = ensureInjected.get(entity);
				ensureInjected.remove(entity);
				// if it does, just proceed as normal (entity will already be the
				// key in the reached map, so .project() wouldn't work)
				if (entity != value) {
					entity = graphProjection.project(entity, value, context, false);
					getCache().put((Entity) entity);
					return (T) entity;
				}
			}
			if (value instanceof HibernateProxy) {
				LazyInitializer lazy = ((HibernateProxy) value)
						.getHibernateLazyInitializer();
				Serializable id = lazy.getIdentifier();
				Class persistentClass = lazy.getPersistentClass();
				Object impl = getCache().get(persistentClass, (Long) id);
				if (impl == null) {
					if (useRawDomainStore) {
						if (DomainStore.stores().writableStore()
								.isCachedTransactional(persistentClass)) {
							impl = (T) Domain.find(persistentClass, (Long) id);
						}
					}
				}
				if (impl == null && instantiateImplCallback != null) {
					if (instantiateImplCallback.instantiateLazyInitializer(lazy,
							context)) {
						impl = ((HibernateProxy) value)
								.getHibernateLazyInitializer()
								.getImplementation();
						impl = graphProjection.project(impl, value, context, false);
						getCache().put((Entity) impl);
					} else if (shellInstantiator != null) {
						impl = shellInstantiator.instantiateShellObject(lazy,
								context);
						if (impl != null) {
							getCache().put((Entity) impl);
						}
					}
				}
				if (impl != null) {
					return (T) impl;
				} else {
					// Serializable identifier = ((HibernateProxy) value)
					// .getHibernateLazyInitializer().getIdentifier();
					// System.out
					// .format("discarded %s: %s\n", context, identifier);
					return null;
				}
			} else {
				Class<? extends Entity> valueClass = entity.getClass();
				Object cached = getCache().get(valueClass, entity.getId());
				if (cached != null) {
					return (T) cached;
				} else {
					T result = null;
					if (useRawDomainStore) {
						if (DomainStore.stores().writableStore()
								.isCached(valueClass)) {
							result = (T) Domain.find(valueClass, entity.getId());
						}
					}
					if (result == null) {
						Entity clonedEntity = (Entity) cloned;
						clonedEntity.setId(entity.getId());
						result = (T) clonedEntity;
					}
					getCache().put((Entity) result);
					return (T) result;
				}
			}
		}
		return super.filterData(value, cloned, context, graphProjection);
	}

	public DetachedEntityCache getCache() {
		if (this.cache == null) {
			this.cache = new DetachedEntityCache();
		}
		return this.cache;
	}

	public Map<? extends Entity, ? extends Entity>
			getEnsureInjected() {
		return this.ensureInjected;
	}

	@Override
	public boolean ignoreObjectHasReadPermissionCheck() {
		return true;
	}

	public boolean isUseRawDomainStore() {
		return this.useRawDomainStore;
	}

	public void setCache(DetachedEntityCache cache) {
		this.cache = cache;
	}

	public void setEnsureInjected(
			Map<? extends Entity, ? extends Entity> ensureInjected) {
		this.ensureInjected = ensureInjected;
	}

	public void setUseRawDomainStore(boolean useRawDomainStore) {
		this.useRawDomainStore = useRawDomainStore;
	}

	@Override
	protected Object clonePersistentSet(Set ps, GraphProjectionContext context,
			GraphProjection graphCloner) throws Exception {
		Set hs = jpaImplementation.createPersistentSetProjection(context);
		graphCloner.getReached().put(ps, hs);
		if (getWasInitialized(ps)) {
			Iterator itr = ps.iterator();
			Object value;
			for (; itr.hasNext();) {
				value = itr.next();
				Object projected = null;
				if (value instanceof HibernateProxy) {
					LazyInitializer lazy = ((HibernateProxy) value)
							.getHibernateLazyInitializer();
					Object impl = ((HibernateProxy) value)
							.getHibernateLazyInitializer().getImplementation();
					projected = graphCloner.project(impl, value, context,
							false);
					getCache().put((Entity) projected);
				} else {
					projected = graphCloner.project(value, context);
				}
				if (value == null || projected != null) {
					hs.add(projected);
				}
			}
		}
		return hs;
	}
}