package cc.alcina.framework.entity.entityaccess.cache;

import java.lang.annotation.Annotation;
import java.util.List;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.spi.ClassLookup;
import cc.alcina.framework.common.client.logic.domaintransform.spi.ObjectLookup;
import cc.alcina.framework.entity.domaintransform.ObjectPersistenceHelper;
import cc.alcina.framework.entity.entityaccess.DetachedEntityCache;

public class SubgraphTransformManager extends TransformManager {
	private DetachedCacheObjectStore store;

	public SubgraphTransformManager() {
		super();
		createObjectLookup();
	}

	@Override
	protected void createObjectLookup() {
		store = new DetachedCacheObjectStore();
		setDomainObjects(store);
	}

	public DetachedEntityCache getDetachedEntityCache() {
		return store.cache;
	}

	@Override
	protected ObjectLookup getObjectLookup() {
		return store;
	}

	class SubgraphClassLookup implements ClassLookup {
		public Class getClassForName(String fqn) {
			return ObjectPersistenceHelper.get().getClassForName(fqn);
		}

		public <T> T newInstance(Class<T> clazz, long objectId, long localId) {
			try {
				HasIdAndLocalId newInstance = (HasIdAndLocalId) clazz
						.newInstance();
				newInstance.setLocalId(localId);
				return (T) newInstance;
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}

		public <T> T newInstance(Class<T> clazz) {
			try {
				return clazz.newInstance();
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}

		@Override
		public String displayNameForObject(Object o) {
			return ObjectPersistenceHelper.get().displayNameForObject(o);
		}

		@Override
		public <A extends Annotation> A getAnnotationForClass(
				Class targetClass, Class<A> annotationClass) {
			return ObjectPersistenceHelper.get().getAnnotationForClass(
					targetClass, annotationClass);
		}

		@Override
		public List<String> getAnnotatedPropertyNames(Class clazz) {
			return ObjectPersistenceHelper.get().getAnnotatedPropertyNames(
					clazz);
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
		public List<PropertyInfoLite> getWritableProperties(Class clazz) {
			return ObjectPersistenceHelper.get().getWritableProperties(clazz);
		}
	}

	SubgraphClassLookup classLookup = new SubgraphClassLookup();

	@Override
	protected ClassLookup classLookup() {
		return classLookup;
	}

	@Override
	protected void updateAssociation(DomainTransformEvent evt,
			HasIdAndLocalId obj, Object tgt, boolean remove,
			boolean collectionPropertyChange) {
		return;
	}
}
