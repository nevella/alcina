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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.hibernate.collection.PersistentSet;
import org.hibernate.proxy.HibernateProxy;

import cc.alcina.framework.entity.util.GraphCloner;
import cc.alcina.framework.entity.util.GraphCloner.ClassFieldPair;
import cc.alcina.framework.entity.util.GraphCloner.CollectionCloneFilter;


/**
 *
 * @author <a href="mailto:nick@alcina.cc">Nick Reddel</a>
 */

 public class HibernateCloneFilter extends CollectionCloneFilter {
	private Set<ClassFieldPair> instantiateProps = new HashSet<ClassFieldPair>();

	public HibernateCloneFilter() {
	}

	public HibernateCloneFilter(Set<ClassFieldPair> instantiateProps) {
		this.instantiateProps = instantiateProps;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T filterData(T value, T cloned, ClassFieldPair context,
			GraphCloner graphCloner) throws Exception {
		if (value instanceof HibernateProxy) {
			if (instantiateProps.contains(context)) {
				Object impl = ((HibernateProxy) value)
						.getHibernateLazyInitializer().getImplementation();
				value = (T) graphCloner.clone(impl, value, context);
				return value;
			} else {
				return null;
			}
		}
		if ((value instanceof Set) && value instanceof PersistentSet) {
			return (T) clonePersistentSet((PersistentSet) value, context,
					graphCloner);
		}
		return super.filterData(value, cloned, context, graphCloner);
	}

	@SuppressWarnings("unchecked")
	protected Object clonePersistentSet(PersistentSet ps,
			ClassFieldPair context, GraphCloner graphCloner) throws Exception {
		HashSet hs = new HashSet();
		if (ps.wasInitialized()) {
			Iterator itr = ps.iterator();
			Object value;
			for (; itr.hasNext();) {
				value = itr.next();
				value = graphCloner.clone(value, context);
				hs.add(value);
			}
		}
		return hs;
	}
}