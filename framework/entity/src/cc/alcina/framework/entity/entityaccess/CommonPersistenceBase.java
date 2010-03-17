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
package cc.alcina.framework.entity.entityaccess;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.actions.ActionLogItem;
import cc.alcina.framework.common.client.csobjects.ObjectCacheItemResult;
import cc.alcina.framework.common.client.csobjects.ObjectCacheItemSpec;
import cc.alcina.framework.common.client.csobjects.SearchResultsBase;
import cc.alcina.framework.common.client.entity.GwtMultiplePersistable;
import cc.alcina.framework.common.client.entity.GwtPersistableObject;
import cc.alcina.framework.common.client.entity.PersistentSingleton;
import cc.alcina.framework.common.client.gwittir.validator.ServerUniquenessValidator;
import cc.alcina.framework.common.client.gwittir.validator.ServerValidator;
import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId.HiliHelper;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.DataTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DataTransformRequest;
import cc.alcina.framework.common.client.logic.domaintransform.DataTransformResponse;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.TransformType;
import cc.alcina.framework.common.client.logic.domaintransform.DataTransform.DataTransformException;
import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.permissions.IVersionableOwnable;
import cc.alcina.framework.common.client.logic.permissions.Permissible;
import cc.alcina.framework.common.client.logic.permissions.PermissionsException;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.WrapperInfo;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.MetricLogging;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.datatransform.DataTransformEventPersistent;
import cc.alcina.framework.entity.datatransform.DataTransformLayerWrapper;
import cc.alcina.framework.entity.datatransform.DataTransformRequestPersistent;
import cc.alcina.framework.entity.datatransform.EntityLayerLocator;
import cc.alcina.framework.entity.datatransform.ObjectPersistenceHelper;
import cc.alcina.framework.entity.datatransform.ThreadlocalTransformManager;
import cc.alcina.framework.entity.datatransform.ThreadlocalTransformManager.HiliLocatorMap;
import cc.alcina.framework.entity.entityaccess.UnwrapInfoItem.UnwrapInfoContainer;
import cc.alcina.framework.entity.util.EntityUtils;
import cc.alcina.framework.entity.util.GraphCloner;
import cc.alcina.framework.entity.util.Multiset;
import cc.alcina.framework.entity.util.GraphCloner.CloneFilter;

@SuppressWarnings("unchecked")
/**
 *
 * @author Nick Reddel
 */
public abstract class CommonPersistenceBase implements CommonPersistenceLocal {
	private static final String PRECACHE_ENTITIES = "precache entities";

	private static final String FLUSH_TRANSFORMS = "flush transforms";

	private static final String PERSIST_TRANSFORMS = "persist transforms";

	private static final String TRANSFORM_FIRE = "transform - fire";

	private static final int PRECACHE_RQ_SIZE = 125;

	private static Map<Long, Integer> clientInstanceAuthMap = new HashMap<Long, Integer>();

	public ClientInstance createClientInstance() {
		fixPermissionsManager(true);
		Class<? extends ClientInstance> clientInstanceImpl = getImplementation(ClientInstance.class);
		try {
			ClientInstance impl = clientInstanceImpl.newInstance();
			getEntityManager().persist(impl);
			impl.setHelloDate(new Date());
			impl.setUser(PermissionsManager.get().getUser());
			impl.setAuth(new Random().nextInt());
			getEntityManager().flush();
			clientInstanceAuthMap.put(impl.getId(), impl.getAuth());
			ClientInstance instance = new EntityUtils().detachedClone(impl,
					false);
			return instance;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	// hibernate
	public void bulkDelete(Class clazz, Collection<Long> ids) {
		getEntityManager().createQuery(
				String.format("delete %s where id in %s ", clazz
						.getSimpleName(), EntityUtils.longListToIdClause(ids)))
				.executeUpdate();
	}

	public abstract <A> Class<? extends A> getImplementation(Class<A> clazz);

	public <T> T ensureObject(T t, String key, String value) throws Exception {
		T newT = (T) getItemByKeyValue(t.getClass(), key, value, false);
		if (newT != null) {
			return newT;
		}
		PropertyDescriptor descriptor = SEUtilities.descriptorByName(t
				.getClass(), key);
		descriptor.getWriteMethod().invoke(t, value);
		return getEntityManager().merge(t);
	}

	public <A> Set<A> getAll(Class<A> clazz) {
		Query query = getEntityManager().createQuery(
				String.format("from %s ", clazz.getSimpleName()));
		List results = query.getResultList();
		return new LinkedHashSet<A>(results);
	}

	public <T> T getItemById(Class<T> clazz, Long id) {
		return getItemById(clazz, id, false, false);
	}

	public <T> T getItemById(Class<T> clazz, Long id, boolean clean,
			boolean unwrap) {
		T t = getEntityManager().find(clazz, id);
		if (clean) {
			t = new EntityUtils().detachedClone(t);
		}
		if (unwrap) {
			unwrap((HasId) t);
		}
		return t;
	}

	public <T> T getItemByKeyValue(Class<T> clazz, String key, Object value,
			boolean createIfNonexistent) {
		return getItemByKeyValue(clazz, key, value, createIfNonexistent, null,
				false);
	}

	public <T> T getItemByKeyValueKeyValue(Class<T> clazz, String key1,
			Object value1, String key2, Object value2) {
		String eql = String.format("from %s where %s=? and %s=?", clazz
				.getSimpleName(), key1, key2);
		Query q = getEntityManager().createQuery(eql).setParameter(1, value1)
				.setParameter(2, value2);
		List l = q.getResultList();
		return (T) ((l.size() == 0) ? null : l.get(0));
	}

	public <T extends GwtPersistableObject> WrappedObject<T> getObjectWrapperForUser(
			Class<T> c, long id) throws Exception {
		fixPermissionsManager();
		WrappedObject<T> wrapper = EntityLayerLocator.get()
				.wrappedObjectProvider().getObjectWrapperForUser(c, id,
						getEntityManager());
		checkWrappedObjectAccess(null, wrapper, c);
		return wrapper;
	}

	protected void checkWrappedObjectAccess(HasId wrapper,
			WrappedObject wrapped, Class clazz) throws PermissionsException {
		if (!PersistentSingleton.class.isAssignableFrom(clazz)
				&& wrapped != null
				&& wrapped.getUser().getId() != PermissionsManager.get()
						.getUserId()) {
			if (wrapper != null) {
				if (wrapper instanceof IVersionableOwnable) {
					IVersionableOwnable ivo = (IVersionableOwnable) wrapper;
					if (ivo.getOwner().getId() == wrapped.getUser().getId()) {
						return;// permitted
					}
				}
			}
			if (PermissionsManager.get().isPermissible(new Permissible() {
				public String rule() {
					return null;
				}

				public AccessLevel accessLevel() {
					return AccessLevel.ADMIN;
				}
			})) {
				System.err
						.println(CommonUtils
								.format(
										"Warn - allowing access to %1 : %2 only via admin override",
										HiliHelper.asDomainPoint(wrapper),
										HiliHelper.asDomainPoint(wrapped)));
				return;// permitted
			}
			throw new PermissionsException(CommonUtils.format(
					"Permissions exception: "
							+ "access denied to object  %1 for user %2",
					wrapped.getId(), PermissionsManager.get().getUserId()));
		}
	}

	public long log(String message, String componentKey) {
		// not required...useful but
		return 0;
	}

	public void logActionItem(ActionLogItem result) {
		fixPermissionsManager();
		getEntityManager().merge(result);
	}

	public long merge(HasId hi) {
		fixPermissionsManager();
		persistWrappables(hi);
		HasId merge = getEntityManager().merge(hi);
		return merge.getId();
	}

	public void remove(Object o) {
		getEntityManager().remove(o);
	}

	public IUser mergeUser(IUser user) {
		fixPermissionsManager();
		IUser merge = getEntityManager().merge(user);
		getEntityManager().flush();
		return merge;
	}

	public <G extends GwtPersistableObject> Long persist(G gwpo)
			throws Exception {
		fixPermissionsManager();
		WrappedObject<G> wrapper = (WrappedObject<G>) getObjectWrapperForUser(
				gwpo.getClass(), gwpo.getId());
		wrapper.setObject(gwpo);
		return wrapper.getId();
	}

	public SearchResultsBase search(SearchDefinition def, int pageNumber) {
		Searcher searcher = (Searcher) Registry.get().instantiateSingle(
				Searcher.class, def.getClass());
		if (!(searcher instanceof HandlesPermissionsManager)) {
			fixPermissionsManager();
		}
		SearchResultsBase result = searcher.search(def, pageNumber,
				getEntityManager());
		return new EntityUtils().detachedClone(result);
	}

	public void setField(Class clazz, Long id, String key, Object value)
			throws Exception {
		Object inst = getEntityManager().find(clazz, id);
		PropertyDescriptor descriptor = SEUtilities
				.descriptorByName(clazz, key);
		descriptor.getWriteMethod().invoke(inst, value);
		getEntityManager().merge(inst);
	}

	public DataTransformLayerWrapper transform(DataTransformRequest request,
			HiliLocatorMap locatorMap, boolean persistTransforms,
			boolean possiblyReconstitueLocalIdMap)
			throws DataTransformException {
		try {
			fixPermissionsManager(true);
			ObjectPersistenceHelper.get();
			ThreadlocalTransformManager tm = ThreadlocalTransformManager.cast();
			// We know this is thread-local, so we can clear the tm transforms
			tm.resetTltm(locatorMap);
			tm.setEntityManager(getEntityManager());
			ClientInstance persistentClientInstance = findImplInstance(
					ClientInstance.class, request.getClientInstance().getId());
			if (persistentClientInstance.getAuth() != null
					&& !(persistentClientInstance.getAuth().equals(request
							.getClientInstance().getAuth()))) {
				throw new DataTransformException("Invalid client instance auth");
			}
			tm.setClientInstance(persistentClientInstance);
			if (locatorMap != null && possiblyReconstitueLocalIdMap
					&& locatorMap.isEmpty()) {
				tm.reconstituteHiliMap();
			}
			Integer lastTransformId = getLastTransformId(request
					.getClientInstance().getId(), request.getRequestId());
			List<DataTransformRequest> dtrs = new ArrayList<DataTransformRequest>();
			dtrs.addAll(request.getPriorRequestsWithoutResponse());
			dtrs.add(request);
			for (int i = dtrs.size() - 1; i >= 0; i--) {
				DataTransformRequest dtr = dtrs.get(i);
				if (lastTransformId != null
						&& dtr.getRequestId() <= lastTransformId) {
					dtrs.remove(i);
				}
			}
			EntityLayerLocator.get().getMetricLogger().info(
					String.format("data transform - %s - clid:"
							+ "%s - rqid:%s - lasttransid:%s",
							persistentClientInstance.getUser().getUserName(),
							request.getClientInstance().getId(), request
									.getRequestId(), lastTransformId));
			for (DataTransformRequest dtr : dtrs) {
				List<DataTransformEvent> items = dtr.getItems();
				MetricLogging.get().lowPriorityStart(PRECACHE_ENTITIES);
				preCacheEntities(items);
				MetricLogging.get().lowPriorityEnd(PRECACHE_ENTITIES);
				MetricLogging.get().lowPriorityStart(TRANSFORM_FIRE);
				for (DataTransformEvent dte : items) {
					tm.fireDataTransform(dte);
				}
				MetricLogging.get().lowPriorityEnd(TRANSFORM_FIRE);
				MetricLogging.get().lowPriorityStart(PERSIST_TRANSFORMS);
				if (persistTransforms) {
					Class<? extends DataTransformRequestPersistent> dtrqImpl = getImplementation(DataTransformRequestPersistent.class);
					Class<? extends DataTransformEventPersistent> dtrEvtImpl = getImplementation(DataTransformEventPersistent.class);
					DataTransformRequestPersistent dtrp = dtrqImpl
							.newInstance();
					getEntityManager().persist(dtrp);
					dtrp.wrap(dtr);
					dtrp.setClientInstance(persistentClientInstance);
					for (DataTransformEvent evt : dtr.getItems()) {
						DataTransformEventPersistent dtep = dtrEvtImpl
								.newInstance();
						getEntityManager().persist(dtep);
						dtep.wrap(evt);
						if (dtep.getObjectId() == 0
								&& dtep.getTransformType() != TransformType.DELETE_OBJECT) {
							dtep.setObjectId(tm.getObject(
									dtep.getObjectClass(), 0,
									dtep.getObjectLocalId()).getId());
						}
						dtep.setServerCommitDate(new Date());
						dtep.setDataTransformRequestPersistent(dtrp);
						dtrp.getItems().add(dtep);
					}
				}
				MetricLogging.get().lowPriorityEnd(PERSIST_TRANSFORMS);
			}
			MetricLogging.get().lowPriorityStart(FLUSH_TRANSFORMS);
			getEntityManager().flush();
			MetricLogging.get().lowPriorityEnd(FLUSH_TRANSFORMS);
			DataTransformResponse dtr = new DataTransformResponse();
			dtr.getEventsToUseForClientUpdate().addAll(
					tm.getModificationEvents());
			dtr.setRequestId(request.getRequestId());
			DataTransformLayerWrapper wrapper = new DataTransformLayerWrapper();
			wrapper.locatorMap = locatorMap;
			wrapper.response = dtr;
			return wrapper;
		} catch (Exception e) {
			e.printStackTrace();
			if (e instanceof DataTransformException) {
				throw (DataTransformException) e;
			}
			throw new DataTransformException(e);
		}
	}

	private void preCacheEntities(List<DataTransformEvent> items) {
		Multiset<Class, Set<Long>> lkp = new Multiset<Class, Set<Long>>();
		for (DataTransformEvent dte : items) {
			if (dte.getObjectId() != 0) {
				lkp.add(dte.getObjectClass(), dte.getObjectId());
			}
			if (dte.getValueId() != 0) {
				lkp.add(dte.getValueClass(), dte.getValueId());
			}
		}
		for (Entry<Class, Set<Long>> entry : lkp.entrySet()) {
			Class storageClass = null;
			Class clazz = entry.getKey();
			if (clazz.getAnnotation(Entity.class) != null) {
				storageClass = clazz;
			}
			if (GwtPersistableObject.class.isAssignableFrom(clazz)) {
				storageClass = getImplementation(WrappedObject.class);
			}
			if (storageClass != null) {
				List<Long> ids = new ArrayList<Long>(entry.getValue());
				for (int i = 0; i < ids.size(); i += PRECACHE_RQ_SIZE) {
					List<Long> idsSlice = new ArrayList<Long>();
					int sliceEnd = Math.min(ids.size() - i, PRECACHE_RQ_SIZE);
					for (int j = 0; j < sliceEnd; j++) {
						idsSlice.add(ids.get(i + j));
					}
					getEntityManager().createQuery(
							String.format("from %s where id in %s",
									storageClass.getSimpleName(), EntityUtils
											.longListToIdClause(idsSlice)))
							.getResultList();
				}
			}
		}
	}

	public <T extends HasId> Collection<T> unwrap(Collection<T> wrappers) {
		preloadWrappedObjects(wrappers);
		for (HasId wrapper : wrappers) {
			unwrap(wrapper);
		}
		return wrappers;
	}

	private <T extends HasId> void preloadWrappedObjects(Collection<T> wrappers) {
		List<Long> wrapperIds = new ArrayList<Long>();
		try {
			for (HasId wrapper : wrappers) {
				PropertyDescriptor[] pds = Introspector.getBeanInfo(
						wrapper.getClass()).getPropertyDescriptors();
				for (PropertyDescriptor pd : pds) {
					if (pd.getReadMethod() != null) {
						WrapperInfo info = pd.getReadMethod().getAnnotation(
								WrapperInfo.class);
						if (info != null) {
							PropertyDescriptor idpd = SEUtilities
									.descriptorByName(wrapper.getClass(), info
											.idPropertyName());
							Long wrapperId = (Long) idpd.getReadMethod()
									.invoke(wrapper,
											CommonUtils.EMPTY_OBJECT_ARRAY);
							if (wrapperId != null) {
								wrapperIds.add(wrapperId);
							}
						}
					}
				}
			}
			Query query = getEntityManager().createQuery(
					"from "
							+ getImplementation(WrappedObject.class)
									.getSimpleName() + " where id in "
							+ EntityUtils.longListToIdClause(wrapperIds));
			query.getResultList();
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public HasId unwrap(HasId wrapper) {
		try {
			fixPermissionsManager();
			PropertyDescriptor[] pds = Introspector.getBeanInfo(
					wrapper.getClass()).getPropertyDescriptors();
			for (PropertyDescriptor pd : pds) {
				if (pd.getReadMethod() != null) {
					WrapperInfo info = pd.getReadMethod().getAnnotation(
							WrapperInfo.class);
					if (info != null) {
						PropertyDescriptor idpd = SEUtilities.descriptorByName(
								wrapper.getClass(), info.idPropertyName());
						Long wrapperId = (Long) idpd.getReadMethod().invoke(
								wrapper, CommonUtils.EMPTY_OBJECT_ARRAY);
						if (wrapperId != null) {
							Class<? extends GwtPersistableObject> pType = (Class<? extends GwtPersistableObject>) pd
									.getPropertyType();
							WrappedObject wrappedObject = EntityLayerLocator
									.get().wrappedObjectProvider()
									.getObjectWrapperForUser(pType, wrapperId,
											getEntityManager());
							checkWrappedObjectAccess(wrapper, wrappedObject,
									pType);
							Object unwrapped = wrappedObject.getObject();
							pd.getWriteMethod().invoke(wrapper, unwrapped);
						}
					}
				}
			}
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
		return wrapper;
	}

	public UnwrapInfoContainer prepareUnwrap(Class<? extends HasId> clazz,
			Long id, CloneFilter fieldFilter, CloneFilter dataFilter) {
		HasId wrapper = getItemById(clazz, id);
		UnwrapInfoContainer result = new UnwrapInfoContainer();
		result.setHasId(wrapper);
		try {
			PropertyDescriptor[] pds = Introspector.getBeanInfo(
					wrapper.getClass()).getPropertyDescriptors();
			for (PropertyDescriptor pd : pds) {
				if (pd.getReadMethod() != null) {
					WrapperInfo info = pd.getReadMethod().getAnnotation(
							WrapperInfo.class);
					if (info != null) {
						PropertyDescriptor idpd = SEUtilities.descriptorByName(
								wrapper.getClass(), info.idPropertyName());
						Long wrapperId = (Long) idpd.getReadMethod().invoke(
								wrapper, CommonUtils.EMPTY_OBJECT_ARRAY);
						if (wrapperId != null) {
							Class<? extends GwtPersistableObject> pType = (Class<? extends GwtPersistableObject>) pd
									.getPropertyType();
							WrappedObject wrappedObject = (WrappedObject) getObjectWrapperForUser(
									pType, wrapperId);
							result.getItems().add(
									new UnwrapInfoItem(pd.getName(),
											wrappedObject));
						}
					}
				}
			}
			return new GraphCloner(fieldFilter, dataFilter).clone(result, null);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public <T extends ServerValidator> List<T> validate(List<T> validators) {
		fixPermissionsManager();
		ArrayList<T> result = new ArrayList<T>();
		for (T serverValidator : validators) {
			if (serverValidator instanceof ServerUniquenessValidator) {
				ServerUniquenessValidator suv = (ServerUniquenessValidator) serverValidator;
				int ctr = 0;
				String value = suv.getValue();
				suv.setSuggestedValue(value);
				while (true) {
					Object item = getItemByKeyValue(suv.getObjectClass(), suv
							.getPropertyName(), value, false, suv.getOkId(),
							suv.isCaseInsensitive());
					if (item == null) {
						if (ctr != 0) {
							suv.setSuggestedValue(value);
							suv.setMessage("Item exists. Suggested value: "
									+ value);
						}
						break;
					}
					// no suggestions, just error
					if (suv.getValueTemplate() == null) {
						suv.setMessage("Item exists");
						break;
					}
					ctr++;
					value = String.format(suv.getValueTemplate(), suv
							.getValue() == null ? "" : suv.getValue(), ctr);
				}
			} else {
				Class c = Registry.get().lookupSingle(ServerValidator.class,
						serverValidator.getClass());
				if (c != null) {
					ServerValidatorHandler handler = (ServerValidatorHandler) Registry
							.get().instantiateSingle(ServerValidator.class,
									serverValidator.getClass());
					handler.handle(serverValidator, getEntityManager());
				}
			}
			result.add(serverValidator);
		}
		return result;
	}

	public List<ObjectCacheItemResult> cache(List<ObjectCacheItemSpec> specs)
			throws Exception {
		fixPermissionsManager();
		ObjectPersistenceHelper.get();
		long t1 = System.currentTimeMillis();
		ThreadlocalTransformManager tm = ThreadlocalTransformManager.cast();
		tm.setEntityManager(getEntityManager());
		List<ObjectCacheItemResult> cache = tm.cache(specs);
		cache = new EntityUtils().detachedClone(cache);
		EntityLayerLocator.get().getMetricLogger().debug(
				"cache get - total (ms):" + (System.currentTimeMillis() - t1));
		return cache;
	}

	private <T> T getItemByKeyValue(Class<T> clazz, String key, Object value,
			boolean createIfNonexistent, Long ignoreId, boolean caseInsensitive) {
		try {
			fixPermissionsManager();
			String eql = String.format(
					value == null ? "from %s where %s is null"
							: caseInsensitive ? "from %s where lower(%s) = ?"
									: "from %s where %s = ?", clazz
							.getSimpleName(), key);
			if (ignoreId != null) {
				eql += " AND id != " + ignoreId;
			}
			Query q = getEntityManager().createQuery(eql);
			if (value != null) {
				q.setParameter(1, caseInsensitive ? value.toString()
						.toLowerCase() : value);
			}
			List l = q.getResultList();
			if (l.size() == 0 && createIfNonexistent) {
				T inst = clazz.newInstance();
				getEntityManager().persist(inst);
				PropertyDescriptor descriptor = SEUtilities.descriptorByName(
						inst.getClass(), key);
				descriptor.getWriteMethod().invoke(inst, value);
				return inst;
			}
			return (T) ((l.size() == 0) ? null : l.get(0));
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	private Integer getLastTransformId(long clientInstanceId, int firstRequestId) {
		String eql = String.format("select max(dtrq.requestId) as maxId "
				+ "from %s dtrq where dtrq.clientInstance.id=%s ",
				getImplementation(DataTransformRequestPersistent.class)
						.getSimpleName(), clientInstanceId, firstRequestId);
		Integer result = (Integer) getEntityManager().createQuery(eql)
				.getSingleResult();
		return result;
	}

	private void persistWrappables(HasId hi) {
		try {
			PropertyDescriptor[] pds = Introspector.getBeanInfo(hi.getClass())
					.getPropertyDescriptors();
			for (PropertyDescriptor pd : pds) {
				if (pd.getReadMethod() != null) {
					WrapperInfo info = pd.getReadMethod().getAnnotation(
							WrapperInfo.class);
					if (info != null) {
						Object obj = pd.getReadMethod().invoke(hi,
								CommonUtils.EMPTY_OBJECT_ARRAY);
						if (obj instanceof GwtPersistableObject) {
							if (!(GwtMultiplePersistable.class
									.isAssignableFrom(obj.getClass()))) {
								throw new Exception(
										"Trying to persist a per-user object via wrapping");
							}
							GwtPersistableObject gwpo = (GwtPersistableObject) obj;
							if (info.toStringPropertyName().length() != 0) {
								PropertyDescriptor tspd = SEUtilities
										.descriptorByName(hi.getClass(), info
												.toStringPropertyName());
								tspd.getWriteMethod().invoke(hi,
										gwpo.toString());
							}
							Long persistId = persist(gwpo);
							PropertyDescriptor idpd = SEUtilities
									.descriptorByName(hi.getClass(), info
											.idPropertyName());
							idpd.getWriteMethod().invoke(hi, persistId);
						}
					}
				}
			}
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	protected <T> T findImplInstance(Class<? extends T> clazz, long id) {
		Class<?> implClazz = getImplementation(clazz);
		return (T) getEntityManager().find(implClazz, id);
	}

	public abstract void fixPermissionsManager();

	protected abstract void fixPermissionsManager(boolean forWriting);

	protected abstract EntityManager getEntityManager();
}
