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

import org.hibernate.LazyInitializationException;
import org.hibernate.proxy.HibernateProxy;

import cc.alcina.framework.entity.entityaccess.DetachedEntityCache;
import cc.alcina.framework.entity.entityaccess.JPAImplementation;
import cc.alcina.framework.entity.util.GraphCloner.CloneFilter;
import cc.alcina.framework.entity.util.GraphCloner.InstantiateImplCallback;


/**
 *
 * @author <a href="mailto:nick@alcina.cc">Nick Reddel</a>
 */

 public class JPAHibernateImpl implements JPAImplementation {
	public boolean isLazyInitialisationException(Exception e) {
		return e instanceof LazyInitializationException;
	}
	@SuppressWarnings("unchecked")
	public <T> T getInstantiatedObject(T object) {
		if (object instanceof HibernateProxy) {
			return (T) ((HibernateProxy) object).getHibernateLazyInitializer()
					.getImplementation();
		}
		return object;
	}
	public CloneFilter getResolvingFilter(InstantiateImplCallback callback,DetachedEntityCache cache){
		EntityCacheHibernateResolvingFilter filter = new EntityCacheHibernateResolvingFilter(callback);
		if (cache!=null){
			filter.setCache(cache);
		}
		return filter;
	}
}
