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
import java.lang.reflect.Field;
import java.util.Collection;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.hibernate.LazyInitializationException;
import org.hibernate.engine.spi.IdentifierValue;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.tuple.IdentifierProperty;

import cc.alcina.framework.common.client.logic.domaintransform.ClassRef;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException.DomainTransformExceptionType;
import cc.alcina.framework.entity.entityaccess.DetachedEntityCache;
import cc.alcina.framework.entity.entityaccess.JPAImplementation;
import cc.alcina.framework.entity.util.EntityUtils;
import cc.alcina.framework.entity.util.GraphProjection.GraphProjectionContext;
import cc.alcina.framework.entity.util.GraphProjection.GraphProjectionFilter;
import cc.alcina.framework.entity.util.GraphProjection.InstantiateImplCallback;
import cc.alcina.framework.entity.util.GraphProjection.InstantiateImplCallbackWithShellObject;

/**
 * 
 * @author Nick Reddel
 */
public class JPAHibernateImpl implements JPAImplementation {
	private boolean cacheDisabled;

	private java.util.logging.Level level;

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

	public GraphProjectionFilter getResolvingFilter(
			InstantiateImplCallback callback, DetachedEntityCache cache) {
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
					String.format("delete %s where id in %s ",
							clazz.getSimpleName(),
							EntityUtils.longsToIdClause(ids))).executeUpdate();
		} catch (Exception e) {
			// probably a reference error, try with parent delete/cascade
			return false;
		}
		return true;
	}

	public void cache(Query query) {
		query.setHint("org.hibernate.cacheable", true);
	}

	public void interpretException(DomainTransformException ex) {
		DomainTransformExceptionType type = interpretExceptionType(ex);
		if (type != null) {
			ex.setType(type);
		}
	}

	public DomainTransformExceptionType interpretExceptionType(Exception ex) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		ex.printStackTrace(pw);
		String st = sw.toString();
		if (st.contains("OptimisticLockException")) {
			return DomainTransformExceptionType.OPTIMISTIC_LOCK_EXCEPTION;
		}
		if (st.contains("org.hibernate.exception.ConstraintViolationException")) {
			return DomainTransformExceptionType.FK_CONSTRAINT_EXCEPTION;
		}
		if (st.contains("java.beans.IntrospectionException")) {
			return DomainTransformExceptionType.INTROSPECTION_EXCEPTION;
		}
		return null;
	}

	public File getConfigDirectory() {
		return new File(System.getProperty("jboss.server.base.dir")
				+ File.separator + "configuration" + File.separator);
	}

	public IdentifierValue setUnsavedValue(IdentifierProperty ip,
			IdentifierValue newUnsavedValue) throws Exception {
		IdentifierValue backup = ip.getUnsavedValue();
		Field f = ip.getClass().getDeclaredField("unsavedValue");
		f.setAccessible(true);
		f.set(ip, newUnsavedValue);
		return backup;
	}

	@Override
	public Object beforeSpecificSetId(EntityManager entityManager,
			Object toPersist) throws Exception {
		SessionImplementor session = (SessionImplementor) entityManager
				.getDelegate();
		EntityPersister persister = session.getEntityPersister(toPersist
				.getClass().getName(), toPersist);
		IdentifierProperty ip = persister.getEntityMetamodel()
				.getIdentifierProperty();
		IdentifierValue backupUnsavedValue = setUnsavedValue(ip,
				IdentifierValue.ANY);
		return new SavedId(ip, backupUnsavedValue);
	}

	private static class SavedId {
		private final IdentifierProperty ip;

		private final IdentifierValue backupUnsavedValue;

		public SavedId(IdentifierProperty ip, IdentifierValue backupUnsavedValue) {
			this.ip = ip;
			this.backupUnsavedValue = backupUnsavedValue;
		}
	}

	public static final InstantiateImplCallback CLASSREF_GETTER_CALLBACK = new InstantiateImplCallback<LazyInitializer>() {
		public boolean instantiateLazyInitializer(LazyInitializer initializer,
				GraphProjectionContext context) {
			Class persistentClass = initializer.getPersistentClass();
			return ClassRef.class.isAssignableFrom(persistentClass);
		}

	};
	@Override
	public InstantiateImplCallback getClassrefInstantiator(){
		return CLASSREF_GETTER_CALLBACK;
	}
	@Override
	public void afterSpecificSetId(Object fromBefore) throws Exception {
		// restore the backuped unsavedValue
		SavedId savedId = (SavedId) fromBefore;
		setUnsavedValue(savedId.ip, savedId.backupUnsavedValue);
	}

	@Override
	public void muteClassloaderLogging(boolean mute) {
		java.util.logging.Logger logger = java.util.logging.Logger
				.getLogger("org.jboss.modules");
		if (mute) {
			level = logger.getLevel();
			logger.setLevel(java.util.logging.Level.SEVERE);
		} else {
			logger.setLevel(level);
		}
	}
}
