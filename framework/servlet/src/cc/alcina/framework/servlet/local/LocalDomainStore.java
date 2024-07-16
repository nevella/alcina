package cc.alcina.framework.servlet.local;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.domain.DomainClassDescriptor;
import cc.alcina.framework.common.client.domain.DomainDescriptor;
import cc.alcina.framework.common.client.domain.DomainFilter;
import cc.alcina.framework.common.client.domain.DomainQuery;
import cc.alcina.framework.common.client.domain.IDomainStore;
import cc.alcina.framework.common.client.domain.LocalDomain;
import cc.alcina.framework.common.client.domain.TransactionEnvironment;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domain.VersionableEntity;
import cc.alcina.framework.common.client.logic.domaintransform.ClientTransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.ClientTransformManager.ClientTransformManagerCommon;
import cc.alcina.framework.common.client.logic.domaintransform.CollectionModification.CollectionModificationEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.EntityLocator;
import cc.alcina.framework.common.client.logic.domaintransform.TransformCollation.EntityCollation;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.spi.ObjectStore;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.persistence.domain.descriptor.JobDomain;
import cc.alcina.framework.entity.transform.AdjunctTransformCollation;
import cc.alcina.framework.entity.transform.DomainTransformEventPersistent;
import cc.alcina.framework.entity.transform.DomainTransformLayerWrapper;
import cc.alcina.framework.entity.transform.DomainTransformRequestPersistent;
import cc.alcina.framework.entity.transform.EntityLocatorMap;
import cc.alcina.framework.entity.transform.TransformPersistenceToken;
import cc.alcina.framework.entity.transform.event.DomainTransformPersistenceEvent;
import cc.alcina.framework.entity.transform.event.DomainTransformPersistenceEventType;
import cc.alcina.framework.entity.transform.event.DomainTransformPersistenceListener;
import cc.alcina.framework.gwt.client.logic.CommitToStorageTransformListener;
import cc.alcina.framework.servlet.local.LocalDomainStore.DomainHandlerLds.Query;

/**
 * A non-persistent entity store. Copies some tx-based environment behaviours
 * (e.g. 'commit') to a non-tx environment, using single-threaded access to
 * control concurrency
 * 
 * Deletion is handled fairly brusquely - basically deletion events are collated
 * then applied right at the end of the transform processing
 * 
 * This will all go away with localdomain.mvcc (the real solution)
 */
public class LocalDomainStore {
	private static LocalDomainStore instance;
	/*
	 * Listen on tms,
	 */

	public static LocalDomainStore get() {
		return instance;
	}

	private LocalDomain domain;

	private SingleThreadedTransformManager singleThreadedTransformManager;

	CommitToStorageTransformListenerNoRemote commitToStorageTransformListener;

	private ObjectStore objectStore;

	private DomainDescriptor domainDescriptor;

	PersistenceEvents persistenceEvents = new PersistenceEvents();

	private PersistenceImplementations persistenceImplementations;

	Logger logger = LoggerFactory.getLogger(getClass());

	// apply commits to the graph (index)
	private DomainTransformPersistenceListener applyListener = evt -> {
		if (evt.getPersistenceEventType() == DomainTransformPersistenceEventType.COMMIT_OK) {
			this.apply(evt);
		}
	};

	boolean committingRequest;

	public LocalDomainStore(DomainDescriptor domainDescriptor,
			PersistenceImplementations persistenceImplementations) {
		this.domainDescriptor = domainDescriptor;
		this.persistenceImplementations = persistenceImplementations;
		this.domain = new LocalDomain(domainDescriptor);
		IDomainStore.State.nonTransactional = true;
		this.domain.initialise();
		this.objectStore = new ObjectStoreImpl();
		this.singleThreadedTransformManager = new SingleThreadedTransformManager();
		commitToStorageTransformListener = new CommitToStorageTransformListenerNoRemote();
		singleThreadedTransformManager.initObjectStore();
		LocalDomainStore.instance = this;
		Domain.registerHandler(new DomainHandlerLds());
		persistenceEvents.addDomainTransformPersistenceListener(applyListener);
		Registry.register().singleton(TransactionEnvironment.class,
				new TransactionEnvironmentNonTx());
		JobDomain.get().onDomainWarmupComplete(persistenceEvents);
	}

	public void commit() {
		LocalDomainQueue.run(() -> commitToStorageTransformListener.flush());
	}

	public CommitToStorageTransformListener
			getCommitToStorageTransformListener() {
		return commitToStorageTransformListener;
	}

	public PersistenceEvents getPersistenceEvents() {
		return persistenceEvents;
	}

	public ClientTransformManager getTransformManager() {
		return singleThreadedTransformManager;
	}

	void apply(DomainTransformPersistenceEvent evt) {
		AdjunctTransformCollation collation = evt.getTransformPersistenceToken()
				.getTransformCollation();
		/*
		 * De-index
		 */
		collation.allEntityCollations()
				.filter(ec -> ec.getTransforms().size() > 0).forEach(ec -> {
					/*
					 * only de-index if created prior to this request
					 */
					if (!ec.isCreated()) {
						try {
							TransformManager
									.get().allowApplyTransformsToDeletedEntities = true;
							List<DomainTransformEvent> transforms = ec
									.getTransforms();
							TransformManager.get().replayLocalEvents(transforms,
									true);
							index(ec.getEntity(), false, ec, true);
							TransformManager.get().replayLocalEvents(transforms,
									false);
							TransformManager.get()
									.setIgnorePropertyChanges(false);
						} finally {
							TransformManager
									.get().allowApplyTransformsToDeletedEntities = false;
						}
					}
				});
		/*
		 * Index
		 */
		collation.allEntityCollations()
				.filter(ec -> ec.getTransforms().size() > 0 && !ec.isDeleted())
				.forEach(ec -> index(ec.getEntity(), true, ec, true));
		// at the end remove deleted from the graph
		collation.allEntityCollations().filter(ec -> ec.isDeleted())
				.forEach(ec -> domain.getCache().removeLocal(ec.getEntity()));
	}

	void index(Entity entity, boolean add, EntityCollation entityCollation,
			boolean committed) {
		Class<? extends Entity> clazz = entity.entityClass();
		DomainClassDescriptor<?> itemDescriptor = domainDescriptor.perClass
				.get(clazz);
		if (itemDescriptor != null) {
			itemDescriptor.index(entity, add, committed, entityCollation);
			itemDescriptor
					.getDependentObjectsWithDerivedProjections(entity,
							entityCollation == null ? null
									: entityCollation
											.getTransformedPropertyNames())
					.forEach(e -> index(e, add, entityCollation, committed));
		}
	}

	boolean isEmptyCommitQueue() {
		return commitToStorageTransformListener.isEmptyCommitQueue();
	}

	<V extends Entity> Stream<V> query0(Class<V> entityClass, Query<V> query) {
		List<Predicate> predicates = query.getFilters().stream()
				.map(DomainFilter::asPredicate).collect(Collectors.toList());
		// naive - does not optimise with indicies -- FIXME - ma.2
		return Domain.stream(entityClass).filter(e -> {
			for (Predicate predicate : predicates) {
				if (!predicate.test(e)) {
					return false;
				}
			}
			return true;
		});
	}

	class CommitToStorageTransformListenerNoRemote
			extends CommitToStorageTransformListener {
		/*
		 * A simplified version of the superclass methods (which just pushes the
		 * grouped transforms as a 'persistent' request)
		 *
		 * Deliberately don't sync on the superclass collectionsMonitor (any
		 * access should be single-threaded)
		 */
		@Override
		protected synchronized void commit0() {
			DomainTransformRequestPersistent request = Reflections.newInstance(
					persistenceImplementations.getPersistentRequestClass());
			int requestId = localRequestId++;
			request.setRequestId(requestId);
			request.setClientInstance(
					PermissionsManager.get().getClientInstance());
			transformQueue.forEach(event -> {
				DomainTransformEventPersistent persistentEvent = Reflections
						.newInstance(persistenceImplementations
								.getPersistentEventClass());
				persistentEvent.wrap(event);
				request.getEvents().add(persistentEvent);
			});
			transformQueue.clear();
			persistenceEvents.publish(request);
			// nuffink doink...yet
			// super.commit0();
			/*
			 * create a faux DTRP publish on access queue
			 */
		}

		@Override
		public void domainTransform(DomainTransformEvent event) {
			LocalDomainQueue.checkOnDomainThread();
			super.domainTransform(event);
		}

		long getCurrentRequestId() {
			return localRequestId;
		}

		boolean isEmptyCommitQueue() {
			return transformQueue.isEmpty();
		}

		@Override
		public boolean isQueueCommitTimerDisabled() {
			// only explicit commits supported
			return true;
		}

		public void dumpTransformQueue() {
			Ax.out(transformQueue);
		}
	}

	class DomainHandlerLds implements Domain.DomainHandler {
		@Override
		public <V extends Entity> void async(Class<V> clazz, long objectId,
				boolean create, Consumer<V> resultConsumer) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <V extends Entity> V detachedVersion(V v) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <V extends Entity> V find(Class clazz, long id) {
			return domain.getCache().get(new EntityLocator(clazz, 0, id));
		}

		@Override
		public <V extends Entity> V find(EntityLocator locator) {
			return domain.getCache().get(locator);
		}

		@Override
		public <V extends Entity> V find(V v) {
			LocalDomainQueue.checkOnDomainThread();
			return domain.getCache().get(v.toLocator());
		}

		@Override
		public <V extends Entity> boolean isDomainVersion(V v) {
			return true;
		}

		@Override
		public <V extends Entity> boolean isMvccObject(V v) {
			return false;
		}

		@Override
		public <V extends Entity> DomainQuery<V> query(Class<V> clazz) {
			return new Query(clazz);
		}

		@Override
		public <V extends Entity> Stream<V> stream(Class<V> clazz) {
			LocalDomainQueue.checkOnDomainThread();
			return domain.getCache().stream(clazz);
		}

		class Query<V extends Entity> extends DomainQuery<V> {
			public Query(Class<V> entityClass) {
				super(entityClass);
			}

			@Override
			public List<V> list() {
				return stream().collect(Collectors.toList());
			}

			@Override
			public Stream<V> stream() {
				LocalDomainQueue.checkOnDomainThread();
				return query0(entityClass, this);
			}
		}
	}

	class ObjectStoreImpl implements ObjectStore {
		@Override
		public void changeMapping(Entity obj, long id, long localId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void deregister(Entity entity) {
			// domain.getCache().removeLocal(entity);
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
			return domain.getCache().get(clazz, id, localId);
		}

		@Override
		public <T extends Entity> T getObject(T bean) {
			return domain.getCache().get(bean.toLocator());
		}

		@Override
		public void invalidate(Class<? extends Entity> clazz) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void mapObject(Entity entity) {
			if (!domain.getCache().contains(entity)) {
				domain.add(entity);
				entity.addPropertyChangeListener(
						singleThreadedTransformManager);
			}
		}

		@Override
		public void registerObjects(Collection objects) {
			Preconditions.checkArgument(objects.isEmpty());
		}

		@Override
		public void removeListeners() {
			// NOOP
		}
	}

	public class PersistenceEvents
			implements DomainTransformPersistenceListener.Has {
		List<DomainTransformPersistenceListener> listeners = new ArrayList<>();

		private EntityLocatorMap locatorMap = new EntityLocatorMap();

		@Override
		public void addDomainTransformPersistenceListener(
				DomainTransformPersistenceListener listener) {
			listeners.add(listener);
		}

		public void publish(DomainTransformRequestPersistent request) {
			try {
				committingRequest = true;
				TransformPersistenceToken token = new TransformPersistenceToken(
						request, locatorMap, false, true, false, logger, true);
				DomainTransformLayerWrapper layerWrapper = new DomainTransformLayerWrapper(
						token);
				DomainTransformPersistenceEvent persistenceEvent = new DomainTransformPersistenceEvent(
						token, layerWrapper,
						DomainTransformPersistenceEventType.PREPARE_COMMIT,
						true);
				/*
				 * Update date fields
				 */
				AdjunctTransformCollation collation = persistenceEvent
						.getTransformPersistenceToken().getTransformCollation();
				Date time = new Date();
				collation.allEntityCollations().forEach(ec -> {
					Entity entity = ec.getEntity();
					if (entity instanceof VersionableEntity) {
						VersionableEntity versionable = (VersionableEntity) entity;
						if (ec.isCreated()) {
							versionable.setCreationDate(time);
						}
						versionable.setLastModificationDate(time);
					}
				});
				for (DomainTransformPersistenceListener listener : listeners) {
					listener.onDomainTransformRequestPersistence(
							persistenceEvent);
				}
				persistenceEvent = new DomainTransformPersistenceEvent(token,
						layerWrapper,
						DomainTransformPersistenceEventType.COMMIT_OK, true);
				for (DomainTransformPersistenceListener listener : listeners) {
					listener.onDomainTransformRequestPersistence(
							persistenceEvent);
				}
			} finally {
				committingRequest = false;
			}
		}
	}

	public interface PersistenceImplementations {
		Class<? extends DomainTransformEventPersistent>
				getPersistentEventClass();

		Class<? extends DomainTransformRequestPersistent>
				getPersistentRequestClass();
	}

	class SingleThreadedTransformManager extends ClientTransformManagerCommon {
		@Override
		public void fireCollectionModificationEvent(
				CollectionModificationEvent event) {
			// NOOP - FIXME - adjunct - remove this method (belongs in store)
		}

		@Override
		protected void initObjectStore() {
			setObjectStore(objectStore);
		}

		@Override
		public boolean isIgnoreUnrecognizedDomainClassException() {
			// does not use classrefs
			return true;
		}

		@Override
		protected void maybeFireCollectionModificationEvent(
				Class<? extends Object> collectionClass,
				boolean fromPropertyChange) {
			// NOOP - FIXME - adjunct - remove this method (belongs in store)
		}

		@Override
		public <T extends Entity> T registerDomainObject(T entity) {
			if (getObjectStore() != null && entity != null) {
				if (entity.getId() == 0) {
					Entity createdObject = getObjectStore().getObject(entity);
					if (createdObject != null) {
						// not sure this should be called anywhere - but
						// certainly not here
						// getObjectStore().deregister(createdObject);
					}
				}
				getObjectStore().mapObject(entity);
			}
			return entity;
		}
	}

	public void dumpCommitQueue() {
		commitToStorageTransformListener.dumpTransformQueue();
	}
}
