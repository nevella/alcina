package cc.alcina.framework.common.client.logic.domaintransform;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.common.client.csobjects.ObjectDeltaResult;
import cc.alcina.framework.common.client.csobjects.ObjectDeltaSpec;
import cc.alcina.framework.common.client.entity.WrapperPersistable;
import cc.alcina.framework.common.client.logic.MutablePropertyChangeSupport;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domain.HasVersionNumber;
import cc.alcina.framework.common.client.logic.domaintransform.CollectionModification.CollectionModificationEvent;
import cc.alcina.framework.common.client.logic.domaintransform.CollectionModification.CollectionModificationSupport;
import cc.alcina.framework.common.client.logic.permissions.IGroup;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.Association;
import cc.alcina.framework.common.client.logic.reflection.Bean;
import cc.alcina.framework.common.client.logic.reflection.ClientBeanReflector;
import cc.alcina.framework.common.client.logic.reflection.ClientPropertyReflector;
import cc.alcina.framework.common.client.logic.reflection.ClientReflector;
import cc.alcina.framework.common.client.logic.reflection.DomainProperty;
import cc.alcina.framework.common.client.logic.reflection.ObjectPermissions;
import cc.alcina.framework.common.client.logic.reflection.PropertyPermissions;
import cc.alcina.framework.common.client.logic.reflection.Wrapper;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.provider.TextProvider;
import cc.alcina.framework.common.client.remote.CommonRemoteServiceExtAsync;
import cc.alcina.framework.common.client.util.CloneHelper;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.UnsortedMultikeyMap;
import cc.alcina.framework.gwt.client.ClientBase;
import cc.alcina.framework.gwt.client.ClientNotifications;
import cc.alcina.framework.gwt.client.widget.ModalNotifier;
import cc.alcina.framework.gwt.client.widget.ModalNotifier.ModalNotifierNull;

public abstract class ClientTransformManager extends TransformManager {
	public static ClientTransformManager cast() {
		return (ClientTransformManager) TransformManager.get();
	}

	protected ClientDomainSync cache;

	private ClientTransformManager.PersistableTransformListener persistableTransformListener;

	private DomainTransformExceptionFilter domainTransformExceptionFilter;

	private Map<Class, Boolean> requiresEditPrep = new HashMap<Class, Boolean>();

	private boolean firePropertyChangesOnConsumedCollectionMods;

	public ClientTransformManager() {
		super();
		cache = new ClientDomainSync();
	}

	@Override
	public void clearUserObjects() {
		getCache().clearUserObjects();
		requiresEditPrep.clear();
		super.clearUserObjects();
	}

	public ClientDomainSync getCache() {
		return cache;
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

	/**
	 * Useful series of actions when persisting a Entity with references to a
	 * WrappedObject
	 *
	 * @see TransformManager#promoteToDomainObject(Object) wrt what to do with
	 *      promoted objects
	 * @param referrer
	 */
	public <T extends Entity> T persistWrappedObjectReferrer(final T referrer,
			boolean onlyLocalGraph) {
		Reflections.iterateForPropertyWithAnnotation(referrer.getClass(),
				Wrapper.class, (annotation, propertyReflector) -> {
					WrapperPersistable obj = (WrapperPersistable) propertyReflector
							.getPropertyValue(referrer);
					Reflections.propertyAccessor().setPropertyValue(referrer,
							annotation.toStringPropertyName(), obj.toString());
				});
		T target = referrer;
		if (isProvisionalObject(referrer)) {
			try {
				CollectionModificationSupport.queue(true);
				final T promoted = promoteToDomainObject(referrer);
				target = promoted;
				// copy, because at the moment wrapped refs don't get handled by
				// the TM
				Reflections.iterateForPropertyWithAnnotation(
						referrer.getClass(), Wrapper.class,
						(annotation, propertyReflector) -> {
							propertyReflector.setPropertyValue(promoted,
									propertyReflector
											.getPropertyValue(referrer));
						});
			} finally {
				CollectionModificationSupport.queue(false);
			}
		}
		final Entity finalTarget = target;
		if (!onlyLocalGraph) {
			Reflections.iterateForPropertyWithAnnotation(referrer.getClass(),
					Wrapper.class, (annotation, propertyReflector) -> {
						WrapperPersistable persistableObject = (WrapperPersistable) propertyReflector
								.getPropertyValue(finalTarget);
						AsyncCallback<Long> savedCallback = new AsyncCallback<Long>() {
							@Override
							public void onFailure(Throwable caught) {
								throw new WrappedRuntimeException(caught);
							}

							@Override
							public void onSuccess(Long result) {
								Reflections.propertyAccessor().setPropertyValue(
										finalTarget,
										annotation.idPropertyName(), result);
							}
						};
						callRemotePersistence(persistableObject, savedCallback);
					});
		}
		return target;
	}

	public Collection prepareObject(Entity domainObject, boolean autoSave,
			boolean afterCreation, boolean forEditing) {
		List children = new ArrayList();
		ClientBeanReflector bi = ClientReflector.get()
				.beanInfoForClass(domainObject.getClass());
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
		Bean beanInfo = bi.getAnnotation(Bean.class);
		for (ClientPropertyReflector pr : prs) {
			PropertyPermissions pp = pr
					.getAnnotation(PropertyPermissions.class);
			DomainProperty instructions = pr
					.getAnnotation(DomainProperty.class);
			if (!PermissionsManager.get().checkEffectivePropertyPermission(op,
					pp, domainObject, false)) {
				continue;
			}
			String propertyName = pr.getPropertyName();
			Object currentValue = Reflections.propertyAccessor()
					.getPropertyValue(domainObject, propertyName);
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
								.createDomainObject(pr.getPropertyType())
						: TransformManager.get()
								.createProvisionalObject(pr.getPropertyType());
				Reflections.propertyAccessor().setPropertyValue(domainObject,
						propertyName, newObj);
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
						Reflections.propertyAccessor().setPropertyValue(
								domainObject, propertyName, clonedValue);
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

	public void serializeDomainObjects(ClientInstance clientInstance)
			throws Exception {
		Map<Class<? extends Entity>, Collection<Entity>> collectionMap = getDomainObjects()
				.getCollectionMap();
		Map<Class, List> objCopy = new LinkedHashMap<Class, List>();
		for (Class<? extends Entity> clazz : collectionMap.keySet()) {
			List values = CollectionFilters.filter(collectionMap.get(clazz),
					new CollectionFilter<Entity>() {
						@Override
						public boolean allow(Entity o) {
							return o.getId() != 0;
						}
					});
			objCopy.put(clazz, values);
		}
		new ClientDteWorker(objCopy, clientInstance).start();
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

	@Override
	protected boolean allowUnregisteredEntityTargetObject() {
		return true;
	}

	@Override
	protected boolean alwaysFireObjectOwnerCollectionModifications() {
		return true;
	}

	protected abstract void callRemotePersistence(
			WrapperPersistable persistableObject,
			AsyncCallback<Long> savedCallback);

	protected boolean checkRemoveAssociation(Entity entity, Entity target,
			ClientPropertyReflector propertyReflector) {
		Association association = propertyReflector
				.getAnnotation(Association.class);
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
			((HasVersionNumber) obj)
					.setVersionNumber(event.getObjectVersionNumber());
		}
	}

	@Override
	protected void maybeFireCollectionModificationEvent(
			Class<? extends Object> collectionClass,
			boolean fromPropertyChange) {
		fireCollectionModificationEvent(
				new CollectionModificationEvent(this, collectionClass,
						getDomainObjects().getCollection(collectionClass),
						fromPropertyChange));
	}

	@Override
	protected void beforeDirectCollectionModification(Entity obj, String propertyName,
			Object value,
			CollectionModificationType collectionModificationType) {
		if (isFirePropertyChangesOnConsumedCollectionMods()) {
			modifyCollectionProperty(obj, propertyName,
					Collections.singleton(value), collectionModificationType);
		}
	}

	public class ClientDomainSync {
		private UnsortedMultikeyMap lkp;

		private ModalNotifier notifier;

		public ClientDomainSync() {
			clearUserObjects();
		}

		public void clearUserObjects() {
			lkp = new UnsortedMultikeyMap(3);
		}

		public void update(Entity entity, String propertyName,
				final AsyncCallback callback, final boolean fireTransforms) {
			if (entity.getId() == 0) {
				callback.onSuccess(null);
				return;
			}
			final Object[] spec = { entity.getClass(), entity.getId(),
					propertyName };
			if (lkp.containsKey(spec)) {
				callback.onSuccess(null);
				return;
			}
			Collection value = (Collection) Reflections.propertyAccessor()
					.getPropertyValue(entity, propertyName);
			if (value != null && !value.isEmpty()) {
				callback.onSuccess(null);
				return;
			}
			String message = TextProvider.get().getUiObjectText(
					ClientTransformManager.class, "domain-sync-update",
					"Loading");
			notifier = Registry.impl(ClientNotifications.class)
					.getModalNotifier(message);
			if (notifier == null) {
				notifier = new ModalNotifierNull();
			}
			final long t1 = System.currentTimeMillis();
			AsyncCallback<List<ObjectDeltaResult>> innerCallback = new AsyncCallback<List<ObjectDeltaResult>>() {
				@Override
				public void onFailure(Throwable caught) {
					cleanup();
					callback.onFailure(caught);
				}

				@Override
				public void onSuccess(List<ObjectDeltaResult> result) {
					long t2 = System.currentTimeMillis();
					Registry.impl(ClientNotifications.class)
							.log("Cache load/deser.: " + (t2 - t1));
					notifier.modalOff();
					MutablePropertyChangeSupport.setMuteAll(true);
					ClientTransformManager.PersistableTransformListener pl = getPersistableTransformListener();
					DomainTransformRequest dtr = null;
					if (pl != null) {
						dtr = DomainTransformRequest
								.createNonServerPersistableRequest();
						ClientInstance clientInstance = PermissionsManager.get()
								.getClientInstance();
						dtr.setClientInstance(clientInstance);
					}
					for (ObjectDeltaResult item : result) {
						replayRemoteEvents(item.getTransforms(),
								fireTransforms);
						if (pl != null) {
							dtr.getEvents().addAll(item.getTransforms());
						}
					}
					if (pl != null) {
						// ignore failure, since we at least have a (currently)
						// functioning client, even if persistence is mczapped
						pl.persistableTransform(dtr,
								DeltaApplicationRecordType.REMOTE_DELTA_APPLIED);
					}
					MutablePropertyChangeSupport.setMuteAll(false);
					Object[] spec2 = new Object[4];
					System.arraycopy(spec, 0, spec2, 0, 3);
					spec2[3] = true;
					lkp.put(spec2);
					Registry.impl(ClientNotifications.class)
							.log("Cache dte replay: "
									+ (System.currentTimeMillis() - t2));
					callback.onSuccess(result);
				}

				private void cleanup() {
					notifier.modalOff();
				}
			};
			List<ObjectDeltaSpec> specs = new ArrayList<ObjectDeltaSpec>();
			specs.add(new ObjectDeltaSpec(entity, propertyName));
			notifier.modalOn();
			ClientBase.getCommonRemoteServiceAsyncInstance()
					.getObjectDelta(specs, innerCallback);
		}
	}

	public static class ClientTransformManagerCommon
			extends ClientTransformManager {
		@Override
		protected void callRemotePersistence(
				WrapperPersistable persistableObject,
				AsyncCallback<Long> savedCallback) {
			((CommonRemoteServiceExtAsync) ClientBase
					.getCommonRemoteServiceAsyncInstance())
							.persist(persistableObject, savedCallback);
		}
	}

	public interface PersistableTransformListener {
		public void persistableTransform(DomainTransformRequest dtr,
				DeltaApplicationRecordType type);
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
}
