package cc.alcina.framework.entity.entityaccess.cache;

import java.lang.annotation.Annotation;
import java.util.List;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedCacheObjectStore;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedEntityCache;
import cc.alcina.framework.common.client.logic.domaintransform.spi.ClassLookup;
import cc.alcina.framework.common.client.logic.domaintransform.spi.ObjectLookup;
import cc.alcina.framework.common.client.logic.reflection.PropertyReflector;
import cc.alcina.framework.entity.domaintransform.ObjectPersistenceHelper;
import cc.alcina.framework.entity.entityaccess.cache.mvcc.Transaction;

public class SubgraphTransformManager extends TransformManager {
	protected DetachedCacheObjectStore store;

	SubgraphClassLookup classLookup = new SubgraphClassLookup();

	public SubgraphTransformManager() {
		super();
		createObjectLookup();
	}

	public DetachedEntityCache getDetachedEntityCache() {
		return store.getCache();
	}

	public DetachedCacheObjectStore getStore() {
		return this.store;
	}

	@Override
	protected ClassLookup classLookup() {
		return classLookup;
	}

	@Override
	protected void createObjectLookup() {
		store = new DetachedCacheObjectStore(new DomainStoreEntityCache());
		setDomainObjects(store);
	}

	@Override
	protected Object ensureEndpointInTransformGraph(Object object) {
		if (object instanceof Entity) {
			return getObject((Entity) object);
		}
		return object;
	}

	@Override
	protected Entity getEntityForCreate(DomainTransformEvent event) {
		return null;
	}

	@Override
	protected ObjectLookup getObjectLookup() {
		return store;
	}

	static class SubgraphClassLookup implements ClassLookup {
		@Override
		public String displayNameForObject(Object o) {
			return ObjectPersistenceHelper.get().displayNameForObject(o);
		}

		@Override
		public <A extends Annotation> A getAnnotationForClass(Class targetClass,
				Class<A> annotationClass) {
			return ObjectPersistenceHelper.get()
					.getAnnotationForClass(targetClass, annotationClass);
		}

		@Override
		public Class getClassForName(String fqn) {
			return ObjectPersistenceHelper.get().getClassForName(fqn);
		}

		@Override
		public List<PropertyReflector>
				getPropertyReflectors(Class<?> beanClass) {
			return ObjectPersistenceHelper.get()
					.getPropertyReflectors(beanClass);
		}

		@Override
		public Class getPropertyType(Class clazz, String propertyName) {
			return ObjectPersistenceHelper.get().getPropertyType(clazz,
					propertyName);
		}

		@Override
		public <T> T getTemplateInstance(Class<T> clazz) {
			return ObjectPersistenceHelper.get().getTemplateInstance(clazz);
		}

		@Override
		public List<PropertyInfo> getWritableProperties(Class clazz) {
			return ObjectPersistenceHelper.get().getWritableProperties(clazz);
		}

		@Override
		public <T> T newInstance(Class<T> clazz) {
			try {
				return clazz.newInstance();
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}

		@Override
		public <T> T newInstance(Class<T> clazz, long objectId, long localId) {
			try {
				Entity newInstance = Transaction.current().create((Class) clazz,
						DomainStore.stores().storeFor(clazz));
				newInstance.setLocalId(localId);
				return (T) newInstance;
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
	}
}
