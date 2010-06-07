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
import cc.alcina.framework.common.client.csobjects.LoginResponse;
import cc.alcina.framework.common.client.csobjects.ObjectCacheItemResult;
import cc.alcina.framework.common.client.csobjects.ObjectCacheItemSpec;
import cc.alcina.framework.common.client.csobjects.SearchResultsBase;
import cc.alcina.framework.common.client.csobjects.WebException;
import cc.alcina.framework.common.client.entity.WrapperPersistable;
import cc.alcina.framework.common.client.gwittir.validator.ServerValidator;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.DTRSimpleSerialWrapper;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequestException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformResponse;
import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.permissions.AuthenticationRequired;
import cc.alcina.framework.common.client.logic.reflection.Permission;
import cc.alcina.framework.common.client.search.SearchDefinition;

import com.google.gwt.user.client.rpc.RemoteService;

/**
 *
 * @author Nick Reddel
 */

 public interface CommonRemoteService extends RemoteService {
	public List<ObjectCacheItemResult> cache(List<ObjectCacheItemSpec> specs)
			throws WebException;

	// TODO: permissions
	public <T extends HasIdAndLocalId> T getItemById(String className, Long id)
			throws WebException;

	@AuthenticationRequired(permission = @Permission(access = AccessLevel.ADMIN))
	public List<ActionLogItem> getLogsForAction(RemoteAction action,
			Integer count);

	public LoginResponse hello();

	public Long logClientError(String exceptionToString);

	public LoginResponse login(LoginBean loginBean);

	public void logout();

	@AuthenticationRequired()
	public Long performAction(RemoteAction action);
	
	@AuthenticationRequired()
	public List<Long> listRunningJobs();

	@AuthenticationRequired()
	public ActionLogItem performActionAndWait(RemoteAction action)
			throws WebException;

	@AuthenticationRequired
	public <G extends WrapperPersistable> Long persist(G gwpo)
			throws WebException;

	@AuthenticationRequired()
	public JobInfo pollJobStatus(Long id, boolean cancel);

	public SearchResultsBase search(SearchDefinition def, int pageNumber);

	@AuthenticationRequired
	public DomainTransformResponse transform(DomainTransformRequest request)
			throws DomainTransformException, DomainTransformRequestException;

	public List<ServerValidator> validateOnServer(
			List<ServerValidator> validators) throws WebException;
	public void persistOfflineTransforms(List<DTRSimpleSerialWrapper> uncommitted) throws WebException;
}
