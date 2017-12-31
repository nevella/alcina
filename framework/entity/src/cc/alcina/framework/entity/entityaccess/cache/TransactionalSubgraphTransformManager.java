package cc.alcina.framework.entity.entityaccess.cache;

import java.lang.annotation.Annotation;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.TransformType;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.MapObjectLookupJvm;
import cc.alcina.framework.common.client.logic.domaintransform.spi.ObjectLookup;
import cc.alcina.framework.common.client.logic.domaintransform.spi.PropertyAccessor;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.domaintransform.MethodIndividualPropertyAccessor;

public class TransactionalSubgraphTransformManager
		extends SubgraphTransformManager implements PropertyAccessor {
	static boolean debugSetPropertyValueStackDepth = false;

	TransformManager threadTm = TransformManager.get();

	PropertyAccessor propertyAccessor = Reflections.propertyAccessor();

	MapObjectLookupJvm deleted = new MapObjectLookupJvm();

	MapObjectLookupJvm modified = new MapObjectLookupJvm();

	@Override
	public IndividualPropertyAccessor cachedAccessor(Class clazz,
			String propertyName) {
		return new MethodIndividualPropertyAccessor(clazz, propertyName);
	}

	@Override
	public void consume(DomainTransformEvent event)
			throws DomainTransformException {
		if (event.getTransformType() == TransformType.CREATE_OBJECT) {
			// if an object is newly created (by the tltm), it won't be in the
			// memcache graph - so use it, don't create a new one
			boolean inGraph = false;
			try {
				inGraph = getObject(event) != null;
				if (!inGraph) {
					registerDomainObject(threadTm.getObject(event));
				}
				return;
			} catch (Exception e) {
				// object not in graph, fall through to create
				super.consume(event);
				return;
			}
		}
		if (event.getSource() != null) {
			HasIdAndLocalId source = event.getSource();
			if (source != null && AlcinaMemCache.isRawValue(source)) {
				throw new RuntimeException("Source of transform"
						+ " should be immutable except to post-persistence code");
			}
		}
		HasIdAndLocalId obj = getObject(event);
		if (event.getTransformType() == TransformType.DELETE_OBJECT) {
			if (obj != null) {
				deleted.mapObject(obj);
			}
		} else {
			modified.mapObject(obj);
			mapObjectToCachingProjections(obj);
		}
		super.consume(event);
	}

	public boolean contains(HasIdAndLocalId hili) {
		return getDomainObjects().getObject(hili) != null;
	}

	@Override
	public <A extends Annotation> A getAnnotationForProperty(Class targetClass,
			Class<A> annotationClass, String propertyName) {
		return propertyAccessor.getAnnotationForProperty(targetClass,
				annotationClass, propertyName);
	}

	@Override
	public <T extends HasIdAndLocalId> T getObject(Class<? extends T> c,
			long id, long localId) {
		try {
			T value = super.getObject(c, id, localId);
			if (value != null) {
				return value;
			}
			T nonTransactional = AlcinaMemCache.get().transformManager
					.getObject(c, id, localId);
			if (nonTransactional == null) {
				// create object, can assume the threadTm has it
				return null;
			}
			T newInstance = projectNonTransactional(nonTransactional);
			registerDomainObject(newInstance);
			threadTm.registerDomainObject(newInstance);
			return newInstance;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	@Override
	public Class getPropertyType(Class objectClass, String propertyName) {
		return propertyAccessor.getPropertyType(objectClass, propertyName);
	}

	@Override
	public Object getPropertyValue(Object bean, String propertyName) {
		return propertyAccessor.getPropertyValue(bean, propertyName);
	}

	@Override
	public void setPropertyValue(Object bean, String propertyName,
			Object value) {
		try {
			if (debugSetPropertyValueStackDepth
					&& Thread.currentThread().getStackTrace().length > 80) {
				int j = 3;
			}
			SEUtilities.setPropertyValue(bean, propertyName, value);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	private void mapObjectToCachingProjections(HasIdAndLocalId obj) {
		for (BaseProjectionHasEquivalenceHash listener : AlcinaMemCache
				.get().cachingProjections.getAndEnsure(obj.getClass())) {
			try {
				listener.projectHash(obj);
			} catch (Exception e) {
				// probably not fully instantiated - TODO cleanup (e.g.
				// MorphemeRelation) but iz sorta ok
				// real solution is pause til end of pass, THEN hash'em'all
			}
		}
	}

	@Override
	protected boolean allowUnregisteredHiliTargetObject() {
		return true;
	}

	@Override
	protected void createObjectLookup() {
		// use full lookup- objects could be localid
		store = null;// new DetachedCacheObjectStore(new DetachedEntityCache());
		setDomainObjects(new MapObjectLookupJvm());
	}

	@Override
	protected Object ensureEndpointInTransformGraph(Object object) {
		// if not persisted, just return the object
		if (object instanceof HasIdAndLocalId
				&& ((HasIdAndLocalId) object).getId() != 0) {
			HasIdAndLocalId endpoint = getObject((HasIdAndLocalId) object);
			modified.mapObject(endpoint);
			return endpoint;
		}
		return object;
	}

	@Override
	protected ObjectLookup getObjectLookup() {
		return new ObjectLookup() {
			@Override
			public <T extends HasIdAndLocalId> T getObject(Class<? extends T> c,
					long id, long localId) {
				return (T) TransactionalSubgraphTransformManager.this
						.getObject(c, id, localId);
			}

			@Override
			public <T extends HasIdAndLocalId> T getObject(T bean) {
				return (T) TransactionalSubgraphTransformManager.this.getObject(
						bean.getClass(), bean.getId(), bean.getLocalId());
			}
		};
	}

	/**
	 * TODO - Jira (alcina) - under what circumstances should more of the graph
	 * be projected...and ... two stores - "modified" vs "projected" - not just
	 * the one
	 */
	protected <T extends HasIdAndLocalId> T
			projectNonTransactional(T nonTransactional) throws Exception {
		T newInstance = (T) nonTransactional.getClass().newInstance();
		ResourceUtilities.copyBeanProperties(nonTransactional, newInstance,
				null, true);
		return newInstance;
	}

	@Override
	protected PropertyAccessor propertyAccessor() {
		return this;
	}
}
