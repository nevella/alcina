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

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.hibernate.LazyInitializationException;
import org.hibernate.proxy.HibernateProxy;

import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException.DomainTransformExceptionType;
import cc.alcina.framework.entity.entityaccess.DetachedEntityCache;
import cc.alcina.framework.entity.entityaccess.JPAImplementation;
import cc.alcina.framework.entity.util.EntityUtils;
import cc.alcina.framework.entity.util.GraphProjection.GraphProjectionFilter;
import cc.alcina.framework.entity.util.GraphProjection.InstantiateImplCallback;

/**
 * 
 * @author Nick Reddel
 */
public class JPAHibernateImpl implements JPAImplementation {
	private boolean cacheDisabled;

	public boolean isCacheDisabled() {
		return this.cacheDisabled;
	}

	public void setCacheDisabled(boolean cacheDisabled) {
		this.cacheDisabled = cacheDisabled;
	}

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

	public GraphProjectionFilter getResolvingFilter(InstantiateImplCallback callback,
			DetachedEntityCache cache) {
		EntityCacheHibernateResolvingFilter filter = new EntityCacheHibernateResolvingFilter(
				callback);
		if (cache != null) {
			filter.setCache(cache);
		}
		return filter;
	}

	public boolean bulkDelete(EntityManager em, Class clazz,
			Collection<Long> ids) {
		try {
			em.createQuery(
					String.format("delete %s where id in %s ", clazz
							.getSimpleName(), EntityUtils.longsToIdClause(ids)))
					.executeUpdate();
		} catch (Exception e) {
			//probably a reference error, try with parent delete/cascade
			return false;
		}
		return true;
	}

	public void cache(Query query) {
		query.setHint("org.hibernate.cacheable", true);
	}

	public void interpretException(DomainTransformException ex) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		ex.printStackTrace(pw);
		String st = sw.toString();
		if (st.contains("OptimisticLockException")) {
			ex.setType(DomainTransformExceptionType.OPTIMISTIC_LOCK_EXCEPTION);
		}
		if (st.contains("org.hibernate.exception.ConstraintViolationException")) {
			ex.setType(DomainTransformExceptionType.FK_CONSTRAINT_EXCEPTION);
		}
		// TODO Auto-generated method stub
	}

	public File getConfigDirectory() {
		return new File(System.getProperty("jboss.server.home.dir")
				+ File.separator + "conf" + File.separator);
	}
}
