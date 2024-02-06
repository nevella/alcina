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
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.totsp.gwittir.client.beans.SourcesPropertyChangeEvents;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domain.HasVersionNumber;
import cc.alcina.framework.common.client.logic.domaintransform.CollectionModification.CollectionModificationEvent;
import cc.alcina.framework.common.client.logic.domaintransform.CollectionModification.CollectionModificationListener;
import cc.alcina.framework.common.client.logic.domaintransform.CollectionModification.CollectionModificationSource;
import cc.alcina.framework.common.client.logic.domaintransform.CollectionModification.CollectionModificationSupport;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException.DomainTransformExceptionType;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.StandaloneObjectStoreClient;
import cc.alcina.framework.common.client.logic.domaintransform.spi.ObjectStore;
import cc.alcina.framework.common.client.logic.domaintransform.undo.NullUndoManager;
import cc.alcina.framework.common.client.logic.domaintransform.undo.TransformHistoryManager;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.logic.reflection.Association;
import cc.alcina.framework.common.client.logic.reflection.DomainProperty;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.reflection.ClassReflector;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer;
import cc.alcina.framework.common.client.util.AlcinaBeanSerializer;
import cc.alcina.framework.common.client.util.AlcinaTopics;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CollectionCreators.ConcurrentMapCreator;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.MultikeyMap;
import cc.alcina.framework.common.client.util.SimpleStringParser;
import cc.alcina.framework.common.client.util.SortedMultikeyMap;

/**
 * FIXME - mvcc.adjunct - abstract parts out to ClientTransformManager. Also
 * check all methods referenced (there are some helper methods probably no
 * longer used)
 *
 * <h2>Thread safety notes</h2>
 * <ul>
 * <li>Collection modificiation support is a thread-safe singleton (except for
 * listener add/remove)
 * <li>transformsByType access synchronized
 * </ul>
 *
 * FIXME - reflection - clean up all threadlocal instance access (correctly name
 * the singleton/provider, cleanup on alcinaparallel exit(
 *
 *
 */
// unchecked because reflection is always going to involve a lot of
// casting...alas
public abstract class TransformManager
		implements PropertyChangeListener, CollectionModificationSource {
	protected static final String UNSPECIFIC_PROPERTY_CHANGE = "---";

	public static final String ID_FIELD_NAME = "id";

	public static final String VERSION_FIELD_NAME = "versionNumber";

	public static final transient String CONTEXT_DO_NOT_POPULATE_SOURCE = TransformManager.class
			.getName() + ".CONTEXT_DO_NOT_POPULATE_SOURCE";

	public static final transient String CONTEXT_CONSUME_COLLECTION_MODS_AS_PROPERTY_CHANGES = TransformManager.class
			.getName() + ".CONTEXT_CONSUME_COLLECTION_MODS_AS_PROPERTY_CHANGES";

	public static final transient String CONTEXT_IN_SERIALIZE_PROPERTY_CHANGE_CYCLE = TransformManager.class
			.getName() + ".CONTEXT_IN_SERIALIZE_PROPERTY_CHANGE_CYCLE";

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

	private static TransformManager factoryInstance;

	protected static Map<Integer, List<Entity>> createdLocalAndPromoted = null;

	public static void convertToTargetObject(DomainTransformEvent event) {
		Object value = event.getNewValue();
		if (value == null) {
			return;
		}
		// this dte will never be used - it'll be converted to a series of
		// add/remove refs
		if (value instanceof Set) {
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
			Class clazz = Reflections.forName(event.getValueClassName());
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

	public static DomainTransformEvent createTransformEvent() {
		DomainTransformEvent event = new DomainTransformEvent();
		/*
		 * Not 'UTC' date! No such thing exists - just the epoch date.
		 */
		event.setUtcDate(new Date());
		event.setEventId(nextEventIdCounter());
		return event;
	}

	public static <V> V deserialize(String serialized) {
		return Serializer.get().deserialize(serialized);
	}

	public static String fromEnumValueCollection(Collection objects) {
		return CommonUtils.join(objects, ",");
	}

	public static String fromEnumValues(Object... objects) {
		return CommonUtils.join(objects, ",");
	}

	public static TransformManager get() {
		if (factoryInstance == null) {
			// well, throw an exception.
		}
		TransformManager tm = factoryInstance.getT();
		if (tm != null) {
			return tm;
		}
		return factoryInstance;
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
		return factoryInstance != null;
	}

	public static List<Long> idListToLongs(String str) {
		return idListToLongs(str, false);
	}

	public static List<Long> idListToLongs(String str,
			boolean includeNonPositive) {
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
				if (value > 0 || includeNonPositive) {
					result.add(value);
				}
			}
		}
		return result;
	}

	public static Set<Long> idListToLongSet(String str) {
		return new LinkedHashSet<>(idListToLongs(str));
	}

	public static void ignoreChanges(Runnable runnable) {
		boolean ignorePropertyChanges = get().isIgnorePropertyChanges();
		try {
			TransformManager.get().setIgnorePropertyChanges(true);
			runnable.run();
		} finally {
			TransformManager.get()
					.setIgnorePropertyChanges(ignorePropertyChanges);
		}
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

	public static synchronized long nextEventIdCounter() {
		return ++eventIdCounter;
	}

	public static void register(TransformManager tm) {
		factoryInstance = tm;
	}

	/**
	 * This code ensures (with {@link #replaceWithCreatedLocalObjectHash} that a
	 * persistent entity has the same hashcode as the original hash of the
	 * created local object that was promoted to that entity, if any
	 */
	public static void registerLocalObjectPromotion(Entity entity) {
		if (createdLocalAndPromoted == null) {
			synchronized (TransformManager.class) {
				if (createdLocalAndPromoted == null) {
					createdLocalAndPromoted = Registry
							.impl(ConcurrentMapCreator.class).create();
				}
			}
		}
		synchronized (createdLocalAndPromoted) {
			// use the same code as for Entity.hashCode on an object with
			// zero localid, same
			// class and id (for subsequent lookup)
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

	public static void removePerThreadContext() {
		if (factoryInstance == null) {
			return;
		}
		factoryInstance.removePerThreadContext0();
	}

	public static int replaceWithCreatedLocalObjectHash(Entity entity,
			int hash) {
		if (createdLocalAndPromoted == null || !get().isUseCreatedLocals()) {
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
				null);
	}

	public static <V> V resolveMaybeDeserialize(V existing, String serialized,
			V defaultValue, Class<V> clazz) {
		return resolveMaybeDeserialize(existing, serialized, defaultValue,
				clazz, s -> Serializer.get().deserialize(serialized, clazz));
	}

	private static <V> V resolveMaybeDeserialize(V existing, String serialized,
			V defaultValue, Class<V> clazz, Function<String, V> deserializer) {
		if (existing != null) {
			return existing;
		}
		if (serialized == null
				|| (serialized.isEmpty() && defaultValue != null)) {
			return defaultValue;
		}
		return deserializer.apply(serialized);
	}

	public static String serialize(Object object) {
		return serialize(object, false);
	}

	public static String serialize(Object object,
			boolean hasClassNameProperty) {
		return Serializer.get().serialize(object, hasClassNameProperty);
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

	/**
	 *
	 * <p>
	 * Non-relational objects are -mostly- set-once - this method adds more
	 * support for mutable non-relationals Further support would involve an
	 * abstract base 'MutableNonRelational' class, which would receive
	 * propertychange events and fire this method on the NonRelational's owning
	 * entity
	 *
	 * <p>
	 * But....that would remove developer control over how often the
	 * NonRelational was serialized - which may not be desirable
	 */
	public static void updateSerialized(Entity entity) {
		Reflections.at(entity).properties().forEach(property -> {
			if (property.has(DomainProperty.class)
					&& property.annotation(DomainProperty.class).serialize()) {
				// same logic as #handleCascadedPropertyChange
				SerializablePropertyGroup serializablePropertyGroup = new SerializablePropertyGroup(
						property);
				try {
					LooseContext.pushWithTrue(
							CONTEXT_IN_SERIALIZE_PROPERTY_CHANGE_CYCLE);
					boolean hasClassNameProperty = serializablePropertyGroup.className != null;
					Object value = property.get(entity);
					serializablePropertyGroup.serialized.set(entity,
							serialize(value, hasClassNameProperty));
					if (hasClassNameProperty) {
						serializablePropertyGroup.className.set(entity,
								value == null ? null
										: value.getClass().getName());
					}
				} finally {
					LooseContext.pop();
				}
			}
		});
	}

	private boolean useCreatedLocals = true;

	private ObjectStore objectStore;

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

	protected void addTransformsFromPropertyChange(
			List<DomainTransformEvent> transforms) {
		for (DomainTransformEvent transform : transforms) {
			transform.setInImmediatePropertyChangeCommit(true);
		}
		addTransforms(transforms, true);
		for (DomainTransformEvent transform : transforms) {
			transform.setInImmediatePropertyChangeCommit(false);
		}
	}

	protected boolean allowUnregisteredEntityTargetObject() {
		return false;
	}

	protected boolean alwaysFireObjectOwnerCollectionModifications() {
		return false;
	}

	public void apply(DomainTransformEvent event)
			throws DomainTransformException {
		currentEvent = event;
		ApplyToken token = createApplyToken(event);
		// permissions don't apply to sets, so use xxTargetEntity
		if (!checkPermissions(token.object, event, event.getPropertyName(),
				token.existingTargetEntity)) {
			return;
		}
		if (!checkPermissions(token.object, event, event.getPropertyName(),
				token.existingTargetEntity)) {
			return;
		}
		if (markedForDeletion.contains(token.object)
				|| markedForDeletion.contains(token.newTargetEntity)
				|| markedForDeletion.contains(token.existingTargetEntity)) {
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
			set(token.property(), token.object, token.newTargetObject);
			if (event.getPropertyName()
					.equals(TransformManager.ID_FIELD_NAME)) {
				// FIXME - mvcc.adjunct (clienttransformmanager rework) - remove
				getObjectStore().changeMapping(token.object,
						event.getObjectId(), event.getObjectLocalId());
				registerLocalObjectPromotion(token.object);
			}
			// MVCC - needed?
			if (event.getCommitType() == CommitType.TO_LOCAL_BEAN) {
				removeTransform(event);
			}
			objectModified(token.object, event, false);
			if (token.existingTargetObject != token.newTargetObject) {
				if (token.existingTargetObject instanceof Collection) {
					throw new RuntimeException(
							"Should not null a collection property:\n "
									+ event.toString());
				}
			}
			if (token.domainSerializablePropertyName != null
					&& isIgnorePropertyChanges()) {
				// wipe deserialized object (will force reconstitution)
				set(token.domainSerializableProperty(), token.object, null);
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
					event.getPropertyName(), token.newTargetObject,
					CollectionModificationType.ADD);
			if (shouldApplyCollectionModification(event)) {
				Set set = (Set) token.property().get(token.object);
				if (!set.contains(token.newTargetObject)) {
					doubleCheckAddition(set, token.newTargetObject);
				}
			}
			objectModified(token.object, event, false);
		}
			break;
		case REMOVE_REF_FROM_COLLECTION: {
			beforeDirectCollectionModification(token.object,
					event.getPropertyName(), token.newTargetObject,
					CollectionModificationType.REMOVE);
			if (shouldApplyCollectionModification(event)) {
				Set set = (Set) token.property().get(token.object);
				boolean wasContained = set.remove(token.newTargetObject);
				if (!wasContained) {
					doubleCheckRemoval(set, token.newTargetObject);
				}
			}
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
				Entity entity = newInstance(event.getObjectClass(),
						event.getObjectId(), event.getObjectLocalId());
				if (isZeroCreatedObjectLocalId(event.getObjectClass())) {
					entity.setLocalId(0);
				}
				if (entity.getId() == 0 && event.getObjectId() != 0) {
					// replay
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
				if (getObjectStore() != null && isAddToDomainObjects()) {
					getObjectStore().mapObject(entity);
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
		factoryInstance = null;
	}

	protected void beforeDirectCollectionModification(Entity obj,
			String propertyName, Object newTargetValue,
			CollectionModificationType collectionModificationType) {
		// for clients to force collection modifications to publish as property
		// changes (when replaying remote events). mvcc, force a tx-writeable
		// version of the object
	}

	public boolean checkForExistingLocallyCreatedObjects() {
		return true;
	}

	public void checkNoPendingTransforms() {
		Preconditions.checkState(getTransforms().size() == 0);
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
			DomainTransformEvent evt, String propertyName, Entity change) {
		return true;
	}

	protected void checkVersion(Entity obj, DomainTransformEvent event)
			throws DomainTransformException {
	}

	public void clearTransforms() {
		getTransforms().clear();
		for (CommitType ct : transformsByType.keySet()) {
			transformsByType.get(ct).clear();
		}
		markedForDeletion.clear();
	}

	public void clearUserObjects() {
		setObjectStore(null);
	}

	public void commitProvisionalObjects(Collection c) {
		promoteToDomain(c, false);
	}

	// FIXME - remove, direct call to getDomainObjects
	public boolean containsObject(DomainTransformEvent dte) {
		Entity obj = getObjectStore().getObject(dte.getObjectClass(),
				dte.getObjectId(), dte.getObjectLocalId());
		return obj != null;
	}

	ApplyToken createApplyToken(DomainTransformEvent event)
			throws DomainTransformException {
		return new ApplyToken(event);
	}

	public <T extends Entity> T createDomainObject(Class<T> objectClass) {
		long localId = nextLocalIdCounter();
		T newInstance = newInstance(objectClass, 0, localId);
		Preconditions.checkState(newInstance.getLocalId() != 0);
		registerDomainObject(newInstance);
		fireCreateObjectEvent(objectClass, 0, localId);
		maybeFireCollectionModificationEvent(objectClass, false);
		return newInstance;
	}

	public <T extends Entity> T createProvisionalObject(Class<T> objectClass) {
		long localId = nextLocalIdCounter();
		T newInstance = newInstance(objectClass, 0, localId);
		newInstance.setLocalId(localId);
		registerProvisionalObject(newInstance);
		return newInstance;
	}

	public DomainTransformEvent
			createTransformFromPropertyChange(PropertyChangeEvent event) {
		DomainTransformEvent transform = createTransformEvent();
		transform.setSource((Entity) event.getSource());
		transform.setNewValue(event.getNewValue());
		transform.setPropertyName(event.getPropertyName());
		Entity entity = (Entity) event.getSource();
		Preconditions
				.checkArgument(entity.getId() != 0 || entity.getLocalId() != 0);
		transform.setObjectId(entity.getId());
		transform.setObjectLocalId(entity.getLocalId());
		transform.setObjectClass(entity.entityClass());
		transform.setTransformType(TransformType.CHANGE_PROPERTY_SIMPLE_VALUE);
		maybeAddVersionNumbers(transform, entity, event.getNewValue());
		return transform;
	}

	// underlying set must be ordered
	protected Set<DomainTransformEvent> createTransformSet() {
		return new LinkedHashSet<>();
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
		if (!markedForDeletion.add(entity)) {
			return null;
		}
		registerDomainObject(entity);
		DomainTransformEvent dte = createTransformEvent();
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

	public void deregisterDomainObject(Entity entity) {
		if (getObjectStore() != null) {
			getObjectStore().deregister(entity);
		}
	}

	public void deregisterDomainObjects(Collection<? extends Entity> entities) {
		entities.forEach(this::deregisterDomainObject);
	}

	public void deregisterProvisionalObject(Object o) {
		deregisterProvisionalObjects(CommonUtils.wrapInCollection(o));
	}

	public void deregisterProvisionalObjects(Collection c) {
		if (c == null) {
			return;
		}
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

	protected void doubleCheckAddition(Collection collection, Object tgt) {
		collection.add(tgt);
	}

	protected void doubleCheckRemoval(Collection collection, Object tgt) {
		collection.remove(tgt);
	}

	protected Object ensureEndpointInTransformGraph(Object object) {
		return object;
	}

	protected Entity ensureEndpointWriteable(Entity targetObject) {
		return (Entity) ensureEndpointInTransformGraph(targetObject);
	}

	public void
			fireCollectionModificationEvent(CollectionModificationEvent event) {
		this.collectionModificationSupport
				.fireCollectionModificationEvent(event);
	}

	protected void fireCreateObjectEvent(Class clazz, long id, long localId) {
		DomainTransformEvent dte = createTransformEvent();
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

	public synchronized void fireDomainTransform(DomainTransformEvent event)
			throws DomainTransformException {
		try {
			this.transformListenerSupport.fireDomainTransform(event);
		} catch (DomainTransformException e) {
			throw e;
		}
	}

	protected boolean generateEventIfObjectNotRegistered(Entity entity) {
		return false;
	}

	public <T> Collection<T> getCollection(Class<T> clazz) {
		return getObjectStore().getCollection(clazz);
	}

	protected Entity getEntityForCreate(DomainTransformEvent event) {
		return getObject(event);
	}

	/**
	 * useful support in TLTM, ThreadedClientTM
	 */
	public <H extends Entity> long getLocalIdForClientInstance(H entity) {
		return entity.getLocalId();
	}

	public Entity getObject(DomainTransformEvent dte) {
		return getObject(dte, false);
	}

	public Entity getObject(DomainTransformEvent dte, boolean ignoreSource) {
		Entity obj = getObjectStore().getObject(dte.getObjectClass(),
				dte.getObjectId(), dte.getObjectLocalId());
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
		return getObjectStore().getObject(entityLocator);
	}

	public <T extends Entity> T getObject(T entity) {
		return getObjectStore().getObject(entity);
	}

	public ObjectStore getObjectStore() {
		return this.objectStore;
	}

	protected Collection getProvisionalObjects() {
		return provisionalObjects.keySet();
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
		Class valueClass = evt.getValueClass();
		if (evt.getNewValue() != null || valueClass == null) {
			if (evt.getNewValue() instanceof Entity) {
				Entity entity = getObject((Entity) evt.getNewValue());
				if (entity != null) {
					return entity;
				} else {
					// this is perfectly possible - particularly on the client.
					// allow it there, to save lots of unhelpful
					// register/deregister boilerplate
					if (!allowUnregisteredEntityTargetObject()) {
						throw new RuntimeException(
								"Unable to get target object " + evt);
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
		Enum e = getTargetEnumValue(evt);
		if (e != null) {
			return e;
		}
		if (evt.getValueId() != 0 || evt.getValueLocalId() != 0) {
			Entity object = getObjectStore().getObject(valueClass,
					evt.getValueId(), evt.getValueLocalId());
			if (object != null) {
				return object;
			}
			if (resolveMissingObject(evt)) {
				// will be resolved async (if
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

	/**
	 * @return true if transform should not be persisted
	 */
	protected boolean
			handleCascadedSerializationChange(PropertyChangeEvent event) {
		Property property = Reflections
				.at(Domain.resolveEntityClass(event.getSource().getClass()))
				.property(event.getPropertyName());
		SerializablePropertyGroup serializablePropertyGroup = new SerializablePropertyGroup(
				property);
		Entity entity = (Entity) event.getSource();
		/*
		 * If the serialized version is being changed, null the toSerialize
		 * version *unless* this is called from setToSerialize (to preserve
		 * object refs)
		 */
		switch (serializablePropertyGroup.type) {
		case NOT:
			break;
		// property xxxSerialized has changed
		case SERIALIZED: {
			if (LooseContext.is(CONTEXT_IN_SERIALIZE_PROPERTY_CHANGE_CYCLE)) {
				// setting this serialized value from a non-serialized setter
				// call -
				// store the transform but do not propagate to the corresponding
				// property xxx
			} else {
				try {
					LooseContext.pushWithTrue(
							CONTEXT_IN_SERIALIZE_PROPERTY_CHANGE_CYCLE);
					serializablePropertyGroup.serializable.set(entity, null);
				} finally {
					LooseContext.pop();
				}
			}
			break;
		}
		// property xxx has changed
		case SERIALIZABLE: {
			if (LooseContext.is(CONTEXT_IN_SERIALIZE_PROPERTY_CHANGE_CYCLE)) {
				// property xxx was set to null (by the preceding switch case)
				// to force a refresh - do not
				// propagate
				return true;
			}
			try {
				LooseContext.pushWithTrue(
						CONTEXT_IN_SERIALIZE_PROPERTY_CHANGE_CYCLE);
				boolean hasClassNameProperty = serializablePropertyGroup.className != null;
				serializablePropertyGroup.serialized.set(entity,
						serialize(event.getNewValue(), hasClassNameProperty));
				if (hasClassNameProperty) {
					serializablePropertyGroup.className.set(entity,
							event.getNewValue() == null ? null
									: event.getNewValue().getClass().getName());
				}
				// do not persist as a transform
				return true;
			} finally {
				LooseContext.pop();
			}
		}
		}
		return false;
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

	protected boolean ignorePropertyForObjectsToDtes(Class objectType,
			Class propertyType, String propertyName) {
		return ignorePropertiesForCaching.contains(propertyName)
				|| !PermissionsManager.get().checkReadable(objectType,
						propertyName, null)
				|| (Reflections.at(objectType).property(propertyName)
						.has(AlcinaTransient.class)
						&& AlcinaTransient.Support.isTransient(Reflections
								.at(objectType).property(propertyName)
								.annotation(AlcinaTransient.class)));
	}

	protected void initCollections() {
		provisionalObjects = new IdentityHashMap<>();
	}

	protected void initObjectStore() {
		// FIXME - dirndl 1x2 - to client tm
		setObjectStore(new StandaloneObjectStoreClient(this));
	}

	protected boolean isAddToDomainObjects() {
		return true;
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

	protected boolean isPerformDirectAssociationUpdates(Entity targetObject) {
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

	public boolean isUseCreatedLocals() {
		return this.useCreatedLocals;
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

	public void modifyCollectionProperty(Object objectWithCollection,
			String collectionPropertyName, Object delta,
			CollectionModificationType modificationType) {
		Property property = Reflections.at(objectWithCollection)
				.property(collectionPropertyName);
		Collection old = (Collection) property.get(objectWithCollection);
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
		property.set(objectWithCollection, c);
	}

	protected <E extends Entity> E newInstance(Class<E> entityClass, long id,
			long localId) {
		E entity = Reflections.newInstance(entityClass);
		entity.setId(id);
		entity.setLocalId(localId);
		return entity;
	}

	public synchronized long nextLocalIdCounter() {
		return localIdGenerator.incrementAndGet();
	}

	public boolean objectHasTransforms(Entity entity) {
		return getTransforms().stream().anyMatch(
				transform -> transform.toObjectLocator().matches(entity)
						|| transform.toValueLocator().matches(entity));
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

	public List<DomainTransformEvent> objectsToDtes(Collection objects,
			Class<?> clazz, boolean asObjectSpec) {
		ClassReflector<?> classReflector = Reflections.at(clazz);
		List<Property> properties = classReflector.properties().stream()
				.filter(Property::isWriteable).collect(Collectors.toList());
		Object templateInstance = classReflector.templateInstance();
		Map<String, Object> defaultValues = new HashMap();
		Map<Class<? extends Entity>, Class<? extends Entity>> entityImplementations = new LinkedHashMap<>();
		for (Iterator<Property> itr = properties.iterator(); itr.hasNext();) {
			Property property = itr.next();
			String propertyName = property.getName();
			Class propertyType = property.getType();
			if (property.has(DomainProperty.class)
					&& property.annotation(DomainProperty.class).serialize()) {
				throw new UnsupportedOperationException();
			}
			if (ignorePropertyForObjectsToDtes(clazz, propertyType,
					propertyName)) {
				itr.remove();
			} else {
				Object defaultValue = property.get(templateInstance);
				defaultValues.put(propertyName, defaultValue);
				if (CommonUtils.isOrHasSuperClass(propertyType, Entity.class)) {
					Class implementation = PersistentImpl
							.getImplementationOrSelf(propertyType);
					entityImplementations.put(propertyType,
							implementation != null
									&& implementation != void.class
									&& implementation != Void.class
											? implementation
											: propertyType);
				}
			}
		}
		List<DomainTransformEvent> dtes = new ArrayList<DomainTransformEvent>();
		for (Object o : objects) {
			Object[] arr = asObjectSpec ? (Object[]) o : null;
			Entity entity = asObjectSpec ? null : (Entity) o;
			DomainTransformEvent dte = createTransformEvent();
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
			for (Property property : properties) {
				String propertyName = property.getName();
				Class propertyType = property.getType();
				Object defaultValue = defaultValues.get(propertyName);
				Object value = asObjectSpec ? arr[i++] : property.get(o);
				if (value == null && defaultValue == null) {
					continue;
				}
				if (value != null && defaultValue != null) {
					if (value.equals(defaultValue)) {
						continue;
					}
				}
				if (value instanceof Set) {
					Iterator itr = ((Set) value).iterator();
					for (; itr.hasNext();) {
						Object o2 = itr.next();
						dte = createTransformEvent();
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
				} else {
					dte = createTransformEvent();
					dte.setUtcDate(new Date(0L));
					dte.setObjectId(id);
					dte.setObjectClass(clazz);
					dte.setObjectLocalId(localId);
					if (value instanceof Timestamp) {
						value = new Date(((Timestamp) value).getTime());
					}
					dte.setNewValue(value);
					dte.setPropertyName(propertyName);
					if (entityImplementations.containsKey(propertyType)) {
						dte.setValueClass(
								entityImplementations.get(propertyType));
						long valueId = asObjectSpec ? (Long) value
								: ((Entity) value).getId();
						long valueLocalId = valueId == 0L
								? ((Entity) value).getLocalId()
								: 0L;
						dte.setValueId(valueId);
						dte.setValueLocalId(valueLocalId);
						dte.setTransformType(TransformType.CHANGE_PROPERTY_REF);
					} else {
						convertToTargetObject(dte);
						dte.setTransformType(
								TransformType.CHANGE_PROPERTY_SIMPLE_VALUE);
					}
					dtes.add(dte);
				}
			}
		}
		return dtes;
	}

	protected void performDeleteObject(Entity entity) {
		if (getObjectStore() != null) {
			getObjectStore().deregister(entity);
			maybeFireCollectionModificationEvent(entity.entityClass(), false);
		}
	}

	protected void performDirectAssociationUpdate(Entity objectWithCollection,
			String propertyName, Collection coll, Entity collectionMember,
			boolean remove) {
		boolean contains = coll.contains(collectionMember);
		if (contains ^ remove) {
			// NOOP
			return;
		}
		if (remove) {
			coll.remove(collectionMember);
		} else {
			coll.add(collectionMember);
		}
		DomainTransformEvent event = createTransformEvent();
		event.setSource(objectWithCollection);
		event.setPropertyName(propertyName);
		event.setObjectId(objectWithCollection.getId());
		event.setObjectLocalId(objectWithCollection.getLocalId());
		event.setObjectClass(objectWithCollection.entityClass());
		event.setTransformType(remove ? TransformType.REMOVE_REF_FROM_COLLECTION
				: TransformType.ADD_REF_TO_COLLECTION);
		event.setValueId(collectionMember.getId());
		event.setValueLocalId(collectionMember.getLocalId());
		event.setValueClass(collectionMember.entityClass());
		maybeAddVersionNumbers(event, objectWithCollection, collectionMember);
		addTransformsFromPropertyChange(Collections.singletonList(event));
	}

	public void persistSerializables(Entity<?> entity) {
		Reflections.at(entity.entityClass()).properties().stream()
				.filter(property -> property.has(DomainProperty.class)
						&& property.annotation(DomainProperty.class)
								.serialize())
				.forEach(property -> {
					Object propertyValue = property.get(entity);
					Object copy = Serializer.get().copy(propertyValue);
					property.set(entity, copy);
				});
		;
	}

	protected void promoteToDomain(Collection objects, boolean deregister) {
		try {
			CollectionModificationSupport.queue(true);
			for (Object o : objects) {
				if (o instanceof Entity
						&& getObjectStore().getObject((Entity) o) == null) {
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
					Entity newInstance = (Entity) Reflections
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

	protected Property property(Class clazz, String propertyName) {
		return Reflections.at(clazz).property(propertyName);
	}

	@Override
	public synchronized void propertyChange(PropertyChangeEvent event) {
		if (isIgnorePropertyChanges()
				|| UNSPECIFIC_PROPERTY_CHANGE.equals(event.getPropertyName())) {
			return;
		}
		try {
			if (handleCascadedSerializationChange(event)) {
				return;
			}
		} catch (RuntimeException e) {
			if (GWT.isClient()) {
				AlcinaTopics.transformCascadeException.publish(e);
			}
			throw e;
		}
		List<DomainTransformEvent> transforms = new ArrayList<DomainTransformEvent>();
		DomainTransformEvent transform = createTransformFromPropertyChange(
				event);
		transform.setOldValue(event.getOldValue());
		convertToTargetObject(transform);
		if (transform.getNewValue() == null) {
			transform.setTransformType(TransformType.NULL_PROPERTY_REF);
		}
		if (transform.getValueId() != 0 || transform.getValueLocalId() != 0) {
			transform.setTransformType(TransformType.CHANGE_PROPERTY_REF);
		}
		if (transform.getNewValue() instanceof Set) {
			Set typeCheck = (Set) event.getNewValue();
			typeCheck = (Set) (typeCheck.isEmpty()
					&& event.getOldValue() != null ? event.getOldValue()
							: typeCheck);
			// Note, we explicitly clear nulls here - it would require an
			// expansion of the protocols to implement them
			if (typeCheck.iterator().hasNext()) {
				if (typeCheck.iterator().next() instanceof Entity) {
					Set<Entity> oldValues = (Set) event.getOldValue();
					Set<Entity> newValues = (Set) event.getNewValue();
					oldValues.remove(null);
					newValues.remove(null);
					for (Entity entity : newValues) {
						if (!oldValues.contains(entity)) {
							transform = createTransformFromPropertyChange(
									event);
							transform.setNewValue(null);
							Preconditions.checkArgument(entity.getId() != 0
									|| entity.getLocalId() != 0);
							transform.setValueId(entity.getId());
							transform.setValueLocalId(entity.getLocalId());
							transform.setValueClass(entity.entityClass());
							transform.setTransformType(
									TransformType.ADD_REF_TO_COLLECTION);
							transforms.add(transform);
						}
					}
					for (Entity entity : oldValues) {
						if (!newValues.contains(entity)) {
							transform = createTransformFromPropertyChange(
									event);
							transform.setNewValue(null);
							Preconditions.checkArgument(entity.getId() != 0
									|| entity.getLocalId() != 0);
							transform.setValueId(entity.getId());
							transform.setValueLocalId(entity.getLocalId());
							transform.setValueClass(entity.entityClass());
							transform.setTransformType(
									TransformType.REMOVE_REF_FROM_COLLECTION);
							transforms.add(transform);
						}
					}
				} else if (typeCheck.iterator().next() instanceof Enum) {
					Set<Enum> oldValues = (Set) event.getOldValue();
					Set<Enum> newValues = (Set) event.getNewValue();
					oldValues.remove(null);
					newValues.remove(null);
					for (Enum e : newValues) {
						if (!oldValues.contains(e)) {
							transform = createTransformFromPropertyChange(
									event);
							transform.setNewValue(null);
							transform.setNewStringValue(e.name());
							transform.setValueClass(e.getDeclaringClass());
							transform.setTransformType(
									TransformType.ADD_REF_TO_COLLECTION);
							transforms.add(transform);
						}
					}
					for (Enum e : oldValues) {
						if (!newValues.contains(e)) {
							transform = createTransformFromPropertyChange(
									event);
							transform.setNewValue(null);
							transform.setNewStringValue(e.name());
							transform.setValueClass(e.getDeclaringClass());
							transform.setTransformType(
									TransformType.REMOVE_REF_FROM_COLLECTION);
							transforms.add(transform);
						}
					}
				}
			}
		} else {
			transforms.add(transform);
		}
		addTransformsFromPropertyChange(transforms);
		Entity entity = (Entity) event.getSource();
		if (this.getObjectStore() != null) {
			if (!provisionalObjects.containsKey(entity)) {
				maybeFireCollectionModificationEvent(entity.entityClass(),
						true);
			}
		}
	}

	public void pushTransformsInCurrentThread(
			Collection<DomainTransformEvent> dtes) {
		dtes.forEach(this::addTransform);
	}

	public void register(Collection<? extends Entity> entities,
			boolean register) {
		if (register) {
			registerDomainObjects(entities);
		} else {
			deregisterDomainObjects(entities);
		}
	}

	public void register(Entity entity, boolean register) {
		register(Collections.singleton(entity), register);
	}

	// FIXME - mvcc.adjunct - most app-level calls to this are legacy and can be
	// removed (there should only be a few framework calls)
	public <T extends Entity> T registerDomainObject(T entity) {
		if (getObjectStore() != null && entity != null) {
			if (entity.getId() == 0) {
				Entity createdObject = getObjectStore().getObject(entity);
				if (createdObject != null) {
					getObjectStore().deregister(createdObject);
				}
			}
			getObjectStore().mapObject(entity);
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
		((StandaloneObjectStoreClient) getObjectStore()).registerAsync(entities,
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
		if (this.getObjectStore() != null) {
			getObjectStore().removeListeners();
		}
		initObjectStore();
		getObjectStore().registerObjects(h.registerableDomainObjects());
		ClassRef.add(h.getClassRefs());
	}

	public void registerDomainObjectsInHolderAsync(final DomainModelHolder h,
			final AsyncCallback<Void> postRegisterCallback) {
		if (this.getObjectStore() != null) {
			getObjectStore().removeListeners();
		}
		initObjectStore();
		((StandaloneObjectStoreClient) getObjectStore()).registerAsync(
				h.registerableDomainObjects(), new ScheduledCommand() {
					@Override
					public void execute() {
						ClassRef.add(h.getClassRefs());
						postRegisterCallback.onSuccess(null);
					}
				});
	}

	public <V extends Entity> Set<V> registeredObjectsAsSet(Class<V> clazz) {
		return new LinkedHashSet<V>(getObjectStore().getCollection(clazz));
	}

	public <V extends Entity> V registeredSingleton(Class<V> clazz) {
		Collection<V> c = getObjectStore().getCollection(clazz);
		return c.isEmpty() ? null : c.iterator().next();
	}

	public void registerModelObject(final DomainModelObject h,
			final AsyncCallback<Void> postRegisterCallback) {
		getObjectStore().registerObjects(h.registrableObjects());
		postRegisterCallback.onSuccess(null);
	}

	public void registerModelObjectAsync(final DomainModelObject h,
			final AsyncCallback<Void> postRegisterCallback) {
		((StandaloneObjectStoreClient) getObjectStore())
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

	/*
	 * Overridden by threaded subclasses
	 */
	protected void removePerThreadContext0() {
	}

	public DomainTransformEvent
			removeTransform(DomainTransformEvent transform) {
		transforms.remove(transform);
		getTransformsByCommitType(transform.getCommitType()).remove(transform);
		return transform;
	}

	public void removeTransformsFor(Object object) {
		removeTransformsForObjects(Arrays.asList(object));
	}

	public void removeTransformsForObjects(Collection c) {
		Set<DomainTransformEvent> transforms = new LinkedHashSet<>(
				getTransformsByCommitType(CommitType.TO_LOCAL_BEAN));
		if (!(c instanceof Set)) {
			c = new HashSet(c);
		}
		for (DomainTransformEvent transform : transforms) {
			if (c.contains(transform.provideSourceOrMarker())
					|| c.contains(transform.getNewValue())
					|| c.contains(transform.provideTargetMarkerForRemoval())) {
				removeTransform(transform);
			}
		}
	}

	/*
	 * In an non-transactional system, this is required for pre + post
	 * transaction indexing. Note that it can't support
	 */
	public void replayLocalEvents(List<DomainTransformEvent> transforms,
			boolean undo) {
		if (undo) {
			transforms = transforms.stream().map(DomainTransformEvent::invert)
					.collect(Collectors.toList());
			Collections.reverse(transforms);
		}
		try {
			setIgnorePropertyChanges(true);
			for (DomainTransformEvent transform : transforms) {
				apply(transform);
			}
		} catch (Exception e) {
			// since we're replaying, this is reasonable
			throw WrappedRuntimeException.wrap(e);
		} finally {
			setIgnorePropertyChanges(false);
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

	protected void set(Property property, Entity object, Object value) {
		property.set(object, value);
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

	protected void setObjectStore(ObjectStore domainObjects) {
		this.objectStore = domainObjects;
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

	public void setUseCreatedLocals(boolean useCreatedLocals) {
		this.useCreatedLocals = useCreatedLocals;
	}

	protected boolean
			shouldApplyCollectionModification(DomainTransformEvent event) {
		return true;
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

	protected void updateAssociation(String propertyName, Entity<?> delta,
			Entity associated, boolean remove) {
		Association association = delta == null ? null
				: Reflections.at(delta.entityClass()).property(propertyName)
						.annotation(Association.class);
		if (associated == null || association == null
				|| association.propertyName().length() == 0) {
			return;
		}
		if (markedForDeletion.contains(associated)) {
			return;
		}
		/*
		 * When using Transaction.callInSnapshotTransaction to handle
		 * server-side cascade transforms of client-side deleted objects
		 */
		if (associated.domain().wasRemoved()) {
			return;
		}
		associated = (Entity) ensureEndpointWriteable(associated);
		Property associatedProperty = Reflections.at(associated)
				.property(association.propertyName());
		Object associatedExistingValue = associatedProperty.get(associated);
		associatedExistingValue = ensureEndpointInTransformGraph(
				associatedExistingValue);
		boolean associatedExistingValueCollection = associatedExistingValue instanceof Collection;
		TransformType tt = associatedExistingValueCollection
				? (remove ? TransformType.REMOVE_REF_FROM_COLLECTION
						: TransformType.ADD_REF_TO_COLLECTION)
				: remove ? TransformType.NULL_PROPERTY_REF
						: TransformType.CHANGE_PROPERTY_REF;
		DomainTransformEvent event = createTransformEvent();
		event.setTransformType(tt);
		maybeAddVersionNumbers(event, delta, associated);
		// No! Only should check one end of the relation for permissions
		// checkPermissions(hTgt, evt, assoc.propertyName());
		if (associatedExistingValueCollection) {
			Collection collection = (Collection) associatedExistingValue;
			if (isPerformDirectAssociationUpdates(associated)) {
				performDirectAssociationUpdate(associated,
						association.propertyName(), collection, delta, remove);
			} else {
				try {
					collection = CommonUtils.shallowCollectionClone(collection);
				} catch (Exception e) {
					throw new WrappedRuntimeException(e);
				}
				if (remove) {
					collection.remove(delta);
				} else {
					collection.add(delta);
				}
				associatedProperty.set(associated, collection);
			}
		} else {
			/*
			 * we can get in an ugly loop in the following case: x.parent=p1
			 * x.setParent(p2) (cascade) p1.children.remove(x) (cascade)
			 * x.setParent (null) (cascade) p2.children.remove(x) ... so only
			 * null if the assoc prop is the old value
			 */
			if (remove) {
				if (associatedExistingValue == delta) {
					associatedProperty.set(associated, null);
				}
			} else {
				associatedProperty.set(associated, delta);
			}
			// shouldn't fire for collection props, probly. also, collection
			// mods are very unlikely to collide in a nasty way (since
			// membership is really just a bitset, and colliding colln mods will
			// often not actually hit each other)
			//
			// "probably" means "at the moment we don't fire - i.e. don't mark
			// the target object as updated"
			objectModified(associated, event, true);
		}
	}

	class ApplyToken {
		private static final String SERIALIZED_SUFFIX = "Serialized";

		String domainSerializablePropertyName;

		Entity object;

		TransformType transformType;

		Object existingTargetObject;

		Entity existingTargetEntity;

		Object newTargetObject;

		Entity newTargetEntity;

		private DomainTransformEvent event;

		ApplyToken(DomainTransformEvent event) throws DomainTransformException {
			this.event = event;
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
			existingTargetObject = null;
			String propertyName = event.getPropertyName();
			if (event.isInImmediatePropertyChangeCommit()) {
				existingTargetObject = event.getOldValue();
			} else if (event.getSource() == null || propertyName == null) {
			} else {
				existingTargetObject = property().get(event.getSource());
			}
			existingTargetObject = ensureEndpointInTransformGraph(
					existingTargetObject);
			existingTargetEntity = null;
			if (existingTargetObject instanceof Entity) {
				existingTargetEntity = (Entity) existingTargetObject;
			} else if (event
					.getTransformType() == TransformType.REMOVE_REF_FROM_COLLECTION
					&& existingTargetObject instanceof Set) {
				EntityLocator valueLocator = event.toValueLocator();
				if (valueLocator == null) {
					// an enum set
				} else {
					existingTargetEntity = getObjectStore()
							.getObject(valueLocator);
					existingTargetEntity = (Entity) ensureEndpointInTransformGraph(
							existingTargetEntity);
				}
			}
			newTargetObject = transformType == null ? null
					: getTargetObject(event, false);
			newTargetObject = ensureEndpointInTransformGraph(newTargetObject);
			newTargetEntity = null;
			if (newTargetObject instanceof Entity) {
				newTargetEntity = (Entity) newTargetObject;
			} else if (event
					.getTransformType() == TransformType.ADD_REF_TO_COLLECTION
					&& newTargetObject instanceof Set) {
				EntityLocator valueLocator = event.toValueLocator();
				newTargetEntity = getObjectStore().getObject(valueLocator);
				newTargetEntity = (Entity) ensureEndpointInTransformGraph(
						newTargetEntity);
			}
			if (propertyName != null
					&& propertyName.endsWith(SERIALIZED_SUFFIX)) {
				String serializationSourceName = propertyName.substring(0,
						propertyName.length() - SERIALIZED_SUFFIX.length());
				Property property = Reflections.at(object.entityClass())
						.property(serializationSourceName);
				domainSerializablePropertyName = property != null
						&& property.has(DomainProperty.class)
						&& property.annotation(DomainProperty.class).serialize()
								? serializationSourceName
								: null;
			}
		}

		Property domainSerializableProperty() {
			return Reflections.at(object.entityClass())
					.property(domainSerializablePropertyName);
		}

		Property property() {
			return Reflections.at(object.entityClass())
					.property(event.getPropertyName());
		}
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

	public static class RecordTransformListener
			implements DomainTransformListener {
		@Override
		public void domainTransform(DomainTransformEvent evt) {
			if (evt.getCommitType() == CommitType.TO_LOCAL_BEAN) {
				TransformManager tm = TransformManager.get();
				if (tm.isProvisionalObject(evt.getSource())) {
					return;
				}
				tm.setTransformCommitType(evt, CommitType.TO_LOCAL_GRAPH);
				return;
			}
		}
	}

	static class SerializablePropertyGroup {
		// property 'xxx'
		Property serializable;

		// property 'xxxSerialized'
		Property serialized;

		// property 'xxxClassName'
		Property className;

		String propertyName;

		Type type = Type.NOT;

		public SerializablePropertyGroup(Property property) {
			boolean serializedPropertyChange = false;
			boolean toSerializePropertyChange = false;
			ClassReflector classReflector = Reflections
					.at(property.getOwningType());
			String propertyName = property.getName();
			if (propertyName.endsWith("Serialized")) {
				String sourcePropertyname = propertyName
						.replaceFirst("(.+)Serialized", "$1");
				serializable = classReflector.property(sourcePropertyname);
				serialized = classReflector.property(propertyName);
				serializedPropertyChange = serializable != null
						&& serializable.has(DomainProperty.class)
						&& serializable.annotation(DomainProperty.class)
								.serialize();
				if (serializedPropertyChange) {
					type = Type.SERIALIZED;
				}
			} else {
				serializable = classReflector.property(propertyName);
				toSerializePropertyChange = serializable != null
						&& serializable.has(DomainProperty.class)
						&& serializable.annotation(DomainProperty.class)
								.serialize();
				if (toSerializePropertyChange) {
					serialized = classReflector
							.property(propertyName + "Serialized");
					className = classReflector
							.property(propertyName + "ClassName");
					type = Type.SERIALIZABLE;
				}
			}
		}

		boolean is() {
			return type != Type.NOT;
		}

		enum Type {
			NOT, SERIALIZED, SERIALIZABLE;
		}
	}

	@Reflected
	@Registration.Singleton
	public static class Serializer {
		// must be stateless
		private static Serializer instance;

		public static TransformManager.Serializer get() {
			if (instance == null) {
				instance = Registry.impl(TransformManager.Serializer.class);
			}
			return instance;
		}

		public <T> T copy(T object) {
			return deserialize(serialize(object, false));
		}

		public <V> V deserialize(String serialized) {
			return deserialize(serialized, null);
		}

		public <V> V deserialize(String serialized, Class<V> clazz) {
			if (serialized == null) {
				return null;
			}
			if (serialized.startsWith("{")) {
				return AlcinaBeanSerializer.deserializeHolder(serialized);
			} else {
				return ReflectiveSerializer.deserialize(serialized);
			}
		}

		// subclass support for refactoring, would require calls from
		// FlatTreeSerializer, ReflectiveSerializer, AlcinaBeanSerializer
		public String mapMissingPropertyName(Class type, String propertyName) {
			return null;
		}

		public String serialize(Object object, boolean hasClassNameProperty) {
			return ReflectiveSerializer.serialize(object);
		}
	}
}
