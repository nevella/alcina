package cc.alcina.framework.common.client.logic.domaintransform;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cc.alcina.framework.common.client.CommonLocator;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.common.client.csobjects.ObjectCacheItemResult;
import cc.alcina.framework.common.client.csobjects.ObjectCacheItemSpec;
import cc.alcina.framework.common.client.entity.WrapperPersistable;
import cc.alcina.framework.common.client.logic.MutablePropertyChangeSupport;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domain.HasVersionNumber;
import cc.alcina.framework.common.client.logic.domaintransform.CollectionModification.CollectionModificationSupport;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest.DomainTransformRequestType;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.BeanInfo;
import cc.alcina.framework.common.client.logic.reflection.ClientBeanReflector;
import cc.alcina.framework.common.client.logic.reflection.ClientBeanReflector.HasAnnotationCallback;
import cc.alcina.framework.common.client.logic.reflection.ClientPropertyReflector;
import cc.alcina.framework.common.client.logic.reflection.ClientReflector;
import cc.alcina.framework.common.client.logic.reflection.DomainPropertyInfo;
import cc.alcina.framework.common.client.logic.reflection.ObjectPermissions;
import cc.alcina.framework.common.client.logic.reflection.PropertyPermissions;
import cc.alcina.framework.common.client.logic.reflection.WrapperInfo;
import cc.alcina.framework.common.client.provider.TextProvider;
import cc.alcina.framework.common.client.remote.CommonRemoteServiceExtAsync;
import cc.alcina.framework.common.client.util.CloneHelper;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LookupMapToMap;
import cc.alcina.framework.gwt.client.ClientLayerLocator;
import cc.alcina.framework.gwt.client.widget.ModalNotifier;
import cc.alcina.framework.gwt.client.widget.ModalNotifier.ModalNotifierNull;

import com.google.gwt.user.client.rpc.AsyncCallback;

public abstract class ClientTransformManager extends TransformManager {
	protected ClientDomainSync cache;

	private ClientTransformManager.PersistableTransformListener persistableTransformListener;

	public ClientTransformManager() {
		super();
		cache = new ClientDomainSync();
	}

	public static ClientTransformManager cast() {
		return (ClientTransformManager) TransformManager.get();
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

	public interface PersistableTransformListener {
		public void persistableTransform(DomainTransformRequest dtr);
	}

	class ClientDteWorker extends ClientUIThreadWorker {
		List<DomainTransformEvent> creates = new ArrayList<DomainTransformEvent>();

		List<DomainTransformEvent> mods = new ArrayList<DomainTransformEvent>();

		DomainTransformRequest dtr = new DomainTransformRequest();

		private final Map<Class, List> objCopy;

		private final ClientInstance clientInstance;

		ClientDteWorker(Map<Class, List> objCopy, ClientInstance clientInstance) {
			this.objCopy = objCopy;
			this.clientInstance = clientInstance;
		}

		protected void onComplete() {
			dtr.getEvents().addAll(creates);
			dtr.getEvents().addAll(mods);
			dtr.setClientInstance(clientInstance);
			dtr.setDomainTransformRequestType(DomainTransformRequestType.CLIENT_OBJECT_LOAD);
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

	public class ClientDomainSync {
		private LookupMapToMap lkp;

		private ModalNotifier notifier;

		public ClientDomainSync() {
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
			String message = TextProvider.get().getUiObjectText(
					ClientTransformManager.class, "domain-sync-update",
					"Loading");
			notifier = ClientLayerLocator.get().notifications()
					.getModalNotifier(message);
			if (notifier == null) {
				notifier = new ModalNotifierNull();
			}
			final long t1 = System.currentTimeMillis();
			AsyncCallback<List<ObjectCacheItemResult>> innerCallback = new AsyncCallback<List<ObjectCacheItemResult>>() {
				public void onSuccess(List<ObjectCacheItemResult> result) {
					long t2 = System.currentTimeMillis();
					ClientLayerLocator.get().notifications()
							.log("Cache load/deser.: " + (t2 - t1));
					notifier.modalOff();
					MutablePropertyChangeSupport.setMuteAll(true);
					ClientTransformManager.PersistableTransformListener pl = getPersistableTransformListener();
					DomainTransformRequest dtr = null;
					if (pl != null) {
						dtr = new DomainTransformRequest();
						dtr.setClientInstance(ClientLayerLocator.get()
								.getClientInstance());
						dtr.setDomainTransformRequestType(DomainTransformRequestType.CLIENT_SYNC);
					}
					for (ObjectCacheItemResult item : result) {
						replayRemoteEvents(item.getTransforms(), fireTransforms);
						if (pl != null) {
							dtr.getEvents().addAll(item.getTransforms());
						}
					}
					if (pl != null) {
						// ignore failure, since we at least have a (currently)
						// functioning client, even if persistence is mczapped
						pl.persistableTransform(dtr);
					}
					MutablePropertyChangeSupport.setMuteAll(false);
					Object[] spec2 = new Object[4];
					System.arraycopy(spec, 0, spec2, 0, 3);
					spec2[3] = true;
					lkp.put(spec2);
					ClientLayerLocator
							.get()
							.notifications()
							.log("Cache dte replay: "
									+ (System.currentTimeMillis() - t2));
					callback.onSuccess(result);
				}

				public void onFailure(Throwable caught) {
					cleanup();
					throw new WrappedRuntimeException(caught);
				}

				private void cleanup() {
					notifier.modalOff();
				}
			};
			List<ObjectCacheItemSpec> specs = new ArrayList<ObjectCacheItemSpec>();
			specs.add(new ObjectCacheItemSpec(hili, propertyName));
			notifier.modalOn();
			ClientLayerLocator.get().commonRemoteServiceAsyncInstance()
					.cache(specs, innerCallback);
		}

		public void clearUserObjects() {
			lkp = new LookupMapToMap(3);
		}
	}

	private DomainTransformExceptionFilter domainTransformExceptionFilter;

	public void replayRemoteEvents(Collection<DomainTransformEvent> evts,
			boolean fireTransforms) {
		try {
			setReplayingRemoteEvent(true);
			for (DomainTransformEvent dte : evts) {
				try {
					consume(dte);
				} catch (DomainTransformException e) {
					if (domainTransformExceptionFilter == null
							|| !domainTransformExceptionFilter.ignore(e)) {
						throw e;
					}
				}
				if (dte.getTransformType() == TransformType.CREATE_OBJECT
						&& dte.getObjectId() == 0
						&& dte.getObjectLocalId() != 0) {
					localIdCounter = Math.max(localIdCounter,
							dte.getObjectLocalId());
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

	@Override
	protected void checkVersion(HasIdAndLocalId obj, DomainTransformEvent event)
			throws DomainTransformException {
		if (isReplayingRemoteEvent() && obj instanceof HasVersionNumber
				&& CommonUtils.iv(event.getObjectVersionNumber()) > 0) {
			((HasVersionNumber) obj).setVersionNumber(event
					.getObjectVersionNumber());
		}
	}

	private Map<Class, Boolean> requiresEditPrep = new HashMap<Class, Boolean>();

	public Collection prepareObject(HasIdAndLocalId domainObject,
			boolean autoSave, boolean afterCreation, boolean forEditing) {
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
			if (create && afterCreation) {
				HasIdAndLocalId newObj = autoSave ? TransformManager.get()
						.createDomainObject(pr.getPropertyType())
						: TransformManager.get().createProvisionalObject(
								pr.getPropertyType());
				CommonLocator.get().propertyAccessor()
						.setPropertyValue(domainObject, propertyName, newObj);
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
						Collection<HasIdAndLocalId> hilis = CommonUtils
								.shallowCollectionClone(cl);
						cl.clear();
						for (HasIdAndLocalId hili : hilis) {
							HasIdAndLocalId clonedValue = (HasIdAndLocalId) new CloneHelper()
									.shallowishBeanClone(hili);
							children.add(clonedValue);
							children.addAll(prepareObject(clonedValue,
									autoSave, afterCreation, forEditing));
							cl.add(clonedValue);
						}
					} else {
						HasIdAndLocalId clonedValue = (HasIdAndLocalId) new CloneHelper()
								.shallowishBeanClone(currentValue);
						CommonLocator
								.get()
								.propertyAccessor()
								.setPropertyValue(domainObject, propertyName,
										clonedValue);
						children.add(clonedValue);
						children.addAll(prepareObject(clonedValue, autoSave,
								afterCreation, forEditing));
					}
				}
			}
		}
		return children;
	}

	public void serializeDomainObjects(ClientInstance clientInstance)
			throws Exception {
		Map<Class<? extends HasIdAndLocalId>, Collection<HasIdAndLocalId>> collectionMap = getDomainObjects()
				.getCollnMap();
		Map<Class, List> objCopy = new LinkedHashMap<Class, List>();
		for (Class<? extends HasIdAndLocalId> clazz : collectionMap.keySet()) {
			List values = CollectionFilters.filter(collectionMap.get(clazz),
					new CollectionFilter<HasIdAndLocalId>() {
						@Override
						public boolean allow(HasIdAndLocalId o) {
							return o.getId() != 0;
						}
					});
			objCopy.put(clazz, values);
		}
		new ClientDteWorker(objCopy, clientInstance).start();
	}

	public ClientTransformManager.PersistableTransformListener getPersistableTransformListener() {
		return persistableTransformListener;
	}

	public void setPersistableTransformListener(
			ClientTransformManager.PersistableTransformListener persistableTransformListener) {
		this.persistableTransformListener = persistableTransformListener;
	}

	/**
	 * Useful series of actions when persisting a HasIdAndLocalId with
	 * references to a WrappedObject
	 * 
	 * @see TransformManager#promoteToDomainObject(Object) wrt what to do with
	 *      promoted objects
	 * @param referrer
	 */
	public <T extends HasIdAndLocalId> T persistWrappedObjectReferrer(
			final T referrer, boolean onlyLocalGraph) {
		final ClientBeanReflector beanReflector = ClientReflector.get()
				.beanInfoForClass(referrer.getClass());
		beanReflector.iterateForPropertyWithAnnotation(WrapperInfo.class,
				new HasAnnotationCallback<WrapperInfo>() {
					public void apply(WrapperInfo annotation,
							ClientPropertyReflector propertyReflector) {
						WrapperPersistable obj = (WrapperPersistable) propertyReflector
								.getPropertyValue(referrer);
						CommonLocator
								.get()
								.propertyAccessor()
								.setPropertyValue(referrer,
										annotation.toStringPropertyName(),
										obj.toString());
					}
				});
		T target = referrer;
		if (getProvisionalObjects().contains(referrer)) {
			try {
				CollectionModificationSupport.queue(true);
				final T promoted = promoteToDomainObject(referrer);
				target = promoted;
				// copy, because at the moment wrapped refs don't get handled by
				// the TM
				HasAnnotationCallback<WrapperInfo> callback = new HasAnnotationCallback<WrapperInfo>() {
					public void apply(WrapperInfo annotation,
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
			public void apply(final WrapperInfo annotation,
					final ClientPropertyReflector propertyReflector) {
				WrapperPersistable persistableObject = (WrapperPersistable) propertyReflector
						.getPropertyValue(finalTarget);
				AsyncCallback<Long> savedCallback = new AsyncCallback<Long>() {
					public void onFailure(Throwable caught) {
						throw new WrappedRuntimeException(caught);
					}

					public void onSuccess(Long result) {
						CommonLocator
								.get()
								.propertyAccessor()
								.setPropertyValue(finalTarget,
										annotation.idPropertyName(), result);
					}
				};
				callRemotePersistence(persistableObject, savedCallback);
			}
		};
		if (!onlyLocalGraph) {
			beanReflector.iterateForPropertyWithAnnotation(WrapperInfo.class,
					callback);
		}
		return target;
	}

	public DomainTransformExceptionFilter getDomainTransformExceptionFilter() {
		return this.domainTransformExceptionFilter;
	}

	public void setDomainTransformExceptionFilter(
			DomainTransformExceptionFilter domainTransformExceptionFilter) {
		this.domainTransformExceptionFilter = domainTransformExceptionFilter;
	}

	@Override
	protected boolean allowUnregisteredHiliTargetObject() {
		return true;
	}

	protected abstract void callRemotePersistence(WrapperPersistable persistableObject,
			AsyncCallback<Long> savedCallback);
	public  static class ClientTransformManagerCommon extends ClientTransformManager {
		protected void callRemotePersistence(WrapperPersistable persistableObject,
				AsyncCallback<Long> savedCallback) {
			((CommonRemoteServiceExtAsync) ClientLayerLocator.get()
					.commonRemoteServiceAsyncInstance()).persist(persistableObject,
					savedCallback);
		}
	}
}
