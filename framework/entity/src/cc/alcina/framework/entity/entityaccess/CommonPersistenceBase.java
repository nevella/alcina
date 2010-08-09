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

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import cc.alcina.framework.common.client.CommonLocator;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.actions.ActionLogItem;
import cc.alcina.framework.common.client.csobjects.ObjectCacheItemResult;
import cc.alcina.framework.common.client.csobjects.ObjectCacheItemSpec;
import cc.alcina.framework.common.client.csobjects.SearchResultsBase;
import cc.alcina.framework.common.client.entity.GwtMultiplePersistable;
import cc.alcina.framework.common.client.entity.Iid;
import cc.alcina.framework.common.client.entity.PersistentSingleton;
import cc.alcina.framework.common.client.entity.WrapperPersistable;
import cc.alcina.framework.common.client.gwittir.validator.ServerUniquenessValidator;
import cc.alcina.framework.common.client.gwittir.validator.ServerValidator;
import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId.HiliHelper;
import cc.alcina.framework.common.client.logic.domaintransform.AlcinaPersistentEntityImpl;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.permissions.IGroup;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.permissions.IVersionableOwnable;
import cc.alcina.framework.common.client.logic.permissions.PermissionsException;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager.LoginState;
import cc.alcina.framework.common.client.logic.reflection.WrapperInfo;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.domaintransform.DomainTransformLayerWrapper;
import cc.alcina.framework.entity.domaintransform.ObjectPersistenceHelper;
import cc.alcina.framework.entity.domaintransform.ThreadlocalTransformManager;
import cc.alcina.framework.entity.domaintransform.TransformPersistenceToken;
import cc.alcina.framework.entity.entityaccess.UnwrapInfoItem.UnwrapInfoContainer;
import cc.alcina.framework.entity.logic.EntityLayerLocator;
import cc.alcina.framework.entity.util.EntityUtils;
import cc.alcina.framework.entity.util.GraphProjection;
import cc.alcina.framework.entity.util.GraphProjection.GraphProjectionFilter;
import cc.alcina.framework.entity.util.GraphProjection.InstantiateImplCallback;

@SuppressWarnings("unchecked")
/**
 *
 * @author Nick Reddel
 */
public abstract class CommonPersistenceBase<CI extends ClientInstance, U extends IUser, G extends IGroup, IID extends Iid>
		implements CommonPersistenceLocal {
	private static Map<Long, Integer> clientInstanceAuthMap = new HashMap<Long, Integer>();

	@PersistenceContext
	private EntityManager entityManager;

	public CommonPersistenceBase() {
		ObjectPersistenceHelper.get();
	}

	public CommonPersistenceBase(EntityManager em) {
		this();
		this.setEntityManager(em);
	}

	public void bulkDelete(Class clazz, Collection<Long> ids) {
		if (!EntityLayerLocator.get().jpaImplementation().bulkDelete(
				getEntityManager(), clazz, ids)) {
			List<Object> resultList = getEntityManager().createQuery(
					String.format("from %s where id in %s ", clazz
							.getSimpleName(), EntityUtils
							.longListToIdClause(ids))).getResultList();
			for (Object object : resultList) {
				getEntityManager().remove(object);
			}
		}
	}

	public List<ObjectCacheItemResult> cache(List<ObjectCacheItemSpec> specs)
			throws Exception {
		connectPermissionsManagerToLiveObjects();
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

	public void connectPermissionsManagerToLiveObjects() {
		connectPermissionsManagerToLiveObjects(false);
	}

	public void connectPermissionsManagerToLiveObjects(boolean forWriting) {
		if (getEntityManager().contains(PermissionsManager.get().getUser())) {
			if (!forWriting) {
				PermissionsManager.get().getUserGroups();
			}
			return;
		}
		String userName = PermissionsManager.get().getUserName();
		if (PermissionsManager.get().getLoginState() == LoginState.NOT_LOGGED_IN) {
			userName = getAnonymousUserName();
		}
		PermissionsManager.get().setUser(getUserByName(userName));
		if (!forWriting) {
			PermissionsManager.get().getUserGroups();
		}
	}

	public CI createClientInstance() {
		connectPermissionsManagerToLiveObjects(true);
		Class<? extends CI> clientInstanceImpl = (Class<? extends CI>) getImplementation(ClientInstance.class);
		try {
			CI impl = clientInstanceImpl.newInstance();
			getEntityManager().persist(impl);
			impl.setHelloDate(new Date());
			impl.setUser(PermissionsManager.get().getUser());
			impl.setAuth(Math.abs(new Random().nextInt()));
			getEntityManager().flush();
			clientInstanceAuthMap.put(impl.getId(), impl.getAuth());
			CI instance = new EntityUtils().detachedClone(impl, false);
			return instance;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

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

	public <T extends HasId> T ensurePersistent(T obj) {
		if (getEntityManager().contains(obj)) {
			return obj;
		}
		return (T) getEntityManager().find(obj.getClass(), obj.getId());
	}

	public void expandExceptionInfo(DomainTransformLayerWrapper wrapper) {
		ThreadlocalTransformManager tm = ThreadlocalTransformManager.cast();
		tm.resetTltm(wrapper.locatorMap);
		tm.setEntityManager(getEntityManager());
		for (DomainTransformException ex : wrapper.response
				.getTransformExceptions()) {
			tryAddSourceObjectName(ex);
		}
	}

	public <A> Set<A> getAll(Class<A> clazz) {
		Query query = getEntityManager().createQuery(
				String.format("from %s ", clazz.getSimpleName()));
		List results = query.getResultList();
		return new LinkedHashSet<A>(results);
	}

	public IUser getAnonymousUser() {
		return getUserByName(getAnonymousUserName());
	}

	public abstract String getAnonymousUserName();

	public EntityManager getEntityManager() {
		return entityManager;
	}

	public IID getIidByKey(String iid) {
		List<IID> l = getEntityManager()
				.createQuery(
						"from "
								+ getImplementationSimpleClassName(Iid.class)
								+ " i inner join fetch i.rememberMeUser where i.instanceId = ?")
				.setParameter(1, iid).getResultList();
		return (IID) ((l.size() == 0) ? getNewImplementationInstance(Iid.class)
				: l.get(0));
	}

	@SuppressWarnings("unchecked")
	public <A> Class<? extends A> getImplementation(Class<A> clazz) {
		return Registry.get().lookupSingle(AlcinaPersistentEntityImpl.class,
				clazz);
	}

	public String getImplementationSimpleClassName(Class<?> clazz) {
		return getImplementation(clazz).getSimpleName();
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

	public <A> A getNewImplementationInstance(Class<A> clazz) {
		try {
			return getImplementation(clazz).newInstance();
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public <T extends WrapperPersistable> WrappedObject<T> getObjectWrapperForUser(
			Class<T> c, long id) throws Exception {
		connectPermissionsManagerToLiveObjects();
		WrappedObject<T> wrapper = EntityLayerLocator.get()
				.wrappedObjectProvider().getObjectWrapperForUser(c, id,
						getEntityManager());
		checkWrappedObjectAccess(null, wrapper, c);
		return wrapper;
	}

	public U getSystemUser() {
		return getUserByName(getSystemUserName());
	}

	public U getSystemUser(boolean clean) {
		return (U) getUserByName(getSystemUserName(), clean);
	}

	public abstract String getSystemUserName();

	public U getUserByName(String userName) {
		List<U> l = getEntityManager().createQuery(
				"select distinct u from "
						+ getImplementationSimpleClassName(IUser.class) + " u "
						+ "left join fetch u.primaryGroup "
						+ "left join fetch u.secondaryGroups g "
						+ "left join fetch g.memberOfGroups sg "
						+ "where u.userName = ?").setParameter(1, userName)
				.getResultList();
		return (l.size() == 0) ? null : l.get(0);
	}

	public IUser getUserByName(String userName, boolean clean) {
		IUser user = getUserByName(userName);
		PermissionsManager.get().getUserGroups(user);
		IUser cleaned = new EntityUtils().detachedClone(user,
				clean ? createUserAndGroupInstantiator() : null);
		return cleaned;
	}

	protected abstract InstantiateImplCallback createUserAndGroupInstantiator();

	public List<ActionLogItem> listLogItemsForClass(String className, int count) {
		List list = getEntityManager().createQuery(
				"from " + getImplementationSimpleClassName(ActionLogItem.class)
						+ " a where a.actionClassName=? order"
						+ " by a.actionDate DESC").setParameter(1, className)
				.setMaxResults(count).getResultList();
		return new EntityUtils().detachedClone(list);
	}

	public long log(String message, String componentKey) {
		// not required...useful but
		return 0;
	}

	public void logActionItem(ActionLogItem result) {
		connectPermissionsManagerToLiveObjects();
		getEntityManager().merge(result);
	}

	public long merge(HasId hi) {
		connectPermissionsManagerToLiveObjects();
		persistWrappables(hi);
		HasId merge = getEntityManager().merge(hi);
		return merge.getId();
	}

	public IUser mergeUser(IUser user) {
		connectPermissionsManagerToLiveObjects();
		IUser merge = getEntityManager().merge(user);
		getEntityManager().flush();
		return merge;
	}

	public <WP extends WrapperPersistable> Long persist(WP gwpo)
			throws Exception {
		connectPermissionsManagerToLiveObjects();
		WrappedObject<WP> wrapper = (WrappedObject<WP>) getObjectWrapperForUser(
				gwpo.getClass(), gwpo.getId());
		wrapper.setObject(gwpo);
		return wrapper.getId();
	}

	public UnwrapInfoContainer prepareUnwrap(Class<? extends HasId> clazz,
			Long id, GraphProjectionFilter fieldFilter,
			GraphProjectionFilter dataFilter) {
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
							Class<? extends WrapperPersistable> pType = (Class<? extends WrapperPersistable>) pd
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
			return new GraphProjection(fieldFilter, dataFilter).project(result,
					null);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public void remove(Object o) {
		getEntityManager().remove(o);
	}

	public SearchResultsBase search(SearchDefinition def, int pageNumber) {
		connectPermissionsManagerToLiveObjects();
		String message = def.validatePermissions();
		if (message != null) {
			throw new WrappedRuntimeException(new PermissionsException(message));
		}
		Searcher searcher = (Searcher) Registry.get().instantiateSingle(
				Searcher.class, def.getClass());
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

	public DomainTransformLayerWrapper transformInPersistenceContext(
			TransformPersister persister, TransformPersistenceToken token) {
		return persister.transformInPersistenceContext(token, this,
				getEntityManager());
	}

	public <T extends HasId> Collection<T> unwrap(Collection<T> wrappers) {
		preloadWrappedObjects(wrappers);
		for (HasId wrapper : wrappers) {
			unwrap(wrapper);
		}
		return wrappers;
	}

	public HasId unwrap(HasId wrapper) {
		try {
			connectPermissionsManagerToLiveObjects();
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
							Class<? extends WrapperPersistable> pType = (Class<? extends WrapperPersistable>) pd
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

	public void updateIid(String iidKey, String userName, boolean rememberMe) {
		IID iid = getIidByKey(iidKey);
		iid.setInstanceId(iidKey);
		if (rememberMe) {
			iid.setRememberMeUser(getUserByName(userName));
		} else {
			iid.setRememberMeUser(null);
		}
		getEntityManager().merge(iid);
	}

	public <T extends ServerValidator> List<T> validate(List<T> validators) {
		connectPermissionsManagerToLiveObjects();
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

	private <T> T getItemByKeyValue(Class<T> clazz, String key, Object value,
			boolean createIfNonexistent, Long ignoreId, boolean caseInsensitive) {
		try {
			connectPermissionsManagerToLiveObjects();
			String eql = String.format(
					value == null ? "from %s where %s is null"
							: caseInsensitive ? "from %s where lower(%s) = ?"
									: "from %s where %s = ?", clazz
							.getSimpleName(), key);
			if (ignoreId != null) {
				eql += " AND id != " + ignoreId;
			}
			if (HasId.class.isAssignableFrom(clazz)) {
				eql += " order by id asc";
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
						if (obj instanceof WrapperPersistable) {
							if (!(GwtMultiplePersistable.class
									.isAssignableFrom(obj.getClass()))) {
								throw new Exception(
										"Trying to persist a per-user object via wrapping");
							}
							WrapperPersistable gwpo = (WrapperPersistable) obj;
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

	private void setEntityManager(EntityManager em) {
		entityManager = em;
	}

	private void tryAddSourceObjectName(
			DomainTransformException transformException) {
		try {
			HasIdAndLocalId object = TransformManager.get().getObject(
					transformException.getEvent());
			transformException.setSourceObjectName(CommonLocator.get()
					.classLookup().displayNameForObject(object));
		} catch (Exception e) {
			// we tried
			e.printStackTrace();
		}
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
			if (PermissionsManager.get().isPermissible(PermissionsManager.ADMIN_PERMISSIBLE)) {
				if (!PermissionsManager.get().isPermissible(PermissionsManager.ROOT_PERMISSIBLE)) {
					System.err
							.println(CommonUtils
									.format(
											"Warn - allowing access to %1 : %2 only via admin override",
											HiliHelper.asDomainPoint(wrapper),
											HiliHelper.asDomainPoint(wrapped)));
				}
				return;// permitted
			}
			throw new PermissionsException(CommonUtils.format(
					"Permissions exception: "
							+ "access denied to object  %1 for user %2",
					wrapped.getId(), PermissionsManager.get().getUserId()));
		}
	}

	protected <T> T findImplInstance(Class<? extends T> clazz, long id) {
		Class<?> implClazz = getImplementation(clazz);
		return (T) getEntityManager().find(implClazz, id);
	}
}
