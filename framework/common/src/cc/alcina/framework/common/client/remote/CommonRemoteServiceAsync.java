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

package cc.alcina.framework.common.client.remote;

import java.util.List;

import cc.alcina.framework.common.client.actions.ActionLogItem;
import cc.alcina.framework.common.client.actions.RemoteAction;
import cc.alcina.framework.common.client.csobjects.JobInfo;
import cc.alcina.framework.common.client.csobjects.LoginBean;
import cc.alcina.framework.common.client.csobjects.ObjectCacheItemResult;
import cc.alcina.framework.common.client.csobjects.ObjectCacheItemSpec;
import cc.alcina.framework.common.client.csobjects.SearchResultsBase;
import cc.alcina.framework.common.client.entity.GwtPersistableObject;
import cc.alcina.framework.common.client.gwittir.validator.ServerValidator;
import cc.alcina.framework.common.client.logic.domaintransform.DTRSimpleSerialWrapper;
import cc.alcina.framework.common.client.logic.domaintransform.DataTransformRequest;
import cc.alcina.framework.common.client.logic.permissions.HasIdAndLocalId;
import cc.alcina.framework.common.client.search.SearchDefinition;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 *
 * @author <a href="mailto:nick@alcina.cc">Nick Reddel</a>
 */

 public interface CommonRemoteServiceAsync {
	public void cache(List<ObjectCacheItemSpec> specs,
			AsyncCallback<List<ObjectCacheItemResult>> callback);

	public <T extends HasIdAndLocalId> void getItemById(String className,
			Long id, AsyncCallback<T> callback);

	public void getLogsForAction(RemoteAction action, Integer count,
			AsyncCallback<List<ActionLogItem>> callback);

	public void hello(AsyncCallback callback);

	public void logClientError(String exceptionToString, 
			AsyncCallback<Long> callback);

	public void login(LoginBean loginBean, AsyncCallback callback);

	public void logout(AsyncCallback callback);

	public void performAction(RemoteAction action, AsyncCallback<Long> callback);

	public void listRunningJobs(AsyncCallback<List<Long>> callback);

	public void performActionAndWait(RemoteAction action,
			AsyncCallback<ActionLogItem> callback);

	public void search(SearchDefinition def, int pageNumber,
			AsyncCallback<SearchResultsBase> callback);

	public void transform(DataTransformRequest request, AsyncCallback callback);

	public void validateOnServer(List<ServerValidator> validators,
			AsyncCallback<List<ServerValidator>> callback);

	<G extends GwtPersistableObject> void persist(G gwpo,
			AsyncCallback<Long> callback);

	void persistOfflineTransforms(List<DTRSimpleSerialWrapper> uncommitted,
			AsyncCallback<Void> callback);

	void pollJobStatus(Long id, boolean cancel, AsyncCallback<JobInfo> callback);
}
