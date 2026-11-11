package cc.alcina.framework.gwt.client.logic.domaintransform;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.Scheduler.ScheduledCommand;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domain.HasVersionNumber;
import cc.alcina.framework.common.client.logic.domain.NonDomainTransformPersistable;
import cc.alcina.framework.common.client.logic.domaintransform.ClassRef;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.DeltaApplicationRecordType;
import cc.alcina.framework.common.client.logic.domaintransform.DomainModelHolder;
import cc.alcina.framework.common.client.logic.domaintransform.DomainModelObject;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformExceptionFilter;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.TransformType;
import cc.alcina.framework.common.client.logic.permissions.IGroup;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.permissions.Permissions;
import cc.alcina.framework.common.client.logic.reflection.Association;
import cc.alcina.framework.common.client.logic.reflection.DomainProperty;
import cc.alcina.framework.common.client.logic.reflection.ObjectPermissions;
import cc.alcina.framework.common.client.logic.reflection.PropertyPermissions;
import cc.alcina.framework.common.client.reflection.ClassReflector;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.CloneHelper;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.logic.domaintransform.CollectionModification.CollectionModificationEvent;
import cc.alcina.framework.gwt.client.logic.domaintransform.CollectionModification.CollectionModificationListener;
import cc.alcina.framework.gwt.client.logic.domaintransform.CollectionModification.CollectionModificationSource;
import cc.alcina.framework.gwt.client.logic.domaintransform.CollectionModification.CollectionModificationSupport;
import cc.alcina.framework.gwt.client.logic.domaintransform.lookup.gwt.StandaloneObjectStoreClient;
import cc.alcina.framework.common.client.util.AsyncCallback;

public abstract class ClientTransformManager extends TransformManager
		implements CollectionModificationSource {
	public static ClientTransformManager cast() {
		return (ClientTransformManager) TransformManager.get();
	}

	protected CollectionModificationSupport collectionModificationSupport;

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

	private ClientTransformManager.PersistableTransformListener persistableTransformListener;

	private DomainTransformExceptionFilter domainTransformExceptionFilter;

	private Map<Class, Boolean> requiresEditPrep = new HashMap<Class, Boolean>();

	private boolean firePropertyChangesOnConsumedCollectionMods;

	private boolean provisionalEditing;

	public ClientTransformManager() {
		super();
		this.collectionModificationSupport = new CollectionModificationSupport();
	}

	@Override
	protected boolean allowUnregisteredEntityTargetObject() {
		return true;
	}

	@Override
	protected boolean alwaysFireObjectOwnerCollectionModifications() {
		return true;
	}

	@Override
	protected void beforeDirectCollectionModification(Entity obj,
			String propertyName, Object value,
			CollectionModificationType collectionModificationType) {
		if (isFirePropertyChangesOnConsumedCollectionMods()) {
			modifyCollectionProperty(obj, propertyName,
					Collections.singleton(value), collectionModificationType);
		}
	}

	protected boolean checkRemoveAssociation(Entity entity, Entity target,
			Property property) {
		Association association = property.annotation(Association.class);
		if (association != null && association.dereferenceOnDelete()) {
			return true;
		}
		return !(target instanceof IUser || target instanceof IGroup);
	}

	@Override
	protected void checkVersion(Entity obj, DomainTransformEvent event)
			throws DomainTransformException {
		if (isReplayingRemoteEvent() && obj instanceof HasVersionNumber
				&& CommonUtils.iv(event.getObjectVersionNumber()) > 0) {
			obj.setVersionNumber(event.getObjectVersionNumber());
		}
	}

	@Override
	public void clearUserObjects() {
		requiresEditPrep.clear();
		super.clearUserObjects();
	}

	IdentityHashMap<Entity, List<Object>> projectedProvisionals = new IdentityHashMap<>();

	private List<Object> currentProvisionalProjection;

	public Entity ensureEditable(Entity<?> entity) {
		if (Reflections.at(entity).has(NonDomainTransformPersistable.class)) {
			Entity editable = (Entity) Reflections
					.newInstance(entity.entityClass());
			new CloneHelper().copyBeanProperties(entity, editable, null);
			entity = editable;
			if (isProvisionalEditing()) {
				registerProvisionalObject(entity);
			}
		}
		if (isProvisionalEditing()) {
			if (!isProvisionalObject(entity)) {
				entity = new CloneHelper().shallowishBeanClone(entity);
				currentProvisionalProjection = new ArrayList<>();
				projectedProvisionals.put(entity, currentProvisionalProjection);
				prepareObject(entity, false, false, true);
				currentProvisionalProjection = null;
			}
		} else {
			registerDomainObject(entity);
		}
		return entity;
	}

	@Override
	protected Object ensureEndpointInTransformGraph(Object object) {
		if (object instanceof Entity) {
			registerDomainObject((Entity) object);
		}
		return object;
	}

	@Override
	protected boolean generateEventIfObjectNotRegistered(Entity entity) {
		return true;
	}

	/*
	 * get a local id for assigning to pre-TM-store objects (such as faux
	 * ClientInstance)
	 */
	public long getBootstrapNextLocalId() {
		return localIdGenerator.incrementAndGet();
	}

	public DomainTransformExceptionFilter getDomainTransformExceptionFilter() {
		return this.domainTransformExceptionFilter;
	}

	public ClientTransformManager.PersistableTransformListener
			getPersistableTransformListener() {
		return persistableTransformListener;
	}

	public boolean isFirePropertyChangesOnConsumedCollectionMods() {
		return this.firePropertyChangesOnConsumedCollectionMods;
	}

	public boolean isProvisionalEditing() {
		return this.provisionalEditing;
	}

	@Override
	protected void maybeFireCollectionModificationEvent(
			Class<? extends Object> collectionClass,
			boolean fromPropertyChange) {
		fireCollectionModificationEvent(
				new CollectionModificationEvent(this, collectionClass,
						getObjectStore().getCollection(collectionClass),
						fromPropertyChange));
	}

	public void
			fireCollectionModificationEvent(CollectionModificationEvent event) {
		this.collectionModificationSupport
				.fireCollectionModificationEvent(event);
	}

	@Override
	public void removeCollectionModificationListener(
			CollectionModificationListener listener) {
		this.collectionModificationSupport
				.removeCollectionModificationListener(listener);
	}

	@Override
	protected void promoteToDomain(Collection objects, boolean deregister) {
		LinkedHashSet<Object> withProjected = new LinkedHashSet<>(objects);
		objects.forEach(o -> {
			List<Object> projected = projectedProvisionals.get(o);
			if (projected != null) {
				withProjected.addAll(projected);
			}
		});
		try {
			CollectionModificationSupport.queue(true);
			super.promoteToDomain(withProjected, deregister);
		} finally {
			if (!deregister) {
				registerProvisionalObject(objects);
			}
			CollectionModificationSupport.queue(false);
		}
	}

	public Collection prepareObject(Entity domainObject, boolean autoSave,
			boolean afterCreation, boolean forEditing) {
		List children = new ArrayList();
		ClassReflector<? extends Entity> classReflector = Reflections
				.at(domainObject);
		if (!classReflector.provideIsReflective()) {
			return children;
		}
		if (!autoSave) {
			registerProvisionalObject(domainObject);
			if (currentProvisionalProjection != null) {
				currentProvisionalProjection.add(domainObject);
			}
		}
		Collection<Property> properties = classReflector.properties();
		Class<? extends Object> c = domainObject.getClass();
		Boolean requiresPrep = requiresEditPrep.get(c);
		if (requiresPrep != null) {
			if (!requiresPrep) {
				return children;
			}
		} else {
			requiresEditPrep.put(c, false);
		}
		ObjectPermissions op = classReflector
				.annotation(ObjectPermissions.class);
		for (Property property : properties) {
			PropertyPermissions pp = property
					.annotation(PropertyPermissions.class);
			DomainProperty instructions = property
					.annotation(DomainProperty.class);
			if (!Permissions.get().checkEffectivePropertyPermission(op, pp,
					domainObject, false)) {
				continue;
			}
			if (property.isReadOnly() || property.isWriteOnly()) {
				continue;
			}
			String propertyName = property.getName();
			Object currentValue = property.get(domainObject);
			boolean create = instructions != null
					&& instructions.eagerCreation() && currentValue == null;
			if (requiresPrep == null && instructions != null
					&& (instructions.eagerCreation()
							|| instructions.cloneForProvisionalEditing())) {
				requiresEditPrep.put(c, true);
			}
			if (create && afterCreation) {
				Entity newObj = autoSave
						? TransformManager.get()
								.createDomainObject(property.getType())
						: TransformManager.get()
								.createProvisionalObject(property.getType());
				property.set(domainObject, newObj);
				children.add(newObj);
				children.addAll(prepareObject(newObj, autoSave, afterCreation,
						forEditing));
			} else {
				boolean cloneForEditing = instructions != null
						&& instructions.cloneForProvisionalEditing()
						&& !autoSave && forEditing;
				if (cloneForEditing && !autoSave && currentValue != null) {
					if (currentValue instanceof Collection) {
						Collection cl = (Collection) currentValue;
						Collection<Entity> entities = CommonUtils
								.shallowCollectionClone(cl);
						cl.clear();
						for (Entity entity : entities) {
							Entity clonedValue = new CloneHelper()
									.shallowishBeanClone(entity);
							children.add(clonedValue);
							children.addAll(prepareObject(clonedValue, autoSave,
									afterCreation, forEditing));
							cl.add(clonedValue);
						}
					} else {
						Entity clonedValue = (Entity) new CloneHelper()
								.shallowishBeanClone(currentValue);
						property.set(domainObject, clonedValue);
						children.add(clonedValue);
						children.addAll(prepareObject(clonedValue, autoSave,
								afterCreation, forEditing));
					}
				}
			}
		}
		return children;
	}

	@Override
	public void replayRemoteEvents(Collection<DomainTransformEvent> evts,
			boolean fireTransforms) {
		try {
			setReplayingRemoteEvent(true);
			for (DomainTransformEvent dte : evts) {
				try {
					apply(dte);
				} catch (DomainTransformException e) {
					if (domainTransformExceptionFilter == null
							|| !domainTransformExceptionFilter.ignore(e)) {
						throw e;
					}
				}
				if (dte.getTransformType() == TransformType.CREATE_OBJECT
						&& dte.getObjectId() == 0
						&& dte.getObjectLocalId() != 0) {
					localIdGenerator.set(Math.max(localIdGenerator.get(),
							dte.getObjectLocalId()));
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

	public void setDomainTransformExceptionFilter(
			DomainTransformExceptionFilter domainTransformExceptionFilter) {
		this.domainTransformExceptionFilter = domainTransformExceptionFilter;
	}

	public void setFirePropertyChangesOnConsumedCollectionMods(
			boolean firePropertyChangesOnConsumedCollectionMods) {
		this.firePropertyChangesOnConsumedCollectionMods = firePropertyChangesOnConsumedCollectionMods;
	}

	public void setPersistableTransformListener(
			ClientTransformManager.PersistableTransformListener persistableTransformListener) {
		this.persistableTransformListener = persistableTransformListener;
	}

	public void setProvisionalEditing(boolean provisionalEditing) {
		this.provisionalEditing = provisionalEditing;
	}

	public void setupClientListeners() {
		addDomainTransformListener(new RecordTransformListener());
		addDomainTransformListener(new CommitToLocalDomainTransformListener());
	}

	class ClientDteWorker extends ClientUIThreadWorker {
		List<DomainTransformEvent> creates = new ArrayList<DomainTransformEvent>();

		List<DomainTransformEvent> mods = new ArrayList<DomainTransformEvent>();

		DomainTransformRequest dtr = DomainTransformRequest
				.createNonServerPersistableRequest();

		private final Map<Class, List> objCopy;

		private final ClientInstance clientInstance;

		ClientDteWorker(Map<Class, List> objCopy,
				ClientInstance clientInstance) {
			this.objCopy = objCopy;
			this.clientInstance = clientInstance;
		}

		@Override
		protected boolean isComplete() {
			return objCopy.isEmpty();
		}

		@Override
		protected void onComplete() {
			dtr.getEvents().addAll(creates);
			dtr.getEvents().addAll(mods);
			dtr.setClientInstance(clientInstance);
			getPersistableTransformListener().persistableTransform(dtr,
					DeltaApplicationRecordType.REMOTE_DELTA_APPLIED);
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

	public static class ClientTransformManagerCommon
			extends ClientTransformManager {
	}

	public interface PersistableTransformListener {
		public void persistableTransform(DomainTransformRequest dtr,
				DeltaApplicationRecordType type);
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

	@Override
	protected void initObjectStore() {
		setObjectStore(new StandaloneObjectStoreClient(this));
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
}
