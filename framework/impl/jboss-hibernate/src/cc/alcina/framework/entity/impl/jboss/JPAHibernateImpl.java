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

import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.hibernate.HibernateException;
import org.hibernate.LazyInitializationException;
import org.hibernate.engine.internal.StatefulPersistenceContext;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.IdentifierValue;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.SingleTableEntityPersister;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.tuple.IdentifierProperty;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domain.HiliHelper;
import cc.alcina.framework.common.client.logic.domaintransform.ClassRef;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException.DomainTransformExceptionType;
import cc.alcina.framework.common.client.logic.domaintransform.HiliLocator;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedEntityCache;
import cc.alcina.framework.common.client.logic.permissions.IGroup;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.entityaccess.JPAImplementation;
import cc.alcina.framework.entity.entityaccess.cache.AlcinaMemCache.MemcacheJoinHandler;
import cc.alcina.framework.entity.projection.EntityUtils;
import cc.alcina.framework.entity.projection.GraphProjection;
import cc.alcina.framework.entity.projection.GraphProjection.GraphProjectionContext;
import cc.alcina.framework.entity.projection.GraphProjection.GraphProjectionDataFilter;
import cc.alcina.framework.entity.projection.GraphProjection.InstantiateImplCallback;

/**
 *
 * @author Nick Reddel
 */
@RegistryLocation(registryPoint = JPAImplementation.class, implementationType = ImplementationType.SINGLETON)
@SuppressWarnings("deprecation")
public class JPAHibernateImpl implements JPAImplementation {
	public static final InstantiateImplCallback CLASSREF_AND_USERLAND_GETTER_CALLBACK = new InstantiateImplCallback<LazyInitializer>() {
		public boolean instantiateLazyInitializer(LazyInitializer initializer,
				GraphProjectionContext context) {
			Class persistentClass = initializer.getPersistentClass();
			return ClassRef.class.isAssignableFrom(persistentClass)
					|| IUser.class.isAssignableFrom(persistentClass)
					|| IGroup.class.isAssignableFrom(persistentClass);
		}
	};

	private boolean cacheDisabled;

	private java.util.logging.Level level;

	private PersistenSetProjectionCreator persistenSetProjectionCreator;

	@Override
	public void afterSpecificSetId(Object fromBefore) throws Exception {
		// restore the backuped unsavedValue
		SavedId savedId = (SavedId) fromBefore;
		setUnsavedValue(savedId.ip, savedId.backupUnsavedValue,
				savedId.identifierGenerator);
	}

	@Override
	public boolean areEquivalentIgnoreInstantiationState(Object o1, Object o2) {
		if (o1 instanceof HasIdAndLocalId && o2 instanceof HasIdAndLocalId) {
			HiliLocator l1 = toHiliLocator(o1);
			HiliLocator l2 = toHiliLocator(o2);
			return CommonUtils.equalsWithNullEquality(l1, l2);
		}
		return CommonUtils.equalsWithNullEquality(o1, o2);
	}

	@Override
	public Object beforeSpecificSetId(EntityManager entityManager,
			Object toPersist) throws Exception {
		SessionImplementor session = (SessionImplementor) entityManager
				.getDelegate();
		EntityPersister persister = session
				.getEntityPersister(toPersist.getClass().getName(), toPersist);
		IdentifierGenerator identifierGenerator = persister
				.getIdentifierGenerator();
		IdentifierProperty ip = persister.getEntityMetamodel()
				.getIdentifierProperty();
		IdentifierValue backupUnsavedValue = setUnsavedValue(ip,
				IdentifierValue.ANY, new UseEntityIdGenerator());
		return new SavedId(ip, backupUnsavedValue, identifierGenerator);
	}

	public static class UseEntityIdGenerator implements IdentifierGenerator {
		@Override
		public Serializable generate(SessionImplementor session, Object object)
				throws HibernateException {
			return ((HasIdAndLocalId) object).getId();
		}
	}

	public boolean bulkDelete(EntityManager em, Class clazz,
			Collection<Long> ids) {
		try {
			em.createQuery(String.format("delete %s where id in %s ",
					clazz.getSimpleName(), EntityUtils.longsToIdClause(ids)))
					.executeUpdate();
		} catch (Exception e) {
			// probably a reference error, try with parent delete/cascade
			return false;
		}
		return true;
	}

	public void cache(Query query) {
		query.setHint("org.hibernate.cacheable", true);
	}

	@Override
	public Set createPersistentSetProjection(GraphProjectionContext context) {
		if (persistenSetProjectionCreator == null) {
			persistenSetProjectionCreator = Registry
					.impl(PersistenSetProjectionCreator.class);
		}
		if (GraphProjection.isGenericHiliType(context.field)) {
			return persistenSetProjectionCreator
					.createPersistentSetProjection(context);
		} else {
			return new HashSet();
		}
	}

	@Override
	public String entityDebugString(Object object) {
		try {
			if (object instanceof HibernateProxy) {
				LazyInitializer lazy = ((HibernateProxy) object)
						.getHibernateLazyInitializer();
				Serializable id = lazy.getIdentifier();
				Class clazz = lazy.getPersistentClass();
				return String.format("\tclass: %s\n\tid:\t%s\n\n", clazz, id);
			}
			if (object instanceof HasIdAndLocalId) {
				return object.toString();
			}
		} catch (Exception e) {
			// stale transaction e.g.
			if (object instanceof HasIdAndLocalId) {
				HiliHelper.asDomainPoint((HasId) object);
			}
		}
		return null;
	}

	@Override
	public InstantiateImplCallback getClassrefInstantiator() {
		return CLASSREF_AND_USERLAND_GETTER_CALLBACK;
	}

	public File getConfigDirectory() {
		return new File(System.getProperty("jboss.server.base.dir")
				+ File.separator + "configuration" + File.separator);
	}

	@SuppressWarnings("unchecked")
	public <T> T getInstantiatedObject(T object) {
		if (object instanceof HibernateProxy) {
			return (T) ((HibernateProxy) object).getHibernateLazyInitializer()
					.getImplementation();
		}
		return object;
	}

	@Override
	public MemcacheJoinHandler
			getMemcacheJoinHandler(final PropertyDescriptor pd) {
		final ElementCollection elementCollection = pd.getReadMethod()
				.getAnnotation(ElementCollection.class);
		final Column column = pd.getReadMethod().getAnnotation(Column.class);
		MemcacheJoinHandler handler = null;
		if (elementCollection == null) {
			return null;
		}
		return new MemcacheJoinHandler() {
			@Override
			public String getTargetSql() {
				return column.name();
			}

			@Override
			public void injectValue(ResultSet rs, HasIdAndLocalId source) {
				try {
					String string = rs.getString(getTargetSql());
					Set enums = (Set) pd.getReadMethod().invoke(source,
							new Object[0]);
					enums.add(Enum.valueOf(elementCollection.targetClass(),
							string));
				} catch (Exception e) {
					throw new WrappedRuntimeException(e);
				}
			}
		};
	}

	public GraphProjectionDataFilter getResolvingFilter(
			InstantiateImplCallback callback, DetachedEntityCache cache,
			boolean useMemCache) {
		EntityCacheHibernateResolvingFilter filter = new EntityCacheHibernateResolvingFilter(
				callback);
		filter.setUseRawMemCache(useMemCache);
		if (cache != null) {
			filter.setCache(cache);
		}
		return filter;
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
		if (st.contains(
				"org.hibernate.exception.ConstraintViolationException")) {
			return DomainTransformExceptionType.FK_CONSTRAINT_EXCEPTION;
		}
		if (st.contains("java.beans.IntrospectionException")) {
			return DomainTransformExceptionType.INTROSPECTION_EXCEPTION;
		}
		return null;
	}

	public boolean isCacheDisabled() {
		return this.cacheDisabled;
	}

	public boolean isLazyInitialisationException(Exception e) {
		return e instanceof LazyInitializationException;
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

	public void setCacheDisabled(boolean cacheDisabled) {
		this.cacheDisabled = cacheDisabled;
	}

	public IdentifierValue setUnsavedValue(IdentifierProperty ip,
			IdentifierValue value, IdentifierGenerator identifierGenerator)
			throws Exception {
		IdentifierValue backup = ip.getUnsavedValue();
		{
			Field f = ip.getClass().getDeclaredField("unsavedValue");
			f.setAccessible(true);
			f.set(ip, value);
		}
		{
			Field f = ip.getClass().getDeclaredField("identifierGenerator");
			f.setAccessible(true);
			f.set(ip, identifierGenerator);
		}
		return backup;
	}

	private HiliLocator toHiliLocator(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof HibernateProxy) {
			LazyInitializer lazy = ((HibernateProxy) o)
					.getHibernateLazyInitializer();
			return new HiliLocator(lazy.getPersistentClass(),
					(Long) lazy.getIdentifier(), 0L);
		}
		return new HiliLocator((HasIdAndLocalId) o);
	}

	@RegistryLocation(registryPoint = PersistenSetProjectionCreator.class, implementationType = ImplementationType.SINGLETON)
	public static class PersistenSetProjectionCreator {
		public Set
				createPersistentSetProjection(GraphProjectionContext context) {
			return new HashSet();
		}
	}

	private static class SavedId {
		private final IdentifierProperty ip;

		private final IdentifierValue backupUnsavedValue;

		private final IdentifierGenerator identifierGenerator;

		public SavedId(IdentifierProperty ip,
				IdentifierValue backupUnsavedValue,
				IdentifierGenerator identifierGenerator) {
			this.ip = ip;
			this.backupUnsavedValue = backupUnsavedValue;
			this.identifierGenerator = identifierGenerator;
		}
	}

	@Override
	public Set<HiliLocator>
			getSessionEntityLocators(EntityManager entityManager) {
		Set<HiliLocator> result = new LinkedHashSet<>();
		try {
			SessionImplementor sessionImpl = (SessionImplementor) entityManager
					.getDelegate();
			PersistenceContext persistenceContext = sessionImpl
					.getPersistenceContext();
			Field entitiesField = StatefulPersistenceContext.class
					.getDeclaredField("entitiesByKey");
			Field proxiesField = StatefulPersistenceContext.class
					.getDeclaredField("proxiesByKey");
			Field entityPersisterField = EntityKey.class
					.getDeclaredField("persister");
			entitiesField.setAccessible(true);
			entityPersisterField.setAccessible(true);
			proxiesField.setAccessible(true);
			List<Map> maps = Arrays.asList(
					(Map) entitiesField.get(persistenceContext),
					(Map) proxiesField.get(persistenceContext));
			for (Map map : maps) {
				for (Object obj : map.keySet()) {
					EntityKey key = (EntityKey) obj;
					long id = (long) key.getIdentifier();
					SingleTableEntityPersister persister = (SingleTableEntityPersister) entityPersisterField
							.get(key);
					Class clazz = persister.getEntityMetamodel().getEntityType()
							.getReturnedClass();
					result.add(new HiliLocator(clazz, id, 0));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}
}
