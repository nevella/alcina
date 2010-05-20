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
package cc.alcina.framework.entity.domaintransform;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.ManyToMany;

import cc.alcina.framework.common.client.CommonLocator;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.WrappedRuntimeException.SuggestedAction;
import cc.alcina.framework.common.client.csobjects.LogMessageType;
import cc.alcina.framework.common.client.csobjects.ObjectCacheItemResult;
import cc.alcina.framework.common.client.csobjects.ObjectCacheItemSpec;
import cc.alcina.framework.common.client.entity.WrapperPersistable;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domain.HasVersionNumber;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformListener;
import cc.alcina.framework.common.client.logic.domaintransform.ObjectRef;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.TransformType;
import cc.alcina.framework.common.client.logic.domaintransform.spi.ClassLookup;
import cc.alcina.framework.common.client.logic.domaintransform.spi.ObjectLookup;
import cc.alcina.framework.common.client.logic.domaintransform.spi.PropertyAccessor;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.Association;
import cc.alcina.framework.common.client.logic.reflection.DomainPropertyInfo;
import cc.alcina.framework.common.client.logic.reflection.ObjectPermissions;
import cc.alcina.framework.common.client.logic.reflection.PropertyPermissions;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.MetricLogging;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceLocal;
import cc.alcina.framework.entity.logic.EntityLayerLocator;

import com.totsp.gwittir.client.beans.SourcesPropertyChangeEvents;

@SuppressWarnings("unchecked")
/**
 *
 * @author Nick Reddel
 */
public class ThreadlocalTransformManager extends TransformManager implements
		PropertyAccessor, ObjectLookup, ClassLookup {
	private static ThreadLocal getTTL = new ThreadLocal() {
		protected synchronized Object initialValue() {
			ThreadlocalTransformManager tm = new ThreadlocalTransformManager();
			tm.resetTltm(null);
			return tm;
		}
	};

	public static TransformManager ttmInstance() {
		return new ThreadlocalTransformManager();
	}

	/**
	 * Convenience "override" of TransformManager.get()
	 */
	public static ThreadlocalTransformManager get() {
		return ThreadlocalTransformManager.cast();
	}

	Set<HasIdAndLocalId> modifiedObjects = new HashSet<HasIdAndLocalId>();

	private static List<DomainTransformListener> threadLocalListeners = new ArrayList<DomainTransformListener>();

	public static void addThreadLocalDomainTransformListener(
			DomainTransformListener listener) {
		threadLocalListeners.add(listener);
	}

	List<DomainTransformEvent> modificationEvents = new ArrayList<DomainTransformEvent>();

	private ClientInstance clientInstance;

	Map<Long, HasIdAndLocalId> localIdToEntityMap;

	HiliLocatorMap userSessionHiliMap;

	private EntityManager entityManager;

	private boolean listenToFoundObjects;

	public List<ObjectCacheItemResult> cache(List<ObjectCacheItemSpec> specs)
			throws Exception {
		List<ObjectCacheItemResult> result = new ArrayList<ObjectCacheItemResult>();
		for (ObjectCacheItemSpec itemSpec : specs) {
			ObjectRef ref = itemSpec.getObjectRef();
			String propertyName = itemSpec.getPropertyName();
			Association assoc = CommonLocator.get().propertyAccessor()
					.getAnnotationForProperty(ref.getClassRef().getRefClass(),
							Association.class, propertyName);
			ObjectCacheItemResult itemResult = new ObjectCacheItemResult();
			itemResult.setItemSpec(itemSpec);
			String eql = buildEqlForSpec(itemSpec, assoc.implementationClass());
			long t1 = System.currentTimeMillis();
			List results = getEntityManager().createQuery(eql).getResultList();
			EntityLayerLocator.get().getMetricLogger().debug(
					"cache eql - total (ms):"
							+ (System.currentTimeMillis() - t1));
			try {
				itemResult.setTransforms(objectsToDtes(results, assoc
						.implementationClass(), true));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			result.add(itemResult);
		}
		return result;
	}

	public boolean checkPropertyAccess(HasIdAndLocalId hili,
			String propertyName, boolean read) throws IntrospectionException {
		if (hili.getId() != 0) {
			PropertyPermissions pp = SEUtilities.descriptorByName(
					hili.getClass(), propertyName).getReadMethod()
					.getAnnotation(PropertyPermissions.class);
			ObjectPermissions op = hili.getClass().getAnnotation(
					ObjectPermissions.class);
			return PermissionsManager.get().checkEffectivePropertyPermission(
					op, pp, hili, read);
		}
		return true;
	}

	@Override
	/**
	 * TODO - ignore collection mods to collection properties with 
	 * the @OneToMany annotation (inefficient and unnecessary)
	 */
	public void consume(DomainTransformEvent evt)
			throws DomainTransformException {
		super.consume(evt);
	}

	@Override
	public <T extends HasIdAndLocalId> T createDomainObject(Class<T> objectClass) {
		long localId = nextLocalIdCounter();
		T newInstance = newInstance(objectClass, localId);
		// logic should probably be made clearer here - if id==0, we're not in
		// an
		// entitymanager context
		// so we want to record creates
		// nah - in fact, always record creates (even if in-em), but don't
		// process in consume() if obj exists
		// if (newInstance.getId() == 0) {
		fireCreateObjectEvent(newInstance.getClass(), newInstance.getId(),
				newInstance.getLocalId());
		// }
		registerDomainObject(newInstance);
		return newInstance;
	}

	@Override
	// for the moment, just ignore double deletes.
	public DomainTransformEvent deleteObject(HasIdAndLocalId hili) {
		DomainTransformEvent event = super.deleteObject(hili);
		if (event != null) {
			addTransform(event);
		}
		return event;
	}

	@Override
	public void deregisterObject(HasIdAndLocalId hili) {
		HasIdAndLocalId object = getObject(hili);
		try {
			PropertyDescriptor[] pds = Introspector
					.getBeanInfo(hili.getClass()).getPropertyDescriptors();
			for (PropertyDescriptor pd : pds) {
				if (Set.class.isAssignableFrom(pd.getPropertyType())) {
					Association info = pd.getReadMethod().getAnnotation(
							Association.class);
					Set set = (Set) pd.getReadMethod().invoke(hili,
							CommonUtils.EMPTY_OBJECT_ARRAY);
					if (info != null && set != null) {
						for (Object o2 : set) {
							String accessorName = "get"
									+ CommonUtils.capitaliseFirst(info
											.propertyName());
							Object o3 = o2.getClass().getMethod(accessorName,
									new Class[0]).invoke(o2,
									CommonUtils.EMPTY_OBJECT_ARRAY);
							if (o3 instanceof Set) {
								Set assocSet = (Set) o3;
								assocSet.remove(hili);
							}
							// direct references (parent/one-one) are not
							// removed, throw a dependency exception instead
						}
					}
				}
			}
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
		entityManager.remove(object);
	}

	public String displayNameForObject(Object o) {
		return ObjectPersistenceHelper.get().displayNameForObject(o);
	}

	public List<String> getAnnotatedPropertyNames(Class clazz) {
		return ObjectPersistenceHelper.get().getAnnotatedPropertyNames(clazz);
	}

	public <A extends Annotation> A getAnnotationForClass(Class targetClass,
			Class<A> annotationClass) {
		return (A) targetClass.getAnnotation(annotationClass);
	}

	public <A extends Annotation> A getAnnotationForProperty(Class targetClass,
			Class<A> annotationClass, String propertyName) {
		try {
			PropertyDescriptor[] pds = Introspector.getBeanInfo(targetClass)
					.getPropertyDescriptors();
			for (PropertyDescriptor pd : pds) {
				if (pd.getName().equals(propertyName)) {
					return pd.getReadMethod().getAnnotation(annotationClass);
				}
			}
			return null;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public Class getClassForName(String fqn) {
		// Should never be called - but anyway...
		try {
			return Class.forName(fqn);
		} catch (ClassNotFoundException e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public ClientInstance getClientInstance() {
		return this.clientInstance;
	}

	public EntityManager getEntityManager() {
		return entityManager;
	}

	public List<DomainTransformEvent> getModificationEvents() {
		return this.modificationEvents;
	}

	public <T extends HasIdAndLocalId> T getObject(Class<? extends T> c,
			long id, long localId) {
		if (!HasIdAndLocalId.class.isAssignableFrom(c)) {
			throw new WrappedRuntimeException(
					"Attempting to obtain incompatible bean: " + c,
					SuggestedAction.NOTIFY_WARNING);
		}
		if (id == 0) {
			if (localIdToEntityMap.containsKey(localId)) {
				return (T) localIdToEntityMap.get(localId);
			}
			if (userSessionHiliMap != null && localId != 0) {
				id = userSessionHiliMap.containsKey(localId) ? userSessionHiliMap
						.get(localId).id
						: 0;
			}
		}
		if (id != 0 && getEntityManager() != null) {
			if (WrapperPersistable.class.isAssignableFrom(c)) {
				try {
					Class okClass = c;
					T wofu = (T) EntityLayerLocator.get()
							.wrappedObjectProvider().getWrappedObjectForUser(
									okClass, id, getEntityManager());
					return (T) wofu;
				} catch (Exception e) {
					throw new WrappedRuntimeException(e);
				}
			}
			T t = getEntityManager().find(c, id);
			//this may be a performance hit - but worth it - otherwise all sorts of potential problems
			//basically, transform events should (must) always have refs to "real" objects, not wrappers
			t = EntityLayerLocator.get().jpaImplementation().getInstantiatedObject(t);
			if (listenToFoundObjects
					&& t instanceof SourcesPropertyChangeEvents) {
				((SourcesPropertyChangeEvents) t)
						.addPropertyChangeListener(this);
			}
			if (localId != 0 && t != null) {
				localIdToEntityMap.put(localId, t);
			}
			return t;
		}
		return null;
	}

	public Class getPropertyType(Class clazz, String propertyName) {
		return ObjectPersistenceHelper.get().getPropertyType(clazz,
				propertyName);
	}

	public Object getPropertyValue(Object bean, String propertyName) {
		if (!(bean instanceof HasIdAndLocalId)) {
			throw new WrappedRuntimeException(
					"Bean not an instance of HasIdAndLocalId: " + bean,
					SuggestedAction.NOTIFY_WARNING);
		}
		HasIdAndLocalId hili = (HasIdAndLocalId) bean;
		try {
			return SEUtilities.descriptorByName(bean.getClass(), propertyName)
					.getReadMethod().invoke(bean);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	@Override
	public TransformManager getT() {
		return (TransformManager) getTTL.get();
	}

	public <T> T getTemplateInstance(Class<T> clazz) {
		return ObjectPersistenceHelper.get().getTemplateInstance(clazz);
	}

	public List<PropertyInfoLite> getWritableProperties(Class clazz) {
		return ObjectPersistenceHelper.get().getWritableProperties(clazz);
	}

	public boolean isListenToFoundObjects() {
		return listenToFoundObjects;
	}

	public <T> T newInstance(Class<T> clazz) {
		return ObjectPersistenceHelper.get().newInstance(clazz);
	}

	/**
	 * Can be called from the server layer(entityManager==null)
	 */
	public <T> T newInstance(Class<T> clazz, long localId) {
		try {
			if (HasIdAndLocalId.class.isAssignableFrom(clazz)) {
				HasIdAndLocalId newInstance = (HasIdAndLocalId) clazz
						.newInstance();
				newInstance.setLocalId(localId);
				localIdToEntityMap.put(localId, newInstance);
				if (entityManager != null) {
					entityManager.persist(newInstance);
				}
				if (userSessionHiliMap != null) {
					userSessionHiliMap.put(localId, new HiliLocator(
							(Class<? extends HasIdAndLocalId>) clazz,
							newInstance.getId()));
				}
				return (T) newInstance;
			}
			throw new Exception("only construct hilis here");
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	@Override
	// NOTE - doesn't register children (unlike client)
	public void registerDomainObject(HasIdAndLocalId hili) {
		if (hili instanceof SourcesPropertyChangeEvents) {
			listenTo((SourcesPropertyChangeEvents) hili);
		}
	}

	public void resetTltm(HiliLocatorMap locatorMap) {
		setEntityManager(null);
		this.userSessionHiliMap = locatorMap;
		localIdToEntityMap = new HashMap<Long, HasIdAndLocalId>();
		modifiedObjects = new HashSet<HasIdAndLocalId>();
		modificationEvents = new ArrayList<DomainTransformEvent>();
		transformListenerSupport.clear();
		clearTransforms();
		addDomainTransformListener(new ServerTransformListener());
		for (DomainTransformListener listener : threadLocalListeners) {
			addDomainTransformListener(listener);
		}
	}

	public void setClientInstance(ClientInstance clientInstance) {
		this.clientInstance = clientInstance;
	}

	public void setEntityManager(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	public void setListenToFoundObjects(boolean registerFoundObjects) {
		this.listenToFoundObjects = registerFoundObjects;
	}

	public void setPropertyValue(Object bean, String propertyName, Object value) {
		if (!(bean instanceof HasIdAndLocalId)) {
			throw new WrappedRuntimeException(
					"Attempting to serialize incompatible bean: " + bean,
					SuggestedAction.NOTIFY_WARNING);
		}
		HasIdAndLocalId hili = (HasIdAndLocalId) bean;
		if (hili.getId() != 0) {
			try {
				PropertyDescriptor[] pds = Introspector.getBeanInfo(
						bean.getClass()).getPropertyDescriptors();
				for (PropertyDescriptor pd : pds) {
					if (pd.getName().equals(propertyName)) {
						pd.getWriteMethod().invoke(bean, value);
						return;
					}
				}
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		} else {
		}
		throw new WrappedRuntimeException(
				"Attempting to alter property of non-persistent bean: " + bean,
				SuggestedAction.NOTIFY_WARNING);
	}

	private String buildEqlForSpec(ObjectCacheItemSpec itemSpec,
			Class assocClass) throws Exception {
		ObjectRef ref = itemSpec.getObjectRef();
		Class refClass = ref.getClassRef().getRefClass();
		List<String> projections = new ArrayList<String>();
		ClassLookup classLookup = CommonLocator.get().classLookup();
		String specProperty = null;
		projections.add(CommonUtils.format("t.%1 as %1", "id"));
		List<PropertyInfoLite> pds = classLookup
				.getWritableProperties(assocClass);
		for (PropertyInfoLite pd : pds) {
			String propertyName = pd.getPropertyName();
			if (ignorePropertyForCaching(assocClass, pd.getPropertyType(),
					propertyName)) {
				continue;
			}
			Class clazz = pd.getPropertyType();
			if (!HasIdAndLocalId.class.isAssignableFrom(clazz)) {
				projections.add(CommonUtils.format("t.%1 as %1", propertyName));
			} else {
				projections.add(CommonUtils.format("t.%1.id as %1_id",
						propertyName));
				if (clazz == refClass) {
					specProperty = propertyName;
				}
			}
		}
		String template = "select %1 from %2 t where t.%3.id=%4";
		return CommonUtils.format(template, CommonUtils.join(projections, ","),
				assocClass.getSimpleName(), specProperty, ref.getId());
	}

	private void checkPropertyReadAccessAndThrow(HasIdAndLocalId hili,
			String propertyName, DomainTransformEvent evt)
			throws DomainTransformException, IntrospectionException {
		if (!checkPropertyAccess(hili, propertyName, true)) {
			throw new DomainTransformException(new Exception(
					"Permission denied : write - object/property " + evt));
		}
	}

	private boolean checkPropertyWriteAccessAndThrow(HasIdAndLocalId hili,
			String propertyName, DomainTransformEvent evt)
			throws DomainTransformException, IntrospectionException {
		if (!checkPropertyAccess(hili, propertyName, false)) {
			DomainPropertyInfo ann = getAnnotationForProperty(hili.getClass(),
					DomainPropertyInfo.class, propertyName);
			if (ann != null && ann.silentFailOnIllegalWrites()) {
				return false;
			}
			throw new DomainTransformException(new Exception(
					"Permission denied : write - object/property " + evt));
		}
		return true;
	}

	private void listenTo(SourcesPropertyChangeEvents spce) {
		spce.addPropertyChangeListener(this);
	}

	public void reconstituteHiliMap() {
		if (clientInstance != null) {
			CommonPersistenceLocal cp = EntityLayerLocator.get()
					.commonPersistenceProvider().getCommonPersistence();
			String message = "Reconstitute hili map - clientInstance: "
					+ clientInstance.getId();
			// System.out.println(message);
			// cp.log(message, LogMessageType.INFO.toString());
			String dteName = cp.getImplementation(
					DomainTransformEventPersistent.class).getSimpleName();
			MetricLogging.get().start(message);
			List<? extends DomainTransformEvent> dtes = getEntityManager()
					.createQuery(
							"from "
									+ dteName
									+ " dte "
									+ " where dte.domainTransformRequestPersistent.clientInstance.id = ?"
									+ " and dte.objectLocalId!=0 and dte.transformType = ?")
					.setParameter(1, clientInstance.getId()).setParameter(2,
							TransformType.CREATE_OBJECT).getResultList();
			// force non-empty
			userSessionHiliMap.put((long) -1, null);
			for (DomainTransformEvent dte : dtes) {
				userSessionHiliMap.put(dte.getObjectLocalId(), new HiliLocator(
						null, dte.getObjectId()));
			}
			MetricLogging.get().end(message);
		}
	}

	@Override
	protected boolean checkPermissions(HasIdAndLocalId hili,
			DomainTransformEvent evt, String propertyName, Object change) {
		try {
			if (hili == null) {
				hili = (HasIdAndLocalId) evt.getObjectClass().newInstance();
			} else {
				hili = EntityLayerLocator.get().jpaImplementation()
						.getInstantiatedObject(hili);
			}
			ObjectPermissions op = hili.getClass().getAnnotation(
					ObjectPermissions.class);
			op = op == null ? PermissionsManager.get()
					.getDefaultObjectPermissions() : op;
			switch (evt.getTransformType()) {
			case ADD_REF_TO_COLLECTION:
			case REMOVE_REF_FROM_COLLECTION:
				checkPropertyReadAccessAndThrow(hili, propertyName, evt);
				// if (change instanceof HasIdAndLocalId){
				// checkPropertyWriteAccessAndThrow(change, propertyName, evt);
				// }
				// TODO
				break;
			case NULL_PROPERTY_REF:
			case CHANGE_PROPERTY_REF:
			case CHANGE_PROPERTY_SIMPLE_VALUE:
				return checkPropertyWriteAccessAndThrow(hili, propertyName, evt);
			case CREATE_OBJECT:
				if (!PermissionsManager.get().isPermissible(hili, op.create())) {
					throw new DomainTransformException(new Exception(
							"Permission denied : create - object " + evt));
				}
				break;
			case DELETE_OBJECT:
				if (!PermissionsManager.get().isPermissible(hili, op.delete())) {
					throw new DomainTransformException(new Exception(
							"Permission denied : delete - object " + evt));
				}
				break;
			}
			// TODO:3.2, check r/w access for bean for add/remove ref
			// check r/w access for bean for all
		} catch (Exception e) {
			if (e instanceof DomainTransformException) {
				DomainTransformException dtex = (DomainTransformException) e;
				dtex.setEvent(evt);
				evt.setSource(hili);
				evt.setPropertyName(propertyName);
			}
			EntityLayerLocator.get().log(LogMessageType.TRANSFORM_EXCEPTION,
					"Data transform permissions exception", e);
			throw new WrappedRuntimeException(e);
		}
		return true;
	}

	@Override
	protected Enum getTargetEnumValue(DomainTransformEvent evt) {
		return ObjectPersistenceHelper.get().getTargetEnumValue(evt);
	}

	@Override
	protected void objectModified(HasIdAndLocalId hili,
			DomainTransformEvent evt, boolean targetObject) {
		boolean addToResults = false;
		if (evt.getTransformType() == TransformType.CREATE_OBJECT) {
			addToResults = true;
			evt.setGeneratedServerId(hili.getId());
		}
		if (hili instanceof HasVersionNumber && !modifiedObjects.contains(hili)) {
			addToResults = true;
			modifiedObjects.add(hili);
			HasVersionNumber hv = (HasVersionNumber) hili;
			if (targetObject) {
				evt.setValueVersionNumber(hv.getVersionNumber() + 1);
				if (evt.getValueId() == 0) {
					evt.setValueId(hili.getId());
				}
			} else {
				evt.setObjectVersionNumber(hv.getVersionNumber() + 1);
				if (evt.getObjectId() == 0) {
					evt.setObjectId(hili.getId());
				}
			}
		}
		if (addToResults) {
			modificationEvents.add(evt);
		}
	}

	@Override
	// No need for property changes here
	// TODO - isn't this a huge hit?
	protected void updateAssociation(DomainTransformEvent evt,
			HasIdAndLocalId obj, Object tgt, boolean remove,
			boolean collectionPropertyChange) {
		ManyToMany manyToMany = CommonLocator.get().propertyAccessor()
				.getAnnotationForProperty(evt.getObjectClass(),
						ManyToMany.class, evt.getPropertyName());
		if (manyToMany != null && manyToMany.mappedBy().length() != 0) {
			super.updateAssociation(evt, obj, tgt, remove, false);
		}
	}

	public static class HiliLocator {
		public Class<? extends HasIdAndLocalId> clazz;

		public long id;

		public HiliLocator(Class<? extends HasIdAndLocalId> clazz, long id) {
			this.clazz = clazz;
			this.id = id;
		}
	}

	public static ThreadlocalTransformManager cast() {
		return (ThreadlocalTransformManager) TransformManager.get();
	}
}
