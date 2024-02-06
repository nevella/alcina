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
import java.util.List;

import javax.persistence.EntityManager;

import org.slf4j.Logger;

import cc.alcina.framework.common.client.csobjects.SearchResultsBase;
import cc.alcina.framework.common.client.entity.ClientLogRecord.ClientLogRecords;
import cc.alcina.framework.common.client.log.ILogRecord;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.common.client.util.ThrowingFunction;
import cc.alcina.framework.entity.persistence.metric.InternalMetric;
import cc.alcina.framework.entity.persistence.transform.TransformCache;
import cc.alcina.framework.entity.persistence.transform.TransformPersister.TransformPersisterToken;
import cc.alcina.framework.entity.transform.DomainTransformLayerWrapper;
import cc.alcina.framework.entity.transform.DomainTransformRequestPersistent;
import cc.alcina.framework.entity.transform.EntityLocatorMap;
import cc.alcina.framework.entity.transform.TransformPersistenceToken;

/**
 * 
 * @author Nick Reddel
 */
public interface CommonPersistenceLocal {
	public <V> V callWithEntityManager(ThrowingFunction<EntityManager, V> call);

	void changeJdbcConnectionUrl(String newUrl);

	<T> T ensure(Class<T> clazz, String key, Object value);

	void ensurePublicationCounters();

	public void expandExceptionInfo(DomainTransformLayerWrapper wrapper);

	public <T> T findImplInstance(Class<? extends T> clazz, long id);

	Integer getHighestPersistedRequestIdForClientInstance(
			long clientInstanceId);

	public <T> T getItemByKeyValueKeyValue(Class<T> clazz, String key1,
			Object value1, String key2, Object value2);

	public long getLastTransformId();

	public EntityLocatorMap getLocatorMap(Long clientInstanceId);

	long getNextPublicationIdForUser(IUser user);

	public List<DomainTransformRequestPersistent>
			getPersistentTransformRequests(long fromId, long toId,
					Collection<Long> specificIds, boolean mostRecentOnly,
					boolean populateTransformSourceObjects, Logger logger);

	List<Long> listRecentClientInstanceIds(String iidKey);

	public long log(String message, String componentKey);

	public long log(String message, String componentKey, String data);

	public void persistClientLogRecords(List<ClientLogRecords> records);

	public void persistInternalMetrics(List<InternalMetric> metrics);

	public <T extends ILogRecord> long persistLogRecord(T logRecord);

	public void ping();

	public EntityLocatorMap reconstituteEntityMap(long clientInstanceId);

	public boolean
			removeProcessedRequests(TransformPersistenceToken persistenceToken);

	public SearchResultsBase search(SearchDefinition def);

	public void setField(Class clazz, Long id, String key, Object value)
			throws Exception;

	public DomainTransformLayerWrapper transformInPersistenceContext(
			TransformPersisterToken persisterToken,
			TransformPersistenceToken persistenceToken,
			DomainTransformLayerWrapper wrapper);

	public void updatePublicationMimeMessageId(Long publicationId,
			String mimeMessageId);

	/**
	 * Used for supporting mixed rpc/transform domain loads
	 * 
	 */
	public TransformCache warmupTransformCache();
}