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
import java.util.HashSet;
import java.util.Iterator;

import org.hibernate.collection.PersistentSet;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.entity.entityaccess.DetachedEntityCache;
import cc.alcina.framework.entity.util.GraphProjection;
import cc.alcina.framework.entity.util.GraphProjection.ClassFieldPair;
import cc.alcina.framework.entity.util.GraphProjection.InstantiateImplCallback;
import cc.alcina.framework.entity.util.GraphProjection.InstantiateImplCallbackWithShellObject;

@SuppressWarnings("unchecked")
/**
 *
 * @author Nick Reddel
 */
public class EntityCacheHibernateResolvingFilter extends HibernateCloneFilter {
	private DetachedEntityCache cache;

	private InstantiateImplCallbackWithShellObject shellInstantiator;

	public DetachedEntityCache getCache() {
		return this.cache;
	}

	public void setCache(DetachedEntityCache cache) {
		this.cache = cache;
	}

	private InstantiateImplCallback instantiateImplCallback;

	public EntityCacheHibernateResolvingFilter() {
		cache = DetachedEntityCache.get();
	}

	public EntityCacheHibernateResolvingFilter(
			InstantiateImplCallback instantiateImplCallback) {
		this();
		this.instantiateImplCallback = instantiateImplCallback;
		if (instantiateImplCallback instanceof InstantiateImplCallbackWithShellObject) {
			InstantiateImplCallbackWithShellObject shellInstantiator = (InstantiateImplCallbackWithShellObject) instantiateImplCallback;
		}
	}

	@Override
	public <T> T filterData(T value, T cloned, ClassFieldPair context,
			GraphProjection graphCloner) throws Exception {
		if (value instanceof HasIdAndLocalId) {
			HasIdAndLocalId hili = (HasIdAndLocalId) value;
			if (value instanceof HibernateProxy) {
				LazyInitializer lazy = ((HibernateProxy) value)
						.getHibernateLazyInitializer();
				Serializable id = lazy.getIdentifier();
				Object impl = cache.get(lazy.getPersistentClass(), (Long) id);
				if (impl == null && instantiateImplCallback != null) {
					if (instantiateImplCallback.instantiateLazyInitializer(
							lazy, context)) {
						impl = ((HibernateProxy) value)
								.getHibernateLazyInitializer()
								.getImplementation();
						impl = graphCloner.project(impl, value, context);
						cache.put((HasIdAndLocalId) impl);
					} else if (shellInstantiator != null) {
						impl = shellInstantiator.instantiateShellObject(lazy,
								context);
						if (impl != null) {
							cache.put((HasIdAndLocalId) impl);
						}
					}
				}
				if (impl != null) {
					return (T) impl;
				} else {
					// System.out.println("discarded "
					// + context
					// + ":"
					// + ((HibernateProxy) value)
					// .getHibernateLazyInitializer()
					// .getIdentifier());
					return null;
				}
			} else {
				Object cached = cache.get(value.getClass(), hili.getId());
				if (cached != null) {
					return (T) cached;
				} else {
					HasIdAndLocalId clonedHili = (HasIdAndLocalId) cloned;
					clonedHili.setId(hili.getId());
					cache.put(clonedHili);
					return (T) clonedHili;
				}
			}
		}
		return super.filterData(value, cloned, context, graphCloner);
	}

	@Override
	protected Object clonePersistentSet(PersistentSet ps,
			ClassFieldPair context, GraphProjection graphCloner)
			throws Exception {
		HashSet hs = new HashSet();
		graphCloner.getReached().put(ps, hs);
		if (ps.wasInitialized()) {
			Iterator itr = ps.iterator();
			Object value;
			for (; itr.hasNext();) {
				value = itr.next();
				if (value instanceof HibernateProxy) {
					LazyInitializer lazy = ((HibernateProxy) value)
							.getHibernateLazyInitializer();
					Object impl = ((HibernateProxy) value)
							.getHibernateLazyInitializer().getImplementation();
					value = graphCloner.project(impl, value, context);
					cache.put((HasIdAndLocalId) value);
				} else {
					value = graphCloner.project(value, context);
				}
				hs.add(value);
			}
		}
		return hs;
	}
}