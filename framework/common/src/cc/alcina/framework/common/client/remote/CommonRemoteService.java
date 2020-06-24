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

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.ui.SuggestOracle.Response;

import cc.alcina.framework.common.client.csobjects.JobTracker;
import cc.alcina.framework.common.client.csobjects.LoginBean;
import cc.alcina.framework.common.client.csobjects.LoginResponse;
import cc.alcina.framework.common.client.csobjects.ObjectDeltaResult;
import cc.alcina.framework.common.client.csobjects.ObjectDeltaSpec;
import cc.alcina.framework.common.client.csobjects.WebException;
import cc.alcina.framework.common.client.gwittir.validator.ServerValidator;
import cc.alcina.framework.common.client.logic.domaintransform.DeltaApplicationRecord;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequestException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformResponse;
import cc.alcina.framework.common.client.logic.domaintransform.DomainUpdate;
import cc.alcina.framework.common.client.logic.domaintransform.DomainUpdate.DomainTransformCommitPosition;
import cc.alcina.framework.common.client.logic.permissions.PermissionsException;
import cc.alcina.framework.common.client.logic.permissions.WebMethod;
import cc.alcina.framework.gwt.client.gwittir.widget.BoundSuggestBox.BoundSuggestOracleRequest;

/**
 * 
 * @author Nick Reddel
 */
public interface CommonRemoteService extends RemoteService {
	// for dumping dbs
	@WebMethod
	public void dumpData(String data);

	public List<ObjectDeltaResult> getObjectDelta(List<ObjectDeltaSpec> specs)
			throws WebException;

	public LoginResponse hello();

	@WebMethod()
	public List<String> listRunningJobs();

	@WebMethod
	public String loadData(String key);

	public Long logClientError(String exceptionToString);

	public Long logClientError(String exceptionToString, String exceptionType);

	// to handle rpc interface drift, use escaped log records
	public void logClientRecords(String serializedLogRecords);

	public LoginResponse login(LoginBean loginBean);

	public void logout();

	public void persistOfflineTransforms(
			List<DeltaApplicationRecord> uncommitted) throws WebException;

	public void ping();

	@WebMethod()
	public JobTracker pollJobStatus(String id, boolean cancel);

	@WebMethod
	public DomainTransformResponse transform(DomainTransformRequest request)
			throws DomainTransformException, DomainTransformRequestException;

	public List<ServerValidator> validateOnServer(
			List<ServerValidator> validators) throws WebException;

	@WebMethod
	public DomainUpdate waitForTransforms(
			DomainTransformCommitPosition position) throws PermissionsException;

	Response suggest(BoundSuggestOracleRequest request);
}
