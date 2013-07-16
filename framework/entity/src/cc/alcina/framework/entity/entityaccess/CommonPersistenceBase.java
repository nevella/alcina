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
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import cc.alcina.framework.common.client.CommonLocator;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.actions.ActionLogItem;
import cc.alcina.framework.common.client.csobjects.ObjectCacheItemResult;
import cc.alcina.framework.common.client.csobjects.ObjectCacheItemSpec;
import cc.alcina.framework.common.client.csobjects.SearchResultsBase;
import cc.alcina.framework.common.client.entity.ClientLogRecord;
import cc.alcina.framework.common.client.entity.ClientLogRecord.ClientLogRecords;
import cc.alcina.framework.common.client.entity.ClientLogRecordPersistent;
import cc.alcina.framework.common.client.entity.GwtMultiplePersistable;
import cc.alcina.framework.common.client.entity.Iid;
import cc.alcina.framework.common.client.entity.WrapperPersistable;
import cc.alcina.framework.common.client.gwittir.validator.ServerUniquenessValidator;
import cc.alcina.framework.common.client.gwittir.validator.ServerValidator;
import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.AlcinaPersistentEntityImpl;
import cc.alcina.framework.common.client.logic.domaintransform.ClassRef;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.permissions.IGroup;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.permissions.PermissionsException;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager.LoginState;
import cc.alcina.framework.common.client.logic.reflection.WrapperInfo;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LongPair;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.domaintransform.DomainTransformEventPersistent;
import cc.alcina.framework.entity.domaintransform.DomainTransformLayerWrapper;
import cc.alcina.framework.entity.domaintransform.DomainTransformRequestPersistent;
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
	// note - this'll be the stack depth of the eql ast processor
	private static final int PRECACHE_RQ_SIZE = 500;

	private static Class<? extends HandshakeObjectProvider> handshakeObjectProviderClass = DefaultHandshakeObjectProvider.class;

	public CommonPersistenceBase() {
		ObjectPersistenceHelper.get();
	}

	public CommonPersistenceBase(EntityManager em) {
		this();
		this.setEntityManager(em);
	}

	/**
	 * An implementation can perform a (direct sql) accelerated delete - but you
	 * may want to avoid this if the entities to delete require delete/cascade
	 */
	public void bulkDelete(Class clazz, Collection<Long> ids, boolean tryImpl) {
		AppPersistenceBase.checkNotReadOnly();
		if (!tryImpl
				|| !EntityLayerLocator.get().jpaImplementation()
						.bulkDelete(getEntityManager(), clazz, ids)) {
			List<Object> resultList = getEntityManager().createQuery(
					String.format("from %s where id in %s ",
							clazz.getSimpleName(),
							EntityUtils.longsToIdClause(ids))).getResultList();
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
		EntityLayerLocator
				.get()
				.getMetricLogger()
				.debug("cache get - total (ms):"
						+ (System.currentTimeMillis() - t1));
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

	/**
	 * Note...we deliberately crop the client instance user - at the servlet
	 * layer we (should) always want it lightweight
	 */
	@Override
	public CI createClientInstance(String userAgent) {
		return (CI) getHandshakeObjectProvider()
				.createClientInstance(userAgent);
	}

	public <T> T ensureObject(T t, String key, String value) throws Exception {
		T newT = (T) getItemByKeyValue(t.getClass(), key, value, false);
		if (newT != null) {
			return newT;
		}
		AppPersistenceBase.checkNotReadOnly();
		PropertyDescriptor descriptor = SEUtilities.descriptorByName(
				t.getClass(), key);
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

	@Override
	public <T> T findImplInstance(Class<? extends T> clazz, long id) {
		Class<?> implClazz = getImplementation(clazz);
		return (T) getEntityManager().find(implClazz, id);
	}

	public <A> Set<A> getAll(Class<A> clazz) {
		Query query = getEntityManager().createQuery(
				String.format("from %s ", clazz.getSimpleName()));
		List results = query.getResultList();
		return new LinkedHashSet<A>(results);
	}

	public IUser getAnonymousUser() {
		return getUserByName(getAnonymousUserName(), true);
	}

	@Override
	public abstract String getAnonymousUserName();

	@Override
	public <US extends IUser> US getCleanedUserById(long userId) {
		Class<? extends IUser> impl = getImplementation(IUser.class);
		IUser found = getEntityManager().find(impl, userId);
		if (found != null) {
			return (US) getUserByName(found.getUserName(), true);
		}
		return null;
	}

	public abstract EntityManager getEntityManager();

	private HandshakeObjectProvider handshakeObjectProvider;

	public HandshakeObjectProvider getHandshakeObjectProvider() {
		if (handshakeObjectProvider == null) {
			try {
				handshakeObjectProvider = handshakeObjectProviderClass
						.newInstance();
				handshakeObjectProvider.setCommonPersistence(this);
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
		return handshakeObjectProvider;
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
		if (t == null) {
			return t;
		}
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

	@Override
	public <T> T getItemByKeyValue(Class<T> clazz, String key, Object value,
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
				AppPersistenceBase.checkNotReadOnly();
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

	public <T> T getItemByKeyValueKeyValue(Class<T> clazz, String key1,
			Object value1, String key2, Object value2) {
		String eql = String.format("from %s where %s=? and %s=?",
				clazz.getSimpleName(), key1, key2);
		Query q = getEntityManager().createQuery(eql).setParameter(1, value1)
				.setParameter(2, value2);
		List l = q.getResultList();
		return (T) ((l.size() == 0) ? null : l.get(0));
	}

	public <T> List<T> getItemsByIdsAndClean(Class<T> clazz,
			Collection<Long> ids,
			InstantiateImplCallback instantiateImplCallback) {
		String eql = String.format("from %s where  id in %s order by id",
				clazz.getSimpleName(), EntityUtils.longsToIdClause(ids));
		List results = getEntityManager().createQuery(eql).getResultList();
		return new EntityUtils()
				.detachedClone(results, instantiateImplCallback);
	}

	public long getLastTransformId() {
		String eql = String
				.format("select max(dtep.id) from %s dtep ",
						getImplementationSimpleClassName(DomainTransformEventPersistent.class));
		Long l = (Long) getEntityManager().createQuery(eql).getSingleResult();
		return CommonUtils.lv(l);
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
				.wrappedObjectProvider()
				.getObjectWrapperForUser(c, id, getEntityManager());
		checkWrappedObjectAccess(null, wrapper, c);
		return wrapper;
	}

	@Override
	public LongPair getMinMaxIdRange(Class clazz) {
		Class implClass = getImplementation(clazz);
		clazz = implClass == null ? clazz : implClass;
		String eql = String.format("select min(id),max(id) from %s",
				clazz.getSimpleName());
		Object[] result = (Object[]) getEntityManager().createQuery(eql)
				.getSingleResult();
		return result[0] == null ? new LongPair() : new LongPair(
				(Long) result[0], (Long) result[1]);
	}

	@Override
	public List<DomainTransformRequestPersistent> getPersistentTransformRequests(
			long fromId, long toId, String specificIds) {
		String idFilter = specificIds == null ? String.format(
				"dtrp.id>=%s and dtrp.id<=%s", fromId, toId) : String.format(
				"dtrp.id in (%s)", specificIds);
		String eql = String.format("select distinct dtrp " + "from %s dtrp "
				+ "inner join fetch dtrp.events "
				+ "inner join fetch dtrp.clientInstance " + " where %s "
				+ "order by dtrp.id",
				getImplementation(DomainTransformRequestPersistent.class)
						.getSimpleName(), idFilter);
		List<DomainTransformRequestPersistent> dtrps = new ArrayList<DomainTransformRequestPersistent>(
				getEntityManager().createQuery(eql).getResultList());
		return new EntityUtils().detachedClone(dtrps, EntityLayerLocator.get()
				.jpaImplementation().getClassrefInstantiator());
	}

	public U getSystemUser() {
		return getUserByName(getSystemUserName());
	}

	public U getSystemUser(boolean clean) {
		return (U) getUserByName(getSystemUserName(), clean);
	}

	public abstract String getSystemUserName();

	/**
	 * Note, if you're going to use the user on the servlet layer, always use
	 * the 'clean' version of this function
	 */
	public U getUserByName(String userName) {
		List<U> l = getEntityManager()
				.createQuery(
						"select distinct u from "
								+ getImplementationSimpleClassName(IUser.class)
								+ " u " + "left join fetch u.primaryGroup "
								+ "left join fetch u.secondaryGroups g "
								+ "left join fetch g.memberOfGroups sg "
								+ "where u.userName = ?")
				.setParameter(1, userName).getResultList();
		return (l.size() == 0) ? null : l.get(0);
	}

	/**
	 * Assume that this is always an in-system call (since we're after a
	 * specific user) so _don't clean based on permissions_
	 */
	public IUser getUserByName(String userName, boolean clean) {
		IUser user = getUserByName(userName);
		PermissionsManager.get().getUserGroups(user);
		IUser cleaned = new EntityUtils().detachedCloneIgnorePermissions(user,
				clean ? createUserAndGroupInstantiator() : null);
		return cleaned;
	}

	@Override
	public G getGroupByName(String groupName) {
		List<G> l = getEntityManager()
				.createQuery(
						"select distinct g from "
								+ getImplementationSimpleClassName(IGroup.class)
								+ " g "
								+ "left join fetch g.memberOfGroups sg "
								+ "left join fetch g.memberUsers su "
								+ "where g.groupName = ?")
				.setParameter(1, groupName).getResultList();
		return (l.size() == 0) ? null : l.get(0);
	}

	/**
	 * Assume that this is always an in-system call (since we're after a
	 * specific user) so _don't clean based on permissions_
	 */
	@Override
	public G getGroupByName(String groupName, boolean clean) {
		G group = getGroupByName(groupName);
		G cleaned = new EntityUtils().detachedCloneIgnorePermissions(group,
				clean ? createUserAndGroupInstantiator() : null);
		return cleaned;
	}

	public List<ActionLogItem> listLogItemsForClass(String className, int count) {
		List list = getEntityManager()
				.createQuery(
						"from "
								+ getImplementationSimpleClassName(ActionLogItem.class)
								+ " a where a.actionClassName=? order"
								+ " by a.actionDate DESC")
				.setParameter(1, className).setMaxResults(count)
				.getResultList();
		return new EntityUtils().detachedClone(list);
	}

	public long log(String message, String componentKey) {
		// not required...useful but
		return 0;
	}

	public void logActionItem(ActionLogItem result) {
		AppPersistenceBase.checkNotReadOnly();
		connectPermissionsManagerToLiveObjects();
		getEntityManager().merge(result);
	}

	public long merge(HasId hi) {
		AppPersistenceBase.checkNotReadOnly();
		connectPermissionsManagerToLiveObjects();
		persistWrappables(hi);
		HasId merge = getEntityManager().merge(hi);
		return merge.getId();
	}

	public IUser mergeUser(IUser user) {
		AppPersistenceBase.checkNotReadOnly();
		connectPermissionsManagerToLiveObjects();
		IUser merge = getEntityManager().merge(user);
		getEntityManager().flush();
		return merge;
	}

	public <WP extends WrapperPersistable> Long persist(WP gwpo)
			throws Exception {
		AppPersistenceBase.checkNotReadOnly();
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
							if (info.defaultImplementationType() != Void.class) {
								pType = info.defaultImplementationType();
							}
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
		AppPersistenceBase.checkNotReadOnly();
		if (o instanceof HasIdAndLocalId) {
			HasIdAndLocalId hili = (HasIdAndLocalId) getEntityManager().find(
					o.getClass(), ((HasIdAndLocalId) o).getId());
			getEntityManager().remove(hili);
		} else {
			throw new RuntimeException("Cannot remove detached non-hili " + o);
		}
	}

	public SearchResultsBase search(SearchDefinition def, int pageNumber) {
		connectPermissionsManagerToLiveObjects();
		String message = def.validatePermissions();
		if (message != null) {
			throw new WrappedRuntimeException(new PermissionsException(message));
		}
		Searcher searcher = (Searcher) Registry.get().instantiateSingle(
				Searcher.class, def.getClass());
		try {
			LooseContext.push();
			SearchResultsBase result = searcher.search(def, pageNumber,
					getEntityManager());
			if (LooseContext.getBoolean(Searcher.CONTEXT_RESULTS_ARE_DETACHED)) {
				return result;
			} else {
				return new EntityUtils().detachedClone(result);
			}
		} finally {
			LooseContext.pop();
		}
	}

	public abstract void setEntityManager(EntityManager entityManager);

	public void setField(Class clazz, Long id, String key, Object value)
			throws Exception {
		AppPersistenceBase.checkNotReadOnly();
		Object inst = getEntityManager().find(clazz, id);
		PropertyDescriptor descriptor = SEUtilities
				.descriptorByName(clazz, key);
		descriptor.getWriteMethod().invoke(inst, value);
		getEntityManager().merge(inst);
	}

	public DomainTransformLayerWrapper transformInPersistenceContext(
			TransformPersister persister, TransformPersistenceToken token) {
		AppPersistenceBase.checkNotReadOnly();
		return persister.transformInPersistenceContext(token, this,
				getEntityManager());
	}

	public <T extends HasId> Collection<T> unwrap(Collection<T> wrappers) {
		preloadWrappedObjects(wrappers);
		RuntimeException lastException = null;
		for (HasId wrapper : wrappers) {
			try {
				unwrap(wrapper);
			} catch (RuntimeException e) {
				System.out.println(e.getMessage());
				lastException = e;
			}
		}
		if (lastException != null) {
			throw lastException;
		}
		return wrappers;
	}

	public HasId unwrap(HasId wrapper) {
		try {
			connectPermissionsManagerToLiveObjects();
			new WrappedObjectPersistence().unwrap(wrapper, getEntityManager(),
					EntityLayerLocator.get().wrappedObjectProvider());
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
		return wrapper;
	}

	public void updateIid(String iidKey, String userName, boolean rememberMe) {
		getHandshakeObjectProvider().updateIid(iidKey, userName, rememberMe);
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
					Object item = getItemByKeyValue(suv.getObjectClass(),
							suv.getPropertyName(), value, false, suv.getOkId(),
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
					value = String.format(suv.getValueTemplate(),
							suv.getValue() == null ? "" : suv.getValue(), ctr);
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

	public boolean validateClientInstance(long id, int auth) {
		if (Registry.impl(ClientInstanceAuthenticationCache.class).isCached(id,
				auth)) {
			return true;
		}
		Class<? extends CI> clientInstanceImpl = (Class<? extends CI>) getImplementation(ClientInstance.class);
		CI ci = getItemById(clientInstanceImpl, id);
		return ci != null && ci.getAuth() == auth;
	}

	/**
	 * Used for supporting mixed rpc/transform domain loads
	 * 
	 * @param userId
	 */
	public TransformCache warmupTransformCache() {
		TransformCache result = new TransformCache();
		List<DomainTransformEventPersistent> recentTransforms = getRecentTransforms(
				getSharedTransformClasses(), getSharedTransformWarmupSize(), 0L);
		result.sharedTransformClasses = getSharedTransformClasses();
		result.perUserTransformClasses = getPerUserTransformClasses();
		if (!recentTransforms.isEmpty()) {
			result.putSharedTransforms(recentTransforms);
			recentTransforms = getRecentTransforms(
					getPerUserTransformClasses(), 0, result.cacheValidFrom);
			result.putPerUserTransforms(recentTransforms);
		} else {
			result.invalid = true;
		}
		return result;
	}

	private void persistWrappables(HasId hi) {
		AppPersistenceBase.checkNotReadOnly();
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
										.descriptorByName(hi.getClass(),
												info.toStringPropertyName());
								tspd.getWriteMethod().invoke(hi,
										gwpo.toString());
							}
							Long persistId = persist(gwpo);
							PropertyDescriptor idpd = SEUtilities
									.descriptorByName(hi.getClass(),
											info.idPropertyName());
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
									.descriptorByName(wrapper.getClass(),
											info.idPropertyName());
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
			for (int i = 0; i < wrapperIds.size(); i += PRECACHE_RQ_SIZE) {
				List<Long> subList = wrapperIds.subList(i,
						Math.min(wrapperIds.size(), i + PRECACHE_RQ_SIZE));
				Query query = getEntityManager().createQuery(
						"from "
								+ getImplementation(WrappedObject.class)
										.getSimpleName() + " where id in "
								+ EntityUtils.longsToIdClause(subList));
				query.getResultList();
			}
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	private void tryAddSourceObjectName(
			DomainTransformException transformException) {
		try {
			HasIdAndLocalId object = TransformManager.get().getObject(
					transformException.getEvent());
			transformException.setSourceObjectName(CommonLocator.get()
					.classLookup().displayNameForObject(object));
		} catch (Exception e) {
			System.out.println("Unable to add source object name - reason: "
					+ e.getMessage());
			// we tried
			// e.printStackTrace();
		}
	}

	protected void checkWrappedObjectAccess(HasId wrapper,
			WrappedObject wrapped, Class clazz) throws PermissionsException {
		new WrappedObjectPersistence().checkWrappedObjectAccess(wrapper,
				wrapped, clazz);
	}

	protected abstract InstantiateImplCallback createUserAndGroupInstantiator();

	protected Collection<Class> getPerUserTransformClasses() {
		return new ArrayList<Class>();
	}

	protected List<DomainTransformEventPersistent> getRecentTransforms(
			Collection<Class> sourceObjectClasses, int maxTransforms,
			long sinceId) {
		Set<Long> classRefIds = new HashSet<Long>();
		for (Class clazz : sourceObjectClasses) {
			ClassRef classRef = ClassRef.forClass(clazz);
			classRefIds.add(classRef.getId());
		}
		String eql = String
				.format("select dtep from %s dtep "
						+ "where  dtep.id>? and dtep.objectClassRef.id in %s "
						+ "order by dtep.id desc",
						getImplementationSimpleClassName(DomainTransformEventPersistent.class),
						EntityUtils.longsToIdClause(classRefIds));
		Query query = getEntityManager().createQuery(eql).setParameter(1,
				sinceId);
		if (sinceId == 0) {
			query.setMaxResults(maxTransforms);
		}
		return new EntityUtils().detachedClone(query.getResultList(),
				EntityLayerLocator.get().jpaImplementation()
						.getClassrefInstantiator());
	}

	protected Collection<Class> getSharedTransformClasses() {
		return new ArrayList<Class>();
	}

	/**
	 * This is deliberately low - for big initial objects size, 1000 is quite
	 * reasonable (i.e. client load will be much faster)
	 */
	protected int getSharedTransformWarmupSize() {
		return 100;
	}

	@Override
	public ClientInstance getClientInstance(String clientInstanceId) {
		return getHandshakeObjectProvider().getClientInstance(
				Long.parseLong(clientInstanceId));
	}

	static class DefaultHandshakeObjectProvider implements
			HandshakeObjectProvider {
		private CommonPersistenceBase cp;

		@Override
		public ClientInstance createClientInstance(String userAgent) {
			AppPersistenceBase.checkNotReadOnly();
			cp.connectPermissionsManagerToLiveObjects(true);
			Class<? extends ClientInstance> clientInstanceImpl = cp
					.getImplementation(ClientInstance.class);
			try {
				ClientInstance impl = clientInstanceImpl.newInstance();
				cp.getEntityManager().persist(impl);
				impl.setHelloDate(new Date());
				impl.setUser(PermissionsManager.get().getUser());
				impl.setAuth(Math.abs(new Random().nextInt()));
				impl.setUserAgent(userAgent);
				cp.getEntityManager().flush();
				IUser clonedUser = (IUser) cp
						.getNewImplementationInstance(IUser.class);
				ResourceUtilities.copyBeanProperties(PermissionsManager.get()
						.getUser(), clonedUser, null, false, Arrays
						.asList(new String[] { "primaryGroup",
								"secondaryGroups" }));
				ClientInstance instance = new EntityUtils().detachedClone(impl,
						false);
				Registry.impl(ClientInstanceAuthenticationCache.class)
						.cacheAuthentication(instance);
				instance.setUser(new EntityUtils().detachedClone(clonedUser,
						false));
				return instance;
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}

		@Override
		public void updateIid(String iidKey, String userName, boolean rememberMe) {
			Iid iid = cp.getIidByKey(iidKey);
			iid.setInstanceId(iidKey);
			if (rememberMe) {
				iid.setRememberMeUser(cp.getUserByName(userName));
			} else {
				iid.setRememberMeUser(null);
			}
			Registry.impl(ClientInstanceAuthenticationCache.class)
					.cacheIid(iid);
			cp.getEntityManager().merge(iid);
		}

		@Override
		public void setCommonPersistence(CommonPersistenceBase commonPersistence) {
			this.cp = commonPersistence;
		}

		@Override
		public ClientInstance getClientInstance(long clientInstanceId) {
			cp.connectPermissionsManagerToLiveObjects(true);
			ClientInstance instance = (ClientInstance) cp.getEntityManager()
					.find(cp.getImplementation(ClientInstance.class),
							clientInstanceId);
			return new EntityUtils().detachedClone(instance, false);
		}
	}

	public static class ReadonlyHandshakeObjectProvider implements
			HandshakeObjectProvider {
		static long clientInstanceIdCounter = 0;

		private CommonPersistenceBase cp;

		@Override
		public ClientInstance createClientInstance(String userAgent) {
			long newId = 0;
			synchronized (ReadonlyHandshakeObjectProvider.class) {
				if (clientInstanceIdCounter == 0) {
					clientInstanceIdCounter = (Long) cp
							.getEntityManager()
							.createQuery(
									String.format(
											"select max(id) from %s",
											cp.getImplementationSimpleClassName(ClientInstance.class)))
							.getSingleResult();
				}
				newId = ++clientInstanceIdCounter;
			}
			cp.connectPermissionsManagerToLiveObjects(true);
			Class<? extends ClientInstance> clientInstanceImpl = cp
					.getImplementation(ClientInstance.class);
			try {
				ClientInstance impl = clientInstanceImpl.newInstance();
				impl.setId(newId);
				impl.setHelloDate(new Date());
				impl.setUser(PermissionsManager.get().getUser());
				impl.setAuth(Math.abs(new Random().nextInt()));
				impl.setUserAgent(userAgent);
				IUser clonedUser = (IUser) cp
						.getNewImplementationInstance(IUser.class);
				ResourceUtilities.copyBeanProperties(PermissionsManager.get()
						.getUser(), clonedUser, null, false, Arrays
						.asList(new String[] { "primaryGroup",
								"secondaryGroups" }));
				ClientInstance instance = new EntityUtils().detachedClone(impl,
						false);
				Registry.impl(ClientInstanceAuthenticationCache.class)
						.cacheAuthentication(instance);
				instance.setUser(new EntityUtils().detachedClone(clonedUser,
						false));
				return instance;
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}

		@Override
		public void updateIid(String iidKey, String userName, boolean rememberMe) {
			// ignore
		}

		@Override
		public void setCommonPersistence(CommonPersistenceBase commonPersistence) {
			this.cp = commonPersistence;
		}

		@Override
		public ClientInstance getClientInstance(long clientInstanceId) {
			throw new UnsupportedOperationException();
		}
	}

	public static Class<? extends HandshakeObjectProvider> getHandshakeObjectProviderClass() {
		return handshakeObjectProviderClass;
	}

	public static void setHandshakeObjectProviderClass(
			Class<? extends HandshakeObjectProvider> handshakeObjectProviderClass) {
		CommonPersistenceBase.handshakeObjectProviderClass = handshakeObjectProviderClass;
	}

	@Override
	public <T extends WrapperPersistable> T getWrappedObjectForUser(
			Class<? extends T> c, long id) throws Exception {
		T wofu = (T) EntityLayerLocator.get().wrappedObjectProvider()
				.getWrappedObjectForUser(c, id, getEntityManager());
		return (T) wofu;
	}

	@Override
	public void persistClientLogRecords(List<ClientLogRecords> recordsList) {
		List<ClientLogRecord> records = new ArrayList<ClientLogRecord>();
		for (ClientLogRecords r : recordsList) {
			records.addAll(r.getLogRecords());
		}
		for (ClientLogRecord clr : records) {
			ClientLogRecordPersistent clrp = getNewImplementationInstance(ClientLogRecordPersistent.class);
			getEntityManager().persist(clrp);
			clrp.wrap(clr);
		}
	}

	@Override
	public String getUserNameFor(long validatedClientInstanceId) {
		String userName = Registry
				.impl(ClientInstanceAuthenticationCache.class).getUserNameFor(
						validatedClientInstanceId);
		if (userName == null) {
			Class<? extends CI> clientInstanceImpl = (Class<? extends CI>) getImplementation(ClientInstance.class);
			CI ci = getItemById(clientInstanceImpl, validatedClientInstanceId);
			if (ci != null) {
				userName = ci.getUser().getUserName();
			}
		}
		return userName;
	}

	@Override
	public String getRememberMeUserName(String iidKey) {
		String userName = Registry
				.impl(ClientInstanceAuthenticationCache.class)
				.iidUserNameByKey(iidKey);
		if (userName == null) {
			Iid iid = getIidByKey(iidKey);
			if (iid != null) {
				Registry.impl(ClientInstanceAuthenticationCache.class)
						.cacheIid(iid);
				userName = iid.getRememberMeUser() == null ? null : iid
						.getRememberMeUser().getUserName();
			}
		}
		return userName;
	}
}
