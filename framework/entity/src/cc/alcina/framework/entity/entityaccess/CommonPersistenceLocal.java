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
import java.util.List;
import java.util.Set;

import cc.alcina.framework.common.client.actions.ActionLogItem;
import cc.alcina.framework.common.client.csobjects.ObjectCacheItemResult;
import cc.alcina.framework.common.client.csobjects.ObjectCacheItemSpec;
import cc.alcina.framework.common.client.csobjects.SearchResultsBase;
import cc.alcina.framework.common.client.entity.GwtPersistableObject;
import cc.alcina.framework.common.client.entity.Iid;
import cc.alcina.framework.common.client.gwittir.validator.ServerValidator;
import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.entity.datatransform.DomainTransformLayerWrapper;
import cc.alcina.framework.entity.datatransform.ThreadlocalTransformManager.HiliLocatorMap;
import cc.alcina.framework.entity.entityaccess.UnwrapInfoItem.UnwrapInfoContainer;
import cc.alcina.framework.entity.util.GraphCloner.CloneFilter;


/**
 *
 * @author Nick Reddel
 */

 public interface CommonPersistenceLocal {
	public List<ObjectCacheItemResult> cache(List<ObjectCacheItemSpec> specs)
			throws Exception;

	public abstract ClientInstance createClientInstance();

	public <T> T ensureObject(T t, String key, String value) throws Exception;

	public <A> Set<A> getAll(Class<A> clazz);

	public IUser getAnonymousUser();

	public abstract Iid getIidByKey(String iid);

	public abstract <A> Class<? extends A> getImplementation(Class<A> clazz);

	public <T> T getItemById(Class<T> clazz, Long id);

	public <T> T getItemById(Class<T> clazz, Long id, boolean clean,
			boolean unwrap);

	public <T> T getItemByKeyValue(Class<T> clazz, String key, Object value,
			boolean createIfNonexistent);

	public <T> T getItemByKeyValueKeyValue(Class<T> clazz, String key1,
			Object value1, String key2, Object value2);

	public long getNextPublicationIdForUser(IUser user);

	public <T extends GwtPersistableObject> WrappedObject<T> getObjectWrapperForUser(
			Class<T> c, long id) throws Exception;

	public abstract IUser getSystemUser();

	public abstract IUser getSystemUser(boolean clean);

	public abstract IUser getUserByName(String userName);

	public abstract IUser getUserByName(String userName, boolean clean);

	public List<ActionLogItem> listLogItemsForClass(String className, int count);

	public long log(String message, String componentKey);

	public abstract void logActionItem(ActionLogItem result);

	public abstract long merge(HasId hi);

	public abstract IUser mergeUser(IUser user);

	public <G extends GwtPersistableObject> Long persist(G gwpo)
			throws Exception;

	public void remove(Object o);

	public SearchResultsBase search(SearchDefinition def, int pageNumber);

	public void setField(Class clazz, Long id, String key, Object value)
			throws Exception;

	public DomainTransformLayerWrapper transform(DomainTransformRequest request,
			HiliLocatorMap locatorMap, boolean persistTransforms,
			boolean possiblyReconstitueLocalIdMap)
			throws DomainTransformException;

	public <T extends HasId> Collection<T> unwrap(Collection<T> wrappers);

	public HasId unwrap(HasId wrapper);

	public abstract void updateIid(String iidKey, String userName,
			boolean rememberMe);

	public <T extends ServerValidator> List<T> validate(List<T> validators);

	public abstract void fixPermissionsManager();

	public UnwrapInfoContainer prepareUnwrap(Class<? extends HasId> clazz,
			Long id,CloneFilter fieldFilter, CloneFilter dataFilter);

	public void bulkDelete(Class clazz, Collection<Long> ids);
}