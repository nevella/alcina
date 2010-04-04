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
package cc.alcina.framework.common.client.logic.domaintransform;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cc.alcina.framework.common.client.CommonLocator;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.WrappedRuntimeException.SuggestedAction;
import cc.alcina.framework.common.client.csobjects.ObjectCacheItemResult;
import cc.alcina.framework.common.client.csobjects.ObjectCacheItemSpec;
import cc.alcina.framework.common.client.entity.GwtPersistableObject;
import cc.alcina.framework.common.client.logic.MutablePropertyChangeSupport;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.CollectionModification.CollectionModificationEvent;
import cc.alcina.framework.common.client.logic.domaintransform.CollectionModification.CollectionModificationListener;
import cc.alcina.framework.common.client.logic.domaintransform.CollectionModification.CollectionModificationSource;
import cc.alcina.framework.common.client.logic.domaintransform.CollectionModification.CollectionModificationSupport;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest.DomainTransformRequestType;
import cc.alcina.framework.common.client.logic.domaintransform.spi.ClassLookup;
import cc.alcina.framework.common.client.logic.domaintransform.spi.ObjectLookup;
import cc.alcina.framework.common.client.logic.domaintransform.spi.PropertyAccessor;
import cc.alcina.framework.common.client.logic.domaintransform.spi.ClassLookup.PropertyInfoLite;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.Association;
import cc.alcina.framework.common.client.logic.reflection.BeanInfo;
import cc.alcina.framework.common.client.logic.reflection.ClientBeanReflector;
import cc.alcina.framework.common.client.logic.reflection.ClientPropertyReflector;
import cc.alcina.framework.common.client.logic.reflection.ClientReflector;
import cc.alcina.framework.common.client.logic.reflection.DomainPropertyInfo;
import cc.alcina.framework.common.client.logic.reflection.ObjectPermissions;
import cc.alcina.framework.common.client.logic.reflection.PropertyPermissions;
import cc.alcina.framework.common.client.logic.reflection.WrapperInfo;
import cc.alcina.framework.common.client.logic.reflection.ClientBeanReflector.HasAnnotationCallback;
import cc.alcina.framework.common.client.util.CloneHelper;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.DomainObjectCloner;
import cc.alcina.framework.common.client.util.LookupMapToMap;
import cc.alcina.framework.common.client.util.SimpleStringParser;
import cc.alcina.framework.gwt.client.ClientBase;
import cc.alcina.framework.gwt.client.ClientLayerLocator;
import cc.alcina.framework.gwt.client.ide.provider.CollectionFilter;
import cc.alcina.framework.gwt.client.ide.provider.DefaultCollectionFilter;
import cc.alcina.framework.gwt.client.widget.dialog.CancellableRemoteDialog;
import cc.alcina.framework.gwt.client.widget.dialog.NonCancellableRemoteDialog;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.totsp.gwittir.client.beans.SourcesPropertyChangeEvents;

/**
 * TODO - abstract parts out to ClientTransformManager
 * 
 * @author nick@alcina.cc
 * 
 */
@SuppressWarnings("unchecked")
// unchecked because reflection is always going to involve a lot of
// casting...alas
public class TransformManager implements PropertyChangeListener, ObjectLookup,
		CollectionModificationSource {
	private static final String UNSPECIFIC_PROPERTY_CHANGE = "---";

	public static final String ID_FIELD_NAME = "id";

	public static final String LOCAL_ID_FIELD_NAME = "localId";

	public static final String VERSION_FIELD_NAME = "versionNumber";

	protected static final Set<String> ignorePropertiesForCaching = new HashSet<String>(
			Arrays.asList(new String[] { "class", "id", "localId",
					"propertyChangeListeners" }));

	MapObjectLookup domainObjects;

	private static long eventIdCounter = 1;

	private static long localIdCounter = 1;

	protected TransformManagerCache cache;

	final Set<DomainTransformEvent> transforms = new LinkedHashSet<DomainTransformEvent>();

	final Map<CommitType, LinkedHashSet<DomainTransformEvent>> transformsByType = new HashMap<CommitType, LinkedHashSet<DomainTransformEvent>>();

	private PersistableTransformListener persistableTransformListener;

	protected DomainTransformSupport transformListenerSupport;

	private static TransformManager theInstance;

	public static TransformManager get() {
		if (theInstance == null) {
			theInstance = new TransformManager();
		}
		TransformManager tm = theInstance.getT();
		if (tm != null) {
			return tm;
		}
		return theInstance;
	}

	public static void register(TransformManager tm) {
		theInstance = tm;
	}

	private Collection provisionalObjects = new LinkedHashSet();

	private boolean ignorePropertyChanges;

	private boolean replayingRemoteEvent;

	private CollectionModificationSupport collectionModificationSupport;

	protected TransformManager() {
		cache = new TransformManagerCache();
		this.transformListenerSupport = new DomainTransformSupport();
		this.collectionModificationSupport = new CollectionModificationSupport();
	}

	public void addCollectionModificationListener(
			CollectionModificationListener listener) {
		this.collectionModificationSupport
				.addCollectionModificationListener(listener);
	}

	public void addCollectionModificationListener(
			CollectionModificationListener listener, Class listenedClass) {
		this.collectionModificationSupport.addCollectionModificationListener(
				listener, listenedClass);
	}

	public void addCollectionModificationListener(
			CollectionModificationListener listener, Class listenedClass,
			boolean filteringListener) {
		this.collectionModificationSupport.addCollectionModificationListener(
				listener, listenedClass, filteringListener);
	}

	public boolean currentTransformIsDuringCreationRequest() {
		return currentEvent.getObjectLocalId()!=0;
	}

	/**
	 * Order must be: local (>bean), containerupdater, localstorage,
	 * remotestorage By default, transform manager handles the first two itself
	 */
	public void addDomainTransformListener(DomainTransformListener listener) {
		this.transformListenerSupport.addDomainTransformListener(listener);
	}

	public void clearUserObjects() {
		getCache().clearUserObjects();
		requiresEditPrep.clear();
		domainObjects = null;
	}

	public void addTransform(DomainTransformEvent evt) {
		transforms.add(evt);
		getTransformsByCommitType(evt.getCommitType()).add(evt);
	}

	public void appShutdown() {
		theInstance = null;
	}

	public HasIdAndLocalId clone(HasIdAndLocalId obj) {
		try {
			DomainObjectCloner cloner = new DomainObjectCloner();
			HasIdAndLocalId ret = cloner.deepBeanClone(obj);
			promoteToDomain(cloner.getProvisionalObjects(), true);
			return getObject(ret);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	private DomainTransformEvent currentEvent;

	public void consume(DomainTransformEvent evt) {
		currentEvent = evt;
		HasIdAndLocalId obj = null;
		if (evt.getTransformType() != TransformType.CREATE_OBJECT) {
			obj = getObject(evt);
		}
		Object tgt = (evt.getTransformType() == TransformType.NULL_PROPERTY_REF) ? null
				: getTargetObject(evt, false);
		if (!checkPermissions(obj, evt, evt.getPropertyName(), tgt)) {
			return;
		}
		switch (evt.getTransformType()) {
		// these cases will fire a new transform event (temp obj > domain obj),
		// so should not be processed further
		case NULL_PROPERTY_REF:
		case CHANGE_PROPERTY_REF:
		case CHANGE_PROPERTY_SIMPLE_VALUE:
			if (isReplayingRemoteEvent() && obj == null) {
				// it's been deleted on the client, but we've just now got the
				// creation id
				// note, should never occur TODO: notify server
				return;
			}
			CommonLocator.get().propertyAccessor().setPropertyValue(obj,
					evt.getPropertyName(), tgt);
			String pn = evt.getPropertyName();
			if (pn.equals(TransformManager.ID_FIELD_NAME)
					|| pn.equals(TransformManager.LOCAL_ID_FIELD_NAME)) {
				domainObjects.changeMapping(obj, evt.getObjectId(), evt
						.getObjectLocalId());
			}
			if (evt.getCommitType() == CommitType.TO_LOCAL_BEAN) {
				removeTransform(evt);
			}
			objectModified(obj, evt, false);
			switch (evt.getTransformType()) {
			case NULL_PROPERTY_REF:
			case CHANGE_PROPERTY_REF:
				if (evt.getOldValue() != null
						&& evt.getOldValue() instanceof HasIdAndLocalId) {
					updateAssociation(evt, obj, (HasIdAndLocalId) evt
							.getOldValue(), true, true);
				}
				updateAssociation(evt, obj, tgt, false, true);
				break;
			}
			break;
		// add and removeref will not cause a property change, so no transform
		// removal
		case ADD_REF_TO_COLLECTION:
			((Set) CommonLocator.get().propertyAccessor().getPropertyValue(obj,
					evt.getPropertyName())).add(tgt);
			objectModified(obj, evt, false);
			updateAssociation(evt, obj, tgt, false, false);
			collectionChanged(obj, tgt);
			break;
		case REMOVE_REF_FROM_COLLECTION:
			((Set) CommonLocator.get().propertyAccessor().getPropertyValue(obj,
					evt.getPropertyName())).remove(tgt);
			updateAssociation(evt, obj, tgt, true, false);
			collectionChanged(obj, tgt);
			break;
		case DELETE_OBJECT:
			deregisterObject((HasIdAndLocalId) obj);
			break;
		case CREATE_OBJECT:
			HasIdAndLocalId hili = (HasIdAndLocalId) CommonLocator.get()
					.classLookup().newInstance(evt.getObjectClass(),
							evt.getObjectLocalId());
			hili.setLocalId(evt.getObjectLocalId());
			if (hili.getId() == 0) {// replay from server
				hili.setId(evt.getObjectId());
			}
			evt.setObjectId(hili.getId());
			if (evt.getCommitType() == CommitType.TO_REMOTE_STORAGE) {
				objectModified(hili, evt, false);
			}
			if (domainObjects != null) {
				domainObjects.mapObject(hili);
				fireCollectionModificationEvent(new CollectionModificationEvent(
						this, hili.getClass(), domainObjects.getCollection(hili
								.getClass())));
			}
			break;
		default:
			throw new WrappedRuntimeException(
					"Transform type not implemented: " + evt.getTransformType(),
					SuggestedAction.NOTIFY_WARNING);
		}
		currentEvent=null;
	}

	public void convertToTargetObject(DomainTransformEvent evt) {
		Object value = evt.getNewValue();
		if (value == null) {
			return;
		}
		evt.setValueClass(value.getClass());
		if (value.getClass() == Integer.class
				|| value.getClass() == String.class
				|| value.getClass() == Double.class
				|| value.getClass() == Float.class
				|| value.getClass() == Short.class
				|| value.getClass() == Boolean.class || value instanceof Enum) {
			evt.setNewStringValue(value.toString());
		} else if (value.getClass() == Long.class) {
			evt.setNewStringValue(SimpleStringParser
					.longToGwtDoublesToString((Long) value));
		} else if (value.getClass() == Date.class) {
			evt.setNewStringValue(SimpleStringParser
					.longToGwtDoublesToString((((Date) value).getTime())));
		} else if (value instanceof HasIdAndLocalId) {
			HasIdAndLocalId hili = (HasIdAndLocalId) value;
			evt.setValueId(hili.getId());
			evt.setValueLocalId(hili.getLocalId());
		}
	}

	public <T extends HasIdAndLocalId> T createDomainObject(Class<T> objectClass) {
		long localId = TransformManager.get().nextLocalIdCounter();
		T newInstance = CommonLocator.get().classLookup().newInstance(
				objectClass, localId);
		newInstance.setLocalId(localId);
		registerDomainObject(newInstance);
		return newInstance;
	}

	public <T extends HasIdAndLocalId> T createProvisionalObject(
			Class<T> objectClass) {
		long localId = TransformManager.get().nextLocalIdCounter();
		T newInstance = CommonLocator.get().classLookup().newInstance(
				objectClass, localId);
		newInstance.setLocalId(localId);
		registerProvisionalObject(newInstance);
		return newInstance;
	}

	public void deregisterObject(HasIdAndLocalId hili) {
		if (domainObjects != null) {
			removeAssociations(hili);
			domainObjects.deregisterObject(hili);
			fireCollectionModificationEvent(new CollectionModificationEvent(
					this, hili.getClass(), domainObjects.getCollection(hili
							.getClass())));
		}
	}

	public void deregisterProvisionalObject(Object o) {
		deregisterProvisionalObjects(CommonUtils.wrapInCollection(o));
	}

	public void deregisterProvisionalObjects(Collection c) {
		provisionalObjects.removeAll(c);
		for (Object b : c) {
			if (b instanceof SourcesPropertyChangeEvents) {
				SourcesPropertyChangeEvents sb = (SourcesPropertyChangeEvents) b;
				sb.removePropertyChangeListener(this);
			}
		}
		removeTransformsForObjects(c);
	}

	public <V extends HasIdAndLocalId> Set<V> registeredObjectsAsSet(
			Class<V> clazz) {
		return new LinkedHashSet<V>(getDomainObjects().getCollection(clazz));
	}

	public <V extends HasIdAndLocalId> V registeredSingleton(Class<V> clazz) {
		Collection<V> c = getDomainObjects().getCollection(clazz);
		return c.isEmpty() ? null : c.iterator().next();
	}

	public <V extends HasIdAndLocalId> List<V> filter(Class<V> clazz,
			CollectionFilter<V> filter) {
		List<V> result = new ArrayList<V>(getDomainObjects().getCollection(
				clazz));
		if (filter != null) {
			result = DefaultCollectionFilter.filter(result, filter);
		}
		if (!result.isEmpty() && result.get(0) instanceof Comparable) {
			Collections.sort((List) result);
		}
		return result;
	}

	public void fireCollectionModificationEvent(
			CollectionModificationEvent event) {
		this.collectionModificationSupport
				.fireCollectionModificationEvent(event);
	}

	public void fireDomainTransform(DomainTransformEvent event)
			throws DomainTransformException {
		this.transformListenerSupport.fireDomainTransform(event);
	}

	public DomainTransformEvent deleteObject(HasIdAndLocalId hili) {
		DomainTransformEvent dte = new DomainTransformEvent();
		dte.setObjectId(hili.getId());
		dte.setObjectLocalId(hili.getLocalId());
		dte.setObjectClass(hili.getClass());
		dte.setTransformType(TransformType.DELETE_OBJECT);
		try {
			fireDomainTransform(dte);
			return dte;
		} catch (DomainTransformException e) {
			DomainTransformRuntimeException dtre = new DomainTransformRuntimeException(
					e.getMessage());
			dtre.setEvent(e.getEvent());
			throw dtre;
		}
	}

	public MapObjectLookup getDomainObjects() {
		return this.domainObjects;
	}

	public <T extends HasIdAndLocalId> T getObject(Class<? extends T> c,
			long id, long localId) {
		if (this.domainObjects != null) {
			return domainObjects.getObject(c, id, localId);
		}
		return null;
	}

	public HasIdAndLocalId getObject(DomainTransformEvent dte) {
		HasIdAndLocalId obj = CommonLocator.get().objectLookup()
				.getObject(dte.getObjectClass(), dte.getObjectId(),
						dte.getObjectLocalId());
		dte.setSource(obj);
		return obj;
	}

	public <T extends HasIdAndLocalId> T getObject(T hili) {
		return (T) CommonLocator.get().objectLookup().getObject(
				hili.getClass(), hili.getId(), hili.getLocalId());
	}

	public Collection getProvisionalObjects() {
		return provisionalObjects;
	}

	public TransformManager getT() {
		return null;
	}

	public boolean resolveMissingObject(DomainTransformEvent evt) {
		return isReplayingRemoteEvent();
	}

	public Object getTargetObject(DomainTransformEvent evt, boolean oldValue) {
		if (evt.getNewValue() != null || evt.getValueClass() == null) {
			if (evt.getNewValue() instanceof HasIdAndLocalId) {
				HasIdAndLocalId hili = CommonLocator.get().objectLookup()
						.getObject((HasIdAndLocalId) evt.getNewValue());
				if (hili != null) {
					return hili;
				} else {
					// this should probably never happen...??
					throw new WrappedRuntimeException(
							"Unable to get target object " + evt,
							SuggestedAction.NOTIFY_ERROR);
				}
			}
			return evt.getNewValue();
		}
		if (evt.getValueClass() == String.class) {
			return evt.getNewStringValue();
		}
		if (evt.getValueClass() == Long.class) {
			return SimpleStringParser.toLong(evt.getNewStringValue());
		}
		if (evt.getValueClass() == Double.class) {
			return Double.valueOf(evt.getNewStringValue());
		}
		if (evt.getValueClass() == Integer.class) {
			return Integer.valueOf(evt.getNewStringValue());
		}
		if (evt.getValueClass() == Boolean.class) {
			return Boolean.valueOf(evt.getNewStringValue());
		}
		if (evt.getValueClass() == Date.class) {
			return new Date(SimpleStringParser.toLong(evt.getNewStringValue()));
		}
		Enum e = getTargetEnumValue(evt);
		if (e != null) {
			return e;
		}
		if (evt.getValueId() != 0 || evt.getValueLocalId() != 0) {
			HasIdAndLocalId object = CommonLocator.get().objectLookup()
					.getObject(evt.getValueClass(), evt.getValueId(),
							evt.getValueLocalId());
			if (object != null) {
				return object;
			}
			if (resolveMissingObject(evt)) {// will be resolved async (if
				// involving a server get)
				return null;
			}
		}
		throw new WrappedRuntimeException("Unable to get target object " + evt,
				SuggestedAction.NOTIFY_ERROR);
	}

	public Set<DomainTransformEvent> getTransforms() {
		return this.transforms;
	}

	public LinkedHashSet<DomainTransformEvent> getTransformsByCommitType(
			CommitType ct) {
		if (transformsByType.get(ct) == null) {
			transformsByType.put(ct, new LinkedHashSet<DomainTransformEvent>());
		}
		return transformsByType.get(ct);
	}

	protected void clearTransforms() {
		getTransforms().clear();
		for (CommitType ct : transformsByType.keySet()) {
			transformsByType.get(ct).clear();
		}
	}

	public boolean isIgnorePropertyChanges() {
		return this.ignorePropertyChanges;
	}

	public boolean isReplayingRemoteEvent() {
		return this.replayingRemoteEvent;
	}

	public synchronized long nextEventIdCounter() {
		return eventIdCounter++;
	}

	public synchronized long nextLocalIdCounter() {
		return localIdCounter++;
	}

	private Map<Class, Boolean> requiresEditPrep = new HashMap<Class, Boolean>();

	public Collection prepareForEditing(HasIdAndLocalId domainObject,
			boolean autoSave) {
		List children = new ArrayList();
		ClientBeanReflector bi = ClientReflector.get().beanInfoForClass(
				domainObject.getClass());
		if (bi == null) {
			return children;
		}
		Collection<ClientPropertyReflector> prs = bi.getPropertyReflectors()
				.values();
		Class<? extends Object> c = domainObject.getClass();
		Boolean requiresPrep = requiresEditPrep.get(c);
		if (requiresPrep != null) {
			if (!requiresPrep) {
				return children;
			}
		} else {
			requiresEditPrep.put(c, false);
		}
		ObjectPermissions op = bi.getAnnotation(ObjectPermissions.class);
		BeanInfo beanInfo = bi.getAnnotation(BeanInfo.class);
		for (ClientPropertyReflector pr : prs) {
			PropertyPermissions pp = pr
					.getAnnotation(PropertyPermissions.class);
			DomainPropertyInfo instructions = pr
					.getAnnotation(DomainPropertyInfo.class);
			if (!PermissionsManager.get().checkEffectivePropertyPermission(op,
					pp, domainObject, false)) {
				continue;
			}
			String propertyName = pr.getPropertyName();
			Object currentValue = CommonLocator.get().propertyAccessor()
					.getPropertyValue(domainObject, propertyName);
			boolean create = instructions != null
					&& instructions.eagerCreation() && currentValue == null;
			if (requiresPrep == null
					&& instructions != null
					&& (instructions.eagerCreation() || instructions
							.cloneForProvisionalEditing())) {
				requiresEditPrep.put(c, true);
			}
			if (create) {
				HasIdAndLocalId newObj = autoSave ? TransformManager.get()
						.createDomainObject(pr.getPropertyType())
						: TransformManager.get().createProvisionalObject(
								pr.getPropertyType());
				CommonLocator.get().propertyAccessor().setPropertyValue(
						domainObject, propertyName, newObj);
				children.add(newObj);
				children.addAll(prepareForEditing(newObj, autoSave));
			} else {
				boolean cloneForEditing = instructions != null
						&& instructions.cloneForProvisionalEditing()
						&& !autoSave;
				if (cloneForEditing && !autoSave && currentValue != null) {
					if (currentValue instanceof Collection) {
						Collection cl = (Collection) currentValue;
						Collection<HasIdAndLocalId> hilis = CommonUtils
								.shallowCollectionClone(cl);
						cl.clear();
						for (HasIdAndLocalId hili : hilis) {
							HasIdAndLocalId clonedValue = (HasIdAndLocalId) new CloneHelper()
									.shallowishBeanClone(hili);
							children.add(clonedValue);
							children.addAll(prepareForEditing(clonedValue,
									autoSave));
							cl.add(clonedValue);
						}
					} else {
						HasIdAndLocalId clonedValue = (HasIdAndLocalId) new CloneHelper()
								.shallowishBeanClone(currentValue);
						CommonLocator.get().propertyAccessor()
								.setPropertyValue(domainObject, propertyName,
										clonedValue);
						children.add(clonedValue);
						children
								.addAll(prepareForEditing(clonedValue, autoSave));
					}
				}
			}
			// boolean cloneForEditing =
		}
		return children;
	}

	/**
	 * <p>
	 * Can be called multiple times - transforms will be removed after the first
	 * </p>
	 * <p>
	 * <b>Note</b> - Make sure you change an existing object reference to the
	 * returned value if you're going to make further changes to the (now
	 * promoted) object - e.g.
	 * </p>
	 * <code>
	 * mm.wrapper=TransformManager.get().promoteToDomainObject(mm.wrapper);
	 * mm.wrapper.setSaved(true);
	 * </code>
	 * <p>
	 * <i>not</i>
	 * </p>
	 * <code>
	 * TransformManager.get().promoteToDomainObject(mm.wrapper);
	 * mm.wrapper.setSaved(true);
	 * </code>
	 * <p>
	 * 
	 * Generally, though, you'll want to do all the modifications on the
	 * provisional object so that CollectionModificationListeners on the
	 * TransformManager will receive the domain object with all changes applied.
	 * 
	 * </p>
	 * 
	 * @param o
	 *            - the object to be promoted
	 * @return the newly promoted object, if it implements HasIdAndLocalId,
	 *         otherwise null
	 */
	public <T extends Object> T promoteToDomainObject(T o) {
		promoteToDomain(CommonUtils.wrapInCollection(o), true);
		if (o instanceof HasIdAndLocalId) {
			return (T) getObject((HasIdAndLocalId) o);
		}
		return null;
	}

	public void commitProvisionalObjects(Collection c) {
		promoteToDomain(c, false);
	}

	public boolean dirty(Collection provisionalObjects) {
		Collection<DomainTransformEvent> trs = TransformManager.get()
				.getTransformsByCommitType(CommitType.TO_LOCAL_BEAN);
		for (DomainTransformEvent dte : trs) {
			if (provisionalObjects.contains(dte.getSource())) {
				return true;
			}
		}
		return false;
	}

	public void propertyChange(PropertyChangeEvent evt) {
		if (ignorePropertyChanges
				|| UNSPECIFIC_PROPERTY_CHANGE.equals(evt.getPropertyName())) {
			return;
		}
		List<DomainTransformEvent> transforms = new ArrayList<DomainTransformEvent>();
		DomainTransformEvent dte = createTransformFromPropertyChange(evt);
		convertToTargetObject(dte);
		if (dte.getNewValue() == null) {
			dte.setTransformType(TransformType.NULL_PROPERTY_REF);
		}
		if (dte.getValueId() != 0 || dte.getValueLocalId() != 0) {
			dte.setTransformType(TransformType.CHANGE_PROPERTY_REF);
		}
		if (dte.getNewValue() instanceof Set) {
			Set typeCheck = (Set) evt.getNewValue();
			typeCheck = (Set) (typeCheck.isEmpty() ? evt.getOldValue()
					: typeCheck);
			if (typeCheck.iterator().next() instanceof HasIdAndLocalId) {
				Set<HasIdAndLocalId> oldValues = (Set) evt.getOldValue();
				Set<HasIdAndLocalId> newValues = (Set) evt.getNewValue();
				for (HasIdAndLocalId hili : newValues) {
					if (!oldValues.contains(hili)) {
						dte = createTransformFromPropertyChange(evt);
						dte.setNewValue(null);
						dte.setValueId(hili.getId());
						dte.setValueLocalId(hili.getLocalId());
						dte.setValueClass(hili.getClass());
						dte
								.setTransformType(TransformType.ADD_REF_TO_COLLECTION);
						transforms.add(dte);
					}
				}
				for (HasIdAndLocalId hili : oldValues) {
					if (!newValues.contains(hili)) {
						dte = createTransformFromPropertyChange(evt);
						dte.setNewValue(null);
						dte.setValueId(hili.getId());
						dte.setValueLocalId(hili.getLocalId());
						dte.setValueClass(hili.getClass());
						dte
								.setTransformType(TransformType.REMOVE_REF_FROM_COLLECTION);
						transforms.add(dte);
					}
				}
			} else if (typeCheck.iterator().next() instanceof Enum) {
				Set<Enum> oldValues = (Set) evt.getOldValue();
				Set<Enum> newValues = (Set) evt.getNewValue();
				for (Enum e : newValues) {
					if (!oldValues.contains(e)) {
						dte = createTransformFromPropertyChange(evt);
						dte.setNewValue(null);
						dte.setNewStringValue(e.name());
						dte.setValueClass(e.getClass());
						dte
								.setTransformType(TransformType.ADD_REF_TO_COLLECTION);
						transforms.add(dte);
					}
				}
				for (Enum e : oldValues) {
					if (!newValues.contains(e)) {
						dte = createTransformFromPropertyChange(evt);
						dte.setNewValue(null);
						dte.setNewStringValue(e.name());
						dte.setValueClass(e.getClass());
						dte
								.setTransformType(TransformType.REMOVE_REF_FROM_COLLECTION);
						transforms.add(dte);
					}
				}
			}
		} else {
			transforms.add(dte);
		}
		for (DomainTransformEvent domainTransformEvent : transforms) {
			addTransform(domainTransformEvent);
			try {
				fireDomainTransform(domainTransformEvent);
			} catch (DomainTransformException e) {
				DomainTransformRuntimeException dtre = new DomainTransformRuntimeException(
						e.getMessage());
				dtre.setEvent(e.getEvent());
				throw dtre;
			}
		}
		Object hili = evt.getSource();
		if (this.domainObjects != null) {
			if (!provisionalObjects.contains(hili)) {
				fireCollectionModificationEvent(new CollectionModificationEvent(
						this, hili.getClass(), domainObjects.getCollection(hili
								.getClass()), true));
			}
		}
	}

	public void registerDomainObjectsInHolder(DomainObjectHolder h) {
		if (this.domainObjects != null) {
			domainObjects.removeListeners();
		}
		this.domainObjects = new MapObjectLookup(this, h.topLevelObjects());
		ClassRef.add(h.getClassRefs());
	}

	protected void synthesiseCreateObjectEvent(Class clazz, long id,
			long localId) {
		if (id != 0) {
			return;
		}
		DomainTransformEvent dte = new DomainTransformEvent();
		dte.setSource(null);
		dte.setObjectId(id);
		dte.setObjectLocalId(localId);
		dte.setObjectClass(clazz);
		dte.setTransformType(TransformType.CREATE_OBJECT);
		addTransform(dte);
		try {
			fireDomainTransform(dte);
		} catch (DomainTransformException e) {
			DomainTransformRuntimeException dtre = new DomainTransformRuntimeException(
					e.getMessage());
			dtre.setEvent(e.getEvent());
			throw dtre;
		}
	}

	// a bit roundabout, but to ensure compatibility with the event system
	// essentially registers a synthesised object, then replaces it in the
	// mapping with the real one
	public void registerDomainObject(HasIdAndLocalId hili) {
		synthesiseCreateObjectEvent(hili.getClass(), hili.getId(), hili
				.getLocalId());
		if (domainObjects != null) {
			if (hili.getId() == 0) {
				HasIdAndLocalId createdObject = domainObjects.getObject(hili);
				domainObjects.deregisterObject(createdObject);
			}
			domainObjects.mapObject(hili);
		}
	}

	public void registerProvisionalObject(Object o) {
		Collection c = CommonUtils.wrapInCollection(o);
		provisionalObjects.addAll(c);
		for (Object b : c) {
			if (b instanceof SourcesPropertyChangeEvents) {
				SourcesPropertyChangeEvents sb = (SourcesPropertyChangeEvents) b;
				sb.removePropertyChangeListener(this);
				sb.addPropertyChangeListener(this);
			}
		}
	}

	public void removeCollectionModificationListener(
			CollectionModificationListener listener) {
		this.collectionModificationSupport
				.removeCollectionModificationListener(listener);
	}

	public void removeDomainTransformListener(DomainTransformListener listener) {
		this.transformListenerSupport.removeDomainTransformListener(listener);
	}

	public void replayRemoteEvents(Collection<DomainTransformEvent> evts,
			boolean fireTransforms) {
		try {
			setReplayingRemoteEvent(true);
			for (DomainTransformEvent dte : evts) {
				consume(dte);
				if (dte.getTransformType() == TransformType.CREATE_OBJECT
						&& dte.getObjectId() == 0
						&& dte.getObjectLocalId() != 0) {
					localIdCounter = Math.max(localIdCounter, dte
							.getObjectLocalId());
				}
				if (fireTransforms) {
					fireDomainTransform(dte);
				}
			}
		} catch (DomainTransformException e) {
			// shouldn't happen
			throw new WrappedRuntimeException(e);
		} finally {
			setReplayingRemoteEvent(false);
		}
	}

	public void setIgnorePropertyChanges(boolean ignorePropertyChanges) {
		this.ignorePropertyChanges = ignorePropertyChanges;
	}

	public void setReplayingRemoteEvent(boolean replayingRemoteEvent) {
		this.replayingRemoteEvent = replayingRemoteEvent;
	}

	public void setTransformCommitType(DomainTransformEvent evt, CommitType ct) {
		getTransformsByCommitType(evt.getCommitType()).remove(evt);
		evt.setCommitType(ct);
		getTransformsByCommitType(evt.getCommitType()).add(evt);
	}

	public void setupClientListeners() {
		addDomainTransformListener(new EditCommitTransformHandler());
		addDomainTransformListener(new ContainerUpdateTransformHandler());
	}

	private DomainTransformEvent createTransformFromPropertyChange(
			PropertyChangeEvent evt) {
		DomainTransformEvent dte = new DomainTransformEvent();
		dte.setSource(evt.getSource());
		dte.setOldValue(evt.getOldValue());
		dte.setNewValue(evt.getNewValue());
		dte.setPropertyName(evt.getPropertyName());
		HasIdAndLocalId dObj = (HasIdAndLocalId) evt.getSource();
		dte.setObjectId(dObj.getId());
		dte.setObjectLocalId(dObj.getLocalId());
		dte.setObjectClass(dObj.getClass());
		dte.setTransformType(TransformType.CHANGE_PROPERTY_SIMPLE_VALUE);
		return dte;
	}

	private void removeAssociations(HasIdAndLocalId hili) {
		ClientBeanReflector bi = ClientReflector.get().beanInfoForClass(
				hili.getClass());
		Collection<ClientPropertyReflector> prs = bi.getPropertyReflectors()
				.values();
		for (ClientPropertyReflector pr : prs) {
			DomainTransformEvent dte = new DomainTransformEvent();
			dte.setPropertyName(pr.getPropertyName());
			if (!ClientReflector.get()
					.isStandardJavaClass(pr.getPropertyType())) {
				Object object = CommonLocator.get().propertyAccessor()
						.getPropertyValue(hili, pr.getPropertyName());
				if (object != null && !(object instanceof Collection)) {
					updateAssociation(dte, hili, object, true, true);
				}
			}
		}
	}

	/**
	 * For subclasses Transform manager (client) explicitly doesn't check -
	 * that's handled by (what's) the presented UI
	 * 
	 * @param propertyName
	 * @param tgt
	 */
	protected boolean checkPermissions(HasIdAndLocalId eventTarget,
			DomainTransformEvent evt, String propertyName, Object change) {
		return true;
	}

	protected Enum getTargetEnumValue(DomainTransformEvent evt) {
		if (evt.getNewValue() instanceof Enum) {
			return (Enum) evt.getNewValue();
		}
		if (evt.getValueClass().isEnum()) {
			try {
				return Enum.valueOf(evt.getValueClass(), evt
						.getNewStringValue());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return null;
	}

	/**
	 * for subclasses to handle version increments
	 * 
	 * @param hili
	 * @param evt
	 */
	protected void objectModified(HasIdAndLocalId hili, DomainTransformEvent evt,
			boolean targetObject) {
	}

	public boolean hasUnsavedChanges(Object object) {
		Collection objects = CommonUtils.wrapInCollection(object);
		Collection<DomainTransformEvent> trs = TransformManager.get()
				.getTransformsByCommitType(CommitType.TO_LOCAL_BEAN);
		for (DomainTransformEvent dte : trs) {
			if (objects.contains(dte.getSource())) {
				return true;
			}
		}
		return false;
	}

	protected void promoteToDomain(Collection objects, boolean deregister) {
		try {
			CollectionModificationSupport.queue(true);
			for (Object o : objects) {
				if (o instanceof HasIdAndLocalId
						&& CommonLocator.get().objectLookup().getObject(
								(HasIdAndLocalId) o) == null) {
					HasIdAndLocalId hili = (HasIdAndLocalId) o;
					// if this is a new object, we want to register a blank
					// object,
					// so
					// property changes are played back properly against it
					// HasIdAndLocalId newInstance = (HasIdAndLocalId)
					// GWTDomainReflector
					// .get().newInstance(o.getClass(), 0);
					// newInstance.setLocalId(((HasIdAndLocalId)
					// o).getLocalId());
					// TransformManager.get().registerObject(newInstance);
					// actually, this should ALL be done by event - consume
					// note 2. it could be. but creation of provisional objects
					// doesn't generate a "create" event...no, in fact current
					// way
					// is better. so ignore all this. it works, it's fine
					synthesiseCreateObjectEvent(hili.getClass(), hili.getId(),
							hili.getLocalId());
				}
			}
			Collection<DomainTransformEvent> trs = TransformManager.get()
					.getTransformsByCommitType(CommitType.TO_LOCAL_BEAN);
			trs = (Set) ((LinkedHashSet) trs).clone();
			TransformManager.get().deregisterProvisionalObjects(objects);
			for (DomainTransformEvent dte : trs) {
				if (objects.contains(dte.getSource())) {
					if (dte.getTransformType() == TransformType.ADD_REF_TO_COLLECTION
							|| dte.getTransformType() == TransformType.REMOVE_REF_FROM_COLLECTION) {
						try {
							// ?why not consume?...aha,yup, not committed both
							// ways
							// yet.
							//
							TransformManager.get().fireDomainTransform(dte);
						} catch (Exception e) {
							throw new WrappedRuntimeException(e);
						}
					} else {
						TransformManager.get().consume(dte);
					}
				}
			}
		} finally {
			if (!deregister) {
				TransformManager.get().registerProvisionalObject(objects);
			}
			CollectionModificationSupport.queue(false);
		}
	}

	protected void removeTransform(DomainTransformEvent evt) {
		transforms.remove(evt);
		getTransformsByCommitType(evt.getCommitType()).remove(evt);
	}

	protected void removeTransformsForObjects(Collection c) {
		Set<DomainTransformEvent> trs = (Set<DomainTransformEvent>) TransformManager
				.get().getTransformsByCommitType(CommitType.TO_LOCAL_BEAN)
				.clone();
		for (DomainTransformEvent dte : trs) {
			if (c.contains(dte.getSource())) {
				removeTransform(dte);
			}
		}
	}

	protected void updateAssociation(DomainTransformEvent evt,
			HasIdAndLocalId obj, Object tgt, boolean remove,
			boolean collectionPropertyChange) {
		Association assoc = obj == null ? null : CommonLocator.get()
				.propertyAccessor().getAnnotationForProperty(obj.getClass(),
						Association.class, evt.getPropertyName());
		if (tgt == null || assoc == null || assoc.propertyName().length() == 0) {
			return;
		}
		HasIdAndLocalId hTgt = (HasIdAndLocalId) tgt;
		Object associatedObject = CommonLocator.get().propertyAccessor()
				.getPropertyValue(tgt, assoc.propertyName());
		boolean assocOjbIsCollection = associatedObject instanceof Collection;
		TransformType tt = assocOjbIsCollection ? (remove ? TransformType.REMOVE_REF_FROM_COLLECTION
				: TransformType.ADD_REF_TO_COLLECTION)
				: remove ? TransformType.NULL_PROPERTY_REF
						: TransformType.CHANGE_PROPERTY_REF;
		evt = new DomainTransformEvent();
		evt.setTransformType(tt);
		// No! Only should check one end of the relation for permissions
		// checkPermissions(hTgt, evt, assoc.propertyName());
		if (assocOjbIsCollection) {
			Collection c = (Collection) associatedObject;
			if (collectionPropertyChange && !assoc.silentUpdates()) {
				try {
					c = CommonUtils.shallowCollectionClone(c);
				} catch (Exception e) {
					throw new WrappedRuntimeException(e);
				}
			}
			if (remove) {
				c.remove(obj);
			} else {
				c.add(obj);
			}
			if (collectionPropertyChange && !assoc.silentUpdates()) {
				CommonLocator.get().propertyAccessor().setPropertyValue(tgt,
						assoc.propertyName(), c);
			}
		} else {
			CommonLocator.get().propertyAccessor().setPropertyValue(tgt,
					assoc.propertyName(), remove ? null : obj);
		}
		objectModified(hTgt, evt, true);
	}

	public TransformManagerCache getCache() {
		return cache;
	}

	public static enum CollectionModificationType {
		ADD, REMOVE
	}

	public void modifyCollectionProperty(Object objectWithCollection,
			String collectionPropertyName, Object delta,
			CollectionModificationType modificationType) {
		Collection deltaC = CommonUtils.wrapInCollection(delta);
		Collection c = CommonUtils
				.shallowCollectionClone((Collection) CommonLocator.get()
						.propertyAccessor().getPropertyValue(
								objectWithCollection, collectionPropertyName));
		if (modificationType == CollectionModificationType.ADD) {
			c.addAll(deltaC);
		} else {
			c.removeAll(deltaC);
		}
		CommonLocator.get().propertyAccessor().setPropertyValue(
				objectWithCollection, collectionPropertyName, c);
	}

	public void serializeDomainObjects(ClientInstance clientInstance)
			throws Exception {
		Map<Class<? extends HasIdAndLocalId>, Map<Long, HasIdAndLocalId>> idMap = domainObjects.idMap;
		Map<Class, List> objCopy = new LinkedHashMap<Class, List>();
		for (Class<? extends HasIdAndLocalId> clazz : idMap.keySet()) {
			ArrayList values = new ArrayList(idMap.get(clazz).values());
			objCopy.put(clazz, values);
		}
		new ClientDteWorker(objCopy, clientInstance).start();
	}

	public <T> Collection<T> getCollection(Class<T> clazz) {
		return getDomainObjects().getCollection(clazz);
	}

	public static abstract class ClientWorker {
		protected int iterationCount = 200;

		protected int targetIterationTimeMs = 200;

		protected int index;

		protected int lastPassIterationsPerformed = 0;

		protected int allocateToNonWorkerFactor = 2;

		private long startTime;

		protected ClientWorker() {
		}

		protected ClientWorker(int iterationCount, int targetIterationTimeMs) {
			this.iterationCount = iterationCount;
			this.targetIterationTimeMs = targetIterationTimeMs;
		}

		public void start() {
			startTime = System.currentTimeMillis();
			iterate();
		}

		protected void iterate() {
			if (isComplete()) {
				ClientLayerLocator.get().clientBase().log(
						CommonUtils.format("Itr [%1] [Complete] - %2 ms",
								CommonUtils.simpleClassName(getClass()), System
										.currentTimeMillis()
										- startTime));
				onComplete();
				return;
			}
			long t0 = System.currentTimeMillis();
			performIteration();
			long t1 = System.currentTimeMillis();
			int timeTaken = (int) (t1 - t0);
			timeTaken = Math.min(timeTaken, targetIterationTimeMs * 10);
			// no totally lost loops if debugging
			if (lastPassIterationsPerformed == iterationCount) {
				if (timeTaken * 2 < targetIterationTimeMs) {
					iterationCount *= 2;
				}
				if (timeTaken > targetIterationTimeMs * 2) {
					iterationCount /= 2;
				}
				iterationCount = Math.max(iterationCount, 10);
			}
			ClientLayerLocator.get().clientBase().log(
					CommonUtils.format("Itr [%1] [x%3] - %2 ms", CommonUtils
							.simpleClassName(getClass()), timeTaken,
							lastPassIterationsPerformed));
			new Timer() {
				@Override
				public void run() {
					iterate();
				}
			}.schedule((int) timeTaken * allocateToNonWorkerFactor + 1);
		}

		protected abstract void onComplete();

		protected abstract boolean isComplete();

		protected abstract void performIteration();
	}

	class ClientDteWorker extends ClientWorker {
		List<DomainTransformEvent> creates = new ArrayList<DomainTransformEvent>();

		List<DomainTransformEvent> mods = new ArrayList<DomainTransformEvent>();

		DomainTransformRequest dtr = new DomainTransformRequest();

		private final Map<Class, List> objCopy;

		private final ClientInstance clientInstance;

		ClientDteWorker(Map<Class, List> objCopy, ClientInstance clientInstance) {
			this.objCopy = objCopy;
			this.clientInstance = clientInstance;
			ClientBase clientBase = ClientLayerLocator.get().clientBase();
		}

		protected void onComplete() {
			dtr.getItems().addAll(creates);
			dtr.getItems().addAll(mods);
			dtr.setClientInstance(clientInstance);
			dtr
					.setDomainTransformRequestType(DomainTransformRequestType.CLIENT_OBJECT_LOAD);
			ClientBase clientBase = ClientLayerLocator.get().clientBase();
			getPersistableTransformListener().persistableTransform(dtr);
		}

		@Override
		protected boolean isComplete() {
			return objCopy.isEmpty();
		}

		@Override
		protected void performIteration() {
			Class clazz = objCopy.keySet().iterator().next();
			List values = objCopy.get(clazz);
			if (values.size() > iterationCount) {
				List v1 = new ArrayList();
				List v2 = new ArrayList();
				for (int i = 0; i < values.size(); i++) {
					Object v = values.get(i);
					if (i < iterationCount) {
						v1.add(v);
					} else {
						v2.add(v);
					}
				}
				values = v1;
				objCopy.put(clazz, v2);
			} else {
				objCopy.remove(clazz);
			}
			lastPassIterationsPerformed = values.size();
			try {
				List<DomainTransformEvent> dtes = objectsToDtes(values, clazz,
						false);
				for (DomainTransformEvent dte : dtes) {
					if (dte.getTransformType() == TransformType.CREATE_OBJECT) {
						creates.add(dte);
					} else {
						mods.add(dte);
					}
				}
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
	}

	/**
	 * some sort of note to self TODO: 3.2 - use case, we've loaded single user
	 * & recursive member groups, now we're merging all users/groups we
	 * essentially want a recursive scan on all properties (ouch) of incoming
	 * objects replacing incoming object refs with local refs if they exist it's
	 * going to be linear...but it's going to be long...but it's gotta be done
	 * remember, if yr about to replace, merge replacement first (cos replaced
	 * will be gone might be a circular ref problem here...aha...something to
	 * think on ...wait a sec...why not just clear all current info if humanly
	 * possible?
	 * 
	 * @param hasIdAndLocalId
	 * @param obj
	 */
	public static class MapObjectLookup implements ObjectLookup {
		private final PropertyChangeListener listener;

		private Map<Class<? extends HasIdAndLocalId>, Map<Long, HasIdAndLocalId>> idMap;

		private Map<Class<? extends HasIdAndLocalId>, Map<Long, HasIdAndLocalId>> localIdMap;

		private Map<Class<? extends HasIdAndLocalId>, Set<HasIdAndLocalId>> collnMap;

		private Map<Class, Boolean> registerChildren = new HashMap<Class, Boolean>();

		public MapObjectLookup(PropertyChangeListener listener,
				List topLevelObjects) {
			this.listener = listener;
			this.idMap = new HashMap<Class<? extends HasIdAndLocalId>, Map<Long, HasIdAndLocalId>>();
			this.localIdMap = new HashMap<Class<? extends HasIdAndLocalId>, Map<Long, HasIdAndLocalId>>();
			this.collnMap = new HashMap<Class<? extends HasIdAndLocalId>, Set<HasIdAndLocalId>>();
			registerObjects(topLevelObjects);
		}

		public void changeMapping(HasIdAndLocalId obj, long id, long localId) {
			Class<? extends HasIdAndLocalId> clazz = obj.getClass();
			ensureCollections(clazz);
			idMap.get(clazz).remove(id);
			localIdMap.get(clazz).remove(localId);
			mapObject(obj);
		}

		public void deregisterObject(HasIdAndLocalId hili) {
			Class<? extends HasIdAndLocalId> clazz = hili.getClass();
			ensureCollections(clazz);
			idMap.get(clazz).remove(hili.getId());
			localIdMap.get(clazz).remove(hili.getLocalId());
			collnMap.get(clazz).remove(hili);
			if (hili instanceof SourcesPropertyChangeEvents) {
				SourcesPropertyChangeEvents sb = (SourcesPropertyChangeEvents) hili;
				sb.removePropertyChangeListener(listener);
			}
		}

		public void deregisterObjects(Collection<HasIdAndLocalId> objects) {
			if (objects == null) {
				return;
			}
			for (HasIdAndLocalId hili : objects) {
				if (hili == null) {
					continue;
				}
				deregisterObject(hili);
			}
		}

		<T> Collection<T> getCollection(Class<T> clazz) {
			ensureCollections(clazz);
			return (Collection<T>) collnMap.get(clazz);
		}

		public Map<Class<? extends HasIdAndLocalId>, Set<HasIdAndLocalId>> getCollnMap() {
			return this.collnMap;
		}

		public <T extends HasIdAndLocalId> T getObject(Class<? extends T> c,
				long id, long localId) {
			if (idMap.get(c) == null) {
				return null;
			}
			if (id != 0) {
				return (T) idMap.get(c).get(id);
			} else {
				return (T) localIdMap.get(c).get(localId);
			}
		}

		public HasIdAndLocalId getObject(HasIdAndLocalId bean) {
			return (HasIdAndLocalId) getObject(bean.getClass(), bean.getId(),
					bean.getLocalId());
		}

		public void registerObjects(Collection objects) {
			for (Object o : objects) {
				if (o == null) {
					continue;
				}
				if (o instanceof Collection) {
					flattenCollection((Collection) o);
				} else {
					mapObject((HasIdAndLocalId) o);
				}
			}
		}

		private void removeListenerFromMap(
				Map<Class<? extends HasIdAndLocalId>, Map<Long, HasIdAndLocalId>> map) {
			for (Map m : map.values()) {
				for (Object o : m.values()) {
					if (o instanceof SourcesPropertyChangeEvents) {
						SourcesPropertyChangeEvents sb = (SourcesPropertyChangeEvents) o;
						sb.removePropertyChangeListener(listener);
					}
				}
			}
		}

		protected void flattenCollection(Collection c) {
			if (c == null) {
				return;
			}
			Collection<HasIdAndLocalId> ch = c;
			for (HasIdAndLocalId obj : ch) {
				mapObject(obj);
			}
		}

		protected void mapObject(HasIdAndLocalId obj) {
			if (obj == null) {
				int k = 3;
			}
			if (obj.getId() == 0 && obj.getLocalId() == 0) {
				return;
			}
			Class<? extends HasIdAndLocalId> clazz = obj.getClass();
			ensureCollections(clazz);
			collnMap.get(clazz).remove(obj);
			collnMap.get(clazz).add(obj);
			if (obj.getId() != 0) {
				Map<Long, HasIdAndLocalId> clMap = idMap.get(clazz);
				// if (!clMap.containsKey(obj.getId())) {
				clMap.put(obj.getId(), obj);
				// } else {
				// merge(clMap.get(obj.getId()), obj);
				// }
			}
			if (obj.getLocalId() != 0) {
				Map<Long, HasIdAndLocalId> clMap = localIdMap.get(clazz);
				// if (!clMap.containsKey(obj.getLocalId())) {
				clMap.put(obj.getLocalId(), obj);
				// } else {
				// merge(clMap.get(obj.getLocalId()), obj);
				// }
			}
			if (obj instanceof SourcesPropertyChangeEvents) {
				SourcesPropertyChangeEvents sb = (SourcesPropertyChangeEvents) obj;
				sb.removePropertyChangeListener(listener);
				sb.addPropertyChangeListener(listener);
				// try {
				// sb.removePropertyChangeListener(listener);
				// sb.addPropertyChangeListener(listener);
				// } catch (Exception e) {
				// // for testing, deserialized objects may not have listeners
				// // nasty-hack
				// }
			}
			boolean lookupCreated = registerChildren.containsKey(clazz);
			if (ClientReflector.isDefined()
					&& (!registerChildren.containsKey(clazz) || registerChildren
							.get(clazz))) {
				boolean shouldMapChildren = lookupCreated;
				ClientBeanReflector bi = ClientReflector.get()
						.beanInfoForClass(clazz);
				Collection<ClientPropertyReflector> prs = bi == null ? new ArrayList<ClientPropertyReflector>()
						: bi.getPropertyReflectors().values();
				for (ClientPropertyReflector pr : prs) {
					DomainPropertyInfo dpi = pr
							.getAnnotation(DomainPropertyInfo.class);
					if (dpi != null && dpi.registerChildren()) {
						shouldMapChildren = true;
						Collection<HasIdAndLocalId> colln = (Collection<HasIdAndLocalId>) CommonUtils
								.wrapInCollection(CommonLocator.get()
										.propertyAccessor().getPropertyValue(
												obj, pr.getPropertyName()));
						if (colln != null) {
							for (HasIdAndLocalId hili : colln) {
								mapObject(hili);
							}
						}
					}
				}
				if (!lookupCreated) {
					registerChildren.put(clazz, shouldMapChildren);
				}
			}
		}

		protected void removeListeners() {
			removeListenerFromMap(idMap);
			removeListenerFromMap(localIdMap);
		}

		void ensureCollections(Class c) {
			if (!idMap.containsKey(c)) {
				idMap.put(c, new LinkedHashMap<Long, HasIdAndLocalId>());
				localIdMap.put(c, new LinkedHashMap<Long, HasIdAndLocalId>());
				collnMap.put(c, new LinkedHashSet<HasIdAndLocalId>());
			}
		}
	}

	static class ContainerUpdateTransformHandler implements
			DomainTransformListener {
		public void domainTransform(DomainTransformEvent evt) {
			if (evt.getCommitType() == CommitType.TO_LOCAL_GRAPH) {
				TransformManager tm = TransformManager.get();
				switch (evt.getTransformType()) {
				case CREATE_OBJECT:
				case DELETE_OBJECT:
				case ADD_REF_TO_COLLECTION:
				case REMOVE_REF_FROM_COLLECTION:
					tm.consume(evt);
					break;
				}
				tm.setTransformCommitType(evt, CommitType.TO_LOCAL_STORAGE);
			}
		}
	}

	static class EditCommitTransformHandler implements DomainTransformListener {
		public void domainTransform(DomainTransformEvent evt) {
			if (evt.getCommitType() == CommitType.TO_LOCAL_BEAN) {
				TransformManager tm = TransformManager.get();
				if (tm.getProvisionalObjects().contains(evt.getSource())) {
					return;
				}
				evt.setUtcDate(CommonLocator.get().currentUtcDateProvider()
						.currentUtcDate());
				evt.setEventId(tm.nextEventIdCounter());
				tm.setTransformCommitType(evt, CommitType.TO_LOCAL_GRAPH);
				return;
			}
		}
	}

	public class TransformManagerCache {
		private LookupMapToMap lkp;

		public TransformManagerCache() {
			clearUserObjects();
		}

		public void update(HasIdAndLocalId hili, String propertyName,
				final AsyncCallback callback, final boolean fireTransforms) {
			if (hili.getId() == 0) {
				callback.onSuccess(null);
				return;
			}
			final Object[] spec = { hili.getClass(), hili.getId(), propertyName };
			if (lkp.containsKey(spec)) {
				callback.onSuccess(null);
				return;
			}
			Collection value = (Collection) CommonLocator.get()
					.propertyAccessor().getPropertyValue(hili, propertyName);
			if (value != null && !value.isEmpty()) {
				callback.onSuccess(null);
				return;
			}
			final CancellableRemoteDialog crd = new NonCancellableRemoteDialog(
					"Loading", null);
			crd.show();
			final long t1 = System.currentTimeMillis();
			AsyncCallback<List<ObjectCacheItemResult>> innerCallback = new AsyncCallback<List<ObjectCacheItemResult>>() {
				public void onSuccess(List<ObjectCacheItemResult> result) {
					long t2 = System.currentTimeMillis();
					ClientLayerLocator.get().clientBase().log(
							"Cache load/deser.: " + (t2 - t1));
					cleanup();
					MutablePropertyChangeSupport.setMuteAll(true);
					for (ObjectCacheItemResult item : result) {
						// ObjectRef ref = item.getItemSpec().getObjectRef();
						// HasIdAndLocalId hili = getObject(ref.getClassRef()
						// .getRefClass(), ref.getId(), ref.getLocalId());
						// PlatformLocator.get().propertyAccessor()
						// .setPropertyValue(hili,
						// item.getItemSpec().getPropertyName(),
						// item.getResult());
						// TransformManager.this.getDomainObjects()
						// .registerObjects(item.getResult());
						replayRemoteEvents(item.getTransforms(), fireTransforms);
						PersistableTransformListener pl = getPersistableTransformListener();
						if (pl != null) {
							DomainTransformRequest dtr = new DomainTransformRequest();
							dtr.setItems(item.getTransforms());
							dtr.setClientInstance(ClientLayerLocator.get()
									.clientBase().getClientInstance());
							dtr
									.setDomainTransformRequestType(DomainTransformRequestType.CLIENT_SYNC);
							pl.persistableTransform(dtr);
						}
					}
					MutablePropertyChangeSupport.setMuteAll(false);
					Object[] spec2 = new Object[4];
					System.arraycopy(spec, 0, spec2, 0, 3);
					spec2[3] = true;
					lkp.put(spec2);
					ClientLayerLocator.get().clientBase().log(
							"Cache dte replay: "
									+ (System.currentTimeMillis() - t2));
					callback.onSuccess(result);
				}

				public void onFailure(Throwable caught) {
					cleanup();
					ClientLayerLocator.get().clientBase().onUncaughtException(
							caught);
				}

				private void cleanup() {
					crd.hide();
				}
			};
			List<ObjectCacheItemSpec> specs = new ArrayList<ObjectCacheItemSpec>();
			specs.add(new ObjectCacheItemSpec(hili, propertyName));
			crd.show();
			ClientLayerLocator.get().commonRemoteServiceAsync().cache(specs,
					innerCallback);
		}

		public void clearUserObjects() {
			lkp = new LookupMapToMap(3);
		}
	}

	public static String stringId(HasIdAndLocalId hili) {
		return hili.getId() != 0 ? hili.getId() + "" : hili.getLocalId() + "L";
	}

	public void setPersistableTransformListener(
			PersistableTransformListener persistableTransformListener) {
		this.persistableTransformListener = persistableTransformListener;
	}

	public PersistableTransformListener getPersistableTransformListener() {
		return persistableTransformListener;
	}

	protected boolean ignorePropertyForCaching(Class objectType,
			Class propertyType, String propertyName) {
		return ignorePropertiesForCaching.contains(propertyName)
				|| propertyType == Class.class
				|| !PermissionsManager.get().checkReadable(objectType,
						propertyName, null);
	}

	public List<DomainTransformEvent> objectsToDtes(List objects, Class clazz,
			boolean asObjectSpec) throws Exception {
		ClassLookup classLookup = CommonLocator.get().classLookup();
		List<PropertyInfoLite> pds = classLookup.getWritableProperties(clazz);
		Object templateInstance = classLookup.getTemplateInstance(clazz);
		PropertyAccessor accessor = CommonLocator.get().propertyAccessor();
		Map<String, Object> defaultValues = new HashMap();
		Set<Class> implementsHili = new HashSet<Class>();
		for (Iterator<PropertyInfoLite> itr = pds.iterator(); itr.hasNext();) {
			PropertyInfoLite info = itr.next();
			String propertyName = info.getPropertyName();
			if (ignorePropertyForCaching(clazz, info.getPropertyType(),
					propertyName)) {
				itr.remove();
			} else {
				Object defaultValue = accessor.getPropertyValue(
						templateInstance, propertyName);
				defaultValues.put(propertyName, defaultValue);
				try {
					Object template = classLookup.getTemplateInstance(info
							.getPropertyType());
					if (template instanceof HasIdAndLocalId) {
						implementsHili.add(info.getPropertyType());
					}
				} catch (Exception e) {
					// probably primitive - ignore
				}
			}
		}
		List<DomainTransformEvent> dtes = new ArrayList<DomainTransformEvent>();
		if (CommonUtils.simpleClassName(clazz).contains("SerializationHelper")) {
			int j = 2;
		}
		for (Object o : objects) {
			Object[] arr = asObjectSpec ? (Object[]) o : null;
			HasIdAndLocalId hili = asObjectSpec ? null : (HasIdAndLocalId) o;
			DomainTransformEvent dte = new DomainTransformEvent();
			dte.setSource(null);
			Long id = asObjectSpec ? (Long) arr[0] : hili.getId();
			dte.setObjectId(id);
			dte.setObjectClass(clazz);
			dte.setTransformType(TransformType.CREATE_OBJECT);
			dtes.add(dte);
			int i = 1;
			for (PropertyInfoLite pd : pds) {
				String propertyName = pd.getPropertyName();
				Class propertyType = pd.getPropertyType();
				Object defaultValue = defaultValues.get(propertyName);
				Object value = asObjectSpec ? arr[i++] : CommonLocator.get()
						.propertyAccessor().getPropertyValue(o, propertyName);
				if (value == null && defaultValue == null) {
					continue;
				}
				if (value != null && defaultValue != null) {
					if (value.equals(defaultValue)) {
						continue;
					}
				}
				if (value instanceof Collection) {
					if (pd.isSerializableCollection()) {
						Iterator itr = ((Set) value).iterator();
						for (; itr.hasNext();) {
							Object o2 = itr.next();
							dte = new DomainTransformEvent();
							dte.setObjectId(id);
							dte.setObjectClass(clazz);
							dte.setPropertyName(propertyName);
							dte
									.setTransformType(TransformType.ADD_REF_TO_COLLECTION);
							if (o2 instanceof HasIdAndLocalId) {
								HasIdAndLocalId h2 = (HasIdAndLocalId) o2;
								dte.setNewValue(null);
								dte.setValueId(h2.getId());
								dte.setValueLocalId(h2.getLocalId());
								dte.setValueClass(h2.getClass());
							} else if (o2 instanceof Enum) {
								Enum e = (Enum) o2;
								dte.setNewValue(null);
								dte.setNewStringValue(e.name());
								dte.setValueClass(e.getClass());
							}
							dtes.add(dte);
						}
					}
				} else {
					dte = new DomainTransformEvent();
					dte.setObjectId(id);
					dte.setObjectClass(clazz);
					if (value instanceof Timestamp) {
						value = new Date(((Timestamp) value).getTime());
					}
					dte.setNewValue(value);
					dte.setPropertyName(propertyName);
					if (!implementsHili.contains(propertyType)) {
						convertToTargetObject(dte);
						dte
								.setTransformType(TransformType.CHANGE_PROPERTY_SIMPLE_VALUE);
					} else {
						dte.setValueClass(propertyType);
						dte.setValueId(asObjectSpec ? (Long) value
								: ((HasIdAndLocalId) value).getId());
						dte.setTransformType(TransformType.CHANGE_PROPERTY_REF);
					}
					dtes.add(dte);
				}
			}
		}
		return dtes;
	}

	protected void collectionChanged(Object obj, Object tgt) {
		// changes won't be noticed unless we do this
		if (obj instanceof GwtPersistableObject) {
			((GwtPersistableObject) obj)
					.forceFirePropertyChange(UNSPECIFIC_PROPERTY_CHANGE);
		}
	}

	public interface PersistableTransformListener {
		public void persistableTransform(DomainTransformRequest dtr);
	}

	/**
	 * Useful series of actions when persisting a HasIdAndLocalId with
	 * references to a WrappedObject
	 * 
	 * @param referrer
	 */
	public void persistWrappedObjectReferrer(final HasIdAndLocalId referrer,
			boolean onlyLocalGraph) {
		final ClientBeanReflector beanReflector = ClientReflector.get()
				.beanInfoForClass(referrer.getClass());
		beanReflector.iterateForPropertyWithAnnotation(WrapperInfo.class,
				new HasAnnotationCallback<WrapperInfo>() {
					public void callback(WrapperInfo annotation,
							ClientPropertyReflector propertyReflector) {
						GwtPersistableObject obj = (GwtPersistableObject) propertyReflector
								.getPropertyValue(referrer);
						CommonLocator.get().propertyAccessor()
								.setPropertyValue(referrer,
										annotation.toStringPropertyName(),
										obj.toString());
					}
				});
		HasIdAndLocalId target = referrer;
		if (getProvisionalObjects().contains(referrer)) {
			try {
				CollectionModificationSupport.queue(true);
				final HasIdAndLocalId promoted = promoteToDomainObject(referrer);
				target = promoted;
				// copy, because at the moment wrapped refs don't get handled by
				// the TM
				HasAnnotationCallback<WrapperInfo> callback = new HasAnnotationCallback<WrapperInfo>() {
					public void callback(WrapperInfo annotation,
							ClientPropertyReflector propertyReflector) {
						propertyReflector.setPropertyValue(promoted,
								propertyReflector.getPropertyValue(referrer));
					}
				};
				beanReflector.iterateForPropertyWithAnnotation(
						WrapperInfo.class, callback);
			} finally {
				CollectionModificationSupport.queue(false);
			}
		}
		final HasIdAndLocalId finalTarget = target;
		HasAnnotationCallback<WrapperInfo> callback = new HasAnnotationCallback<WrapperInfo>() {
			public void callback(final WrapperInfo annotation,
					final ClientPropertyReflector propertyReflector) {
				GwtPersistableObject persistableObject = (GwtPersistableObject) propertyReflector
						.getPropertyValue(finalTarget);
				AsyncCallback<Long> savedCallback = new AsyncCallback<Long>() {
					public void onFailure(Throwable caught) {
						throw new WrappedRuntimeException(caught);
					}

					public void onSuccess(Long result) {
						CommonLocator.get().propertyAccessor()
								.setPropertyValue(finalTarget,
										annotation.idPropertyName(), result);
					}
				};
				ClientLayerLocator.get().commonRemoteServiceAsync().persist(
						persistableObject, savedCallback);
			}
		};
		if (!onlyLocalGraph) {
			beanReflector.iterateForPropertyWithAnnotation(WrapperInfo.class,
					callback);
		}
	}
}
