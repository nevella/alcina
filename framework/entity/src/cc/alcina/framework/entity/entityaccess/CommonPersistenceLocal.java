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

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;

import cc.alcina.framework.common.client.actions.ActionLogItem;
import cc.alcina.framework.common.client.csobjects.ObjectDeltaResult;
import cc.alcina.framework.common.client.csobjects.ObjectDeltaSpec;
import cc.alcina.framework.common.client.csobjects.SearchResultsBase;
import cc.alcina.framework.common.client.entity.ClientLogRecord.ClientLogRecords;
import cc.alcina.framework.common.client.entity.Iid;
import cc.alcina.framework.common.client.entity.WrapperPersistable;
import cc.alcina.framework.common.client.gwittir.validator.ServerValidator;
import cc.alcina.framework.common.client.log.ILogRecord;
import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.EntityLocatorMap;
import cc.alcina.framework.common.client.logic.permissions.IGroup;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.publication.Publication;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.common.client.util.LongPair;
import cc.alcina.framework.entity.domaintransform.DomainTransformLayerWrapper;
import cc.alcina.framework.entity.domaintransform.DomainTransformRequestPersistent;
import cc.alcina.framework.entity.domaintransform.TransformPersistenceToken;
import cc.alcina.framework.entity.entityaccess.TransformPersister.TransformPersisterToken;
import cc.alcina.framework.entity.entityaccess.UnwrapInfoItem.UnwrapInfoContainer;
import cc.alcina.framework.entity.entityaccess.metric.InternalMetric;
import cc.alcina.framework.entity.projection.GraphProjection.GraphProjectionDataFilter;
import cc.alcina.framework.entity.projection.GraphProjection.GraphProjectionFieldFilter;
import cc.alcina.framework.entity.projection.GraphProjection.InstantiateImplCallback;

/**
 * 
 * @author Nick Reddel
 */
public interface CommonPersistenceLocal {
	public void bulkDelete(Class clazz, Collection<Long> ids, boolean tryImpl);


	public abstract void connectPermissionsManagerToLiveObjects();

	public abstract ClientInstance createClientInstance(String userAgent,
			String iid, String ipAddress);

	public <T> T ensureObject(T t, String key, String value) throws Exception;

	public <T extends HasId> T ensurePersistent(T obj);

	public void expandExceptionInfo(DomainTransformLayerWrapper wrapper);

	public <T> T findImplInstance(Class<? extends T> clazz, long id);

	public <A> Set<A> getAll(Class<A> clazz);

	public IUser getAnonymousUser();

	public abstract String getAnonymousUserName();

	public <US extends IUser> US getCleanedUserById(long userId);

	public ClientInstance getClientInstance(Long clientInstanceId);

	public abstract IGroup getGroupByName(String groupName);

	public abstract IGroup getGroupByName(String groupName, boolean clean);

	public abstract Iid getIidByKey(String iid);

	public <T> T getItemById(Class<T> clazz, Long id);

	public <T> T getItemById(Class<T> clazz, Long id, boolean clean,
			boolean unwrap);

	public <T> T getItemByKeyValue(Class<T> clazz, String key, Object value,
			boolean createIfNonexistent);

	public abstract <T> T getItemByKeyValue(Class<T> clazz, String key,
			Object value, boolean createIfNonexistent, Long ignoreId,
			boolean caseInsensitive, boolean livePermissionsManager);

	public <T> T getItemByKeyValueKeyValue(Class<T> clazz, String key1,
			Object value1, String key2, Object value2);

	public <T> List<T> getItemsByIdsAndClean(Class<T> clazz,
			Collection<Long> ids,
			InstantiateImplCallback instantiateImplCallback);

	public long getLastTransformId();

	public EntityLocatorMap getLocatorMap(Long clientInstanceId);

	public abstract LongPair getMinMaxIdRange(Class clazz);

	public Date getMostRecentClientInstanceCreationDate(IUser o);

	public <A> A getNewImplementationInstance(Class<A> clazz);

	public List<ObjectDeltaResult> getObjectDelta(List<ObjectDeltaSpec> specs)
			throws Exception;

	public <T extends WrapperPersistable> WrappedObject<T>
			getObjectWrapperForUser(Class<T> c, long id) throws Exception;

	public List<DomainTransformRequestPersistent>
			getPersistentTransformRequests(long fromId, long toId,
					Collection<Long> specificIds, boolean mostRecentOnly,
					boolean populateTransformSourceObjects, Logger logger);

	public Publication getPublication(long id);

	public List<Publication> getPublications(Collection<Long> ids);

	public String getRememberMeUserName(String iid);

	public abstract IUser getSystemUser();

	public abstract IUser getSystemUser(boolean clean);

	public abstract IUser getUserByName(String userName);

	public abstract IUser getUserByName(String userName, boolean clean);

	public String
			getUserNameForClientInstanceId(long validatedClientInstanceId);

	public <T extends WrapperPersistable> T getWrappedObjectForUser(
			Class<? extends T> c, long wrappedObjectId) throws Exception;

	public abstract boolean isValidIid(String iidKey);

	public List<ActionLogItem> listLogItemsForClass(String className,
			int count);

	public long log(String message, String componentKey);

	public long log(String message, String componentKey, String data);

	public abstract void logActionItem(ActionLogItem result);

	public abstract long merge(HasId hi);

	public abstract IUser mergeUser(IUser user);

	public <G extends WrapperPersistable> Long persist(G gwpo) throws Exception;

	public void persistClientLogRecords(List<ClientLogRecords> records);

	public void persistInternalMetrics(List<InternalMetric> metrics);

	public <T extends ILogRecord> long persistLogRecord(T logRecord);

	public UnwrapInfoContainer prepareUnwrap(Class<? extends HasId> clazz,
			Long id, GraphProjectionFieldFilter fieldFilter,
			GraphProjectionDataFilter dataFilter);

	public EntityLocatorMap reconstituteEntityMap(long l2);

	public void remove(Object o);

	public SearchResultsBase search(SearchDefinition def, int pageNumber);

	public void setField(Class clazz, Long id, String key, Object value)
			throws Exception;

	public DomainTransformLayerWrapper transformInPersistenceContext(
			TransformPersisterToken persisterToken,
			TransformPersistenceToken persistenceToken,
			DomainTransformLayerWrapper wrapper);

	public <T extends HasId> Collection<T> unwrap(Collection<T> wrappers);

	public HasId unwrap(HasId wrapper);

	public abstract void updateIid(String iidKey, String userName,
			boolean rememberMe);

	public void updatePublicationMimeMessageId(Long publicationId,
			String mimeMessageId);

	public <T extends ServerValidator> List<T> validate(List<T> validators);

	public boolean validateClientInstance(long id, int auth);

	/**
	 * Used for supporting mixed rpc/transform domain loads
	 * 
	 */
	public TransformCache warmupTransformCache();

	Integer getHighestPersistedRequestIdForClientInstance(
			long clientInstanceId);

	long getMaxPublicationIdForUser(IUser user);

	List<Long> listRecentClientInstanceIds(String iidKey);


	void changeJdbcConnectionUrl(String newUrl);
}