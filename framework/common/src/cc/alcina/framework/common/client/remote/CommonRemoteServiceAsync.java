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
import java.util.Map;
import java.util.Set;

import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.common.client.actions.RemoteAction;
import cc.alcina.framework.common.client.csobjects.JobTracker;
import cc.alcina.framework.common.client.csobjects.LoginBean;
import cc.alcina.framework.common.client.csobjects.SearchResultsBase;
import cc.alcina.framework.common.client.gwittir.validator.ServerValidator;
import cc.alcina.framework.common.client.log.ILogRecord;
import cc.alcina.framework.common.client.logic.domaintransform.DeltaApplicationRecord;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformResponse;
import cc.alcina.framework.common.client.logic.domaintransform.DomainUpdate;
import cc.alcina.framework.common.client.logic.domaintransform.DomainUpdate.DomainTransformCommitPosition;
import cc.alcina.framework.common.client.logic.domaintransform.EntityLocator;
import cc.alcina.framework.common.client.logic.reflection.Registration.EnvironmentRegistration;
import cc.alcina.framework.common.client.search.SearchDefinition;

/**
 *
 * @author Nick Reddel
 */
@EnvironmentRegistration
public interface CommonRemoteServiceAsync {
	public void getJobLog(long jobId, AsyncCallback<String> callback);

	public void getLogsForAction(RemoteAction action, Integer count,
			AsyncCallback<List<JobTracker>> callback);

	void getPersistentLocators(Set<EntityLocator> locators,
			AsyncCallback<Map<EntityLocator, EntityLocator>> callback);

	public void hello(AsyncCallback callback);

	public void listRunningJobs(AsyncCallback<List<String>> callback);

	void log(ILogRecord remoteLogRecord, AsyncCallback<Long> callback);

	public void logClientError(String exceptionToString,
			AsyncCallback<Long> callback);

	void logClientError(String exceptionToString, String exceptionType,
			AsyncCallback<Long> callback);

	void logClientRecords(String serializedLogRecords,
			AsyncCallback<Void> callback);

	public void login(LoginBean loginBean, AsyncCallback callback);

	public void logout(AsyncCallback callback);

	public void performAction(RemoteAction action,
			AsyncCallback<String> callback);

	void persistOfflineTransforms(List<DeltaApplicationRecord> uncommitted,
			AsyncCallback<Void> callback);

	void ping(AsyncCallback<Void> callback);

	public void pollJobStatus(JobTracker.Request request,
			AsyncCallback<JobTracker.Response> callback);

	void pollJobStatus(String id, boolean cancel,
			AsyncCallback<JobTracker> callback);

	public void search(SearchDefinition def,
			AsyncCallback<SearchResultsBase> callback);

	public void transform(DomainTransformRequest request,
			AsyncCallback<DomainTransformResponse> callback);

	public void validateOnServer(List<ServerValidator> validators,
			AsyncCallback<List<ServerValidator>> callback);

	void waitForTransforms(DomainTransformCommitPosition position,
			AsyncCallback<DomainUpdate> callback);
}
