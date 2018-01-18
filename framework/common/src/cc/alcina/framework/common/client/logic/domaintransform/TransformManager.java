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
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.totsp.gwittir.client.beans.SourcesPropertyChangeEvents;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.WrappedRuntimeException.SuggestedAction;
import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.common.client.collections.PropertyFilter;
import cc.alcina.framework.common.client.entity.WrapperPersistable;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domain.HasVersionNumber;
import cc.alcina.framework.common.client.logic.domaintransform.CollectionModification.CollectionModificationEvent;
import cc.alcina.framework.common.client.logic.domaintransform.CollectionModification.CollectionModificationListener;
import cc.alcina.framework.common.client.logic.domaintransform.CollectionModification.CollectionModificationSource;
import cc.alcina.framework.common.client.logic.domaintransform.CollectionModification.CollectionModificationSupport;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException.DomainTransformExceptionType;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.MapObjectLookupClient;
import cc.alcina.framework.common.client.logic.domaintransform.spi.ClassLookup;
import cc.alcina.framework.common.client.logic.domaintransform.spi.ClassLookup.PropertyInfoLite;
import cc.alcina.framework.common.client.logic.domaintransform.spi.ObjectLookup;
import cc.alcina.framework.common.client.logic.domaintransform.spi.ObjectStore;
import cc.alcina.framework.common.client.logic.domaintransform.spi.PropertyAccessor;
import cc.alcina.framework.common.client.logic.domaintransform.undo.NullUndoManager;
import cc.alcina.framework.common.client.logic.domaintransform.undo.TransformHistoryManager;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.Association;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.AlcinaBeanSerializer;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.CurrentUtcDateProvider;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.MultikeyMap;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.common.client.util.SimpleStringParser;
import cc.alcina.framework.common.client.util.SortedMultikeyMap;

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
	protected static final String UNSPECIFIC_PROPERTY_CHANGE = "---";

	public static final String ID_FIELD_NAME = "id";

	public static final String LOCAL_ID_FIELD_NAME = "localId";

	public static final String VERSION_FIELD_NAME = "versionNumber";

	public static final transient String CONTEXT_DO_NOT_POPULATE_SOURCE = TransformManager.class
			.getName() + ".CONTEXT_DO_NOT_POPULATE_SOURCE";

	public static final transient String CONTEXT_CONSUME_COLLECTION_MODS_AS_PROPERTY_CHANGES = TransformManager.class
			.getName() + ".CONTEXT_CONSUME_COLLECTION_MODS_AS_PROPERTY_CHANGES";

	protected static final Set<String> ignorePropertiesForCaching = new HashSet<String>(
			Arrays.asList(new String[] { "class", "id", "localId",
					"propertyChangeListeners" }));

	private static long eventIdCounter = 0;

	protected static SequentialIdGenerator localIdGenerator = new SequentialIdGenerator();

	private static TransformManager theInstance;

	public static String fromEnumValueCollection(Collection objects) {
		return CommonUtils.join(objects, ",");
	}

	public static String fromEnumValues(Object... objects) {
		return CommonUtils.join(objects, ",");
	}

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

	public static <H> Set<H> getDeltaSet(Collection<H> old, Object delta,
			CollectionModificationType modificationType) {
		Collection deltaC = CommonUtils.wrapInCollection(delta);
		Set c = new LinkedHashSet(old);
		if (modificationType == CollectionModificationType.ADD) {
			c.addAll(deltaC);
		} else {
			c.removeAll(deltaC);
		}
		return c;
	}

	public static long getEventIdCounter() {
		return eventIdCounter;
	}

	public static boolean hasInstance() {
		return theInstance != null;
	}

	public static List<Long> idListToLongs(String str) {
		ArrayList<Long> result = new ArrayList<Long>();
		if (CommonUtils.isNullOrEmpty(str)) {
			return result;
		}
		if (!str.matches("[0-9, \r\n\t()\\[\\]]+")) {
			return result;
		}
		String[] strs = str.replace("(", "").replace(")", "").replace("[", "")
				.replace("]", "").split(",\\s*");
		for (String s : strs) {
			String t = s.trim();
			if (t.length() > 0) {
				long value = Long.parseLong(t);
				if (value > 0) {
					result.add(value);
				}
			}
		}
		return result;
	}

	public static String
			logTransformStats(Set<DomainTransformEvent> transforms) {
		// group by obj class, property, type
		MultikeyMap<Integer> map = new SortedMultikeyMap<>(3);
		transforms.stream().forEach(transform -> {
			map.addInteger(1, transform.getObjectClass().getSimpleName(),
					Optional.ofNullable(transform.getPropertyName())
							.orElse("*"),
					CommonUtils.friendlyConstant(transform.getTransformType()));
		});
		return map.asTuples(3).stream().map(Object::toString)
				.collect(Collectors.joining("\n"));
	}

	public static void register(TransformManager tm) {
		theInstance = tm;
	}

	public static String stringId(HasIdAndLocalId hili) {
		return hili.getId() != 0 ? hili.getId() + "" : hili.getLocalId() + "L";
	}

	public static <E extends Enum> Set<E> toEnumValues(String s,
			Class<E> clazz) {
		Set<E> result = new LinkedHashSet<>();
		if (s != null) {
			for (String sPart : s.split(",")) {
				E value = CommonUtils.getEnumValueOrNull(clazz, sPart);
				if (value == null && sPart.length() > 0) {
					System.out.println(CommonUtils.formatJ(
							"Warning - can't deserialize %s for %s", sPart,
							clazz));
				}
				result.add(value);
			}
			result.remove(null);
		}
		return result;
	}

	private ObjectStore domainObjects;

	final Set<DomainTransformEvent> transforms = new LinkedHashSet<DomainTransformEvent>();

	final Map<CommitType, LinkedHashSet<DomainTransformEvent>> transformsByType = new HashMap<CommitType, LinkedHashSet<DomainTransformEvent>>();

	protected DomainTransformSupport transformListenerSupport;

	protected IdentityHashMap<Object, Boolean> provisionalObjects = new IdentityHashMap<>();

	private boolean ignorePropertyChanges;

	private boolean replayingRemoteEvent;

	protected CollectionModificationSupport collectionModificationSupport;

	protected DomainTransformEvent currentEvent;

	private TransformHistoryManager undoManager = new NullUndoManager();

	private boolean ignoreUnrecognizedDomainClassException;

	protected TransformManager() {
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
		this.collectionModificationSupport
				.addCollectionModificationListener(listener, listenedClass);
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

	public void addTransforms(List<DomainTransformEvent> transforms,
			boolean fireEvents) {
		for (DomainTransformEvent domainTransformEvent : transforms) {
			addTransform(domainTransformEvent);
			if (fireEvents) {
				try {
					fireDomainTransform(domainTransformEvent);
				} catch (DomainTransformException e) {
					DomainTransformRuntimeException dtre = new DomainTransformRuntimeException(
							e.getMessage());
					dtre.setEvent(e.getEvent());
					throw dtre;
				}
			}
		}
	}

	public void appShutdown() {
		theInstance = null;
	}

	public void clearTransforms() {
		getTransforms().clear();
		for (CommitType ct : transformsByType.keySet()) {
			transformsByType.get(ct).clear();
		}
	}

	public void clearUserObjects() {
		setDomainObjects(null);
	}

	public void commitProvisionalObjects(Collection c) {
		promoteToDomain(c, false);
	}

	@SuppressWarnings("incomplete-switch")
	public void consume(DomainTransformEvent event)
			throws DomainTransformException {
		currentEvent = event;
		HasIdAndLocalId obj = null;
		TransformType transformType = event.getTransformType();
		if (transformType != TransformType.CREATE_OBJECT) {
			obj = getObject(event);
			if (obj == null) {
				throw new DomainTransformException(event,
						DomainTransformExceptionType.SOURCE_ENTITY_NOT_FOUND);
			}
		}
		Object existingTargetValue = null;
		if (event.isInImmediatePropertyChangeCommit()) {
			existingTargetValue = event.getOldValue();
		} else if (event.getSource() == null
				|| event.getPropertyName() == null) {
		} else {
			existingTargetValue = propertyAccessor().getPropertyValue(
					event.getSource(), event.getPropertyName());
		}
		existingTargetValue = ensureEndpointInTransformGraph(
				existingTargetValue);
		Object newTargetValue = transformType == null ? null
				: getTargetObject(event, false);
		if (!checkPermissions(obj, event, event.getPropertyName(),
				existingTargetValue)) {
			return;
		}
		if (!checkPermissions(obj, event, event.getPropertyName(),
				newTargetValue)) {
			return;
		}
		getUndoManager().prepareUndo(event);
		checkVersion(obj, event);
		switch (transformType) {
		case CHANGE_PROPERTY_SIMPLE_VALUE:
		case ADD_REF_TO_COLLECTION:
		case REMOVE_REF_FROM_COLLECTION:
		case CHANGE_PROPERTY_REF:
			if (event.getValueClass() == null) {
				throw new RuntimeException(
						"null value class for modification requiring a class");
			}
		}
		switch (transformType) {
		// these cases will fire a new transform event (temp obj > domain obj),
		// so should not be processed further
		case NULL_PROPERTY_REF: {
			int tmpDebug = 0;
		}
		case CHANGE_PROPERTY_REF:
		case CHANGE_PROPERTY_SIMPLE_VALUE:
			if (isReplayingRemoteEvent() && obj == null) {
				// it's been deleted on the client, but we've just now got the
				// creation id
				// note, should never occur TODO: notify server
				return;
			}
			propertyAccessor().setPropertyValue(obj, event.getPropertyName(),
					newTargetValue);
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
			switch (transformType) {
			case NULL_PROPERTY_REF:
			case CHANGE_PROPERTY_REF:
				boolean equivalentValues = CommonUtils.equalsWithNullEquality(
						existingTargetValue, newTargetValue);
				boolean equalValues = existingTargetValue == newTargetValue;
				if (!equalValues && (updateAssociationsWithoutNoChangeCheck()
						|| !equivalentValues)) {
					if (existingTargetValue instanceof Collection) {
						throw new RuntimeException(
								"Should not null a collection property:\n "
										+ event.toString());
					}
					/*
					 * sort of gnarly here - when we're using transactional
					 * memcache we may want to replace the collection member
					 * (non-transactional) with a transactional clone - which
					 * equals() - but we definitely don't want to get stuck in a
					 * loop. on the other hand, the client should always fire
					 * collection mods
					 */
					boolean fireCollectionMods = !equivalentValues
							|| alwaysFireObjectOwnerCollectionModifications();
					updateAssociation(event, obj, existingTargetValue, true,
							fireCollectionMods);
					updateAssociation(event, obj, newTargetValue, false,
							fireCollectionMods);
				}
				break;
			}
			break;
		// add and removeref will not cause a property change, so no transform
		// removal
		case ADD_REF_TO_COLLECTION: {
			maybeModifyAsPropertyChange(obj, event.getPropertyName(),
					newTargetValue, CollectionModificationType.ADD);
			Set set = (Set) propertyAccessor().getPropertyValue(obj,
					event.getPropertyName());
			if (!set.contains(newTargetValue)) {
				doubleCheckAddition(set, newTargetValue);
			}
			objectModified(obj, event, false);
			updateAssociation(event, obj, newTargetValue, false, true);
			collectionChanged(obj, newTargetValue);
		}
			break;
		case REMOVE_REF_FROM_COLLECTION: {
			maybeModifyAsPropertyChange(obj, event.getPropertyName(),
					newTargetValue, CollectionModificationType.REMOVE);
			Set set = (Set) propertyAccessor().getPropertyValue(obj,
					event.getPropertyName());
			boolean wasContained = set.remove(newTargetValue);
			if (!wasContained) {
				doubleCheckRemoval(set, newTargetValue);
			}
		}
			updateAssociation(event, obj, newTargetValue, true, true);
			collectionChanged(obj, newTargetValue);
			break;
		case DELETE_OBJECT:
			performDeleteObject((HasIdAndLocalId) obj);
			break;
		case CREATE_OBJECT:
			if (event.getObjectId() != 0) {
				// three possibilities:
				// 1. replaying a server create,(on the client)
				// 2. recording an in-entity-manager create
				// 3. doing a database-regeneration
				// if (2), break at this point
				if (getObjectForCreate(event) != null) {
					break;
				}
			}
			long creationLocalId = isZeroCreatedObjectLocalId(
					event.getObjectClass()) ? 0 : event.getObjectLocalId();
			HasIdAndLocalId hili = (HasIdAndLocalId) classLookup().newInstance(
					event.getObjectClass(), event.getObjectId(),
					event.getObjectLocalId());
			hili.setLocalId(creationLocalId);
			if (hili.getId() == 0 && event.getObjectId() != 0) {// replay from
																// server -
				// huh? unless newInstance does something weird, should never
				// reach here
				hili.setId(event.getObjectId());
			}
			event.setObjectId(hili.getId());
			if (event.getCommitType() == CommitType.TO_STORAGE) {
				objectModified(hili, event, false);
			}
			objectCreated(hili);
			if (getDomainObjects() != null) {
				getDomainObjects().mapObject(hili);
				maybeFireCollectionModificationEvent(hili.getClass(), false);
			}
			break;
		default:
			assert false : "Transform type not implemented: " + transformType;
		}
		currentEvent = null;
	}

	public boolean containsObject(DomainTransformEvent dte) {
		HasIdAndLocalId obj = getObjectLookup().getObject(dte.getObjectClass(),
				dte.getObjectId(), dte.getObjectLocalId());
		return obj != null;
	}

	public void convertToTargetObject(DomainTransformEvent evt) {
		Object value = evt.getNewValue();
		if (value == null) {
			return;
		}
		// this dte will never be used - it'll be converted to a series of
		// add/remove refs
		if (value instanceof Set) {
			return;
		}
		if (value instanceof List || value instanceof Map) {
			ClassLookup classLookup = classLookup();
			PropertyInfoLite pd = classLookup
					.getWritableProperties(evt.getObjectClass()).stream()
					.filter(pd1 -> pd1.getPropertyName()
							.equals(evt.getPropertyName()))
					.findFirst().get();
			Preconditions.checkArgument(pd.hasSerializeWithBeanSerialization());
			evt.setNewStringValue(
					Registry.impl(AlcinaBeanSerializer.class).serialize(value));
			evt.setValueClass(String.class);
			return;
		}
		evt.setValueClass(value instanceof Enum
				? ((Enum) value).getDeclaringClass() : value.getClass());
		if (value.getClass() == Integer.class
				|| value.getClass() == String.class
				|| value.getClass() == Double.class
				|| value.getClass() == Float.class
				|| value.getClass() == Short.class
				|| value.getClass() == Boolean.class) {
			evt.setNewStringValue(value.toString());
		} else if (value.getClass() == Long.class) {
			evt.setNewStringValue(SimpleStringParser.toString((Long) value));
		} else if (value.getClass() == Date.class) {
			evt.setNewStringValue(
					SimpleStringParser.toString((((Date) value).getTime())));
		} else if (value instanceof Enum) {
			// make sure the enum is reflect-instantiable (although not strictly
			// necessary here, it's a common dev problem to miss this
			// annotation, and here is the best place to catch it
			Class clazz = classLookup()
					.getClassForName(evt.getValueClassName());
			evt.setNewStringValue(((Enum) value).name());
		} else if (value instanceof HasIdAndLocalId) {
			HasIdAndLocalId hili = (HasIdAndLocalId) value;
			evt.setValueId(hili.getId());
			evt.setValueLocalId(hili.getLocalId());
		}
	}

	public <T extends HasIdAndLocalId> T
			createDomainObject(Class<T> objectClass) {
		long localId = nextLocalIdCounter();
		T newInstance = classLookup().newInstance(objectClass, 0, localId);
		newInstance.setLocalId(localId);
		// a bit roundabout, but to ensure compatibility with the event system
		// essentially registers a synthesised object, then replaces it in the
		// mapping with the real one
		fireCreateObjectEvent(objectClass, 0, localId);
		registerDomainObject(newInstance);
		return newInstance;
	}

	public <T extends HasIdAndLocalId> T
			createProvisionalObject(Class<T> objectClass) {
		long localId = nextLocalIdCounter();
		T newInstance = classLookup().newInstance(objectClass, 0, localId);
		newInstance.setLocalId(localId);
		registerProvisionalObject(newInstance);
		return newInstance;
	}

	public DomainTransformEvent
			createTransformFromPropertyChange(PropertyChangeEvent evt) {
		DomainTransformEvent dte = new DomainTransformEvent();
		dte.setSource((HasIdAndLocalId) evt.getSource());
		dte.setNewValue(evt.getNewValue());
		dte.setPropertyName(evt.getPropertyName());
		HasIdAndLocalId dObj = (HasIdAndLocalId) evt.getSource();
		dte.setObjectId(dObj.getId());
		dte.setObjectLocalId(dObj.getLocalId());
		dte.setObjectClass(dObj.getClass());
		dte.setTransformType(TransformType.CHANGE_PROPERTY_SIMPLE_VALUE);
		maybeAddVersionNumbers(dte, dObj, evt.getNewValue());
		return dte;
	}

	public boolean currentTransformIsDuringCreationRequest() {
		return currentEvent.getObjectLocalId() != 0;
	}

	public <H extends HasIdAndLocalId> void
			deleteMultiple(Collection<H> collection) {
		new ArrayList<H>(collection).forEach(hili -> deleteObject(hili, true));
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
		registerDomainObject(hili);
		doCascadeDeletes(hili);
		removeAssociations(hili);
		DomainTransformEvent dte = new DomainTransformEvent();
		dte.setObjectId(hili.getId());
		dte.setObjectLocalId(hili.getLocalId());
		dte.setObjectClass(hili.getClass());
		dte.setTransformType(TransformType.DELETE_OBJECT);
		dte.setSource(hili);
		addTransform(dte);
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

	public void deleteObjectOrRemoveTransformsIfLocal(HasIdAndLocalId hili) {
		if (hili.getId() != 0) {
			deleteObject(hili);
			return;
		}
		Set<DomainTransformEvent> toRemove = new LinkedHashSet<DomainTransformEvent>();
		LinkedHashSet<DomainTransformEvent> trs = getTransformsByCommitType(
				CommitType.TO_LOCAL_BEAN);
		for (DomainTransformEvent dte : trs) {
			HasIdAndLocalId source = dte.getSource() != null ? dte.getSource()
					: getObject(dte);
			if (hili.equals(source)) {
				toRemove.add(dte);
			}
			if (dte.getValueId() != 0 || dte.getValueLocalId() != 0) {
				HasIdAndLocalId object = getObjectLookup().getObject(
						dte.getValueClass(), dte.getValueId(),
						dte.getValueLocalId());
				if (hili.equals(object)) {
					toRemove.add(dte);
				}
			}
		}
		trs.removeAll(toRemove);
		transforms.removeAll(toRemove);
	}

	public void deleteObjects(Class<? extends HasIdAndLocalId> clazz,
			Collection<Long> ids) {
		for (Long id : ids) {
			HasIdAndLocalId hili = Reflections.classLookup().newInstance(clazz);
			hili.setId(id);
			deleteObject(hili, true);
		}
	}

	public void deregisterDomainObject(Object o) {
		deregisterDomainObjects(CommonUtils.wrapInCollection(o));
	}

	public void deregisterDomainObjects(Collection<HasIdAndLocalId> hilis) {
		if (getDomainObjects() != null) {
			getDomainObjects().deregisterObjects(hilis);
		}
	}

	public void deregisterProvisionalObject(Object o) {
		deregisterProvisionalObjects(CommonUtils.wrapInCollection(o));
	}

	public void deregisterProvisionalObjects(Collection c) {
		provisionalObjects.keySet().removeAll(c);
		for (Object b : c) {
			if (b instanceof SourcesPropertyChangeEvents) {
				SourcesPropertyChangeEvents sb = (SourcesPropertyChangeEvents) b;
				sb.removePropertyChangeListener(this);
			}
		}
		removeTransformsForObjects(c);
	}

	public boolean dirty(Collection provisionalObjects) {
		Collection<DomainTransformEvent> trs = getTransformsByCommitType(
				CommitType.TO_LOCAL_BEAN);
		for (DomainTransformEvent dte : trs) {
			if (provisionalObjects.contains(dte.getSource())) {
				return true;
			}
		}
		return false;
	}

	public <V extends HasIdAndLocalId> V ensure(Collection<V> instances,
			Class<V> clazz, String key, Object value, HasIdAndLocalId parent,
			String parentPropertyName) {
		V instance = CommonUtils.first(CollectionFilters.filter(instances,
				new PropertyFilter<V>(key, value)));
		if (instance != null) {
			return instance;
		}
		instance = createDomainObject(clazz);
		propertyAccessor().setPropertyValue(instance, key, value);
		if (parent != null) {
			propertyAccessor().setPropertyValue(instance, parentPropertyName,
					parent);
		}
		return instance;
	}

	public <V extends HasIdAndLocalId> List<V> filter(Class<V> clazz,
			CollectionFilter<V> filter) {
		List<V> result = new ArrayList<V>(
				getDomainObjects().getCollection(clazz));
		if (filter != null) {
			result = CollectionFilters.filter(result, filter);
		}
		if (!result.isEmpty() && result.get(0) instanceof Comparable) {
			Collections.sort((List) result);
		}
		return result;
	}

	public <V extends HasIdAndLocalId> V find(Class<V> clazz, String key,
			Object value) {
		return CommonUtils
				.first(filter(clazz, new PropertyFilter<V>(key, value)));
	}

	public void
			fireCollectionModificationEvent(CollectionModificationEvent event) {
		this.collectionModificationSupport
				.fireCollectionModificationEvent(event);
	}

	public synchronized void fireDomainTransform(DomainTransformEvent event)
			throws DomainTransformException {
		this.transformListenerSupport.fireDomainTransform(event);
	}

	public <V extends HasIdAndLocalId> List<V> fromIdList(Class<V> clazz,
			String idStr) {
		List<Long> ids = idListToLongs(idStr);
		List<V> result = new ArrayList<V>();
		for (Long id : ids) {
			result.add(getObject(clazz, id, 0));
		}
		return result;
	}

	public <T> Collection<T> getCollection(Class<T> clazz) {
		return getDomainObjects().getCollection(clazz);
	}

	// TODO - Jira - get rid of objectstore vs objectlookup
	public ObjectStore getDomainObjects() {
		return this.domainObjects;
	}

	/**
	 * useful support in TLTM, ThreadedClientTM
	 */
	public <H extends HasIdAndLocalId> long
			getLocalIdForClientInstance(H hili) {
		return hili.getLocalId();
	}

	public <T extends HasIdAndLocalId> T getObject(Class<? extends T> c,
			long id, long localId) {
		if (this.getDomainObjects() != null) {
			return getDomainObjects().getObject(c, id, localId);
		}
		return null;
	}

	public HasIdAndLocalId getObject(DomainTransformEvent dte) {
		return getObject(dte, false);
	}

	public HasIdAndLocalId getObject(DomainTransformEvent dte,
			boolean ignoreSource) {
		HasIdAndLocalId obj = getObject(dte.getObjectClass(), dte.getObjectId(),
				dte.getObjectLocalId());
		if (obj == null && (ignoreSource
				|| LooseContext.is(CONTEXT_DO_NOT_POPULATE_SOURCE))) {
			return null;
		}
		if (obj == null && dte.getSource() != null) {
			// if create, natural behaviour is return null, ignoring source
			if (dte.getTransformType() != TransformType.CREATE_OBJECT
					&& dte.getTransformType() != TransformType.DELETE_OBJECT) {
				String message = CommonUtils.formatJ(
						"calling getobject() on a provisional/deregistered object transform "
								+ "- will harm the transform. use getsource() - \n%s\n",
						dte);
				throw new RuntimeException(message);
			}
		}
		dte.setSource(obj);
		return obj;
	}

	public <T extends HasIdAndLocalId> T getObject(HiliLocator hiliLocator) {
		return (T) getObject(hiliLocator.getClazz(), hiliLocator.getId(), 0L);
	}

	public <T extends HasIdAndLocalId> T getObject(T hili) {
		return (T) getObjectLookup().getObject(hili.getClass(), hili.getId(),
				hili.getLocalId());
	}

	public TransformManager getT() {
		return null;
	}

	public Enum getTargetEnumValue(DomainTransformEvent evt) {
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

	public Object getTargetObject(DomainTransformEvent evt, boolean oldValue)
			throws DomainTransformException {
		Class valueClass = evt.getValueClass();
		if (evt.getNewValue() != null || valueClass == null) {
			if (evt.getNewValue() instanceof HasIdAndLocalId) {
				HasIdAndLocalId hili = getObjectLookup()
						.getObject((HasIdAndLocalId) evt.getNewValue());
				if (hili != null) {
					return hili;
				} else {
					// this is perfectly possible - particularly on the client.
					// allow it there, to save lots of unhelpful
					// register/deregister boilerplate
					if (!allowUnregisteredHiliTargetObject()) {
						throw new WrappedRuntimeException(
								"Unable to get target object " + evt,
								SuggestedAction.NOTIFY_ERROR);
					}
				}
			}
			return evt.getNewValue();
		}
		if (valueClass == String.class) {
			return evt.getNewStringValue();
		}
		if (valueClass == Long.class || valueClass == long.class) {
			return SimpleStringParser.toLong(evt.getNewStringValue());
		}
		if (valueClass == Double.class || valueClass == double.class) {
			return Double.valueOf(evt.getNewStringValue());
		}
		if (valueClass == Integer.class || valueClass == int.class) {
			return Integer.valueOf(evt.getNewStringValue());
		}
		if (valueClass == Boolean.class || valueClass == boolean.class) {
			return Boolean.valueOf(evt.getNewStringValue());
		}
		if (valueClass == Date.class) {
			return new Date(SimpleStringParser.toLong(evt.getNewStringValue()));
		}
		if (valueClass == List.class || valueClass == Map.class) {
			ClassLookup classLookup = classLookup();
			PropertyInfoLite pd = classLookup
					.getWritableProperties(evt.getObjectClass()).stream()
					.filter(pd1 -> pd1.getPropertyName()
							.equals(evt.getPropertyName()))
					.findFirst().get();
			Preconditions.checkArgument(pd.hasSerializeWithBeanSerialization());
			return Registry.impl(AlcinaBeanSerializer.class)
					.deserialize(evt.getNewStringValue());
		}
		Enum e = getTargetEnumValue(evt);
		if (e != null) {
			return e;
		}
		if (evt.getValueId() != 0 || evt.getValueLocalId() != 0) {
			HasIdAndLocalId object = getObjectLookup().getObject(valueClass,
					evt.getValueId(), evt.getValueLocalId());
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

	public LinkedHashSet<DomainTransformEvent>
			getTransformsByCommitType(CommitType ct) {
		if (transformsByType.get(ct) == null) {
			transformsByType.put(ct, new LinkedHashSet<DomainTransformEvent>());
		}
		return transformsByType.get(ct);
	}

	public TransformHistoryManager getUndoManager() {
		return undoManager;
	}

	public boolean hasUnsavedChanges(Object object) {
		Collection objects = CommonUtils.wrapInCollection(object);
		Collection<DomainTransformEvent> trs = getTransformsByCommitType(
				CommitType.TO_LOCAL_BEAN);
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

	public boolean isIgnoreUnrecognizedDomainClassException() {
		return this.ignoreUnrecognizedDomainClassException;
	}

	public boolean isInCreationRequest(HasIdAndLocalId hasOwner) {
		return false;
	}

	public <T extends HasIdAndLocalId> boolean
			isProvisionalObject(final T object) {
		if (getProvisionalObjects().contains(object)) {
			for (Object o : getProvisionalObjects()) {
				if (o == object) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean isRegistered(HasIdAndLocalId hili) {
		HasIdAndLocalId registered = getObject(hili);
		return registered == hili;
	}

	public boolean isReplayingRemoteEvent() {
		return this.replayingRemoteEvent;
	}

	public void modifyCollectionProperty(Object objectWithCollection,
			String collectionPropertyName, Object delta,
			CollectionModificationType modificationType) {
		Collection deltaC = CommonUtils.wrapInCollection(delta);
		Collection old = (Collection) propertyAccessor()
				.getPropertyValue(objectWithCollection, collectionPropertyName);
		Collection c = CommonUtils.shallowCollectionClone(old);
		if (c == null) {
			// handles the case when we're working within a transaction and try
			// to clone, say a PersistentSet
			c = new LinkedHashSet(old);
		}
		if (modificationType == CollectionModificationType.ADD) {
			c.addAll(deltaC);
		} else {
			c.removeAll(deltaC);
		}
		propertyAccessor().setPropertyValue(objectWithCollection,
				collectionPropertyName, c);
	}

	public synchronized long nextEventIdCounter() {
		return ++eventIdCounter;
	}

	public synchronized long nextLocalIdCounter() {
		return localIdGenerator.incrementAndGet();
	}

	public List<DomainTransformEvent> objectsToDtes(Collection objects,
			Class clazz, boolean asObjectSpec) throws Exception {
		ClassLookup classLookup = classLookup();
		List<PropertyInfoLite> pds = classLookup.getWritableProperties(clazz);
		Object templateInstance = classLookup.getTemplateInstance(clazz);
		PropertyAccessor accessor = propertyAccessor();
		Map<String, Object> defaultValues = new HashMap();
		Set<Class> implementsHili = new HashSet<Class>();
		for (Iterator<PropertyInfoLite> itr = pds.iterator(); itr.hasNext();) {
			PropertyInfoLite info = itr.next();
			String propertyName = info.getPropertyName();
			if (ignorePropertyForCaching(clazz, info.getPropertyType(),
					propertyName)) {
				itr.remove();
			} else {
				Object defaultValue = accessor
						.getPropertyValue(templateInstance, propertyName);
				defaultValues.put(propertyName, defaultValue);
				try {
					if (info.getPropertyType() == Set.class
							|| info.getPropertyType() == List.class
							|| CommonUtils.isStandardJavaClassOrEnum(
									info.getPropertyType())) {
						continue;
					}
					Object template = classLookup
							.getTemplateInstance(info.getPropertyType());
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
			dte.setUtcDate(new Date(0L));
			Long id = asObjectSpec ? (Long) arr[0] : hili.getId();
			long localId = id == 0 ? hili.getLocalId() : 0L;
			dte.setObjectLocalId(localId);
			dte.setObjectId(id);
			dte.setObjectClass(clazz);
			dte.setTransformType(TransformType.CREATE_OBJECT);
			dtes.add(dte);
			int i = 1;
			for (PropertyInfoLite pd : pds) {
				String propertyName = pd.getPropertyName();
				Class propertyType = pd.getPropertyType();
				Object defaultValue = defaultValues.get(propertyName);
				Object value = asObjectSpec ? arr[i++]
						: propertyAccessor().getPropertyValue(o, propertyName);
				if (value == null && defaultValue == null) {
					continue;
				}
				if (value != null && defaultValue != null) {
					if (value.equals(defaultValue)) {
						continue;
					}
				}
				if (value instanceof Collection || value instanceof Map) {
					if (pd.isSerializableCollection()) {
						Iterator itr = ((Set) value).iterator();
						for (; itr.hasNext();) {
							Object o2 = itr.next();
							dte = new DomainTransformEvent();
							dte.setUtcDate(new Date(0L));
							dte.setObjectId(id);
							dte.setObjectLocalId(localId);
							dte.setObjectClass(clazz);
							dte.setPropertyName(propertyName);
							dte.setTransformType(
									TransformType.ADD_REF_TO_COLLECTION);
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
								dte.setValueClass(e.getDeclaringClass());
							}
							dtes.add(dte);
						}
					} else if (pd.hasSerializeWithBeanSerialization()) {
						dte = new DomainTransformEvent();
						dte.setUtcDate(new Date(0L));
						dte.setObjectId(id);
						dte.setObjectLocalId(localId);
						dte.setObjectClass(clazz);
						dte.setPropertyName(propertyName);
						dte.setNewValue(null);
						AlcinaBeanSerializer serializer = Registry
								.impl(AlcinaBeanSerializer.class);
						String serialized = serializer.serialize(value);
						dte.setNewStringValue(serialized);
						dte.setValueClass(String.class);
					}
				} else {
					dte = new DomainTransformEvent();
					dte.setUtcDate(new Date(0L));
					dte.setObjectId(id);
					dte.setObjectClass(clazz);
					if (value instanceof Timestamp) {
						value = new Date(((Timestamp) value).getTime());
					}
					dte.setNewValue(value);
					dte.setPropertyName(propertyName);
					if (!implementsHili.contains(propertyType)) {
						convertToTargetObject(dte);
						dte.setTransformType(
								TransformType.CHANGE_PROPERTY_SIMPLE_VALUE);
					} else {
						dte.setValueClass(propertyType);
						long valueId = asObjectSpec ? (Long) value
								: ((HasIdAndLocalId) value).getId();
						long valueLocalId = valueId == 0L
								? ((HasIdAndLocalId) value).getLocalId() : 0L;
						dte.setValueId(valueId);
						dte.setValueLocalId(valueLocalId);
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

	public synchronized void propertyChange(PropertyChangeEvent evt) {
		if (isIgnorePropertyChanges()
				|| UNSPECIFIC_PROPERTY_CHANGE.equals(evt.getPropertyName())) {
			return;
		}
		List<DomainTransformEvent> transforms = new ArrayList<DomainTransformEvent>();
		DomainTransformEvent dte = createTransformFromPropertyChange(evt);
		dte.setOldValue(evt.getOldValue());
		convertToTargetObject(dte);
		if (dte.getNewValue() == null) {
			dte.setTransformType(TransformType.NULL_PROPERTY_REF);
		}
		if (dte.getValueId() != 0 || dte.getValueLocalId() != 0) {
			dte.setTransformType(TransformType.CHANGE_PROPERTY_REF);
		}
		if (dte.getNewValue() instanceof Set) {
			Set typeCheck = (Set) evt.getNewValue();
			typeCheck = (Set) (typeCheck.isEmpty() && evt.getOldValue() != null
					? evt.getOldValue() : typeCheck);
			// Note, we explicitly clear nulls here - it would require an
			// expansion of the protocols to implement them
			if (typeCheck.iterator().hasNext()) {
				if (typeCheck.iterator().next() instanceof HasIdAndLocalId) {
					Set<HasIdAndLocalId> oldValues = (Set) evt.getOldValue();
					Set<HasIdAndLocalId> newValues = (Set) evt.getNewValue();
					oldValues.remove(null);
					newValues.remove(null);
					for (HasIdAndLocalId hili : newValues) {
						if (!oldValues.contains(hili)) {
							dte = createTransformFromPropertyChange(evt);
							dte.setNewValue(null);
							dte.setValueId(hili.getId());
							dte.setValueLocalId(hili.getLocalId());
							dte.setValueClass(hili.getClass());
							dte.setTransformType(
									TransformType.ADD_REF_TO_COLLECTION);
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
							dte.setTransformType(
									TransformType.REMOVE_REF_FROM_COLLECTION);
							transforms.add(dte);
						}
					}
				} else if (typeCheck.iterator().next() instanceof Enum) {
					Set<Enum> oldValues = (Set) evt.getOldValue();
					Set<Enum> newValues = (Set) evt.getNewValue();
					oldValues.remove(null);
					newValues.remove(null);
					for (Enum e : newValues) {
						if (!oldValues.contains(e)) {
							dte = createTransformFromPropertyChange(evt);
							dte.setNewValue(null);
							dte.setNewStringValue(e.name());
							dte.setValueClass(e.getDeclaringClass());
							dte.setTransformType(
									TransformType.ADD_REF_TO_COLLECTION);
							transforms.add(dte);
						}
					}
					for (Enum e : oldValues) {
						if (!newValues.contains(e)) {
							dte = createTransformFromPropertyChange(evt);
							dte.setNewValue(null);
							dte.setNewStringValue(e.name());
							dte.setValueClass(e.getDeclaringClass());
							dte.setTransformType(
									TransformType.REMOVE_REF_FROM_COLLECTION);
							transforms.add(dte);
						}
					}
				}
			}
		} else {
			transforms.add(dte);
		}
		for (DomainTransformEvent event : transforms) {
			event.setInImmediatePropertyChangeCommit(true);
		}
		addTransforms(transforms, true);
		for (DomainTransformEvent event : transforms) {
			event.setInImmediatePropertyChangeCommit(false);
		}
		Object hili = evt.getSource();
		if (this.getDomainObjects() != null) {
			if (provisionalObjects.isEmpty()
					|| !provisionalObjects.containsKey(hili)) {
				maybeFireCollectionModificationEvent(hili.getClass(), true);
			}
		}
		if (dte.getObjectId() == 0 && dte.getObjectLocalId() == 0) {
			// normally a bug
			int debugPoint = 7;
		}
	}

	public void pushTransformsInCurrentThread(
			Collection<DomainTransformEvent> dtes) {
		getTransformsByCommitType(CommitType.TO_LOCAL_BEAN).addAll(dtes);
	}

	public <T extends HasIdAndLocalId> T registerDomainObject(T hili) {
		if (getDomainObjects() != null && hili != null) {
			if (hili.getId() == 0) {
				HasIdAndLocalId createdObject = getDomainObjects()
						.getObject(hili);
				getDomainObjects().deregisterObject(createdObject);
			}
			getDomainObjects().mapObject(hili);
		}
		return hili;
	}

	public void
			registerDomainObjects(Collection<? extends HasIdAndLocalId> hilis) {
		for (HasIdAndLocalId hili : hilis) {
			registerDomainObject(hili);
		}
	}

	public void registerDomainObjectsAsync(Collection<HasIdAndLocalId> hilis,
			final AsyncCallback<Void> postRegisterCallback) {
		((MapObjectLookupClient) getDomainObjects()).registerAsync(hilis,
				new ScheduledCommand() {
					@Override
					public void execute() {
						postRegisterCallback.onSuccess(null);
					}
				});
	}

	/**
	 * Useful for unit tests, hence here rather than clientTM
	 */
	public void registerDomainObjectsInHolder(DomainModelHolder h) {
		if (this.getDomainObjects() != null) {
			getDomainObjects().removeListeners();
		}
		createObjectLookup();
		getDomainObjects().registerObjects(h.registerableDomainObjects());
		ClassRef.add(h.getClassRefs());
	}

	public void registerDomainObjectsInHolderAsync(final DomainModelHolder h,
			final AsyncCallback<Void> postRegisterCallback) {
		if (this.getDomainObjects() != null) {
			getDomainObjects().removeListeners();
		}
		createObjectLookup();
		((MapObjectLookupClient) getDomainObjects()).registerAsync(
				h.registerableDomainObjects(), new ScheduledCommand() {
					@Override
					public void execute() {
						ClassRef.add(h.getClassRefs());
						postRegisterCallback.onSuccess(null);
					}
				});
	}

	public <V extends HasIdAndLocalId> Set<V>
			registeredObjectsAsSet(Class<V> clazz) {
		return new LinkedHashSet<V>(getDomainObjects().getCollection(clazz));
	}

	public <V extends HasIdAndLocalId> V registeredSingleton(Class<V> clazz) {
		Collection<V> c = getDomainObjects().getCollection(clazz);
		return c.isEmpty() ? null : c.iterator().next();
	}

	/**
	 * @see getLocalIdForClientInstance
	 */
	public void registerHiliMappingPriorToLocalIdDeletion(Class clazz, long id,
			long localId) {
		return;
	}

	public void registerModelObject(final DomainModelObject h,
			final AsyncCallback<Void> postRegisterCallback) {
		getDomainObjects().registerObjects(h.registrableObjects());
		postRegisterCallback.onSuccess(null);
	}

	public void registerModelObjectAsync(final DomainModelObject h,
			final AsyncCallback<Void> postRegisterCallback) {
		((MapObjectLookupClient) getDomainObjects())
				.registerAsync(h.registrableObjects(), new ScheduledCommand() {
					@Override
					public void execute() {
						postRegisterCallback.onSuccess(null);
					}
				});
	}

	public void registerProvisionalObject(Object o) {
		Collection c = CommonUtils.wrapInCollection(o);
		for (Object b : c) {
			provisionalObjects.put(b, true);
		}
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

	public void
			removeDomainTransformListener(DomainTransformListener listener) {
		this.transformListenerSupport.removeDomainTransformListener(listener);
	}

	public void removeTransform(DomainTransformEvent evt) {
		transforms.remove(evt);
		getTransformsByCommitType(evt.getCommitType()).remove(evt);
	}

	public void removeTransformsFor(Object object) {
		removeTransformsForObjects(Arrays.asList(object));
	}

	public void removeTransformsForObjects(Collection c) {
		Set<DomainTransformEvent> trs = (Set<DomainTransformEvent>) getTransformsByCommitType(
				CommitType.TO_LOCAL_BEAN).clone();
		if (!(c instanceof Set)) {
			c = new HashSet(c);
		}
		for (DomainTransformEvent dte : trs) {
			if (c.contains(dte.provideSourceOrMarker())
					|| c.contains(dte.getNewValue())
					|| c.contains(dte.provideTargetMarkerForRemoval())) {
				removeTransform(dte);
			}
		}
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

	public void setIgnoreUnrecognizedDomainClassException(
			boolean ignoreUnrecognizedDomainClassException) {
		this.ignoreUnrecognizedDomainClassException = ignoreUnrecognizedDomainClassException;
	}

	public void setReplayingRemoteEvent(boolean replayingRemoteEvent) {
		this.replayingRemoteEvent = replayingRemoteEvent;
	}

	public void setTransformCommitType(DomainTransformEvent evt,
			CommitType ct) {
		getTransformsByCommitType(evt.getCommitType()).remove(evt);
		evt.setCommitType(ct);
		getTransformsByCommitType(evt.getCommitType()).add(evt);
	}

	public void setUndoManager(TransformHistoryManager undoManager) {
		this.undoManager = undoManager;
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

	public String toIdList(Collection<? extends HasIdAndLocalId> hilis) {
		StringBuffer sb = new StringBuffer();
		for (HasIdAndLocalId hili : hilis) {
			if (sb.length() != 0) {
				sb.append(", ");
			}
			sb.append(hili.getId());
		}
		return sb.toString();
	}

	protected boolean allowUnregisteredHiliTargetObject() {
		return false;
	}

	protected boolean alwaysFireObjectOwnerCollectionModifications() {
		return false;
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

	protected void checkVersion(HasIdAndLocalId obj, DomainTransformEvent event)
			throws DomainTransformException {
	}

	protected ClassLookup classLookup() {
		return Reflections.classLookup();
	}

	protected void collectionChanged(Object obj, Object tgt) {
		// changes won't be noticed unless we do this
		if (obj instanceof WrapperPersistable) {
			((WrapperPersistable) obj)
					.fireNullPropertyChange(UNSPECIFIC_PROPERTY_CHANGE);
		}
	}

	protected void createObjectLookup() {
		setDomainObjects(new MapObjectLookupClient(this));
	}

	protected abstract void doCascadeDeletes(HasIdAndLocalId hili);

	protected void doubleCheckAddition(Collection collection, Object tgt) {
		collection.add(tgt);
	}

	protected void doubleCheckRemoval(Collection collection, Object tgt) {
		collection.remove(tgt);
	}

	protected Object ensureEndpointInTransformGraph(Object object) {
		return object;
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

	protected HasIdAndLocalId getObjectForCreate(DomainTransformEvent event) {
		return getObject(event);
	}

	protected ObjectLookup getObjectLookup() {
		return Reflections.objectLookup();
	}

	protected Collection getProvisionalObjects() {
		return provisionalObjects.keySet();
	}

	protected boolean ignorePropertyForCaching(Class objectType,
			Class propertyType, String propertyName) {
		return ignorePropertiesForCaching.contains(propertyName)
				|| propertyType == Class.class || !PermissionsManager.get()
						.checkReadable(objectType, propertyName, null);
	}

	protected boolean isZeroCreatedObjectLocalId(Class clazz) {
		return false;
	}

	protected void maybeAddVersionNumbers(DomainTransformEvent evt,
			HasIdAndLocalId obj, Object tgt) {
		if (obj instanceof HasVersionNumber) {
			evt.setObjectVersionNumber(
					((HasVersionNumber) obj).getVersionNumber());
		}
		if (tgt instanceof HasVersionNumber) {
			evt.setValueVersionNumber(
					((HasVersionNumber) tgt).getVersionNumber());
		}
	}

	protected void maybeFireCollectionModificationEvent(
			Class<? extends Object> collectionClass,
			boolean fromPropertyChange) {
	}

	protected void maybeModifyAsPropertyChange(HasIdAndLocalId obj,
			String propertyName, Object newTargetValue,
			CollectionModificationType collectionModificationType) {
		// for clients to force collection modifications to publish as property
		// changes (when replaying remote events)
	}

	protected void objectCreated(HasIdAndLocalId hili) {
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

	protected void performDeleteObject(HasIdAndLocalId hili) {
		if (getDomainObjects() != null) {
			removeAssociations(hili);
			getDomainObjects().deregisterObject(hili);
			maybeFireCollectionModificationEvent(hili.getClass(), false);
		}
	}

	protected void promoteToDomain(Collection objects, boolean deregister) {
		try {
			objects = (Collection) objects.stream().map(o -> {
				if (o instanceof HasTransformPersistable) {
					return ((HasTransformPersistable) o).resolvePersistable();
				} else {
					return o;
				}
			}).collect(Collectors.toList());
			CollectionModificationSupport.queue(true);
			for (Object o : objects) {
				if (o instanceof HasIdAndLocalId && getObjectLookup()
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
					fireCreateObjectEvent(hili.getClass(), 0,
							hili.getLocalId());
				}
			}
			Collection<DomainTransformEvent> trs = getTransformsByCommitType(
					CommitType.TO_LOCAL_BEAN);
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

	protected PropertyAccessor propertyAccessor() {
		return Reflections.propertyAccessor();
	}

	protected abstract void removeAssociations(HasIdAndLocalId hili);

	protected void setDomainObjects(ObjectStore domainObjects) {
		this.domainObjects = domainObjects;
	}

	protected void updateAssociation(DomainTransformEvent evt,
			HasIdAndLocalId obj, Object tgt, boolean remove,
			boolean collectionPropertyChange) {
		Association assoc = obj == null ? null
				: propertyAccessor().getAnnotationForProperty(obj.getClass(),
						Association.class, evt.getPropertyName());
		if (tgt == null || assoc == null
				|| assoc.propertyName().length() == 0) {
			return;
		}
		HasIdAndLocalId hTgt = (HasIdAndLocalId) tgt;
		hTgt = (HasIdAndLocalId) ensureEndpointInTransformGraph(hTgt);
		Object associatedObject = propertyAccessor().getPropertyValue(tgt,
				assoc.propertyName());
		associatedObject = ensureEndpointInTransformGraph(associatedObject);
		boolean assocObjIsCollection = associatedObject instanceof Collection;
		TransformType tt = assocObjIsCollection
				? (remove ? TransformType.REMOVE_REF_FROM_COLLECTION
						: TransformType.ADD_REF_TO_COLLECTION)
				: remove ? TransformType.NULL_PROPERTY_REF
						: TransformType.CHANGE_PROPERTY_REF;
		evt = new DomainTransformEvent();
		evt.setTransformType(tt);
		maybeAddVersionNumbers(evt, obj, tgt);
		// No! Only should check one end of the relation for permissions
		// checkPermissions(hTgt, evt, assoc.propertyName());
		if (assocObjIsCollection) {
			Collection c = (Collection) associatedObject;
			if (collectionPropertyChange && !assoc.silentUpdates()) {
				try {
					c = CommonUtils.shallowCollectionClone(c);
				} catch (Exception e) {
					throw new WrappedRuntimeException(e);
				}
			}
			if (remove) {
				boolean wasContained = c.remove(obj);
				if (!wasContained) {
					doubleCheckRemoval(c, obj);
				}
			} else {
				if (!c.contains(obj)) {
					doubleCheckAddition(c, obj);
				}
			}
			if (collectionPropertyChange && !assoc.silentUpdates()) {
				propertyAccessor().setPropertyValue(tgt, assoc.propertyName(),
						c);
			}
		} else {
			/*
			 * we can get in an ugly loop in the following case: x.parent=p1
			 * x.setParent(p2) (cascade) p1.children.remove(x) (cascade)
			 * x.setParent (null) (cascade) p2.children.remove(x) ... so only
			 * null if the assoc prop is the old value
			 */
			if (remove) {
				Object current = propertyAccessor().getPropertyValue(tgt,
						assoc.propertyName());
				if (current == obj) {
					propertyAccessor().setPropertyValue(tgt,
							assoc.propertyName(), null);
				}
			} else {
				propertyAccessor().setPropertyValue(tgt, assoc.propertyName(),
						obj);
			}
			// shouldn't fire for collection props, probly. also, collection
			// mods are very unlikely to collide in a nasty way (since
			// membership is really just a bitset, and colliding colln mods will
			// often not actually hit each other)
			objectModified(hTgt, evt, true);
		}
	}

	protected boolean updateAssociationsWithoutNoChangeCheck() {
		return true;
	}

	public enum CollectionModificationType {
		ADD, REMOVE
	}

	public static class CommitToLocalDomainTransformListener
			implements DomainTransformListener {
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
				default:
					break;
				}
				tm.setTransformCommitType(evt, CommitType.TO_STORAGE);
			}
		}
	}

	public static class DomainObjectReverseLookup<K extends HasIdAndLocalId, V extends HasIdAndLocalId>
			implements DomainTransformListener {
		private final Class<K> childClass;

		private final Class<V> parentClass;

		private Set<PropertyInfoLite> pils = new LinkedHashSet<PropertyInfoLite>();

		private Multimap<K, List<V>> lookup;

		public DomainObjectReverseLookup(Class<K> childClass,
				Class<V> parentClass) {
			this.childClass = childClass;
			this.parentClass = parentClass;
			TransformManager.get().addDomainTransformListener(this);
		}

		public void detach() {
			TransformManager.get().removeDomainTransformListener(this);
		}

		@Override
		public void domainTransform(DomainTransformEvent evt)
				throws DomainTransformException {
			if (lookup != null) {
				if (evt.getTransformType() == TransformType.NULL_PROPERTY_REF
						|| evt.getTransformType() == TransformType.CHANGE_PROPERTY_REF) {
					if (pils.contains(new PropertyInfoLite(evt.getObjectClass(),
							evt.getPropertyName()))) {
						lookup = null;
					}
				}
				if (evt.provideIsIdEvent(childClass)) {
					lookup = null;
				}
			}
		}

		public List<V> get(K k) {
			ensureLookup();
			return lookup.get(k);
		}

		private void ensureLookup() {
			if (lookup == null) {
				lookup = new Multimap<K, List<V>>();
				pils.clear();
				Map<Class<? extends HasIdAndLocalId>, Collection<HasIdAndLocalId>> m = TransformManager
						.get().getDomainObjects().getCollectionMap();
				for (Class clazz : m.keySet()) {
					if (parentClass != null && parentClass != clazz) {
						continue;
					}
					Collection<HasIdAndLocalId> objs = m.get(clazz);
					if (objs.isEmpty()) {
						continue;
					}
					ClassLookup classLookup = Reflections.classLookup();
					List<PropertyInfoLite> pds = classLookup
							.getWritableProperties(clazz);
					Object templateInstance = classLookup
							.getTemplateInstance(clazz);
					PropertyAccessor accessor = Reflections.propertyAccessor();
					for (Iterator<PropertyInfoLite> itr = pds.iterator(); itr
							.hasNext();) {
						PropertyInfoLite info = itr.next();
						if (info.getPropertyType() != childClass) {
							itr.remove();
						}
					}
					pils.addAll(pds);
					Object[] args = new Object[0];
					try {
						for (V o : (Collection<V>) m.get(clazz)) {
							for (PropertyInfoLite info : pds) {
								K k = (K) info.getReadMethod().invoke(o, args);
								if (k != null) {
									lookup.add(k, o);
								}
							}
						}
					} catch (Exception e) {
						throw new WrappedRuntimeException(e);
					}
				}
			}
		}
	}

	public static class RecordTransformListener
			implements DomainTransformListener {
		private CurrentUtcDateProvider utcDateProvider = Registry
				.impl(CurrentUtcDateProvider.class);

		public void domainTransform(DomainTransformEvent evt) {
			if (evt.getCommitType() == CommitType.TO_LOCAL_BEAN) {
				TransformManager tm = TransformManager.get();
				if (tm.isProvisionalObject(evt.getSource())) {
					return;
				}
				evt.setUtcDate(utcDateProvider.currentUtcDate());
				evt.setEventId(tm.nextEventIdCounter());
				tm.setTransformCommitType(evt, CommitType.TO_LOCAL_GRAPH);
				return;
			}
		}
	}

	public static <V> V resolveMaybeDeserialize(V existing, String serialized,
			V defaultValue) {
		if (existing != null) {
			return existing;
		}
		if (Ax.isBlank(serialized)) {
			return defaultValue;
		}
		return AlcinaBeanSerializer.deserializeHolder(serialized);
	}

	public boolean isIgnoreProperty(String propertyName) {
		return ignorePropertiesForCaching.contains(propertyName);
	}
}
