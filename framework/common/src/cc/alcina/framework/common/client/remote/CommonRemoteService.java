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
import cc.alcina.framework.common.client.csobjects.LoginResponse;
import cc.alcina.framework.common.client.csobjects.ObjectCacheItemResult;
import cc.alcina.framework.common.client.csobjects.ObjectCacheItemSpec;
import cc.alcina.framework.common.client.csobjects.WebException;
import cc.alcina.framework.common.client.gwittir.validator.ServerValidator;
import cc.alcina.framework.common.client.logic.domaintransform.DTRSimpleSerialWrapper;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequestException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformResponse;
import cc.alcina.framework.common.client.logic.domaintransform.PartialDtrUploadRequest;
import cc.alcina.framework.common.client.logic.domaintransform.PartialDtrUploadResponse;
import cc.alcina.framework.common.client.logic.permissions.WebMethod;

import com.google.gwt.user.client.rpc.RemoteService;

/**
 * 
 * @author Nick Reddel
 */
public interface CommonRemoteService extends RemoteService {
	public List<ObjectCacheItemResult> cache(List<ObjectCacheItemSpec> specs)
			throws WebException;

	public LoginResponse hello();

	@WebMethod()
	public List<Long> listRunningJobs();

	public Long logClientError(String exceptionToString);

	public Long logClientError(String exceptionToString, String exceptionType);

	// to handle rpc interface drift, use escaped log records
	public void logClientRecords(String serializedLogRecords);

	public LoginResponse login(LoginBean loginBean);

	public void ping();

	public void logout();

	public void persistOfflineTransforms(
			List<DTRSimpleSerialWrapper> uncommitted) throws WebException;

	public PartialDtrUploadResponse uploadOfflineTransforms(
			PartialDtrUploadRequest request) throws WebException;

	@WebMethod()
	public JobInfo pollJobStatus(Long id, boolean cancel);

	@WebMethod
	public DomainTransformResponse transform(DomainTransformRequest request)
			throws DomainTransformException, DomainTransformRequestException;

	public List<ServerValidator> validateOnServer(
			List<ServerValidator> validators) throws WebException;

	// for dumping dbs
	@WebMethod
	public void dumpData(String data);

	@WebMethod
	public String loadData(String key);
}
