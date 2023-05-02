package cc.alcina.framework.servlet.local;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.domain.DomainClassDescriptor;
import cc.alcina.framework.common.client.domain.DomainDescriptor;
import cc.alcina.framework.common.client.domain.IDomainStore;
import cc.alcina.framework.common.client.domain.LocalDomain;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.ClassRef;
import cc.alcina.framework.common.client.logic.domaintransform.ClientTransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.ClientTransformManager.ClientTransformManagerCommon;
import cc.alcina.framework.common.client.logic.domaintransform.CollectionModification.CollectionModificationEvent;
import cc.alcina.framework.common.client.logic.domaintransform.PersistentImpl;
import cc.alcina.framework.common.client.logic.domaintransform.TransformCollation.EntityCollation;
import cc.alcina.framework.common.client.logic.domaintransform.spi.ObjectStore;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.reflection.Reflections;
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

/**
 * A non-persistent entity store. Copies some tx-based environment behaviours
 * (e.g. 'commit') to a non-tx environment, using single-threaded access to
 * control concurrency
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

	private CommitToStorageTransformListenerNoRemote commitToStorageTransformListener;

	private ObjectStore objectStore;

	private DomainDescriptor domainDescriptor;

	PersistenceEvents persistenceEvents = new PersistenceEvents();

	private PersistenceImplementations persistenceImplementations;

	Logger logger = LoggerFactory.getLogger(getClass());

	private DomainTransformPersistenceListener indexingListener = evt -> {
		if (evt.getPersistenceEventType() == DomainTransformPersistenceEventType.COMMIT_OK) {
			this.index(evt);
		}
	};

	public LocalDomainStore(DomainDescriptor domainDescriptor,
			PersistenceImplementations persistenceImplementations) {
		this.domainDescriptor = domainDescriptor;
		this.persistenceImplementations = persistenceImplementations;
		this.domain = new LocalDomain(domainDescriptor);
		IDomainStore.State.nonTransactional = true;
		this.domain.initialise();
		registerClassRefs();
		this.objectStore = new ObjectStoreImpl();
		this.singleThreadedTransformManager = new SingleThreadedTransformManager();
		commitToStorageTransformListener = new CommitToStorageTransformListenerNoRemote();
		singleThreadedTransformManager.initObjectStore();
		LocalDomainStore.instance = this;
		persistenceEvents
				.addDomainTransformPersistenceListener(indexingListener);
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

	private void registerClassRefs() {
		List<ClassRef> refs = domainDescriptor.perClass.keySet().stream()
				.map(clazz -> {
					ClassRef ref = PersistentImpl
							.getNewImplementationInstance(ClassRef.class);
					ref.setRefClass(clazz);
					return ref;
				}).collect(Collectors.toList());
		ClassRef.add(refs);
	}

	void index(DomainTransformPersistenceEvent evt) {
		AdjunctTransformCollation collation = evt.getTransformPersistenceToken()
				.getTransformCollation();
		collation.allEntityCollations().forEach(ec -> {
			/*
			 * LocalDomainStores don't allow deletion
			 */
			Preconditions.checkArgument(!ec.isDeleted());
			/*
			 * only de-index if created prior to this request
			 */
			if (!ec.isCreated()) {
				index(ec.getEntity(), false, ec, true);
			}
			index(ec.getEntity(), true, ec, true);
		});
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

	public class PersistenceEvents {
		List<DomainTransformPersistenceListener> listeners = new ArrayList<>();

		private EntityLocatorMap locatorMap = new EntityLocatorMap();

		public void addDomainTransformPersistenceListener(
				DomainTransformPersistenceListener listener) {
			listeners.add(listener);
		}

		public void publish(DomainTransformRequestPersistent request) {
			TransformPersistenceToken token = new TransformPersistenceToken(
					request, locatorMap, false, true, false, logger, true);
			DomainTransformLayerWrapper layerWrapper = new DomainTransformLayerWrapper(
					token);
			DomainTransformPersistenceEvent persistenceEvent = new DomainTransformPersistenceEvent(
					token, layerWrapper,
					DomainTransformPersistenceEventType.PREPARE_COMMIT, true);
			for (DomainTransformPersistenceListener listener : listeners) {
				listener.onDomainTransformRequestPersistence(persistenceEvent);
			}
			persistenceEvent = new DomainTransformPersistenceEvent(token,
					layerWrapper, DomainTransformPersistenceEventType.COMMIT_OK,
					true);
			for (DomainTransformPersistenceListener listener : listeners) {
				listener.onDomainTransformRequestPersistence(persistenceEvent);
			}
		}
	}

	public interface PersistenceImplementations {
		Class<? extends DomainTransformEventPersistent>
				getPersistentEventClass();

		Class<? extends DomainTransformRequestPersistent>
				getPersistentRequestClass();
	}

	class CommitToStorageTransformListenerNoRemote
			extends CommitToStorageTransformListener {
		@Override
		public boolean isQueueCommitTimerDisabled() {
			// only explicit commits supported
			return true;
		}

		/*
		 * A simplified version of the superclass methods (which just pushes the
		 * grouped transforms as a 'persistent' request)
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
	}

	class ObjectStoreImpl implements ObjectStore {
		@Override
		public void changeMapping(Entity obj, long id, long localId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void deregister(Entity entity) {
			throw new UnsupportedOperationException();
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
		public void mapObject(Entity obj) {
			// NOOP - mapping (indexing) happens in the LocalDomain
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
		protected void maybeFireCollectionModificationEvent(
				Class<? extends Object> collectionClass,
				boolean fromPropertyChange) {
			// NOOP - FIXME - adjunct - remove this method (belongs in store)
		}
	}
}
