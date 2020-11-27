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
package cc.alcina.framework.entity.persistence;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import javax.persistence.EntityManager;

import org.slf4j.Logger;

import cc.alcina.framework.common.client.actions.ActionLogItem;
import cc.alcina.framework.common.client.csobjects.SearchResultsBase;
import cc.alcina.framework.common.client.entity.ClientLogRecord.ClientLogRecords;
import cc.alcina.framework.common.client.entity.WrapperPersistable;
import cc.alcina.framework.common.client.gwittir.validator.ServerValidator;
import cc.alcina.framework.common.client.log.ILogRecord;
import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.common.client.logic.domaintransform.EntityLocatorMap;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.publication.Publication;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.common.client.util.LongPair;
import cc.alcina.framework.entity.persistence.UnwrapInfoItem.UnwrapInfoContainer;
import cc.alcina.framework.entity.persistence.metric.InternalMetric;
import cc.alcina.framework.entity.persistence.transform.TransformCache;
import cc.alcina.framework.entity.persistence.transform.TransformPersister.TransformPersisterToken;
import cc.alcina.framework.entity.transform.DomainTransformLayerWrapper;
import cc.alcina.framework.entity.transform.DomainTransformRequestPersistent;
import cc.alcina.framework.entity.transform.TransformPersistenceToken;

/**
 * 
 * @author Nick Reddel
 */
public interface CommonPersistenceLocal {
	public void bulkDelete(Class clazz, Collection<Long> ids, boolean tryImpl);

	public <V> V callWithEntityManager(Function<EntityManager, V> call);

	public <T> T ensureObject(T t, String key, String value) throws Exception;

	public <T extends HasId> T ensurePersistent(T obj);

	public void expandExceptionInfo(DomainTransformLayerWrapper wrapper);

	public <T> T findImplInstance(Class<? extends T> clazz, long id);

	public <A> Set<A> getAll(Class<A> clazz);

	public <T> T getItemById(Class<T> clazz, Long id);

	public <T> T getItemById(Class<T> clazz, Long id, boolean clean,
			boolean unwrap);

	public <T> T getItemByKeyValue(Class<T> clazz, String key, Object value,
			boolean createIfNonexistent);

	public abstract <T> T getItemByKeyValue(Class<T> clazz, String key,
			Object value, boolean createIfNonexistent, Long ignoreId,
			boolean caseInsensitive);

	public <T> T getItemByKeyValueKeyValue(Class<T> clazz, String key1,
			Object value1, String key2, Object value2);

	public long getLastTransformId();

	public EntityLocatorMap getLocatorMap(Long clientInstanceId);

	public abstract LongPair getMinMaxIdRange(Class clazz);

	public Date getMostRecentClientInstanceCreationDate(IUser o);

	public <T extends WrapperPersistable> WrappedObject<T>
			getObjectWrapperForUser(Class<T> c, long id) throws Exception;

	public List<DomainTransformRequestPersistent>
			getPersistentTransformRequests(long fromId, long toId,
					Collection<Long> specificIds, boolean mostRecentOnly,
					boolean populateTransformSourceObjects, Logger logger);

	public Publication getPublication(long id);

	public List<Publication> getPublications(Collection<Long> ids);

	public <T extends WrapperPersistable> T getWrappedObjectForUser(
			Class<? extends T> c, long wrappedObjectId) throws Exception;

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

	public void ping();

	public UnwrapInfoContainer prepareUnwrap(Class<? extends HasId> clazz,
			Long id);

	public EntityLocatorMap reconstituteEntityMap(long clientInstanceId);

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

	public void updatePublicationMimeMessageId(Long publicationId,
			String mimeMessageId);

	public <T extends ServerValidator> List<T> validate(List<T> validators);

	/**
	 * Used for supporting mixed rpc/transform domain loads
	 * 
	 */
	public TransformCache warmupTransformCache();

	void changeJdbcConnectionUrl(String newUrl);

	void ensurePublicationCounters();

	Integer getHighestPersistedRequestIdForClientInstance(
			long clientInstanceId);

	long getNextPublicationIdForUser(IUser user);

	List<Long> listRecentClientInstanceIds(String iidKey);
}