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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cc.alcina.framework.common.client.CommonLocator;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.WrappedRuntimeException.SuggestedAction;
import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.collections.DefaultCollectionFilter;
import cc.alcina.framework.common.client.entity.WrapperPersistable;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.CollectionModification.CollectionModificationEvent;
import cc.alcina.framework.common.client.logic.domaintransform.CollectionModification.CollectionModificationListener;
import cc.alcina.framework.common.client.logic.domaintransform.CollectionModification.CollectionModificationSource;
import cc.alcina.framework.common.client.logic.domaintransform.CollectionModification.CollectionModificationSupport;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException.DomainTransformExceptionType;
import cc.alcina.framework.common.client.logic.domaintransform.spi.ClassLookup;
import cc.alcina.framework.common.client.logic.domaintransform.spi.ClassLookup.PropertyInfoLite;
import cc.alcina.framework.common.client.logic.domaintransform.spi.ObjectLookup;
import cc.alcina.framework.common.client.logic.domaintransform.spi.PropertyAccessor;
import cc.alcina.framework.common.client.logic.domaintransform.undo.NullUndoManager;
import cc.alcina.framework.common.client.logic.domaintransform.undo.TransformHistoryManager;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.logic.reflection.Association;
import cc.alcina.framework.common.client.logic.reflection.ClientBeanReflector;
import cc.alcina.framework.common.client.logic.reflection.ClientPropertyReflector;
import cc.alcina.framework.common.client.logic.reflection.ClientReflector;
import cc.alcina.framework.common.client.logic.reflection.SyntheticGetter;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.SimpleStringParser;

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
public abstract class TransformManager implements PropertyChangeListener,
		ObjectLookup, CollectionModificationSource {
	private static final String UNSPECIFIC_PROPERTY_CHANGE = "---";

	public static final String ID_FIELD_NAME = "id";

	public static final String LOCAL_ID_FIELD_NAME = "localId";

	public static final String VERSION_FIELD_NAME = "versionNumber";

	protected static final Set<String> ignorePropertiesForCaching = new HashSet<String>(
			Arrays.asList(new String[] { "class", "id", "localId",
					"propertyChangeListeners" }));

	public static String stringId(HasIdAndLocalId hili) {
		return hili.getId() != 0 ? hili.getId() + "" : hili.getLocalId() + "L";
	}

	private MapObjectLookup domainObjects;

	private static long eventIdCounter = 1;

	protected static long localIdCounter = 1;

	final Set<DomainTransformEvent> transforms = new LinkedHashSet<DomainTransformEvent>();

	final Map<CommitType, LinkedHashSet<DomainTransformEvent>> transformsByType = new HashMap<CommitType, LinkedHashSet<DomainTransformEvent>>();

	protected DomainTransformSupport transformListenerSupport;

	private static TransformManager theInstance;

	public static TransformManager get() {
		if (theInstance == null) {
			// well, throw an exception.
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

	protected Collection provisionalObjects = new LinkedHashSet();

	private boolean ignorePropertyChanges;

	private boolean replayingRemoteEvent;

	protected CollectionModificationSupport collectionModificationSupport;

	protected DomainTransformEvent currentEvent;

	protected TransformManager() {
		this.transformListenerSupport = new DomainTransformSupport();
		this.collectionModificationSupport = new CollectionModificationSupport();
	}

	private TransformHistoryManager undoManager = new NullUndoManager();

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

	/**
	 * Order must be: local (>bean), containerupdater, localstorage,
	 * remotestorage By default, transform manager handles the first two itself
	 */
	public void addDomainTransformListener(DomainTransformListener listener) {
		this.transformListenerSupport.addDomainTransformListener(listener);
	}

	public void addTransform(DomainTransformEvent evt) {
		transforms.add(evt);
		getTransformsByCommitType(evt.getCommitType()).add(evt);
	}

	public void appShutdown() {
		theInstance = null;
	}

	public void clearUserObjects() {
		setDomainObjects(null);
	}

	public void commitProvisionalObjects(Collection c) {
		promoteToDomain(c, false);
	}

	public void consume(DomainTransformEvent event)
			throws DomainTransformException {
		currentEvent = event;
		HasIdAndLocalId obj = null;
		if (event.getTransformType() != TransformType.CREATE_OBJECT) {
			obj = getObject(event);
			if (obj == null) {
				throw new DomainTransformException(event,
						DomainTransformExceptionType.SOURCE_ENTITY_NOT_FOUND);
			}
		}
		Object tgt = (event.getTransformType() == TransformType.NULL_PROPERTY_REF) ? null
				: getTargetObject(event, false);
		if (!checkPermissions(obj, event, event.getPropertyName(), tgt)) {
			return;
		}
		getUndoManager().prepareUndo(event);
		switch (event.getTransformType()) {
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
			CommonLocator.get().propertyAccessor()
					.setPropertyValue(obj, event.getPropertyName(), tgt);
			String pn = event.getPropertyName();
			if (pn.equals(TransformManager.ID_FIELD_NAME)
					|| pn.equals(TransformManager.LOCAL_ID_FIELD_NAME)) {
				getDomainObjects().changeMapping(obj, event.getObjectId(),
						event.getObjectLocalId());
			}
			if (event.getCommitType() == CommitType.TO_LOCAL_BEAN) {
				removeTransform(event);
			}
			objectModified(obj, event, false);
			switch (event.getTransformType()) {
			case NULL_PROPERTY_REF:
			case CHANGE_PROPERTY_REF:
				if (event.getOldValue() != null
						&& event.getOldValue() instanceof HasIdAndLocalId) {
					updateAssociation(event, obj,
							(HasIdAndLocalId) event.getOldValue(), true, true);
				}
				updateAssociation(event, obj, tgt, false, true);
				break;
			}
			break;
		// add and removeref will not cause a property change, so no transform
		// removal
		case ADD_REF_TO_COLLECTION:
			((Set) CommonLocator.get().propertyAccessor()
					.getPropertyValue(obj, event.getPropertyName())).add(tgt);
			objectModified(obj, event, false);
			updateAssociation(event, obj, tgt, false, true);
			collectionChanged(obj, tgt);
			break;
		case REMOVE_REF_FROM_COLLECTION:
			((Set) CommonLocator.get().propertyAccessor()
					.getPropertyValue(obj, event.getPropertyName()))
					.remove(tgt);
			updateAssociation(event, obj, tgt, true, true);
			collectionChanged(obj, tgt);
			break;
		case DELETE_OBJECT:
			deregisterObject((HasIdAndLocalId) obj);
			break;
		case CREATE_OBJECT:
			if (event.getObjectId() != 0) {
				// three possibilities:
				// 1. replaying a server create,(on the client)
				// 2. recording an in-entity-manager create
				// 3. doing a database-regeneration
				// if (2), break at this point
				if (getObject(event) != null) {
					break;
				}
			}
			HasIdAndLocalId hili = (HasIdAndLocalId) CommonLocator
					.get()
					.classLookup()
					.newInstance(event.getObjectClass(), event.getObjectId(),
							event.getObjectLocalId());
			hili.setLocalId(event.getObjectLocalId());
			if (hili.getId() == 0) {// replay from server -
				hili.setId(event.getObjectId());
			}
			event.setObjectId(hili.getId());
			if (event.getCommitType() == CommitType.TO_STORAGE) {
				objectModified(hili, event, false);
			}
			if (getDomainObjects() != null) {
				getDomainObjects().mapObject(hili);
				fireCollectionModificationEvent(new CollectionModificationEvent(
						this, hili.getClass(), getDomainObjects()
								.getCollection(hili.getClass())));
			}
			break;
		default:
			assert false : "Transform type not implemented: "
					+ event.getTransformType();
		}
		currentEvent = null;
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
			evt.setNewStringValue(SimpleStringParser.toString((Long) value));
		} else if (value.getClass() == Date.class) {
			evt.setNewStringValue(SimpleStringParser.toString((((Date) value)
					.getTime())));
		} else if (value instanceof HasIdAndLocalId) {
			HasIdAndLocalId hili = (HasIdAndLocalId) value;
			evt.setValueId(hili.getId());
			evt.setValueLocalId(hili.getLocalId());
		}
	}

	public <T extends HasIdAndLocalId> T createDomainObject(Class<T> objectClass) {
		long localId = nextLocalIdCounter();
		T newInstance = CommonLocator.get().classLookup()
				.newInstance(objectClass, 0, localId);
		newInstance.setLocalId(localId);
		// a bit roundabout, but to ensure compatibility with the event system
		// essentially registers a synthesised object, then replaces it in the
		// mapping with the real one
		fireCreateObjectEvent(objectClass, 0, localId);
		registerDomainObject(newInstance);
		return newInstance;
	}

	public <T extends HasIdAndLocalId> T createProvisionalObject(
			Class<T> objectClass) {
		long localId = nextLocalIdCounter();
		T newInstance = CommonLocator.get().classLookup()
				.newInstance(objectClass, 0, localId);
		newInstance.setLocalId(localId);
		registerProvisionalObject(newInstance);
		return newInstance;
	}

	public boolean currentTransformIsDuringCreationRequest() {
		return currentEvent.getObjectLocalId() != 0;
	}

	public DomainTransformEvent deleteObject(HasIdAndLocalId hili) {
		return deleteObject(hili, false);
	}

	/**
	 * If calling from the servlet layer, the object will normally not be
	 * 'found' - so this function variant should be called, with the second
	 * parameter equal to true
	 */
	public DomainTransformEvent deleteObject(HasIdAndLocalId hili,
			boolean generateEventIfObjectNotFound) {
		if (!generateEventIfObjectNotFound && getObject(hili) == null) {
			return null;
		}
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

	public void deregisterObject(HasIdAndLocalId hili) {
		if (getDomainObjects() != null) {
			removeAssociations(hili);
			getDomainObjects().deregisterObject(hili);
			fireCollectionModificationEvent(new CollectionModificationEvent(
					this, hili.getClass(), getDomainObjects().getCollection(
							hili.getClass())));
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

	public boolean dirty(Collection provisionalObjects) {
		Collection<DomainTransformEvent> trs = getTransformsByCommitType(CommitType.TO_LOCAL_BEAN);
		for (DomainTransformEvent dte : trs) {
			if (provisionalObjects.contains(dte.getSource())) {
				return true;
			}
		}
		return false;
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

	public <T> Collection<T> getCollection(Class<T> clazz) {
		return getDomainObjects().getCollection(clazz);
	}

	public MapObjectLookup getDomainObjects() {
		return this.domainObjects;
	}

	public <T extends HasIdAndLocalId> T getObject(Class<? extends T> c,
			long id, long localId) {
		if (this.getDomainObjects() != null) {
			return getDomainObjects().getObject(c, id, localId);
		}
		return null;
	}

	public HasIdAndLocalId getObject(DomainTransformEvent dte) {
		HasIdAndLocalId obj = CommonLocator
				.get()
				.objectLookup()
				.getObject(dte.getObjectClass(), dte.getObjectId(),
						dte.getObjectLocalId());
		dte.setSource(obj);
		return obj;
	}

	public <T extends HasIdAndLocalId> T getObject(T hili) {
		return (T) CommonLocator.get().objectLookup()
				.getObject(hili.getClass(), hili.getId(), hili.getLocalId());
	}

	public Collection getProvisionalObjects() {
		return provisionalObjects;
	}

	public TransformManager getT() {
		return null;
	}

	public Object getTargetObject(DomainTransformEvent evt, boolean oldValue)
			throws DomainTransformException {
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
			HasIdAndLocalId object = CommonLocator
					.get()
					.objectLookup()
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
		throw new DomainTransformException(evt,
				DomainTransformExceptionType.TARGET_ENTITY_NOT_FOUND);
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

	public boolean hasUnsavedChanges(Object object) {
		Collection objects = CommonUtils.wrapInCollection(object);
		Collection<DomainTransformEvent> trs = getTransformsByCommitType(CommitType.TO_LOCAL_BEAN);
		for (DomainTransformEvent dte : trs) {
			if (objects.contains(dte.getSource())) {
				return true;
			}
		}
		return false;
	}

	public boolean isIgnorePropertyChanges() {
		return this.ignorePropertyChanges;
	}

	public boolean isReplayingRemoteEvent() {
		return this.replayingRemoteEvent;
	}

	public void modifyCollectionProperty(Object objectWithCollection,
			String collectionPropertyName, Object delta,
			CollectionModificationType modificationType) {
		Collection deltaC = CommonUtils.wrapInCollection(delta);
		Collection c = CommonUtils
				.shallowCollectionClone((Collection) CommonLocator
						.get()
						.propertyAccessor()
						.getPropertyValue(objectWithCollection,
								collectionPropertyName));
		if (modificationType == CollectionModificationType.ADD) {
			c.addAll(deltaC);
		} else {
			c.removeAll(deltaC);
		}
		CommonLocator
				.get()
				.propertyAccessor()
				.setPropertyValue(objectWithCollection, collectionPropertyName,
						c);
	}

	public synchronized long nextEventIdCounter() {
		return eventIdCounter++;
	}

	public synchronized long nextLocalIdCounter() {
		return localIdCounter++;
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
							dte.setTransformType(TransformType.ADD_REF_TO_COLLECTION);
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
						dte.setTransformType(TransformType.CHANGE_PROPERTY_SIMPLE_VALUE);
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
						dte.setTransformType(TransformType.ADD_REF_TO_COLLECTION);
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
						dte.setTransformType(TransformType.REMOVE_REF_FROM_COLLECTION);
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
						dte.setTransformType(TransformType.ADD_REF_TO_COLLECTION);
						transforms.add(dte);
					}
				}
				for (Enum e : oldValues) {
					if (!newValues.contains(e)) {
						dte = createTransformFromPropertyChange(evt);
						dte.setNewValue(null);
						dte.setNewStringValue(e.name());
						dte.setValueClass(e.getClass());
						dte.setTransformType(TransformType.REMOVE_REF_FROM_COLLECTION);
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
		if (this.getDomainObjects() != null) {
			if (!provisionalObjects.contains(hili)) {
				fireCollectionModificationEvent(new CollectionModificationEvent(
						this, hili.getClass(), getDomainObjects()
								.getCollection(hili.getClass()), true));
			}
		}
	}

	public void registerDomainObject(HasIdAndLocalId hili) {
		if (getDomainObjects() != null) {
			if (hili.getId() == 0) {
				HasIdAndLocalId createdObject = getDomainObjects().getObject(
						hili);
				getDomainObjects().deregisterObject(createdObject);
			}
			getDomainObjects().mapObject(hili);
		}
	}
	public void registerDomainObjects(Collection<HasIdAndLocalId> hilis) {
		for (HasIdAndLocalId hili : hilis) {
			registerDomainObject(hili);
		}
	}

	/**
	 * Useful for unit tests, hence here rather than clientTM
	 */
	public void registerDomainObjectsInHolder(DomainModelHolder h) {
		if (this.getDomainObjects() != null) {
			getDomainObjects().removeListeners();
		}
		this.setDomainObjects(new MapObjectLookup(this, h
				.registerableDomainObjects()));
		ClassRef.add(h.getClassRefs());
	}

	public <V extends HasIdAndLocalId> Set<V> registeredObjectsAsSet(
			Class<V> clazz) {
		return new LinkedHashSet<V>(getDomainObjects().getCollection(clazz));
	}

	public <V extends HasIdAndLocalId> V registeredSingleton(Class<V> clazz) {
		Collection<V> c = getDomainObjects().getCollection(clazz);
		return c.isEmpty() ? null : c.iterator().next();
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

	// currently client-only
	public void replayRemoteEvents(Collection<DomainTransformEvent> evts,
			boolean fireTransforms) {
		throw new UnsupportedOperationException();
	}

	public boolean resolveMissingObject(DomainTransformEvent evt) {
		return isReplayingRemoteEvent();
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

	/**
	 * If we're running a TM directly in the entity layer (to both commit to the
	 * db and pass the transforms back to a client), the TLTM will want these
	 * listeners...
	 */
	public void setupClientListeners() {
		addDomainTransformListener(new RecordTransformListener());
		addDomainTransformListener(new CommitToLocalDomainTransformListener());
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
			if (bi.getAnnotation(SyntheticGetter.class) != null) {
				continue;
			}
			DomainTransformEvent dte = new DomainTransformEvent();
			dte.setPropertyName(pr.getPropertyName());
			if (!CommonUtils.isStandardJavaClass(pr.getPropertyType())) {
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
	 * that's handled by (what's) the presented UI note - problems are mostly
	 * thrown as exceptions, exception being
	 * DomainPropertyInfo.silentFailOnIllegalWrites
	 * 
	 * @param propertyName
	 * @param tgt
	 * @return true if OK
	 */
	protected boolean checkPermissions(HasIdAndLocalId eventTarget,
			DomainTransformEvent evt, String propertyName, Object change) {
		return true;
	}

	protected void clearTransforms() {
		getTransforms().clear();
		for (CommitType ct : transformsByType.keySet()) {
			transformsByType.get(ct).clear();
		}
	}

	protected void collectionChanged(Object obj, Object tgt) {
		// changes won't be noticed unless we do this
		if (obj instanceof WrapperPersistable) {
			((WrapperPersistable) obj)
					.fireNullPropertyChange(UNSPECIFIC_PROPERTY_CHANGE);
		}
	}

	protected Enum getTargetEnumValue(DomainTransformEvent evt) {
		if (evt.getNewValue() instanceof Enum) {
			return (Enum) evt.getNewValue();
		}
		if (evt.getValueClass().isEnum()) {
			try {
				return Enum.valueOf(evt.getValueClass(),
						evt.getNewStringValue());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return null;
	}

	protected boolean ignorePropertyForCaching(Class objectType,
			Class propertyType, String propertyName) {
		return ignorePropertiesForCaching.contains(propertyName)
				|| propertyType == Class.class
				|| !PermissionsManager.get().checkReadable(objectType,
						propertyName, null);
	}

	/**
	 * for subclasses to handle version increments
	 * 
	 * @param hili
	 * @param evt
	 */
	protected void objectModified(HasIdAndLocalId hili,
			DomainTransformEvent evt, boolean targetObject) {
	}

	protected void promoteToDomain(Collection objects, boolean deregister) {
		try {
			CollectionModificationSupport.queue(true);
			for (Object o : objects) {
				if (o instanceof HasIdAndLocalId
						&& CommonLocator.get().objectLookup()
								.getObject((HasIdAndLocalId) o) == null) {
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
					fireCreateObjectEvent(hili.getClass(), 0, hili.getLocalId());
				}
			}
			Collection<DomainTransformEvent> trs = getTransformsByCommitType(CommitType.TO_LOCAL_BEAN);
			trs = (Set) ((LinkedHashSet) trs).clone();
			deregisterProvisionalObjects(objects);
			for (DomainTransformEvent dte : trs) {
				if (objects.contains(dte.getSource())) {
					if (dte.getTransformType() == TransformType.ADD_REF_TO_COLLECTION
							|| dte.getTransformType() == TransformType.REMOVE_REF_FROM_COLLECTION) {
						try {
							// ?why not consume?...aha,yup, not committed both
							// ways
							// yet.
							// ...which means what? ahh...that we want to
							// publish the (above) transforms
							// wheras the others will be generated by consume()
							// against the non-provisonal
							// counterpart object
							//
							fireDomainTransform(dte);
						} catch (Exception e) {
							throw new WrappedRuntimeException(e);
						}
					} else {
						try {
							consume(dte);
						} catch (Exception e) {
							throw new WrappedRuntimeException(e);
						}
					}
				}
			}
		} finally {
			if (!deregister) {
				registerProvisionalObject(objects);
			}
			CollectionModificationSupport.queue(false);
		}
	}

	protected void removeTransform(DomainTransformEvent evt) {
		transforms.remove(evt);
		getTransformsByCommitType(evt.getCommitType()).remove(evt);
	}

	public void removeTransformsForObjects(Collection c) {
		Set<DomainTransformEvent> trs = (Set<DomainTransformEvent>) getTransformsByCommitType(
				CommitType.TO_LOCAL_BEAN).clone();
		for (DomainTransformEvent dte : trs) {
			if (c.contains(dte.getSource())) {
				removeTransform(dte);
			}
		}
	}

	protected void setDomainObjects(MapObjectLookup domainObjects) {
		this.domainObjects = domainObjects;
	}

	protected void fireCreateObjectEvent(Class clazz, long id, long localId) {
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

	protected void updateAssociation(DomainTransformEvent evt,
			HasIdAndLocalId obj, Object tgt, boolean remove,
			boolean collectionPropertyChange) {
		Association assoc = obj == null ? null : CommonLocator
				.get()
				.propertyAccessor()
				.getAnnotationForProperty(obj.getClass(), Association.class,
						evt.getPropertyName());
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
				CommonLocator.get().propertyAccessor()
						.setPropertyValue(tgt, assoc.propertyName(), c);
			}
		} else {
			CommonLocator
					.get()
					.propertyAccessor()
					.setPropertyValue(tgt, assoc.propertyName(),
							remove ? null : obj);
		}
		objectModified(hTgt, evt, true);
	}

	public void setUndoManager(TransformHistoryManager undoManager) {
		this.undoManager = undoManager;
	}

	public TransformHistoryManager getUndoManager() {
		return undoManager;
	}

	public enum CollectionModificationType {
		ADD, REMOVE
	}

	static class CommitToLocalDomainTransformListener implements
			DomainTransformListener {
		/**
		 * Until 23/11/2010, case NULL_PROPERTY_REF: case CHANGE_PROPERTY_REF:
		 * were not in the case
		 * 
		 * I think that's in error - but checking. Basically, the transforms
		 * will be ignored if they're a double-dip (the property won't change)
		 */
		public void domainTransform(DomainTransformEvent evt) {
			if (evt.getCommitType() == CommitType.TO_LOCAL_GRAPH) {
				TransformManager tm = TransformManager.get();
				switch (evt.getTransformType()) {
				case CREATE_OBJECT:
				case DELETE_OBJECT:
				case ADD_REF_TO_COLLECTION:
				case REMOVE_REF_FROM_COLLECTION:
				case NULL_PROPERTY_REF:
				case CHANGE_PROPERTY_REF:
					try {
						tm.consume(evt);
					} catch (Exception e) {
						throw new WrappedRuntimeException(e);
					}
					break;
				}
				tm.setTransformCommitType(evt, CommitType.TO_STORAGE);
			}
		}
	}

	static class RecordTransformListener implements DomainTransformListener {
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
}
