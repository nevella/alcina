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
package cc.alcina.framework.entity.transform;

import java.beans.IntrospectionException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.OneToMany;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.totsp.gwittir.client.beans.SourcesPropertyChangeEvents;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.collections.SliceProcessor;
import cc.alcina.framework.common.client.collections.SliceProcessor.SliceSubProcessor;
import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.csobjects.LogMessageType;
import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.domain.DomainStoreProperty;
import cc.alcina.framework.common.client.domain.TransactionId;
import cc.alcina.framework.common.client.logic.domain.DomainTransformPersistable;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domain.HasVersionNumber;
import cc.alcina.framework.common.client.logic.domaintransform.ClassRef;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.CommitType;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformListener;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRuntimeException;
import cc.alcina.framework.common.client.logic.domaintransform.EntityLocator;
import cc.alcina.framework.common.client.logic.domaintransform.PersistentImpl;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.TransformType;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedEntityCache;
import cc.alcina.framework.common.client.logic.domaintransform.spi.ObjectStore;
import cc.alcina.framework.common.client.logic.permissions.AnnotatedPermissible;
import cc.alcina.framework.common.client.logic.permissions.Permissions;
import cc.alcina.framework.common.client.logic.permissions.PermissionsException;
import cc.alcina.framework.common.client.logic.reflection.AssignmentPermission;
import cc.alcina.framework.common.client.logic.reflection.Association;
import cc.alcina.framework.common.client.logic.reflection.ClearStaticFieldsOnAppShutdown;
import cc.alcina.framework.common.client.logic.reflection.DomainProperty;
import cc.alcina.framework.common.client.logic.reflection.ObjectPermissions;
import cc.alcina.framework.common.client.logic.reflection.PropertyPermissions;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.AlcinaTopics;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.SystemoutCounter;
import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.MetricLogging;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.logic.EntityLayerLogging;
import cc.alcina.framework.entity.logic.EntityLayerUtils;
import cc.alcina.framework.entity.persistence.AppPersistenceBase;
import cc.alcina.framework.entity.persistence.JPAImplementation;
import cc.alcina.framework.entity.persistence.domain.DomainStore;
import cc.alcina.framework.entity.persistence.mvcc.Mvcc;
import cc.alcina.framework.entity.persistence.mvcc.MvccObject;
import cc.alcina.framework.entity.persistence.mvcc.ResolvedVersionState;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.entity.persistence.mvcc.Transactions;
import cc.alcina.framework.entity.projection.EntityPersistenceHelper;
import cc.alcina.framework.entity.transform.policy.PersistenceLayerTransformExceptionPolicy;
import cc.alcina.framework.entity.util.MethodContext;

/**
 *
 * TODO - mvcc.5 - remove/encapsulate dependence on DomainStore
 *
 * FIXME - security - see commit message for
 * f07d1cab8b3ed4e13bdf1b796d5edb83ba881ddc
 *
 * @author Nick Reddel
 *
 */
@Registration(ClearStaticFieldsOnAppShutdown.class)
public class ThreadlocalTransformManager extends TransformManager {
	public static final String CONTEXT_TEST_PERMISSIONS = ThreadlocalTransformManager.class
			.getName() + ".CONTEXT_TEST_PERMISSIONS";

	public static final String CONTEXT_FLUSH_BEFORE_DELETE = ThreadlocalTransformManager.class
			.getName() + ".CONTEXT_FLUSH_BEFORE_DELETE";

	public static final String CONTEXT_ALLOW_MODIFICATION_OF_DETACHED_OBJECTS = ThreadlocalTransformManager.class
			.getName() + ".CONTEXT_ALLOW_MODIFICATION_OF_DETACHED_OBJECTS";

	public static final String CONTEXT_THROW_ON_RESET_TLTM = ThreadlocalTransformManager.class
			.getName() + ".CONTEXT_THROW_ON_RESET_TLTM";

	public static final String CONTEXT_LOADING_FOR_TRANSFORM = ThreadlocalTransformManager.class
			.getName() + ".CONTEXT_LOADING_FOR_TRANSFORM";

	public static final String CONTEXT_SILENTLY_IGNORE_READONLY_REGISTRATIONS = ThreadlocalTransformManager.class
			.getName() + ".CONTEXT_SILENTLY_IGNORE_READONLY_REGISTRATIONS";

	public static final String CONTEXT_DISABLE_EVICTION = ThreadlocalTransformManager.class
			.getName() + ".CONTEXT_DISABLE_EVICTION";

	public static final String CONTEXT_TRACE_RECONSTITUTE_ENTITY_MAP = ThreadlocalTransformManager.class
			.getName() + ".CONTEXT_TRACE_RECONSTITUTE_ENTITY_MAP";

	public static final String CONTEXT_NON_LISTENING_DOMAIN = ThreadlocalTransformManager.class
			.getName() + ".CONTEXT_NON_LISTENING_DOMAIN";

	private static ThreadLocalStore threadLocalStore = new ThreadLocalStore();

	static final class ThreadLocalStore extends ThreadLocal<TransformManager> {
		@Override
		protected synchronized TransformManager initialValue() {
			return createInstance();
		}

		ThreadlocalTransformManager originalInstance;

		ThreadlocalTransformManager createInstance() {
			ThreadlocalTransformManager tm = ThreadlocalTransformManager
					.ttmInstance();
			tm.resetTltm(null, null, false, true, true);
			return tm;
		}

		void push(TransformManager otherManager) {
			originalInstance = (ThreadlocalTransformManager) get();
			set(otherManager);
		}

		void pop() {
			set(originalInstance);
		}
	}

	public static void
			enterAltTransformManagerContext(TransformManager otherManager) {
		threadLocalStore.push(otherManager);
	}

	public static void exitAltTransformManagerContext() {
		threadLocalStore.pop();
	}

	private static List<DomainTransformListener> threadLocalListeners = new ArrayList<DomainTransformListener>();

	private static ThreadLocalSequentialIdGenerator tlIdGenerator = new ThreadLocalSequentialIdGenerator();

	static Logger logger = LoggerFactory
			.getLogger(ThreadlocalTransformManager.class);

	public static Map<Long, String> setIgnoreTrace = new LinkedHashMap<>();

	private static AtomicInteger removeListenerExceptionCounter = new AtomicInteger();

	public static boolean ignoreAllTransformPermissions;

	public static final Topic<Thread> topicTransformManagerWasReset = Topic
			.create();

	public static Predicate<Entity> permitEviction = e -> true;

	public static void addThreadLocalDomainTransformListener(
			DomainTransformListener listener) {
		threadLocalListeners.add(listener);
	}

	public static ThreadlocalTransformManager cast() {
		return (ThreadlocalTransformManager) TransformManager.get();
	}

	/**
	 * Convenience "override" of TransformManager.get()
	 */
	public static ThreadlocalTransformManager get() {
		return ThreadlocalTransformManager.cast();
	}

	public static boolean is() {
		return TransformManager.get() instanceof ThreadlocalTransformManager;
	}

	public static boolean isInEntityManagerTransaction() {
		return get() instanceof ThreadlocalTransformManager
				&& cast().getEntityManager() != null;
	}

	public static boolean isServerOnly(DomainTransformEvent evt) {
		Class clazz = evt.getObjectClass();
		if (clazz.getAnnotation(DomainTransformPersistable.class) != null) {
			return true;
		}
		clazz = evt.getValueClass();
		if (clazz != null && clazz
				.getAnnotation(DomainTransformPersistable.class) != null) {
			return true;
		}
		return false;
	}

	// for testing
	public static void registerPerThreadTransformManager(
			TransformManager perThreadTransformManager) {
		threadLocalStore.set(perThreadTransformManager);
	}

	public static ThreadlocalTransformManager ttmInstance() {
		ThreadlocalTransformManager tltm = new ThreadlocalTransformManager();
		return tltm;
	}

	private boolean ignoreTransformPermissions;

	private PersistenceLayerTransformExceptionPolicy exceptionPolicy;

	private boolean useObjectCreationId;

	Set<Entity> modifiedObjects = new HashSet<Entity>();

	List<DomainTransformEvent> modificationEvents = new ArrayList<DomainTransformEvent>();

	private ClientInstance clientInstance;

	private Map<Long, Entity> localIdToEntityMap;

	protected EntityLocatorMap clientInstanceEntityMap;

	private EntityManager entityManager;

	private TransactionId listeningToTransactionId;

	private IdentityHashMap<Entity, Entity> listeningTo = new IdentityHashMap<Entity, Entity>();

	private DetachedEntityCache detachedEntityCache;

	private boolean initialised = false;

	protected Set<EntityLocator> createdObjectLocators = new LinkedHashSet<>();

	private boolean transformsExplicitlyPermitted;

	private Set<DomainTransformEvent> explicitlyPermittedTransforms = new LinkedHashSet<>();

	private volatile boolean useTlIdGenerator = false;

	private Set<DomainTransformEvent> flushAfterTransforms = new LinkedHashSet<>();

	private boolean applyingExternalTransforms;

	public ThreadlocalTransformManager() {
		initObjectStore();
	}

	@Override
	public void addTransform(DomainTransformEvent evt) {
		if (transformsExplicitlyPermitted) {
			explicitlyPermittedTransforms.add(evt);
		}
		if (evt.getTransformType() == TransformType.DELETE_OBJECT
				&& LooseContext.is(CONTEXT_FLUSH_BEFORE_DELETE)) {
			markFlushTransforms();
		}
		super.addTransform(evt);
	}

	@Override
	protected void beforeDirectCollectionModification(Entity obj,
			String propertyName, Object newTargetValue,
			CollectionModificationType collectionModificationType) {
		Transactions.resolve(obj, ResolvedVersionState.WRITE, false);
	}

	@Override
	public boolean checkForExistingLocallyCreatedObjects() {
		// if in db-commit mode, we want a nice crisp fresh untouched instance
		return getEntityManager() == null;
	}

	protected boolean checkHasSufficientInfoForPropertyPersist(Entity entity) {
		return entity.getId() != 0
				|| (localIdToEntityMap.get(entity.getLocalId()) != null)
				|| (entity instanceof SourcesPropertyChangeEvents && listeningTo
						.containsKey((SourcesPropertyChangeEvents) entity))
				|| LooseContext
						.is(CONTEXT_ALLOW_MODIFICATION_OF_DETACHED_OBJECTS);
	}

	@Override
	protected boolean checkPermissions(Entity entity, DomainTransformEvent evt,
			String propertyName, Entity change) {
		return checkPermissions(entity, evt, propertyName, change, false);
	}

	private boolean checkPermissions(Entity entity, DomainTransformEvent evt,
			String propertyName, Entity<?> entityChange, boolean muteLogging) {
		if (isIgnoreTransformPermissions()) {
			return true;
		}
		if (explicitlyPermitted(evt)) {
			return true;
		}
		try {
			if (entity == null) {
				entity = (Entity) Reflections.at(evt.getObjectClass())
						.templateInstance();
			} else {
				entity = ensureNonProxy(entity);
				entity = resolveForPermissionsChecks(entity);
			}
			Class<? extends Entity> objectClass = entity.entityClass();
			ObjectPermissions op = objectClass
					.getAnnotation(ObjectPermissions.class);
			op = op == null ? Permissions.get().getDefaultObjectPermissions()
					: op;
			ObjectPermissions oph = null;
			AssignmentPermission aph = propertyName == null ? null
					: Reflections.at(objectClass).property(propertyName)
							.annotation(AssignmentPermission.class);
			if (entityChange != null) {
				oph = entityChange.entityClass()
						.getAnnotation(ObjectPermissions.class);
				oph = oph == null
						? Permissions.get().getDefaultObjectPermissions()
						: oph;
			}
			switch (evt.getTransformType()) {
			case ADD_REF_TO_COLLECTION:
			case REMOVE_REF_FROM_COLLECTION:
				checkTargetReadAndAssignmentAccessAndThrow(entity, entityChange,
						oph, aph, evt);
				checkPropertyWriteAccessAndThrow(entity, propertyName, evt);
				break;
			case CHANGE_PROPERTY_REF:
				checkTargetReadAndAssignmentAccessAndThrow(entity, entityChange,
						oph, aph, evt);
				checkPropertyWriteAccessAndThrow(entity, propertyName, evt);
				break;
			// deliberate fall-through
			case NULL_PROPERTY_REF:
			case CHANGE_PROPERTY_SIMPLE_VALUE:
				checkPropertyWriteAccessAndThrow(entity, propertyName, evt);
				break;
			case CREATE_OBJECT:
				if (!Permissions.isPermitted(entity, op.create())) {
					throw new DomainTransformException(new PermissionsException(
							"Permission denied : create - object " + evt));
				}
				break;
			case DELETE_OBJECT:
				if (!Permissions.isPermitted(entity, op.delete())) {
					throw new DomainTransformException(new PermissionsException(
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
				evt.setSource(entity);
				evt.setPropertyName(propertyName);
			}
			if (!muteLogging) {
				EntityLayerLogging.log(LogMessageType.TRANSFORM_EXCEPTION,
						"Domain transform permissions exception", e);
			}
			throw new WrappedRuntimeException(e);
		}
		return true;
	}

	public boolean checkPropertyAccess(Entity<?> entity, String propertyName,
			boolean read) throws IntrospectionException {
		if (entity.domain().wasPersisted()
				|| LooseContext.is(CONTEXT_TEST_PERMISSIONS)) {
			Class<? extends Entity> entityClass = entity.entityClass();
			PropertyDescriptor descriptor = SEUtilities
					.getPropertyDescriptorByName(entityClass, propertyName);
			if (descriptor == null) {
				throw new IntrospectionException(
						String.format("Property not found - %s::%s",
								entityClass.getName(), propertyName));
			}
			PropertyPermissions pp = SEUtilities
					.getPropertyDescriptorByName(entityClass, propertyName)
					.getReadMethod().getAnnotation(PropertyPermissions.class);
			ObjectPermissions op = entityClass
					.getAnnotation(ObjectPermissions.class);
			return Permissions.get().checkEffectivePropertyPermission(op, pp,
					entity, read);
		}
		return true;
	}

	private void checkPropertyReadAccessAndThrow(Entity entity,
			String propertyName, DomainTransformEvent evt)
			throws DomainTransformException, IntrospectionException {
		if (!checkPropertyAccess(entity, propertyName, true)) {
			throw new DomainTransformException(new PermissionsException(
					"Permission denied : read - object/property " + evt));
		}
	}

	private boolean checkPropertyWriteAccessAndThrow(Entity<?> entity,
			String propertyName, DomainTransformEvent evt)
			throws DomainTransformException, IntrospectionException {
		if (!checkPropertyAccess(entity, propertyName, false)) {
			DomainProperty ann = Reflections.at(entity.entityClass())
					.property(propertyName).annotation(DomainProperty.class);
			throw new DomainTransformException(new PermissionsException(
					"Permission denied : write - object/property " + evt));
		}
		return true;
	}

	private void checkTargetReadAndAssignmentAccessAndThrow(Entity assigningTo,
			Entity assigning, ObjectPermissions oph, AssignmentPermission aph,
			DomainTransformEvent evt) throws DomainTransformException {
		if (assigning == null) {
			return;
		}
		if (!Permissions.isPermitted(assigning, oph.read())) {
			throw new DomainTransformException(new PermissionsException(
					"Permission denied : read - target object " + evt));
		}
		if (aph != null && !Permissions.isPermitted(assigning, assigningTo,
				new AnnotatedPermissible(aph.value()), false)) {
			throw new DomainTransformException(new PermissionsException(
					"Permission denied : assign - target object " + evt));
		}
	}

	@Override
	protected void checkVersion(Entity obj, DomainTransformEvent event)
			throws DomainTransformException {
		if (exceptionPolicy != null) {
			exceptionPolicy.checkVersion(obj, event);
		}
	}

	@Override
	public DomainTransformEvent delete(Entity entity) {
		if (entity == null) {
			return null;
		}
		Preconditions.checkState(Transaction.current().isWriteable());
		entity = ensureNonProxy(entity);
		DomainTransformEvent event = super.delete(entity);
		if (event != null) {
			addTransform(event);
		}
		return event;
	}

	@Override
	public void deregisterDomainObject(Entity entity) {
		listeningTo.remove(entity);
		entity.removePropertyChangeListener(this);
		super.deregisterDomainObject(entity);
	}

	@Override
	protected void doubleCheckAddition(Collection collection, Object tgt) {
		if (entityManager != null) {
			JPAImplementation jpaImplementation = Registry
					.impl(JPAImplementation.class);
			tgt = jpaImplementation.getInstantiatedObject(tgt);
			for (Iterator itr = collection.iterator(); itr.hasNext();) {
				Object next = itr.next();
				if (jpaImplementation
						.areEquivalentIgnoreInstantiationState(next, tgt)) {
					return;
				}
			}
		}
		collection.add(tgt);
	}

	@Override
	protected void doubleCheckRemoval(Collection collection, Object tgt) {
		JPAImplementation jpaImplementation = Registry
				.impl(JPAImplementation.class);
		tgt = jpaImplementation.getInstantiatedObject(tgt);
		for (Iterator itr = collection.iterator(); itr.hasNext();) {
			Object next = itr.next();
			if (jpaImplementation.areEquivalentIgnoreInstantiationState(next,
					tgt)) {
				itr.remove();
				break;
			}
		}
	}

	@Override
	protected Entity ensureEndpointWriteable(Entity targetObject) {
		return Transactions.resolve(targetObject, ResolvedVersionState.WRITE,
				false);
	}

	protected <T extends Entity> T ensureNonProxy(T entity) {
		if (entity != null && entity.getId() != 0
				&& getEntityManager() != null) {
			entity = Registry.impl(JPAImplementation.class)
					.getInstantiatedObject(entity);
		}
		return entity;
	}

	/*
	 * At the end of a transaction, evict created locals that were never
	 * persisted (to not leak)
	 */
	public void evictNonPromotedLocals(List<Entity> createdLocals) {
		if (LooseContext.is(CONTEXT_DISABLE_EVICTION)
				|| AppPersistenceBase.isInstanceReadOnly()) {
			return;
		}
		DomainStore store = DomainStore.writableStore();
		createdLocals.forEach(e -> {
			if (!e.domain().wasPersisted()) {
				Class entityClass = e.entityClass();
				if (store.isCached(entityClass)) {
					if (permitEviction.test(e)) {
						boolean logEvictions = Configuration.is("logEvictions");
						if (logEvictions) {
							logger.info("Evicting: {}", e.toLocator());
						}
						store.getCache().evictCreatedLocal(e);
					} else {
						logger.warn("Invalid eviction: {}", e);
						logger.warn("Invalid eviction: ", new Exception());
						if (EntityLayerUtils.isTestOrTestServer()
								&& !AppPersistenceBase.isInstanceReadOnly()) {
							throw new RuntimeException();
						}
					}
				}
			} else {
				// retain
			}
		});
	}

	private boolean explicitlyPermitted(DomainTransformEvent evt) {
		return explicitlyPermittedTransforms.contains(evt);
	}

	public void flush() {
		flush(new ArrayList<>());
	}

	public void flush(List<DomainTransformEventPersistent> dtreps) {
		entityManager.flush();
	}

	@Override
	protected boolean generateEventIfObjectNotRegistered(Entity entity) {
		return Ax.isTest()
				|| !DomainStore.writableStore().isCached(entity.entityClass());
	}

	public ClientInstance getClientInstance() {
		return this.clientInstance;
	}

	public EntityLocatorMap getClientInstanceEntityMap() {
		return this.clientInstanceEntityMap;
	}

	public DetachedEntityCache getDetachedEntityCache() {
		return this.detachedEntityCache;
	}

	@Override
	protected Entity getEntityForCreate(DomainTransformEvent event) {
		if (getEntityManager() == null) {
			return super.getEntityForCreate(event);
		} else {
			return null;
		}
	}

	public EntityManager getEntityManager() {
		return entityManager;
	}

	public Map<Long, Entity> getLocalIdToEntityMap() {
		return this.localIdToEntityMap;
	}

	public List<DomainTransformEvent> getModificationEvents() {
		return this.modificationEvents;
	}

	@Override
	public <T extends Entity> T getObject(T entity) {
		if (entity == null) {
			return null;
		}
		entity = ensureNonProxy(entity);
		return super.getObject(entity);
	}

	@Override
	public TransformManager getT() {
		return (TransformManager) threadLocalStore.get();
	}

	@Override
	public boolean handlesAssociationsFor(Class clazz) {
		if (!super.handlesAssociationsFor(clazz)) {
			return false;
		}
		DomainStore store = DomainStore.stores().storeFor(clazz);
		return store.handlesAssociationsFor(clazz);
	}

	@Override
	protected void initObjectStore() {
		setObjectStore(new ObjectStoreImpl());
	}

	@Override
	protected boolean isAddToDomainObjects() {
		return entityManager == null;
	}

	public boolean isApplyingExternalTransforms() {
		return this.applyingExternalTransforms;
	}

	public boolean isExternalCreate() {
		return useTlIdGenerator;
	}

	public boolean isIgnoreTransformPermissions() {
		return this.ignoreTransformPermissions || ignoreAllTransformPermissions;
	}

	@Override
	public boolean isInCreationRequest(Entity entity) {
		return createdObjectLocators.contains(new EntityLocator(
				entity.entityClass(), 0, entity.getLocalId()));
	}

	public boolean isListeningTo(SourcesPropertyChangeEvents spce) {
		return listeningTo.containsKey(spce);
	}

	@Override
	protected boolean isPerformDirectAssociationUpdates(Entity entity) {
		/*
		 * if ==1, only the transform manager listens to property changes. So
		 * synthesising collection updates will have equal effect to
		 * clone/modify
		 */
		return entity.propertyChangeListeners().length == 1;
	}

	public boolean isTransformsExplicitlyPermitted() {
		return this.transformsExplicitlyPermitted;
	}

	/**
	 * for complete database replay
	 */
	public boolean isUseObjectCreationId() {
		return useObjectCreationId;
	}

	private void listenTo(Entity entity) {
		if (!listeningTo.containsKey(entity)) {
			if (Thread.currentThread().getName().contains("dev-cluster-1")) {
				int debug = 3;
			}
			Transaction current = Transaction.current();
			if (current.isReadOnly() && LooseContext
					.is(CONTEXT_SILENTLY_IGNORE_READONLY_REGISTRATIONS)) {
				// edge-case for lazy-external-dev-load
			} else {
				Preconditions.checkState(!current.isReadOnly());
				TransactionId transactionId = current.getId();
				if (listeningToTransactionId == null) {
					listeningToTransactionId = transactionId;
				} else {
					if (!Objects.equals(listeningToTransactionId,
							transactionId)) {
						logger.warn(
								"DEVEX:0 - Listening to object from wrong tx: {} - current : {} - incoming : {}",
								entity.toStringEntity(),
								listeningToTransactionId, current);
						throw new IllegalStateException();
					}
				}
				listeningTo.put(entity, entity);
				entity.addPropertyChangeListener(this);
			}
		}
	}

	public void markFlushTransforms() {
		flushAfterTransforms.add(CommonUtils.last(getTransforms().iterator()));
	}

	@Override
	public void modifyCollectionProperty(Object objectWithCollection,
			String collectionPropertyName, Object delta,
			CollectionModificationType modificationType) {
		/*
		 * If one end is mvcc, want both ends to be (for propagation)
		 */
		Collection deltaC = CommonUtils.wrapInCollection(delta);
		if (objectWithCollection instanceof MvccObject
				|| deltaC.stream().anyMatch(o -> o instanceof MvccObject)) {
			boolean mismatchedEndpoints = !(objectWithCollection instanceof MvccObject
					&& deltaC.stream().allMatch(o -> o instanceof MvccObject));
			if (mismatchedEndpoints) {
				Class<?> clazz = ((Entity) objectWithCollection).entityClass();
				DomainStoreProperty domainStoreProperty = Reflections.at(clazz)
						.property(collectionPropertyName)
						.annotation(DomainStoreProperty.class);
				if (domainStoreProperty != null && domainStoreProperty
						.ignoreMismatchedCollectionModifications()) {
					return;
				} else {
					Preconditions.checkArgument(!mismatchedEndpoints);
				}
			} else {
			}
		}
		super.modifyCollectionProperty(objectWithCollection,
				collectionPropertyName, delta, modificationType);
	}

	/**
	 * Can be called from the server layer(entityManager==null)
	 */
	@Override
	public <E extends Entity> E newInstance(Class<E> clazz, long id,
			long localId) {
		return newInstance(clazz, id, localId, false);
	}

	protected <E extends Entity> E newInstance(Class<E> clazz, long id,
			long localId, boolean externalLocal) {
		try {
			if (Entity.class.isAssignableFrom(clazz)) {
				Preconditions.checkState(Transaction.current().isWriteable());
				Entity newInstance = null;
				if (entityManager == null) {
					DomainStore store = DomainStore.stores().storeFor(clazz);
					newInstance = Transaction.current().create((Class) clazz,
							store, id, localId);
					if (id == 0L) {
						// created ex-store
						if (externalLocal) {
						} else {
							store.getCache().put(newInstance);
						}
					}
				} else {
					newInstance = Reflections.newInstance(clazz);
					newInstance.setLocalId(localId);
				}
				if (entityManager != null) {
					if (isUseObjectCreationId() && id != 0) {
						newInstance.setId(id);
						Object fromBefore = null;
						try {
							fromBefore = Registry.impl(JPAImplementation.class)
									.beforeSpecificSetId(entityManager,
											newInstance);
							entityManager.persist(newInstance);
						} finally {
							if (fromBefore != null) {
								Registry.impl(JPAImplementation.class)
										.afterSpecificSetId(fromBefore);
							}
						}
					} else {
						/*
						 * TransformInPersistenceContext does this at the end of
						 * transform application (before transform event
						 * persistence) so that inserts can be batched
						 */
						// entityManager.persist(newInstance);
					}
				}
				//
				// FIXME - alcina.doc -
				//
				// why maintain localIdToEntityMap (reason: cross-cutting
				// concern to domainstore - it's from pov of the ClientInstance
				// - which may have persisted local objects unknown to this VM's
				// store)
				//
				// localIdToEntityMap needs to be distinct from
				// clientInstanceEntityMap for applying transforms from other
				// servers (don't want to store localids if we don't have to)
				//
				// createdObjectLocators *could* be replaced with a collation,
				// but perf would be worse and layering too - so leave (and
				// explain) - they're only used in (entityManager != null)
				// contexts, but code is cleaner this way
				//
				EntityLocator entityLocator = newInstance.toLocator();
				localIdToEntityMap.put(localId, newInstance);
				createdObjectLocators.add(entityLocator);
				if (!isApplyingExternalTransforms()) {
					if (clientInstanceEntityMap != null) {
						clientInstanceEntityMap.putToLookups(entityLocator);
					}
				}
				return (E) newInstance;
			}
			throw new Exception("only construct entities here");
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	@Override
	public synchronized long nextLocalIdCounter() {
		return useTlIdGenerator ? tlIdGenerator.incrementAndGet()
				: localIdGenerator.incrementAndGet();
	}

	@Override
	protected void objectModified(Entity entity, DomainTransformEvent evt,
			boolean targetObject) {
		boolean addToResults = false;
		if (evt.getTransformType() == TransformType.CREATE_OBJECT) {
			addToResults = true;
		}
		// TODO - think about handling this as a postpersist entity listener?
		// that way we ensure correct version numbers
		if (entity instanceof HasVersionNumber
				&& !modifiedObjects.contains(entity)) {
			addToResults = true;
			modifiedObjects.add(entity);
			HasVersionNumber hv = (HasVersionNumber) entity;
			if (targetObject) {
				evt.setValueVersionNumber(hv.getVersionNumber() + 1);
				if (evt.getValueId() == 0) {
					evt.setValueId(entity.getId());
				}
			} else {
				evt.setObjectVersionNumber(hv.getVersionNumber() + 1);
				if (evt.getObjectId() == 0) {
					evt.setObjectId(entity.getId());
				}
			}
		}
		if (addToResults) {
			modificationEvents.add(evt);
		}
	}

	@Override
	protected /*
				 * if in 'entityManager' mode (i.e. in a db transaction), let
				 * the entityManager handle it - otherwise
				 *
				 * FIXME - mvcc.adjunct - maybe non-em instances should have a
				 * 'domainobjects' (i.e. domain store?)
				 *
				 * In fact...yes. THere's quite a bit of confusion between
				 * object lookup, object store and TM (particularly TLTM).
				 *
				 * Write it down (presumably get rid of object lookup) - i feel
				 * that object store should delegate to domainstore server-side,
				 * but need to make the interface cleaner. Also, how does this
				 * play with PostTransactionEntityResolver...and AdjunctTm?
				 */
	void performDeleteObject(Entity entity) {
		if (entityManager != null) {
			entityManager.remove(entity);
		} else {
			if (handlesAssociationsFor(entity.entityClass())) {
				entity = getObject(entity);
				// will be deregistered with resetTltm, and not wanted if the
				// version is not writeable
				// deregisterDomainObject(entity);
				DomainStore.stores().storeFor(entity.entityClass())
						.remove(entity);
			}
		}
	}

	public void persist(Object object) {
		entityManager.persist(object);
	}

	protected void propertyChangeSuper(PropertyChangeEvent evt) {
		super.propertyChange(evt);
	}

	public boolean provideIsMarkedFlushTransform(DomainTransformEvent event) {
		return flushAfterTransforms.contains(event);
	}

	public EntityLocatorMap reconstituteEntityMap() {
		if (clientInstance != null) {
			String message = "Reconstitute entity map - clientInstance: "
					+ clientInstance.getId();
			if (LooseContext.is(CONTEXT_TRACE_RECONSTITUTE_ENTITY_MAP)) {
				logger.warn(message,
						new Exception("trace reconstitute entity map"));
			}
			// cp.log(message, LogMessageType.INFO.toString());
			String dteName = PersistentImpl
					.getImplementation(DomainTransformEventPersistent.class)
					.getSimpleName();
			String dtrName = PersistentImpl
					.getImplementation(DomainTransformRequestPersistent.class)
					.getSimpleName();
			MetricLogging.get().start(message);
			List<Long> dtrIds = getEntityManager().createQuery(String.format(
					"select dtr.id from %s dtr where dtr.clientInstance.id = ?1",
					dtrName)).setParameter(1, clientInstance.getId())
					.getResultList();
			SystemoutCounter ctr = new SystemoutCounter(1, 10);
			SliceProcessor<Long> processor = new SliceProcessor<>();
			SliceSubProcessor<Long> sub = new SliceSubProcessor<>() {
				@Override
				public void process(List<Long> sublist, int startIndex) {
					if (LooseContext.is(CONTEXT_TRACE_RECONSTITUTE_ENTITY_MAP)
							|| startIndex > 0) {
						logger.info("Reconstitute slice - {}/{}", startIndex,
								dtrIds.size());
					}
					String eql = String.format(
							"select dte.objectId, dte.objectLocalId, dte.objectClassRef.id "
									+ "from  %s dte  "
									+ " where dte.domainTransformRequestPersistent.id in %s "
									+ " and dte.objectLocalId!=0 and dte.transformType = ?1",
							dteName,
							EntityPersistenceHelper.toInClause(sublist));
					List<Object[]> idTuples = getEntityManager()
							.createQuery(eql)
							.setParameter(1, TransformType.CREATE_OBJECT)
							.getResultList();
					for (Object[] obj : idTuples) {
						ClassRef classRef = ClassRef.forId((long) obj[2]);
						clientInstanceEntityMap.putToLookups(
								new EntityLocator(classRef.getRefClass(),
										(Long) obj[0], (Long) obj[1]));
					}
				}
			};
			processor.process(dtrIds, 1000, sub);
			MetricLogging.get().end(message);
		}
		return clientInstanceEntityMap;
	}

	@Override
	public </**
			 * NOTE - doesn't register children (unlike client)
			 *
			 * This can be used for either vm-local or ex-vm localid entities -
			 * as long as the clientinstance is correctly set
			 *
			 * userSessionEntityMap/localIdToEntityMap are not modified (they're
			 * for in-em access)
			 *
			 * FIXME - mvcc.jobs.2 - remove most calls to this (since framework
			 * registers)
			 */
			T extends Entity> T registerDomainObject(T entity) {
		return registerDomainObject(entity, false);
	}

	public <T extends Entity> T registerDomainObject(T entity,
			boolean listenToMvccObjectVersion) {
		boolean mvccObject = Mvcc.isMvccObject(entity);
		if (!mvccObject
				&& DomainStore.writableStore().isCached(entity.entityClass())
				&& !DomainStore.writableStore().getCache().contains(entity)) {
			/*
			 * Usable for synthetic objects  with negative ids)
			 *
			 * Also key for allowing 'synthetic transform' creation against a dev db... e.g:
			 * @formatter:off
			 *
			 * 	IUser user = new IUser(99999).domain().register();
				user.setUsername("Honeybunny");
				console.dumpTransforms();

				@formatter:on
			 */
			DomainStore.writableStore().getCache().put(entity);
		}
		if (!mvccObject || listenToMvccObjectVersion) {
			if (!LooseContext.is(CONTEXT_NON_LISTENING_DOMAIN)) {
				listenTo(entity);
			}
		}
		return entity;
	}

	@Override
	protected void removePerThreadContext0() {
		threadLocalStore.remove();
	}

	public void resetLocalIdCounterForCurrentThread() {
		resetLocalIdCounterForCurrentThread(new AtomicLong(0));
	}

	public void resetLocalIdCounterForCurrentThread(AtomicLong counter) {
		useTlIdGenerator = true;
		tlIdGenerator.reset(counter);
	}

	/**
	 * WARNING!! Do not call in normal client code - instead, call
	 * Transaction.endAndBeginNew (since all listeners will be dropped,
	 * subsequent transforms in this tx will not be picked up)
	 */
	public void resetTltm(EntityLocatorMap locatorMap) {
		resetTltm(locatorMap, null, false, true);
	}

	public void resetTltm(EntityLocatorMap locatorMap,
			PersistenceLayerTransformExceptionPolicy exceptionPolicy,
			boolean keepExplicitlyPermittedAndFlushAfterTransforms,
			boolean emitWarnings) {
		resetTltm(locatorMap, exceptionPolicy,
				keepExplicitlyPermittedAndFlushAfterTransforms, false,
				emitWarnings);
	}

	private void resetTltm(EntityLocatorMap locatorMap,
			PersistenceLayerTransformExceptionPolicy exceptionPolicy,
			boolean keepExplicitlyPermittedAndFlushAfterTransforms,
			boolean initialising, boolean emitWarnings) {
		if (LooseContext.is(CONTEXT_THROW_ON_RESET_TLTM) && !initialising) {
			throw new RuntimeException("Invalid reset");
		}
		setEntityManager(null);
		setDetachedEntityCache(null);
		this.exceptionPolicy = exceptionPolicy;
		this.clientInstanceEntityMap = locatorMap;
		localIdToEntityMap = new HashMap<Long, Entity>();
		modifiedObjects = new HashSet<Entity>();
		modificationEvents = new ArrayList<DomainTransformEvent>();
		transformListenerSupport.clear();
		markedForDeletion = new LinkedHashSet<>();
		createdObjectLocators.clear();
		if (!keepExplicitlyPermittedAndFlushAfterTransforms) {
			explicitlyPermittedTransforms.clear();
			flushAfterTransforms.clear();
		}
		for (Entity entity : listeningTo.keySet()) {
			if (entity != null) {
				try {
					entity.removePropertyChangeListener(this);
				} catch (Exception e) {
					logger.warn("DEVEX:0 - Exception removing listener: {} ",
							entity.toStringEntity());
					if (removeListenerExceptionCounter.incrementAndGet() < 50) {
						logger.warn("DEVEX:0 - Exception removing listener ",
								e);
					}
				}
			}
		}
		listeningToTransactionId = null;
		listeningTo = new IdentityHashMap<>();
		Set<DomainTransformEvent> pendingTransforms = getTransformsByCommitType(
				CommitType.TO_LOCAL_BEAN);
		if (!pendingTransforms.isEmpty() && !AppPersistenceBase.isTest()
				&& emitWarnings) {
			Ax.out("**WARNING ** TLTM - cleared (but still pending) transforms [%s]:\n %s",
					pendingTransforms.size(),
					pendingTransforms.stream()
							.map(DomainTransformEvent::toDebugString)
							.limit(1000).collect(Collectors.toList()));
			new Exception().printStackTrace();
			try {
				AlcinaTopics.devWarning
						.publish(new UncomittedTransformsException());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		clearTransforms();
		addDomainTransformListener(new ServerTransformListener());
		// user cache invalidation
		addDomainTransformListener(Permissions.get());
		for (DomainTransformListener listener : threadLocalListeners) {
			addDomainTransformListener(listener);
		}
		if (initialised) {
			try {
				topicTransformManagerWasReset.publish(Thread.currentThread());
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			initialised = true;
		}
	}

	public void resetTltmNonCommitalTx() {
		resetTltm(null, null, false, false);
	}

	protected Entity resolveForPermissionsChecks(Entity entity) {
		return entity;
	}

	@Override
	protected void set(Property property, Entity entity, Object value) {
		if (checkHasSufficientInfoForPropertyPersist(entity)) {
			property.set(entity, value);
		} else {
			throw new DomainTransformRuntimeException(
					"Attempting to alter property of non-persistent entity: "
							+ entity);
		}
	}

	public void
			setApplyingExternalTransforms(boolean applyingExternalTransforms) {
		this.applyingExternalTransforms = applyingExternalTransforms;
	}

	public void setClientInstance(ClientInstance clientInstance) {
		this.clientInstance = clientInstance;
	}

	public void setClientInstanceEntityMap(
			EntityLocatorMap clientInstanceEntityMap) {
		this.clientInstanceEntityMap = clientInstanceEntityMap;
	}

	public void
			setDetachedEntityCache(DetachedEntityCache detachedEntityCache) {
		this.detachedEntityCache = detachedEntityCache;
	}

	public void setEntityManager(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	public void
			setIgnoreTransformPermissions(boolean ignoreTransformPermissions) {
		this.ignoreTransformPermissions = ignoreTransformPermissions;
	}

	public void setTransformsExplicitlyPermitted(
			boolean transformsExplicitlyPermitted) {
		this.transformsExplicitlyPermitted = transformsExplicitlyPermitted;
	}

	public void setUseObjectCreationId(boolean useObjectCreationId) {
		this.useObjectCreationId = useObjectCreationId;
	}

	@Override
	protected boolean
			shouldApplyCollectionModification(DomainTransformEvent event) {
		// significant optimisation - avoids need to iterate/instantiate the
		// persistent collection if it's @OneToMany and has an @Association.
		// Cannot be used if object permissions depend on child collection
		// removal
		if (entityManager != null) {
			Property property = Reflections.at(event.getObjectClass())
					.property(event.getPropertyName());
			if (property.has(OneToMany.class)
					&& property.has(Association.class)) {
				DomainStoreProperty domainStoreProperty = property
						.annotation(DomainStoreProperty.class);
				if (domainStoreProperty == null || domainStoreProperty
						.optimiseOneToManyCollectionModifications()) {
					return false;
				}
			}
		}
		return true;
	}

	public boolean testPermissions(Entity entity, DomainTransformEvent evt,
			String propertyName, Entity change, boolean read) {
		if (!LooseContext.is(CONTEXT_TEST_PERMISSIONS)) {
			throw new DomainTransformRuntimeException("test property not set");
		}
		if (read) {
			try {
				checkPropertyReadAccessAndThrow(entity, propertyName, evt);
				return true;
			} catch (Exception e) {
				PermissionsException permissionsException = CommonUtils
						.extractCauseOfClass(e, PermissionsException.class);
				if (permissionsException == null) {
					throw new RuntimeException(e);
				} else {
					return false;
				}
			}
		} else {
			return checkPermissions(entity, evt, propertyName, change, true);
		}
	}

	public void useGlobalLocalIdCounter() {
		useTlIdGenerator = false;
	}

	private class ObjectStoreImpl implements ObjectStore {
		@Override
		public void changeMapping(Entity obj, long id, long localId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean contains(Class<? extends Entity> clazz, long id) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean contains(Entity obj) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void deregister(Entity entity) {
			// noop
		}

		@Override
		public <T> Collection<T> getCollection(Class<T> clazz) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Map<Class<? extends Entity>, Collection<Entity>>
				getCollectionMap() {
			throw new UnsupportedOperationException();
		}

		@Override
		public <T extends Entity> T getObject(Class<? extends T> clazz, long id,
				long localId) {
			if (!Entity.class.isAssignableFrom(clazz)) {
				throw new RuntimeException(
						"Attempting to obtain incompatible bean: " + clazz);
			}
			if (id == 0) {
				if (localIdToEntityMap.containsKey(localId)) {
					return (T) localIdToEntityMap.get(localId);
				}
				if (getEntityManager() == null) {
					T entity = DomainStore.stores().storeFor(clazz).getCache()
							.get(new EntityLocator(clazz, id, localId));
					if (entity != null) {
						return entity;
					}
				}
				if (clientInstanceEntityMap != null && localId != 0) {
					id = clientInstanceEntityMap.containsKey(localId)
							? clientInstanceEntityMap.getForLocalId(localId).id
							: 0;
				}
			}
			if (id != 0) {
				if (getEntityManager() != null) {
					T t = getEntityManager().find(clazz, id);
					// this may be a performance hit - but worth it - otherwise
					// all
					// sorts of potential problems
					// basically, transform events should (must) always have
					// refs to
					// "real" objects, not wrappers
					t = ensureNonProxy(t);
					if (localId != 0 && t != null) {
						localIdToEntityMap.put(localId, t);
					}
					return t;
				} else {
					long f_id = id;
					if (DomainStore.writableStore().isCached(clazz)
							&& DomainStore.writableStore().isCached(clazz,
									id)) {
						// optimisation
						return DomainStore.writableStore().getCache().get(clazz,
								id);
					} else {
						return MethodContext.instance()
								// .withContextFalse(
								// LazyPropertyLoadTask.CONTEXT_POPULATE_LAZY_PROPERTIES.getPath())
								.withContextTrue(
										ThreadlocalTransformManager.CONTEXT_LOADING_FOR_TRANSFORM)
								.call(() -> Domain.find(clazz, f_id));
					}
				}
			}
			return null;
		}

		@Override
		public <T extends Entity> T getObject(T bean) {
			return (T) getObject(bean.entityClass(), bean.getId(),
					bean.getLocalId());
		}

		@Override
		public void invalidate(Class<? extends Entity> clazz) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void mapObject(Entity obj) {
			// noop
		}

		@Override
		public void registerObjects(Collection objects) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void removeListeners() {
			throw new UnsupportedOperationException();
		}
	}

	public static class ThreadlocalTransformManagerFactory {
		public ThreadlocalTransformManager create() {
			return new ThreadlocalTransformManager();
		}
	}

	public static class UncomittedTransformsException extends Exception {
	}
}
