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

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.hibernate.proxy.HibernateProxy;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.entity.persistence.JPAImplementation;
import cc.alcina.framework.entity.projection.CollectionProjectionFilter;
import cc.alcina.framework.entity.projection.GraphProjection;
import cc.alcina.framework.entity.projection.GraphProjection.GraphProjectionContext;

/**
 * 
 * @author Nick Reddel
 */
public class Hibernate4CloneFilter extends CollectionProjectionFilter {
	private static final String cn1 = "org.hibernate.collection.PersistentSet";

	private static final String cn2 = "org.hibernate.collection.internal.PersistentSet";

	private Set<GraphProjectionContext> instantiateProps = new HashSet<GraphProjectionContext>();

	protected JPAImplementation jpaImplementation;

	private Method wasInitialized = null;

	public Hibernate4CloneFilter() {
		this.jpaImplementation = Registry.impl(JPAImplementation.class);
	}

	public Hibernate4CloneFilter(Set<GraphProjectionContext> instantiateProps) {
		this.instantiateProps = instantiateProps;
	}

	protected Object clonePersistentSet(Set ps, GraphProjectionContext context,
			GraphProjection graphCloner) throws Exception {
		Set hs = jpaImplementation.createPersistentSetProjection(context);
		if (shouldClone(ps)) {
			Iterator itr = ps.iterator();
			Object value;
			for (; itr.hasNext();) {
				value = itr.next();
				Object projected = graphCloner.project(value, context);
				if (value == null || projected != null) {
					hs.add(projected);
				}
			}
		}
		return hs;
	}

	@Override
	public <T> T filterData(T value, T cloned, GraphProjectionContext context,
			GraphProjection graphCloner) throws Exception {
		if (value instanceof HibernateProxy) {
			if (instantiateProps.contains(context)) {
				Object impl = ((HibernateProxy) value)
						.getHibernateLazyInitializer().getImplementation();
				value = (T) graphCloner.project(impl, value, context);
				return value;
			} else {
				return null;
			}
		}
		if ((value instanceof Set) && (value.getClass().getName().equals(cn1)
				|| value.getClass().getName().equals(cn2))) {
			return (T) clonePersistentSet((Set) value, context, graphCloner);
		}
		return super.filterData(value, cloned, context, graphCloner);
	}

	protected boolean getWasInitialized(Set ps) throws Exception {
		if (wasInitialized == null) {
			wasInitialized = ps.getClass().getMethod("wasInitialized");
		}
		return (Boolean) wasInitialized.invoke(ps);
	}

	protected boolean shouldClone(Set ps) throws Exception {
		return getWasInitialized(ps);
	}
}