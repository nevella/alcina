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

import cc.alcina.framework.common.client.csobjects.JobInfo;
import cc.alcina.framework.common.client.csobjects.LoginBean;
import cc.alcina.framework.common.client.csobjects.ObjectCacheItemResult;
import cc.alcina.framework.common.client.csobjects.ObjectCacheItemSpec;
import cc.alcina.framework.common.client.gwittir.validator.ServerValidator;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.DTRSimpleSerialWrapper;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformResponse;
import cc.alcina.framework.common.client.logic.domaintransform.PartialDtrUploadRequest;
import cc.alcina.framework.common.client.logic.domaintransform.PartialDtrUploadResponse;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * 
 * @author Nick Reddel
 */
public interface CommonRemoteServiceAsync {
	public void cache(List<ObjectCacheItemSpec> specs,
			AsyncCallback<List<ObjectCacheItemResult>> callback);


	public void hello(AsyncCallback callback);

	public void listRunningJobs(AsyncCallback<List<Long>> callback);

	public void logClientError(String exceptionToString,
			AsyncCallback<Long> callback);

	public void login(LoginBean loginBean, AsyncCallback callback);

	public void logout(AsyncCallback callback);

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

	void persistOfflineTransforms(List<DTRSimpleSerialWrapper> uncommitted,
			AsyncCallback<Void> callback);

	void ping(AsyncCallback<Void> callback);

	void pollJobStatus(Long id, boolean cancel, AsyncCallback<JobInfo> callback);

	void uploadOfflineTransforms(PartialDtrUploadRequest request,
			AsyncCallback<PartialDtrUploadResponse> callback);
}
