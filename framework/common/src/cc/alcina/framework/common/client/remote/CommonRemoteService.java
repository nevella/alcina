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

import cc.alcina.framework.common.client.actions.ActionLogItem;
import cc.alcina.framework.common.client.actions.RemoteAction;
import cc.alcina.framework.common.client.csobjects.JobTracker;
import cc.alcina.framework.common.client.csobjects.LoginBean;
import cc.alcina.framework.common.client.csobjects.LoginResponse;
import cc.alcina.framework.common.client.csobjects.SearchResultsBase;
import cc.alcina.framework.common.client.csobjects.WebException;
import cc.alcina.framework.common.client.entity.WrapperPersistable;
import cc.alcina.framework.common.client.gwittir.validator.ServerValidator;
import cc.alcina.framework.common.client.log.ILogRecord;
import cc.alcina.framework.common.client.logic.domaintransform.DeltaApplicationRecord;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequestException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformResponse;
import cc.alcina.framework.common.client.logic.domaintransform.DomainUpdate;
import cc.alcina.framework.common.client.logic.domaintransform.DomainUpdate.DomainTransformCommitPosition;
import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.permissions.PermissionsException;
import cc.alcina.framework.common.client.logic.permissions.WebMethod;
import cc.alcina.framework.common.client.logic.reflection.Permission;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.gwt.client.entity.search.BindableSearchDefinition;
import cc.alcina.framework.gwt.client.entity.search.ModelSearchResults;
import cc.alcina.framework.gwt.client.gwittir.widget.BoundSuggestBox.BoundSuggestOracleRequest;

/**
 * 
 * @author Nick Reddel
 */
public interface CommonRemoteService extends RemoteService {
	@WebMethod(customPermission = @Permission(access = AccessLevel.ADMIN))
	public List<ActionLogItem> getLogsForAction(RemoteAction action,
			Integer count);

	public LoginResponse hello();

	@WebMethod()
	public List<String> listRunningJobs();

	@WebMethod
	public <T extends ILogRecord> Long log(T remoteLogRecord);

	public Long logClientError(String exceptionToString);

	public Long logClientError(String exceptionToString, String exceptionType);

	// to handle rpc interface drift, use escaped log records
	public void logClientRecords(String serializedLogRecords);

	public LoginResponse login(LoginBean loginBean);

	public void logout();

	@WebMethod()
	public String performAction(RemoteAction action);

	@WebMethod
	public <G extends WrapperPersistable> Long persist(G gwpo)
			throws WebException;

	public void persistOfflineTransforms(
			List<DeltaApplicationRecord> uncommitted) throws WebException;

	public void ping();

	@WebMethod()
	public JobTracker pollJobStatus(String id, boolean cancel);

	public SearchResultsBase search(SearchDefinition def, int pageNumber);

	@WebMethod
	public DomainTransformResponse transform(DomainTransformRequest request)
			throws DomainTransformException, DomainTransformRequestException;

	public List<ServerValidator> validateOnServer(
			List<ServerValidator> validators) throws WebException;

	@WebMethod
	public DomainUpdate waitForTransforms(
			DomainTransformCommitPosition position) throws PermissionsException;

	ModelSearchResults getForClass(String className, long objectId);

	ModelSearchResults searchModel(BindableSearchDefinition def);

	Response suggest(BoundSuggestOracleRequest request);
}
