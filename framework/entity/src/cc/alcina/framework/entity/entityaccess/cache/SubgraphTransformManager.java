package cc.alcina.framework.entity.entityaccess.cache;

import java.lang.annotation.Annotation;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException;
import cc.alcina.framework.common.client.logic.domaintransform.EntityLocator;
import cc.alcina.framework.common.client.logic.domaintransform.EntityLocatorMap;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedCacheObjectStore;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedEntityCache;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.LazyObjectLoader;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.MapObjectLookupJvm;
import cc.alcina.framework.common.client.logic.domaintransform.spi.ClassLookup;
import cc.alcina.framework.common.client.logic.domaintransform.spi.ObjectLookup;
import cc.alcina.framework.common.client.logic.reflection.PropertyReflector;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.domaintransform.ObjectPersistenceHelper;
import cc.alcina.framework.entity.entityaccess.cache.PropertyStoreAwareMultiplexingObjectCache.DetachedEntityCacheAccess;
import cc.alcina.framework.entity.entityaccess.cache.mvcc.Transaction;

public class SubgraphTransformManager extends TransformManager {
	public static <T extends Entity> T generateSynthetic(
			EntityLocatorMap locatorMap, Stream<DomainTransformEvent> stream) {
		try {
			SubgraphTransformManagerRecord tm = new SubgraphTransformManagerRecord();
			List<DomainTransformEvent> dtes = stream.map(dte -> {
				try {
					// cheap hack to let local transforms work with
					// DetachedEntityCache
					DomainTransformEvent copy = ResourceUtilities
							.fieldwiseClone(dte, true, false);
					long objectLocalId = copy.getObjectLocalId();
					if (objectLocalId != 0) {
						if (locatorMap.containsKey(objectLocalId)) {
							copy.setObjectId(
									locatorMap.getForLocalId(objectLocalId).id);
							copy.setObjectLocalId(0);
						} else {
							copy.setObjectId(-objectLocalId);
							copy.setObjectLocalId(0L);
						}
					}
					return copy;
				} catch (Exception e) {
					throw new WrappedRuntimeException(e);
				}
			}).collect(Collectors.toList());
			for (DomainTransformEvent dte : dtes) {
				tm.apply(dte);
			}
			Entity firstReferenced = tm.firstReferenced;
			long id = firstReferenced.getId();
			if (id < 0) {
				firstReferenced.setLocalId(-id);
				firstReferenced.setId(0);
			}
			return (T) firstReferenced;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

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
		store = new DetachedCacheObjectStore(new DetachedEntityCacheAccess());
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
	protected Entity getObjectForCreate(DomainTransformEvent event) {
		return null;
	}

	@Override
	protected ObjectLookup getObjectLookup() {
		return store;
	}

	@Override
	protected boolean updateAssociationsWithoutNoChangeCheck() {
		return true;
	}

	public static class SubgraphTransformManagerPreProcess
			extends SubgraphTransformManager {
		public SubgraphTransformManagerPreProcess(EntityLocatorMap locatorMap) {
			((PreProcessBridgeLookup) getDomainObjects()).locatorMap = locatorMap;
		}

		@Override
		protected void createObjectLookup() {
			setDomainObjects(new PreProcessBridgeLookup());
		}

		@Override
		protected ObjectLookup getObjectLookup() {
			return getDomainObjects();
		}
	}

	@FunctionalInterface
	interface LocalReplacementCreationObjectResolver
			extends Function<Long, Entity> {
	}

	static class PreProcessBridgeLookup extends MapObjectLookupJvm {
		private EntityLocatorMap locatorMap;

		public PreProcessBridgeLookup() {
		}

		@Override
		public <T extends Entity> T getObject(Class<? extends T> c, long id,
				long localId) {
			T t = super.getObject(c, id, localId);
			if (t == null) {
				if (id == 0) {
					EntityLocator entityLocator = locatorMap
							.getForLocalId(localId);
					if (entityLocator == null) {
						return null;
					}
					id = entityLocator.id;
				}
				if (id != 0) {
					t = Domain.detachedVersion(c, id);
					mapObject(t);
				}
			}
			return t;
		}
	}

	static class SubgraphClassLookup implements ClassLookup {
		static ThreadLocal<LocalReplacementCreationObjectResolver> localReplacementCreationObjectResolvers = new ThreadLocal<>();

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
				LocalReplacementCreationObjectResolver resolver = localReplacementCreationObjectResolvers
						.get();
				if (resolver != null) {
					Entity local = resolver.apply(localId);
					if (local != null) {
						local.hashCode();
						local.setId(objectId);
						TransformManager.registerLocalObjectPromotion(local);
						return (T) local;
					}
				}
				Entity newInstance = Transaction.current().create((Class) clazz,
						DomainStore.stores().storeFor(clazz));
				newInstance.setLocalId(localId);
				return (T) newInstance;
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}

		public void setLocalReplacementCreationObjectResolver(
				LocalReplacementCreationObjectResolver resolver) {
			localReplacementCreationObjectResolvers.set(resolver);
		}
	}

	static class SubgraphTransformManagerRecord extends SubgraphTransformManager
			implements LazyObjectLoader {
		private Entity firstReferenced = null;

		@Override
		public void apply(DomainTransformEvent event)
				throws DomainTransformException {
			super.apply(event);
			if (firstReferenced == null) {
				Iterator<Entity> iterator = getDetachedEntityCache().allValues()
						.iterator();
				firstReferenced = iterator.hasNext() ? iterator.next() : null;
			}
		}

		@Override
		public <T extends Entity> void loadObject(Class<? extends T> clazz,
				long id, long localId) {
			store.mapObject(Domain.detachedVersion(clazz, id));
		}

		@Override
		protected void createObjectLookup() {
			store = new DetachedCacheObjectStore(new DetachedEntityCache());
			store.setLazyObjectLoader(this);
			setDomainObjects(store);
		}
	}
}
