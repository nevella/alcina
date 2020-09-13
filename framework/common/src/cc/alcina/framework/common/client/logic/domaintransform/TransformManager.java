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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
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
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domain.HasVersionNumber;
import cc.alcina.framework.common.client.logic.domaintransform.CollectionModification.CollectionModificationEvent;
import cc.alcina.framework.common.client.logic.domaintransform.CollectionModification.CollectionModificationListener;
import cc.alcina.framework.common.client.logic.domaintransform.CollectionModification.CollectionModificationSource;
import cc.alcina.framework.common.client.logic.domaintransform.CollectionModification.CollectionModificationSupport;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException.DomainTransformExceptionType;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.MapObjectLookupClient;
import cc.alcina.framework.common.client.logic.domaintransform.spi.ClassLookup;
import cc.alcina.framework.common.client.logic.domaintransform.spi.ClassLookup.PropertyInfo;
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
import cc.alcina.framework.common.client.util.CollectionCreators.ConcurrentMapCreator;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.MultikeyMap;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.common.client.util.SimpleStringParser;
import cc.alcina.framework.common.client.util.SortedMultikeyMap;

/**
 * FIXME - mvcc.adjunct - abstract parts out to ClientTransformManager
 * 
 * <h2>Thread safety notes</h2>
 * <ul>
 * <li>Collection modificiation support is a thread-safe singleton (except for
 * listener add/remove)
 * <li>transformsByType access synchronized
 * </ul>
 *
 * @author nick@alcina.cc
 *
 */
// unchecked because reflection is always going to involve a lot of
// casting...alas
public abstract class TransformManager implements PropertyChangeListener,
		ObjectLookup, CollectionModificationSource {
	protected static final String UNSPECIFIC_PROPERTY_CHANGE = "---";

	public static final String ID_FIELD_NAME = "id";

	public static final String VERSION_FIELD_NAME = "versionNumber";

	public static final transient String CONTEXT_DO_NOT_POPULATE_SOURCE = TransformManager.class
			.getName() + ".CONTEXT_DO_NOT_POPULATE_SOURCE";

	public static final transient String CONTEXT_CONSUME_COLLECTION_MODS_AS_PROPERTY_CHANGES = TransformManager.class
			.getName() + ".CONTEXT_CONSUME_COLLECTION_MODS_AS_PROPERTY_CHANGES";

	protected static final Set<String> ignorePropertiesForCaching = new HashSet<String>(
			Arrays.asList(new String[] { "class", "id", "localId",
					"propertyChangeListeners" }));

	private static long eventIdCounter = 0;

	/*
	 * No localid is allowed past 2 ^ 28 - this gives server apps the headroom
	 * to add cascaded/triggered transforms (during preprocessing)
	 */
	protected static SequentialIdGenerator localIdGenerator = new SequentialIdGenerator(
			1 << 29);

	private static TransformManager theInstance;

	protected static Map<Integer, List<Entity>> createdLocalAndPromoted = null;

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
		str = str.replace("\n", ", ");
		if (!str.matches("[\\-0-9, \r\n\t()\\[\\]]+")) {
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

	public static Set<Long> idListToLongSet(String str) {
		return new LinkedHashSet<>(idListToLongs(str));
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

	// synchronized method because createdLocalAndPromoted is initially null -
	// this method won't be called that often
	public static void registerLocalObjectPromotion(Entity entity) {
		if (createdLocalAndPromoted == null) {
			createdLocalAndPromoted = Registry.impl(ConcurrentMapCreator.class)
					.createMap();
		}
		synchronized (createdLocalAndPromoted) {
			// use the same code as for Entity.hashCode on an object with
			// zero localid, same
			// class and id (for later lookup)
			int withoutLocalIdHash = ((int) entity.getId())
					^ entity.entityClass().getName().hashCode();
			if (withoutLocalIdHash == 0) {
				withoutLocalIdHash = -1;
			}
			createdLocalAndPromoted
					.computeIfAbsent(withoutLocalIdHash, h -> new ArrayList<>())
					.add(entity);
		}
	}

	public static int replaceWithCreatedLocalObjectHash(Entity entity,
			int hash) {
		if (createdLocalAndPromoted == null) {
			return hash;
		}
		List<Entity> promotedEntities = createdLocalAndPromoted.get(hash);
		if (promotedEntities == null) {
			return hash;
		}
		synchronized (promotedEntities) {
			for (Entity promoted : promotedEntities) {
				if (promoted.equals(entity)) {
					return promoted.hashCode();
				}
			}
		}
		return hash;
	}

	public static <V> V resolveMaybeDeserialize(V existing, String serialized,
			V defaultValue) {
		return resolveMaybeDeserialize(existing, serialized, defaultValue,
				s -> AlcinaBeanSerializer.deserializeHolder(s));
	}

	public static <V> V resolveMaybeDeserialize(V existing, String serialized,
			V defaultValue, Function<String, V> deserializer) {
		if (existing != null) {
			return existing;
		}
		if (Ax.isBlank(serialized)) {
			return defaultValue;
		}
		return deserializer.apply(serialized);
	}

	public static String stringId(Entity entity) {
		return entity.getId() != 0 ? entity.getId() + ""
				: entity.getLocalId() + "L";
	}

	public static <E extends Enum> Set<E> toEnumValues(String s,
			Class<E> clazz) {
		Set<E> result = new LinkedHashSet<>();
		if (s != null) {
			for (String sPart : s.split(",")) {
				E value = CommonUtils.getEnumValueOrNull(clazz, sPart);
				if (value == null && sPart.length() > 0) {
					System.out.println(
							Ax.format("Warning - can't deserialize %s for %s",
									sPart, clazz));
				}
				result.add(value);
			}
			result.remove(null);
		}
		return result;
	}

	private ObjectStore domainObjects;

	private Set<DomainTransformEvent> transforms = createTransformSet();

	private Map<CommitType, Set<DomainTransformEvent>> transformsByType = new LinkedHashMap<>();

	protected DomainTransformSupport transformListenerSupport;

	// underlying map must be an identity-, not equals- map
	protected Map<Object, Boolean> provisionalObjects;

	private boolean ignorePropertyChanges;

	private boolean replayingRemoteEvent;

	protected CollectionModificationSupport collectionModificationSupport;

	protected DomainTransformEvent currentEvent;

	private TransformHistoryManager undoManager = new NullUndoManager();

	private boolean ignoreUnrecognizedDomainClassException;

	private boolean associationPropagationDisabled;

	protected Set<Entity> markedForDeletion = new LinkedHashSet<>();

	protected TransformManager() {
		this.transformListenerSupport = new DomainTransformSupport();
		this.collectionModificationSupport = new CollectionModificationSupport();
		initCollections();
	}

	@Override
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

	public void addTransforms(Collection<DomainTransformEvent> transforms,
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

	public void apply(DomainTransformEvent event)
			throws DomainTransformException {
		currentEvent = event;
		ApplyToken token = createApplyToken(event);
		if (!checkPermissions(token.object, event, event.getPropertyName(),
				token.existingTargetValue)) {
			return;
		}
		if (!checkPermissions(token.object, event, event.getPropertyName(),
				token.newTargetValue)) {
			return;
		}
		if (markedForDeletion.contains(token.object)
				|| markedForDeletion.contains(token.newTargetObject)
				|| markedForDeletion.contains(token.existingTargetObject)) {
			throw new DomainTransformException(
					"Modifying object marked for deletion");
		}
		getUndoManager().prepareUndo(event);
		checkVersion(token.object, event);
		switch (token.transformType) {
		case CHANGE_PROPERTY_SIMPLE_VALUE:
		case ADD_REF_TO_COLLECTION:
		case REMOVE_REF_FROM_COLLECTION:
		case CHANGE_PROPERTY_REF:
			if (event.getValueClass() == null) {
				throw new DomainTransformException(
						"null value class for modification requiring a class");
			}
		}
		switch (token.transformType) {
		// these cases will fire a new transform event (temp obj > domain obj),
		// so should not be processed further
		case NULL_PROPERTY_REF:
		case CHANGE_PROPERTY_REF:
		case CHANGE_PROPERTY_SIMPLE_VALUE:
			if (isReplayingRemoteEvent() && token.object == null) {
				// it's been deleted on the client, but we've just now got the
				// creation id - should be very rare occurrence.
				// TODO: notify server
				return;
			}
			propertyAccessor().setPropertyValue(token.object,
					event.getPropertyName(), token.newTargetValue);
			if (event.getPropertyName()
					.equals(TransformManager.ID_FIELD_NAME)) {
				// FIXME - mvcc.3 or 4 (clienttransformmanager rework) - remove
				getDomainObjects().changeMapping(token.object,
						event.getObjectId(), event.getObjectLocalId());
				registerLocalObjectPromotion(token.object);
			}
			// MVCC - needed?
			if (event.getCommitType() == CommitType.TO_LOCAL_BEAN) {
				removeTransform(event);
			}
			objectModified(token.object, event, false);
			switch (token.transformType) {
			case NULL_PROPERTY_REF:
			case CHANGE_PROPERTY_REF:
				if (token.existingTargetValue != token.newTargetValue) {
					if (token.existingTargetValue instanceof Collection) {
						throw new RuntimeException(
								"Should not null a collection property:\n "
										+ event.toString());
					}
				}
				break;
			}
			break;
		// add and removeref will not cause a property change, so no transform
		// removal
		//
		// on the client, if replaying, we want to refire these for the UI to
		// notice ('beforeDirectCollectionModification'). on the server (db
		// commit),
		// it's a lot more efficient to not do that. server-side, mvcc requires
		// a writeable version
		case ADD_REF_TO_COLLECTION: {
			beforeDirectCollectionModification(token.object,
					event.getPropertyName(), token.newTargetValue,
					CollectionModificationType.ADD);
			if (shouldApplyCollectionModification(event)) {
				Set set = (Set) propertyAccessor().getPropertyValue(
						token.object, event.getPropertyName());
				if (!set.contains(token.newTargetValue)) {
					doubleCheckAddition(set, token.newTargetValue);
				}
			}
			objectModified(token.object, event, false);
			collectionChanged(token.object, token.newTargetValue);
		}
			break;
		case REMOVE_REF_FROM_COLLECTION: {
			beforeDirectCollectionModification(token.object,
					event.getPropertyName(), token.newTargetValue,
					CollectionModificationType.REMOVE);
			if (shouldApplyCollectionModification(event)) {
				Set set = (Set) propertyAccessor().getPropertyValue(
						token.object, event.getPropertyName());
				boolean wasContained = set.remove(token.newTargetValue);
				if (!wasContained) {
					doubleCheckRemoval(set, token.newTargetValue);
				}
			}
			collectionChanged(token.object, token.newTargetValue);
			break;
		}
		case DELETE_OBJECT:
			performDeleteObject((Entity) token.object);
			break;
		case CREATE_OBJECT:
			// various possibilities:
			// 1. replaying a server create,(on the client)
			// 2. recording an in-entity-manager create
			// 3. doing a database-regeneration
			// 4. post-process; replaying local-to-vm create
			// 5. post-process; replaying not-local-to-vm create
			// if (2) or (4) , break at this point
			// FIXME - mvcc.adjunct - clean this up further once client
			// localid->id
			// transitions
			// are updated, and provisional -> bubble universe. Some of these
			// cases may
			// not still be here? e.g. (2)
			Entity createdEntity = getEntityForCreate(event);
			if (createdEntity != null) {
				// created locally, most of the registration needs to be handled
				// at the creation site (createDomainObject())
				if (createdEntity.getLocalId() == 0) {
					// increment that version number
					objectModified(createdEntity, event, false);
				}
				maybeFireCollectionModificationEvent(createdEntity.getClass(),
						false);
				break;
			} else {
				long creationLocalId = isZeroCreatedObjectLocalId(
						event.getObjectClass()) ? 0 : event.getObjectLocalId();
				Entity entity = (Entity) classLookup().newInstance(
						event.getObjectClass(), event.getObjectId(),
						event.getObjectLocalId());
				entity.setLocalId(creationLocalId);
				if (entity.getId() == 0 && event.getObjectId() != 0) {// replay
																		// from
																		// server
																		// -
					// huh? unless newInstance does something weird, should
					// never
					// reach here
					entity.setId(event.getObjectId());
				}
				event.setObjectId(entity.getId());
				objectModified(entity, event, false);
				maybeFireCollectionModificationEvent(event.getObjectClass(),
						false);
				if (getDomainObjects() != null) {
					getDomainObjects().mapObject(entity);
				}
			}
			break;
		default:
			assert false : "Transform type not implemented: "
					+ token.transformType;
		}
		currentEvent = null;
	}

	public void appShutdown() {
		theInstance = null;
	}

	public boolean checkForExistingLocallyCreatedObjects() {
		return true;
	}

	public void clearTransforms() {
		getTransforms().clear();
		for (CommitType ct : transformsByType.keySet()) {
			transformsByType.get(ct).clear();
		}
		markedForDeletion.clear();
	}

	public void clearUserObjects() {
		setDomainObjects(null);
	}

	public void commitProvisionalObjects(Collection c) {
		promoteToDomain(c, false);
	}

	public boolean containsObject(DomainTransformEvent dte) {
		Entity obj = getObjectLookup().getObject(dte.getObjectClass(),
				dte.getObjectId(), dte.getObjectLocalId());
		return obj != null;
	}

	public void convertToTargetObject(DomainTransformEvent event) {
		Object value = event.getNewValue();
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
			PropertyInfo pd = classLookup
					.getWritableProperties(event.getObjectClass()).stream()
					.filter(pd1 -> pd1.getPropertyName()
							.equals(event.getPropertyName()))
					.findFirst().get();
			Preconditions.checkArgument(pd.hasSerializeWithBeanSerialization());
			event.setNewStringValue(
					Registry.impl(AlcinaBeanSerializer.class).serialize(value));
			event.setValueClass(String.class);
			return;
		}
		if (value instanceof Entity) {
			Entity entity = (Entity) value;
			if (entity.getId() == 0 && entity.getLocalId() == 0) {
				DomainTransformRuntimeException dtre = new DomainTransformRuntimeException(
						"Set value object with zero id and localid");
				dtre.setEvent(event);
				throw dtre;
			}
			event.setValueId(entity.getId());
			event.setValueLocalId(entity.getLocalId());
			event.setValueClass(entity.entityClass());
			return;
		}
		if (value instanceof Enum) {
			event.setValueClass(((Enum) value).getDeclaringClass());
			// make sure the enum is reflect-instantiable (although not strictly
			// necessary here, it's a common dev problem to miss this
			// annotation, and here is the best place to catch it
			Class clazz = classLookup()
					.getClassForName(event.getValueClassName());
			event.setNewStringValue(((Enum) value).name());
			return;
		}
		Class<? extends Object> valueClass = value.getClass();
		event.setValueClass(valueClass);
		if (valueClass == Integer.class || valueClass == String.class
				|| valueClass == Double.class || valueClass == Float.class
				|| valueClass == Short.class || valueClass == Boolean.class) {
			event.setNewStringValue(value.toString());
		} else if (valueClass == Long.class) {
			event.setNewStringValue(SimpleStringParser.toString((Long) value));
		} else if (valueClass == Date.class) {
			event.setNewStringValue(
					SimpleStringParser.toString((((Date) value).getTime())));
		}
	}

	public <T extends Entity> T createDomainObject(Class<T> objectClass) {
		long localId = nextLocalIdCounter();
		T newInstance = classLookup().newInstance(objectClass, 0, localId);
		Preconditions.checkState(newInstance.getLocalId() != 0);
		registerDomainObject(newInstance);
		fireCreateObjectEvent(objectClass, 0, localId);
		maybeFireCollectionModificationEvent(objectClass, false);
		return newInstance;
	}

	public <T extends Entity> T createProvisionalObject(Class<T> objectClass) {
		long localId = nextLocalIdCounter();
		T newInstance = classLookup().newInstance(objectClass, 0, localId);
		newInstance.setLocalId(localId);
		registerProvisionalObject(newInstance);
		return newInstance;
	}

	public DomainTransformEvent
			createTransformFromPropertyChange(PropertyChangeEvent evt) {
		DomainTransformEvent dte = new DomainTransformEvent();
		dte.setSource((Entity) evt.getSource());
		dte.setNewValue(evt.getNewValue());
		dte.setPropertyName(evt.getPropertyName());
		Entity dObj = (Entity) evt.getSource();
		dte.setObjectId(dObj.getId());
		dte.setObjectLocalId(dObj.getLocalId());
		dte.setObjectClass(dObj.entityClass());
		dte.setTransformType(TransformType.CHANGE_PROPERTY_SIMPLE_VALUE);
		maybeAddVersionNumbers(dte, dObj, evt.getNewValue());
		return dte;
	}

	public boolean currentTransformIsDuringCreationRequest() {
		return currentEvent.getObjectLocalId() != 0;
	}

	/**
	 * If calling from the servlet layer, the object will normally not be
	 * 'found' - so this function variant should be called, with the second
	 * parameter equal to true
	 * 
	 * Order is critical here - fire first (so associations can handle
	 * pre-delete) - then add transform (to queue) - then finally unregister
	 * listeners and remove from backing object cache
	 */
	public DomainTransformEvent delete(Entity entity) {
		if (!generateEventIfObjectNotRegistered(entity)
				&& getObject(entity) == null) {
			return null;
		}
		markedForDeletion.add(entity);
		registerDomainObject(entity);
		DomainTransformEvent dte = new DomainTransformEvent();
		dte.setObjectId(entity.getId());
		dte.setObjectLocalId(entity.getLocalId());
		dte.setObjectClass(entity.entityClass());
		dte.setTransformType(TransformType.DELETE_OBJECT);
		dte.setSource(entity);
		try {
			/*
			 * Order is critical here - fire first (so associations can handle
			 * pre-delete) - then add transform (to queue) - then finally
			 * unregister listeners and remove from backing object cache
			 */
			fireDomainTransform(dte);
			addTransform(dte);
			performDeleteObject(entity);
			return dte;
		} catch (DomainTransformException e) {
			DomainTransformRuntimeException dtre = new DomainTransformRuntimeException(
					e.getMessage());
			dtre.setEvent(e.getEvent());
			throw dtre;
		}
	}

	// FIXME - mvcc.4 - can be removed (since postprocess() ignores
	// created/deleted) - but even better, move that ignore created/deleted to
	// transformpersister
	public void deleteObjectOrRemoveTransformsIfLocal(Entity entity) {
		if (entity.getId() != 0) {
			delete(entity);
			return;
		}
		Set<DomainTransformEvent> toRemove = new LinkedHashSet<DomainTransformEvent>();
		Set<DomainTransformEvent> trs = getTransformsByCommitType(
				CommitType.TO_LOCAL_BEAN);
		for (DomainTransformEvent dte : trs) {
			Entity source = dte.getSource() != null ? dte.getSource()
					: getObject(dte);
			if (entity.equals(source)) {
				toRemove.add(dte);
			}
			if (dte.getValueId() != 0 || dte.getValueLocalId() != 0) {
				Entity object = getObjectLookup().getObject(dte.getValueClass(),
						dte.getValueId(), dte.getValueLocalId());
				if (entity.equals(object)) {
					toRemove.add(dte);
				}
			}
		}
		trs.removeAll(toRemove);
		transforms.removeAll(toRemove);
	}

	public void deregisterDomainObject(Entity entity) {
		deregisterDomainObjects(CommonUtils.wrapInCollection(entity));
	}

	public void deregisterDomainObjects(Collection<Entity> entities) {
		if (getDomainObjects() != null) {
			getDomainObjects().deregisterObjects(entities);
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

	public <V extends Entity> V ensure(Collection<V> instances, Class<V> clazz,
			String key, Object value, Entity parent,
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

	public <V extends Entity> List<V> filter(Class<V> clazz,
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

	public <V extends Entity> V find(Class<V> clazz, String key, Object value) {
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
		try {
			this.transformListenerSupport.fireDomainTransform(event);
		} catch (DomainTransformException e) {
			// if (e.getType() ==
			// DomainTransformExceptionType.SOURCE_ENTITY_NOT_FOUND) {
			// Mvcc.debugSourceNotFound(e);
			// }
			throw e;
		}
	}

	public <V extends Entity> List<V> fromIdList(Class<V> clazz, String idStr) {
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

	// FIXME - mvcc.adjunct - get rid of objectstore vs objectlookup?
	// Objectlookup
	// should probably always go via tm
	public ObjectStore getDomainObjects() {
		return this.domainObjects;
	}

	/**
	 * useful support in TLTM, ThreadedClientTM
	 */
	public <H extends Entity> long getLocalIdForClientInstance(H entity) {
		return entity.getLocalId();
	}

	@Override
	public <T extends Entity> T getObject(Class<? extends T> c, long id,
			long localId) {
		if (this.getDomainObjects() != null) {
			return getDomainObjects().getObject(c, id, localId);
		}
		return null;
	}

	public Entity getObject(DomainTransformEvent dte) {
		return getObject(dte, false);
	}

	public Entity getObject(DomainTransformEvent dte, boolean ignoreSource) {
		Entity obj = getObject(dte.getObjectClass(), dte.getObjectId(),
				dte.getObjectLocalId());
		if (obj == null && (ignoreSource
				|| LooseContext.is(CONTEXT_DO_NOT_POPULATE_SOURCE))) {
			return null;
		}
		if (obj == null && dte.getSource() != null) {
			// if create, natural behaviour is return null, ignoring source
			if (dte.getTransformType() != TransformType.CREATE_OBJECT
					&& dte.getTransformType() != TransformType.DELETE_OBJECT) {
				String message = Ax.format(
						"getObject() returned null - possibly uncommitted object on different thread? "
								+ "Calling getobject() on a provisional/deregistered object transform "
								+ "- will harm the transform. use getsource() - \n%s\n",
						dte);
				throw new RuntimeException(new DomainTransformException(dte,
						DomainTransformExceptionType.UNKNOWN, message));
			}
		}
		dte.setSource(obj);
		return obj;
	}

	public <T extends Entity> T getObject(EntityLocator entityLocator) {
		return (T) getObject(entityLocator.getClazz(), entityLocator.getId(),
				entityLocator.localId);
	}

	@Override
	public <T extends Entity> T getObject(T entity) {
		return (T) getObjectLookup().getObject(entity.entityClass(),
				entity.getId(), entity.getLocalId());
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
				e.printStackTrace();
			}
		}
		return null;
	}

	public Object getTargetObject(DomainTransformEvent evt, boolean oldValue)
			throws DomainTransformException {
		return getTargetObjectWithLookup(evt, getObjectLookup(), oldValue);
	}

	public Object getTargetObjectWithLookup(DomainTransformEvent evt,
			ObjectLookup objectLookup, boolean oldValue)
			throws DomainTransformException {
		Class valueClass = evt.getValueClass();
		if (evt.getNewValue() != null || valueClass == null) {
			if (evt.getNewValue() instanceof Entity) {
				Entity entity = objectLookup
						.getObject((Entity) evt.getNewValue());
				if (entity != null) {
					return entity;
				} else {
					// this is perfectly possible - particularly on the client.
					// allow it there, to save lots of unhelpful
					// register/deregister boilerplate
					if (!allowUnregisteredEntityTargetObject()) {
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
			PropertyInfo pd = classLookup
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
			Entity object = objectLookup.getObject(valueClass, evt.getValueId(),
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

	public synchronized Set<DomainTransformEvent>
			getTransformsByCommitType(CommitType ct) {
		if (transformsByType.get(ct) == null) {
			transformsByType.put(ct, createTransformSet());
		}
		return transformsByType.get(ct);
	}

	public TransformHistoryManager getUndoManager() {
		return undoManager;
	}

	public boolean handlesAssociationsFor(Class clazz) {
		return !associationPropagationDisabled;
	}

	public boolean hasTransforms() {
		return getTransforms().size() > 0;
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

	public boolean isIgnoreProperty(String propertyName) {
		return ignorePropertiesForCaching.contains(propertyName);
	}

	public boolean isIgnorePropertyChanges() {
		return this.ignorePropertyChanges;
	}

	public boolean isIgnoreUnrecognizedDomainClassException() {
		return this.ignoreUnrecognizedDomainClassException;
	}

	public boolean isInCreationRequest(Entity hasOwner) {
		return false;
	}

	public <T extends Entity> boolean isProvisionalObject(final T object) {
		if (getProvisionalObjects().contains(object)) {
			for (Object o : getProvisionalObjects()) {
				if (o == object) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean isRegistered(Entity entity) {
		Entity registered = getObject(entity);
		return registered == entity;
	}

	public boolean isReplayingRemoteEvent() {
		return this.replayingRemoteEvent;
	}

	public void modifyCollectionProperty(Object objectWithCollection,
			String collectionPropertyName, Object delta,
			CollectionModificationType modificationType) {
		Collection old = (Collection) propertyAccessor()
				.getPropertyValue(objectWithCollection, collectionPropertyName);
		if (!(delta instanceof Collection)) {
			switch (modificationType) {
			case ADD:
				if (old.contains(delta)) {
					return;
				}
				break;
			case REMOVE:
				if (!old.contains(delta)) {
					return;
				}
				break;
			}
		}
		Collection deltaC = CommonUtils.wrapInCollection(delta);
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

	public boolean objectHasTransforms(Entity entity) {
		return getTransforms().stream().anyMatch(
				transform -> transform.toObjectLocator().matches(entity)
						|| transform.toValueLocator().matches(entity));
	}

	public List<DomainTransformEvent> objectsToDtes(Collection objects,
			Class clazz, boolean asObjectSpec) throws Exception {
		ClassLookup classLookup = classLookup();
		List<PropertyInfo> pds = classLookup.getWritableProperties(clazz);
		Object templateInstance = classLookup.getTemplateInstance(clazz);
		PropertyAccessor accessor = propertyAccessor();
		Map<String, Object> defaultValues = new HashMap();
		Set<Class> implementsEntity = new HashSet<Class>();
		for (Iterator<PropertyInfo> itr = pds.iterator(); itr.hasNext();) {
			PropertyInfo info = itr.next();
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
					if (template instanceof Entity) {
						implementsEntity.add(info.getPropertyType());
					}
				} catch (Exception e) {
					// probably primitive - ignore
				}
			}
		}
		List<DomainTransformEvent> dtes = new ArrayList<DomainTransformEvent>();
		for (Object o : objects) {
			Object[] arr = asObjectSpec ? (Object[]) o : null;
			Entity entity = asObjectSpec ? null : (Entity) o;
			DomainTransformEvent dte = new DomainTransformEvent();
			dte.setSource(null);
			dte.setUtcDate(new Date(0L));
			Long id = asObjectSpec ? (Long) arr[0] : entity.getId();
			long localId = id == 0 ? entity.getLocalId() : 0L;
			dte.setObjectId(id);
			dte.setObjectLocalId(localId);
			dte.setObjectClass(clazz);
			dte.setTransformType(TransformType.CREATE_OBJECT);
			dtes.add(dte);
			int i = 1;
			for (PropertyInfo pd : pds) {
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
							if (o2 instanceof Entity) {
								Entity h2 = (Entity) o2;
								dte.setNewValue(null);
								dte.setValueId(h2.getId());
								dte.setValueLocalId(h2.getLocalId());
								dte.setValueClass(h2.entityClass());
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
					dte.setObjectClass(clazz);
					dte.setObjectId(id);
					dte.setObjectLocalId(localId);
					if (value instanceof Timestamp) {
						value = new Date(((Timestamp) value).getTime());
					}
					dte.setNewValue(value);
					dte.setPropertyName(propertyName);
					if (!implementsEntity.contains(propertyType)) {
						convertToTargetObject(dte);
						dte.setTransformType(
								TransformType.CHANGE_PROPERTY_SIMPLE_VALUE);
					} else {
						dte.setValueClass(propertyType);
						long valueId = asObjectSpec ? (Long) value
								: ((Entity) value).getId();
						long valueLocalId = valueId == 0L
								? ((Entity) value).getLocalId()
								: 0L;
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
	 * @return the newly promoted object, if it implements Entity, otherwise
	 *         null
	 */
	public <T extends Object> T promoteToDomainObject(T o) {
		promoteToDomain(CommonUtils.wrapInCollection(o), true);
		if (o instanceof Entity) {
			return (T) getObject((Entity) o);
		}
		return null;
	}

	@Override
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
					? evt.getOldValue()
					: typeCheck);
			// Note, we explicitly clear nulls here - it would require an
			// expansion of the protocols to implement them
			if (typeCheck.iterator().hasNext()) {
				if (typeCheck.iterator().next() instanceof Entity) {
					Set<Entity> oldValues = (Set) evt.getOldValue();
					Set<Entity> newValues = (Set) evt.getNewValue();
					oldValues.remove(null);
					newValues.remove(null);
					for (Entity entity : newValues) {
						if (!oldValues.contains(entity)) {
							dte = createTransformFromPropertyChange(evt);
							dte.setNewValue(null);
							dte.setValueId(entity.getId());
							dte.setValueLocalId(entity.getLocalId());
							dte.setValueClass(entity.entityClass());
							dte.setTransformType(
									TransformType.ADD_REF_TO_COLLECTION);
							transforms.add(dte);
						}
					}
					for (Entity entity : oldValues) {
						if (!newValues.contains(entity)) {
							dte = createTransformFromPropertyChange(evt);
							dte.setNewValue(null);
							dte.setValueId(entity.getId());
							dte.setValueLocalId(entity.getLocalId());
							dte.setValueClass(entity.entityClass());
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
		Entity entity = (Entity) evt.getSource();
		if (this.getDomainObjects() != null) {
			if (!provisionalObjects.containsKey(entity)) {
				maybeFireCollectionModificationEvent(entity.entityClass(),
						true);
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

	public <T extends Entity> T registerDomainObject(T entity) {
		if (getDomainObjects() != null && entity != null) {
			if (entity.getId() == 0) {
				Entity createdObject = getDomainObjects().getObject(entity);
				if (createdObject != null) {
					getDomainObjects().deregisterObject(createdObject);
				}
			}
			getDomainObjects().mapObject(entity);
		}
		return entity;
	}

	public void registerDomainObjectIfNonProvisional(Entity entity) {
		if (!provisionalObjects.containsKey(entity)) {
			registerDomainObject(entity);
		}
	}

	public void registerDomainObjects(Collection<? extends Entity> entities) {
		for (Entity entity : entities) {
			registerDomainObject(entity);
		}
	}

	public void registerDomainObjectsAsync(Collection<Entity> entities,
			final AsyncCallback<Void> postRegisterCallback) {
		((MapObjectLookupClient) getDomainObjects()).registerAsync(entities,
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

	public <V extends Entity> Set<V> registeredObjectsAsSet(Class<V> clazz) {
		return new LinkedHashSet<V>(getDomainObjects().getCollection(clazz));
	}

	public <V extends Entity> V registeredSingleton(Class<V> clazz) {
		Collection<V> c = getDomainObjects().getCollection(clazz);
		return c.isEmpty() ? null : c.iterator().next();
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
		registerProvisionalObjects(CommonUtils.wrapInCollection(o));
	}

	public void registerProvisionalObjects(Collection c) {
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

	@Override
	public void removeCollectionModificationListener(
			CollectionModificationListener listener) {
		this.collectionModificationSupport
				.removeCollectionModificationListener(listener);
	}

	public int removeCreateDeleteTransforms() {
		Set<DomainTransformEvent> events = getTransformsByCommitType(
				CommitType.TO_LOCAL_BEAN);
		TransformCollation collation = new TransformCollation(
				events.stream().collect(Collectors.toList()));
		int initial = events.size();
		// collect/stream to avoid concurrent modification exception
		events.stream().collect(Collectors.toList()).stream()
				.filter(collation::isCreatedAndDeleted)
				.forEach(this::removeTransform);
		return initial - events.size();
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
		Set<DomainTransformEvent> trs = new LinkedHashSet<>(
				getTransformsByCommitType(CommitType.TO_LOCAL_BEAN));
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

	public void setAssociationPropagationDisabled(
			boolean associationPropagationDisabled) {
		this.associationPropagationDisabled = associationPropagationDisabled;
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

	public String toIdList(Collection<? extends Entity> entities) {
		StringBuffer sb = new StringBuffer();
		for (Entity entity : entities) {
			if (sb.length() != 0) {
				sb.append(", ");
			}
			sb.append(entity.getId());
		}
		return sb.toString();
	}

	protected boolean allowUnregisteredEntityTargetObject() {
		return false;
	}

	protected boolean alwaysFireObjectOwnerCollectionModifications() {
		return false;
	}

	protected void beforeDirectCollectionModification(Entity obj,
			String propertyName, Object newTargetValue,
			CollectionModificationType collectionModificationType) {
		// for clients to force collection modifications to publish as property
		// changes (when replaying remote events). mvcc, force a tx-writeable
		// version of the object
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
	protected boolean checkPermissions(Entity eventTarget,
			DomainTransformEvent evt, String propertyName, Object change) {
		return true;
	}

	protected void checkVersion(Entity obj, DomainTransformEvent event)
			throws DomainTransformException {
	}

	protected ClassLookup classLookup() {
		return Reflections.classLookup();
	}

	protected void collectionChanged(Object obj, Object tgt) {
		// changes won't be noticed unless we do this -
		//
		// FIXME - mvcc.4 - maybe can get rid
		// of this (check if all wrapperpersistable changes use new collections)
		if (obj instanceof WrapperPersistable) {
			((WrapperPersistable) obj)
					.fireUnspecifiedPropertyChange(UNSPECIFIC_PROPERTY_CHANGE);
		}
	}

	protected void createObjectLookup() {
		setDomainObjects(new MapObjectLookupClient(this));
	}

	// underlying set must be ordered
	protected Set<DomainTransformEvent> createTransformSet() {
		return new LinkedHashSet<>();
	}

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

	protected boolean generateEventIfObjectNotRegistered(Entity entity) {
		return false;
	}

	protected Entity getEntityForCreate(DomainTransformEvent event) {
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

	protected void initCollections() {
		provisionalObjects = new IdentityHashMap<>();
	}

	protected boolean isZeroCreatedObjectLocalId(Class clazz) {
		return false;
	}

	protected void maybeAddVersionNumbers(DomainTransformEvent evt, Entity obj,
			Object tgt) {
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

	/**
	 * for subclasses to handle version increments
	 *
	 * @param entity
	 * @param evt
	 */
	protected void objectModified(Entity entity, DomainTransformEvent evt,
			boolean targetObject) {
	}

	protected void performDeleteObject(Entity entity) {
		if (getDomainObjects() != null) {
			getDomainObjects().deregisterObject(entity);
			maybeFireCollectionModificationEvent(entity.entityClass(), false);
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
				if (o instanceof Entity
						&& getObjectLookup().getObject((Entity) o) == null) {
					Entity entity = (Entity) o;
					// if this is a new object, we want to register a blank
					// object,
					// so
					// property changes are played back properly against it
					// Entity newInstance = (Entity)
					// GWTDomainReflector
					// .get().newInstance(o.getClass(), 0);
					// newInstance.setLocalId(((Entity)
					// o).getLocalId());
					// TransformManager.get().registerObject(newInstance);
					// actually, this should ALL be done by event - consume
					// note 2. it could be. but creation of provisional objects
					// doesn't generate a "create" event...no, in fact current
					// way
					// is better. so ignore all this. it works, it's fine
					// (come adjunct TM this all ggoes away)
					Entity newInstance = (Entity) Reflections.classLookup()
							.newInstance(o.getClass());
					newInstance.setLocalId(entity.getLocalId());
					TransformManager.get().registerDomainObject(newInstance);
					fireCreateObjectEvent(entity.entityClass(), 0,
							entity.getLocalId());
				}
			}
			Collection<DomainTransformEvent> trs = getTransformsByCommitType(
					CommitType.TO_LOCAL_BEAN);
			trs = (Set) ((LinkedHashSet) trs).clone();
			deregisterProvisionalObjects(objects);
			for (DomainTransformEvent event : trs) {
				if (objects.contains(event.getSource())) {
					try {
						ApplyToken token = createApplyToken(event);
						switch (event.getTransformType()) {
						case ADD_REF_TO_COLLECTION:
							modifyCollectionProperty(token.object,
									event.getPropertyName(),
									token.newTargetObject,
									CollectionModificationType.ADD);
							break;
						case REMOVE_REF_FROM_COLLECTION:
							modifyCollectionProperty(token.object,
									event.getPropertyName(),
									token.newTargetObject,
									CollectionModificationType.REMOVE);
							break;
						default:
							apply(event);
							break;
						}
					} catch (Exception e) {
						throw new WrappedRuntimeException(e);
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

	protected void setDomainObjects(ObjectStore domainObjects) {
		this.domainObjects = domainObjects;
	}

	protected boolean
			shouldApplyCollectionModification(DomainTransformEvent event) {
		return true;
	}

	protected void updateAssociation(DomainTransformEvent evt, Entity object,
			Entity targetObject, boolean remove) {
		Association assoc = object == null ? null
				: propertyAccessor().getAnnotationForProperty(
						object.entityClass(), Association.class,
						evt.getPropertyName());
		if (targetObject == null || assoc == null
				|| assoc.propertyName().length() == 0) {
			return;
		}
		if (markedForDeletion.contains(targetObject)) {
			return;
		}
		targetObject = (Entity) ensureEndpointInTransformGraph(targetObject);
		Object associatedObject = propertyAccessor()
				.getPropertyValue(targetObject, assoc.propertyName());
		associatedObject = ensureEndpointInTransformGraph(associatedObject);
		boolean assocObjIsCollection = associatedObject instanceof Collection;
		TransformType tt = assocObjIsCollection
				? (remove ? TransformType.REMOVE_REF_FROM_COLLECTION
						: TransformType.ADD_REF_TO_COLLECTION)
				: remove ? TransformType.NULL_PROPERTY_REF
						: TransformType.CHANGE_PROPERTY_REF;
		evt = new DomainTransformEvent();
		evt.setTransformType(tt);
		maybeAddVersionNumbers(evt, object, targetObject);
		// No! Only should check one end of the relation for permissions
		// checkPermissions(hTgt, evt, assoc.propertyName());
		if (assocObjIsCollection) {
			Collection c = (Collection) associatedObject;
			try {
				c = CommonUtils.shallowCollectionClone(c);
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
			if (remove) {
				boolean wasContained = c.remove(object);
				if (!wasContained) {
					doubleCheckRemoval(c, object);
				}
			} else {
				if (!c.contains(object)) {
					doubleCheckAddition(c, object);
				}
			}
			propertyAccessor().setPropertyValue(targetObject,
					assoc.propertyName(), c);
		} else {
			/*
			 * we can get in an ugly loop in the following case: x.parent=p1
			 * x.setParent(p2) (cascade) p1.children.remove(x) (cascade)
			 * x.setParent (null) (cascade) p2.children.remove(x) ... so only
			 * null if the assoc prop is the old value
			 */
			if (remove) {
				Object current = propertyAccessor()
						.getPropertyValue(targetObject, assoc.propertyName());
				if (current == object) {
					propertyAccessor().setPropertyValue(targetObject,
							assoc.propertyName(), null);
				}
			} else {
				propertyAccessor().setPropertyValue(targetObject,
						assoc.propertyName(), object);
			}
			// shouldn't fire for collection props, probly. also, collection
			// mods are very unlikely to collide in a nasty way (since
			// membership is really just a bitset, and colliding colln mods will
			// often not actually hit each other)
			//
			// "probably" means "at the moment we don't fire - i.e. don't mark
			// the target object as updated"
			objectModified(targetObject, evt, true);
		}
	}

	ApplyToken createApplyToken(DomainTransformEvent event)
			throws DomainTransformException {
		return new ApplyToken(event);
	}

	public enum CollectionModificationType {
		ADD, REMOVE
	}

	public static class CommitToLocalDomainTransformListener
			implements DomainTransformListener {
		AssociationPropagationTransformListener associationPropagation = new AssociationPropagationTransformListener(
				CommitType.TO_LOCAL_GRAPH);

		/**
		 * Until 23/11/2010, case NULL_PROPERTY_REF: case CHANGE_PROPERTY_REF:
		 * were not in the case
		 *
		 * I think that's in error - but checking. Basically, the transforms
		 * will be ignored if they're a double-dip (the property won't change)
		 */
		@Override
		public void domainTransform(DomainTransformEvent evt) {
			if (evt.getCommitType() == CommitType.TO_LOCAL_GRAPH) {
				TransformManager tm = TransformManager.get();
				try {
					associationPropagation.domainTransform(evt);
				} catch (Exception e) {
					throw new WrappedRuntimeException(e);
				}
				tm.setTransformCommitType(evt, CommitType.TO_STORAGE);
			}
		}
	}

	public static class DomainObjectReverseLookup<K extends Entity, V extends Entity>
			implements DomainTransformListener {
		private final Class<K> childClass;

		private final Class<V> parentClass;

		private Set<PropertyInfo> pils = new LinkedHashSet<PropertyInfo>();

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
					if (pils.contains(new PropertyInfo(evt.getObjectClass(),
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
				Map<Class<? extends Entity>, Collection<Entity>> m = TransformManager
						.get().getDomainObjects().getCollectionMap();
				for (Class clazz : m.keySet()) {
					if (parentClass != null && parentClass != clazz) {
						continue;
					}
					Collection<Entity> objs = m.get(clazz);
					if (objs.isEmpty()) {
						continue;
					}
					ClassLookup classLookup = Reflections.classLookup();
					List<PropertyInfo> pds = classLookup
							.getWritableProperties(clazz);
					Object templateInstance = classLookup
							.getTemplateInstance(clazz);
					PropertyAccessor accessor = Reflections.propertyAccessor();
					for (Iterator<PropertyInfo> itr = pds.iterator(); itr
							.hasNext();) {
						PropertyInfo info = itr.next();
						if (info.getPropertyType() != childClass) {
							itr.remove();
						}
					}
					pils.addAll(pds);
					Object[] args = new Object[0];
					try {
						for (V o : (Collection<V>) m.get(clazz)) {
							for (PropertyInfo info : pds) {
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
		@Override
		public void domainTransform(DomainTransformEvent evt) {
			if (evt.getCommitType() == CommitType.TO_LOCAL_BEAN) {
				TransformManager tm = TransformManager.get();
				if (tm.isProvisionalObject(evt.getSource())) {
					return;
				}
				/*
				 * Not 'UTC' date! No such thing exists - just the epoch date.
				 */
				evt.setUtcDate(new Date());
				evt.setEventId(tm.nextEventIdCounter());
				tm.setTransformCommitType(evt, CommitType.TO_LOCAL_GRAPH);
				return;
			}
		}
	}

	class ApplyToken {
		Entity object;

		TransformType transformType;

		private Object existingTargetValue;

		Entity existingTargetObject;

		private Object newTargetValue;

		Entity newTargetObject;

		ApplyToken(DomainTransformEvent event) throws DomainTransformException {
			transformType = event.getTransformType();
			if (transformType != TransformType.CREATE_OBJECT
					|| checkForExistingLocallyCreatedObjects()) {
				object = getObject(event);
			}
			if (object == null) {
				/*
				 * Created objects will already be in the graph if locally
				 * created
				 */
				if (transformType != TransformType.CREATE_OBJECT) {
					throw new DomainTransformException(event,
							DomainTransformExceptionType.SOURCE_ENTITY_NOT_FOUND);
				}
			}
			existingTargetValue = null;
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
			existingTargetObject = null;
			if (existingTargetValue instanceof Entity) {
				existingTargetObject = (Entity) existingTargetValue;
			}
			newTargetValue = transformType == null ? null
					: getTargetObject(event, false);
			newTargetValue = ensureEndpointInTransformGraph(newTargetValue);
			newTargetObject = null;
			if (newTargetValue instanceof Entity) {
				newTargetObject = (Entity) newTargetValue;
			}
		}
	}
}
