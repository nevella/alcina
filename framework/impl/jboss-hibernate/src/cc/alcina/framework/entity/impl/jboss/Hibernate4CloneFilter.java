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

import cc.alcina.framework.entity.util.GraphProjection;
import cc.alcina.framework.entity.util.GraphProjection.ClassFieldPair;
import cc.alcina.framework.entity.util.GraphProjection.CollectionProjectionFilter;

/**
 * 
 * @author Nick Reddel
 */
public class Hibernate4CloneFilter extends CollectionProjectionFilter {
	private Set<ClassFieldPair> instantiateProps = new HashSet<ClassFieldPair>();

	public Hibernate4CloneFilter() {
	}

	public Hibernate4CloneFilter(Set<ClassFieldPair> instantiateProps) {
		this.instantiateProps = instantiateProps;
	}

	private static final String cn1 = "org.hibernate.collection.PersistentSet";

	private static final String cn2 = "org.hibernate.collection.internal.PersistentSet";

	@Override
	@SuppressWarnings("unchecked")
	public <T> T filterData(T value, T cloned, ClassFieldPair context,
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
		if ((value instanceof Set)
				&& (value.getClass().getName().equals(cn1) || value.getClass()
						.getName().equals(cn2))) {
			return (T) clonePersistentSet((Set) value, context,
					graphCloner);
		}
		return super.filterData(value, cloned, context, graphCloner);
	}
	
	private Method wasInitialized=null;
	@SuppressWarnings("unchecked")
	protected Object clonePersistentSet(Set ps,
			ClassFieldPair context, GraphProjection graphCloner)
			throws Exception {
		HashSet hs = new HashSet();
		if (getWasInitialized(ps)) {
			Iterator itr = ps.iterator();
			Object value;
			for (; itr.hasNext();) {
				value = itr.next();
				value = graphCloner.project(value, context);
				hs.add(value);
			}
		}
		return hs;
	}

	protected boolean getWasInitialized(Set ps) throws  Exception{
		if(wasInitialized==null){
			wasInitialized=ps.getClass().getMethod("wasInitialized");
		}
		return (Boolean)wasInitialized.invoke(ps);
	}
}