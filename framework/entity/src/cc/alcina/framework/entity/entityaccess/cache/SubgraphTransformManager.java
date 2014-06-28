package cc.alcina.framework.entity.entityaccess.cache;

import java.lang.annotation.Annotation;
import java.util.List;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedCacheObjectStore;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedEntityCache;
import cc.alcina.framework.common.client.logic.domaintransform.spi.ClassLookup;
import cc.alcina.framework.common.client.logic.domaintransform.spi.ObjectLookup;
import cc.alcina.framework.entity.domaintransform.ObjectPersistenceHelper;
import cc.alcina.framework.entity.domaintransform.ServerTransformManagerSupport;

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

	@Override
	protected ClassLookup classLookup() {
		return classLookup;
	}

	@Override
	protected boolean updateAssociationsWithoutNoChangeCheck() {
		return true;
	}

	@Override
	protected void doCascadeDeletes(HasIdAndLocalId hili) {
		// rely on the client for this...
		// or the servletlayer aspect of tltm
	}

	@Override
	protected void createObjectLookup() {
		store = new DetachedCacheObjectStore(
				new DetachedEntityCacheArrayBacked());
		setDomainObjects(store);
	}

	@Override
	protected Object ensureEndpointInTransformGraph(Object object) {
		if (object instanceof HasIdAndLocalId) {
			return getObject((HasIdAndLocalId) object);
		}
		return object;
	}

	@Override
	protected ObjectLookup getObjectLookup() {
		return store;
	}

	@Override
	protected void removeAssociations(HasIdAndLocalId hili) {
		new ServerTransformManagerSupport().removeAssociations(hili);
	}

	@Override
	protected void updateAssociation(DomainTransformEvent evt,
			HasIdAndLocalId obj, Object tgt, boolean remove,
			boolean collectionPropertyChange) {
		super.updateAssociation(evt, obj, tgt, remove, collectionPropertyChange);
	}

	static class SubgraphClassLookup implements ClassLookup {
		@Override
		public String displayNameForObject(Object o) {
			return ObjectPersistenceHelper.get().displayNameForObject(o);
		}

		@Override
		public List<String> getAnnotatedPropertyNames(Class clazz) {
			return ObjectPersistenceHelper.get().getAnnotatedPropertyNames(
					clazz);
		}

		@Override
		public <A extends Annotation> A getAnnotationForClass(
				Class targetClass, Class<A> annotationClass) {
			return ObjectPersistenceHelper.get().getAnnotationForClass(
					targetClass, annotationClass);
		}

		public Class getClassForName(String fqn) {
			return ObjectPersistenceHelper.get().getClassForName(fqn);
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

		public <T> T newInstance(Class<T> clazz) {
			try {
				return clazz.newInstance();
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
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
	}

	public DetachedCacheObjectStore getStore() {
		return this.store;
	}

	protected HasIdAndLocalId getObjectForCreate(DomainTransformEvent event) {
		return null;
	
	}

}
