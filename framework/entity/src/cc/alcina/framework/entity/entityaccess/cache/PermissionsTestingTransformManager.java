package cc.alcina.framework.entity.entityaccess.cache;

import java.lang.annotation.Annotation;
import java.util.List;

import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedCacheObjectStore;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedEntityCache;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.LazyObjectLoader;
import cc.alcina.framework.common.client.logic.domaintransform.spi.ClassLookup;
import cc.alcina.framework.common.client.logic.domaintransform.spi.ObjectLookup;
import cc.alcina.framework.entity.domaintransform.ThreadlocalTransformManager;

public class PermissionsTestingTransformManager
		extends ThreadlocalTransformManager implements LazyObjectLoader {
	protected DetachedCacheObjectStore store;

	ClassLookupWrapper classLookupWrapper;

	public PermissionsTestingTransformManager() {
		super();
		createObjectLookup();
	}

	@Override
	public <T extends Entity> T getObject(Class<? extends T> c,
			long id, long localId) {
		return getDomainObjects().getObject(c, id, localId);
	}

	@Override
	public TransformManager getT() {
		return this;
	}

	@Override
	public <T extends Entity> void loadObject(Class<? extends T> c,
			long id, long localId) {
		T t = Domain.detachedVersion(c, id);
		store.mapObject(t);
	}

	@Override
	protected ClassLookup classLookup() {
		if (classLookupWrapper == null) {
			classLookupWrapper = new ClassLookupWrapper(super.classLookup());
		}
		return classLookupWrapper;
	}

	@Override
	protected void createObjectLookup() {
		store = new DetachedCacheObjectStore(new DetachedEntityCache());
		store.setLazyObjectLoader(this);
		setDomainObjects(store);
	}

	@Override
	protected ObjectLookup getObjectLookup() {
		return store;
	}

	class ClassLookupWrapper implements ClassLookup {
		private ClassLookup delegate;

		public ClassLookupWrapper(ClassLookup delegate) {
			this.delegate = delegate;
		}

		@Override
		public String displayNameForObject(Object o) {
			return this.delegate.displayNameForObject(o);
		}

		@Override
		public List<String> getAnnotatedPropertyNames(Class clazz) {
			return this.delegate.getAnnotatedPropertyNames(clazz);
		}

		@Override
		public <A extends Annotation> A getAnnotationForClass(Class targetClass,
				Class<A> annotationClass) {
			return this.delegate.getAnnotationForClass(targetClass,
					annotationClass);
		}

		@Override
		public Class getClassForName(String fqn) {
			return this.delegate.getClassForName(fqn);
		}

		@Override
		public Class getPropertyType(Class clazz, String propertyName) {
			return this.delegate.getPropertyType(clazz, propertyName);
		}

		@Override
		public <T> T getTemplateInstance(Class<T> clazz) {
			return this.delegate.getTemplateInstance(clazz);
		}

		@Override
		public List<PropertyInfoLite> getWritableProperties(Class clazz) {
			return this.delegate.getWritableProperties(clazz);
		}

		@Override
		public <T> T newInstance(Class<T> clazz) {
			return this.delegate.newInstance(clazz);
		}

		@Override
		public <T> T newInstance(Class<T> clazz, long objectId, long localId) {
			return PermissionsTestingTransformManager.this.newInstance(clazz,
					objectId, localId);
		}
	}
}