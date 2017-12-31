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

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.SuggestOracle.Response;

import cc.alcina.framework.common.client.csobjects.JobTracker;
import cc.alcina.framework.common.client.csobjects.LoginBean;
import cc.alcina.framework.common.client.csobjects.ObjectDeltaResult;
import cc.alcina.framework.common.client.csobjects.ObjectDeltaSpec;
import cc.alcina.framework.common.client.gwittir.validator.ServerValidator;
import cc.alcina.framework.common.client.logic.domaintransform.DeltaApplicationRecord;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformResponse;
import cc.alcina.framework.common.client.logic.domaintransform.DomainUpdate;
import cc.alcina.framework.common.client.logic.domaintransform.DomainUpdate.DomainTransformCommitPosition;
import cc.alcina.framework.common.client.logic.domaintransform.PartialDtrUploadRequest;
import cc.alcina.framework.common.client.logic.domaintransform.PartialDtrUploadResponse;
import cc.alcina.framework.gwt.client.gwittir.widget.BoundSuggestBox.BoundSuggestOracleRequest;

/**
 * 
 * @author Nick Reddel
 */
public interface CommonRemoteServiceAsync {
	public void getObjectDelta(List<ObjectDeltaSpec> specs,
			AsyncCallback<List<ObjectDeltaResult>> callback);

	public void hello(AsyncCallback callback);

	public void listRunningJobs(AsyncCallback<List<String>> callback);

	public void logClientError(String exceptionToString,
			AsyncCallback<Long> callback);

	public void login(LoginBean loginBean, AsyncCallback callback);

	public void logout(AsyncCallback callback);

	public void suggest(BoundSuggestOracleRequest request,
			AsyncCallback<Response> asyncCallback);

	public void transform(DomainTransformRequest request,
			AsyncCallback<DomainTransformResponse> callback);

	public void validateOnServer(List<ServerValidator> validators,
			AsyncCallback<List<ServerValidator>> callback);

	void dumpData(String data, AsyncCallback<Void> callback);

	void loadData(String key, AsyncCallback<String> callback);

	void logClientError(String exceptionToString, String exceptionType,
			AsyncCallback<Long> callback);

	void logClientRecords(String serializedLogRecords,
			AsyncCallback<Void> callback);

	void persistOfflineTransforms(List<DeltaApplicationRecord> uncommitted,
			AsyncCallback<Void> callback);

	void ping(AsyncCallback<Void> callback);

	void pollJobStatus(String id, boolean cancel,
			AsyncCallback<JobTracker> callback);

	void uploadOfflineTransforms(PartialDtrUploadRequest request,
			AsyncCallback<PartialDtrUploadResponse> callback);

	void waitForTransforms(DomainTransformCommitPosition position,
			AsyncCallback<DomainUpdate> callback);
}
