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
package cc.alcina.framework.servlet.servlet;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.ui.SuggestOracle.Response;
import com.google.gwt.user.server.rpc.RPCRequest;
import java.util.function.Function;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.actions.RemoteAction;
import cc.alcina.framework.common.client.actions.SynchronousAction;
import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.csobjects.JobTracker;
import cc.alcina.framework.common.client.csobjects.LogMessageType;
import cc.alcina.framework.common.client.csobjects.SearchResultsBase;
import cc.alcina.framework.common.client.csobjects.WebException;
import cc.alcina.framework.common.client.domain.search.DomainSearcher;
import cc.alcina.framework.common.client.entity.ClientLogRecord;
import cc.alcina.framework.common.client.entity.ClientLogRecord.ClientLogRecords;
import cc.alcina.framework.gwt.client.gwittir.validator.ServerValidator;
import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.log.ILogRecord;
import cc.alcina.framework.common.client.logic.ObfuscateOnLog;
import cc.alcina.framework.common.client.logic.domain.DomainTransformPersistable;
import cc.alcina.framework.common.client.logic.domaintransform.DeltaApplicationRecord;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException.DomainTransformExceptionType;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequestException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformResponse;
import cc.alcina.framework.common.client.logic.domaintransform.DomainUpdate;
import cc.alcina.framework.common.client.logic.domaintransform.DomainUpdate.DomainTransformCommitPosition;
import cc.alcina.framework.common.client.logic.domaintransform.EntityLocator;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.permissions.Permissions;
import cc.alcina.framework.common.client.logic.permissions.PermissionsException;
import cc.alcina.framework.common.client.logic.permissions.WebMethod;
import cc.alcina.framework.common.client.logic.reflection.Permission;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.publication.ContentDefinition;
import cc.alcina.framework.common.client.publication.request.ContentRequestBase;
import cc.alcina.framework.common.client.publication.request.PublicationResult;
import cc.alcina.framework.gwt.client.remote.CommonRemoteService;
import cc.alcina.framework.gwt.client.remote.ReflectiveRemoteServiceHandler;
import cc.alcina.framework.gwt.client.remote.SearchRemoteService;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.persistence.CommonPersistenceBase;
import cc.alcina.framework.entity.persistence.CommonPersistenceProvider;
import cc.alcina.framework.entity.persistence.domain.DomainStore;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.entity.persistence.transform.TransformCommit;
import cc.alcina.framework.entity.projection.GraphProjection.GraphProjectionFieldFilter;
import cc.alcina.framework.entity.projection.GraphProjections;
import cc.alcina.framework.gwt.client.gwittir.widget.BoundSuggestBox.BoundSuggestOracleRequest;
import cc.alcina.framework.gwt.client.gwittir.widget.BoundSuggestOracleResponseElement;
import cc.alcina.framework.gwt.client.gwittir.widget.BoundSuggestOracleResponseElement.BoundSuggestOracleModel;
import cc.alcina.framework.servlet.SessionProvider;
import cc.alcina.framework.servlet.authentication.AuthenticationManager;
import cc.alcina.framework.servlet.job.JobRegistry;
import cc.alcina.framework.servlet.servlet.handler.GetPersistentLocatorsHandler;
import cc.alcina.framework.servlet.task.TaskPublish;

/**
 * Tests (todo) for transform persistence: invalid clientauth multiple
 * simultaneous (identical clientinstance, non-) cross-server-restart
 *
 * <p>
 * Readonly: most checks happen of simple methods happen at the persistence
 * layer so not needed here
 * </p>
 *
 *
 */
public abstract class CommonRemoteServiceServlet extends AlcinaRpcServlet
		implements CommonRemoteService, SearchRemoteService,
		ReflectiveRemoteServiceHandler {
	public static final String CONTEXT_THREAD_LOCAL_HTTP_RESPONSE_HEADERS = CommonRemoteServiceServlet.class
			.getName() + ".CONTEXT_THREAD_LOCAL_HTTP_RESPONSE_HEADERS";

	public static final String CONTEXT_METRIC_OBSERVER = CommonRemoteServiceServlet.class
			.getName() + ".CONTEXT_METRIC_OBSERVER";

	protected Logger logger = LoggerFactory.getLogger(getClass());

	protected <T> T defaultProjection(T t) {
		return GraphProjections.defaultProjections().project(t);
	}

	@Override
	@WebMethod(
		readonlyPermitted = true,
		customPermission = @Permission(access = AccessLevel.EVERYONE))
	public String getJobLog(long jobId) {
		Job job = Job.byId(jobId);
		job.domain().ensurePopulated();
		Preconditions.checkState(
				Permissions.get().isAdmin() || IUser.current() == job.getUser(),
				"Illegal access to job " + jobId);
		return job.getLog();
	}

	protected Logger getLogger() {
		return logger;
	}

	@Override
	@WebMethod(readonlyPermitted = true)
	public List<JobTracker> getLogsForAction(RemoteAction action,
			Integer count) {
		return JobRegistry.get().getLogsForAction(action, count);
	}

	@Override
	public Map<EntityLocator, EntityLocator>
			getPersistentLocators(Set<EntityLocator> locators) {
		// FIXME - low - permissions (although honestly almost totally harmless)
		return new GetPersistentLocatorsHandler().handle(locators);
	}

	protected String getRpcHandlerThreadNameSuffix(RPCRequest rpcRequest) {
		try {
			Method method = this.getClass().getMethod(
					rpcRequest.getMethod().getName(),
					rpcRequest.getMethod().getParameterTypes());
			if (method.isAnnotationPresent(WebMethod.class)) {
				WebMethod webMethod = method.getAnnotation(WebMethod.class);
				return webMethod.rpcHandlerThreadNameSuffix();
			}
			return "";
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	protected HttpSession getSession() {
		return getSession(CONTEXT_REQUEST.getTyped());
	}

	protected HttpSession getSession(HttpServletRequest request) {
		return getSession(request, CONTEXT_RESPONSE.getTyped());
	}

	protected HttpSession getSession(HttpServletRequest request,
			HttpServletResponse response) {
		return Registry.impl(SessionProvider.class).getSession(request,
				response);
	}

	protected String getUserAgent() {
		return getUserAgent(CONTEXT_REQUEST.getTyped());
	}

	protected String invokeAndEncodeResponse(RPCRequest rpcRequest)
			throws SerializationException {
		return RpcRequestRouter.get().invokeAndEncodeResponse(this, rpcRequest);
	}

	protected boolean isPersistOfflineTransforms() {
		return true;
	}

	@Override
	@WebMethod(readonlyPermitted = true)
	public // FIXME - mvcc.jobs - remove
	List<String> listRunningJobs() {
		throw new UnsupportedOperationException();
	}

	@Override
	@WebMethod(
		readonlyPermitted = true,
		customPermission = @Permission(access = AccessLevel.EVERYONE))
	public Long log(ILogRecord logRecord) {
		return Registry.impl(CommonPersistenceProvider.class)
				.getCommonPersistence().persistLogRecord(logRecord);
	}

	@Override
	@WebMethod(
		readonlyPermitted = true,
		customPermission = @Permission(access = AccessLevel.EVERYONE))
	public Long logClientError(String exceptionToString) {
		return logClientError(exceptionToString,
				LogMessageType.CLIENT_EXCEPTION.toString());
	}

	@Override
	@WebMethod(
		readonlyPermitted = true,
		customPermission = @Permission(access = AccessLevel.EVERYONE))
	public Long logClientError(String exceptionToString, String exceptionType) {
		String remoteAddr = getRemoteAddress();
		String f_exceptionToString = CommonUtils.nullToEmpty(exceptionToString)
				.replace('\0', ' ');
		Callable<Long> callable = () -> {
			if (f_exceptionToString.matches(
					".*ReflectiveSearchDefinitionSerializer.FlatTreeException.*")) {
				Ax.out("Client exception:\n%s", f_exceptionToString);
			}
			return Registry.impl(CommonPersistenceProvider.class)
					.getCommonPersistence()
					.log(f_exceptionToString, exceptionType);
		};
		return CommonPersistenceBase.CONTEXT_CLIENT_IP_ADDRESS
				.callWith(callable, remoteAddr);
	}

	@Override
	public void logClientRecords(String serializedLogRecords) {
		Function<String, ClientLogRecords> converter = new Function<String, ClientLogRecord.ClientLogRecords>() {
			@Override
			public ClientLogRecords apply(String original) {
				try {
					return TransformManager.deserialize(original);
				} catch (Exception e) {
					System.out.format(
							"problem deserializing clientlogrecord:\n%s\n",
							original);
					e.printStackTrace();
					if (Configuration.is("throwLogClientRecordExceptions")) {
						throw new WrappedRuntimeException(e);
					}
					return null;
				}
			}
		};
		List<String> lines = Arrays.asList(serializedLogRecords.split("\n"));
		List<ClientLogRecords> records = lines.stream().map(converter)
				.filter(Objects::nonNull).collect(Collectors.toList());
		String remoteAddr = getRemoteAddress();
		for (ClientLogRecords r : records) {
			for (ClientLogRecord clr : r.getLogRecords()) {
				clr.setIpAddress(remoteAddr);
				sanitiseClrString(clr);
			}
		}
		Registry.impl(CommonPersistenceProvider.class).getCommonPersistence()
				.persistClientLogRecords(records);
	}

	protected int nextTransformRequestId() {
		return Registry.impl(TransformCommit.class).nextTransformRequestId();
	}

	protected void onAfterAlcinaAuthentication(String methodName) {
	}

	/**
	 * Note - don't (normally) call this server-side, particularly in a loop,
	 * since it spawns a potentially unlimited number of performers
	 */
	@Override
	public String performAction(RemoteAction action) {
		Job job = null;
		if (action instanceof SynchronousAction) {
			job = JobRegistry.get().perform(action);
		} else {
			job = JobRegistry.createBuilder().withTask(action).create();
		}
		Transaction.commit();
		DomainStore.waitUntilCurrentRequestsProcessed();
		String idString = String.valueOf(job.getId());
		return idString;
	}

	@Override
	public void persistOfflineTransforms(
			List<DeltaApplicationRecord> uncommitted) throws WebException {
		TransformCommit.commitBulkTransforms(uncommitted, true, false);
	}

	@Override
	public void ping() {
	}

	@Override
	public JobTracker.Response pollJobStatus(JobTracker.Request request) {
		List<Job> jobs = request.getIds().stream().map(Job::byId)
				.filter(Objects::nonNull).collect(Collectors.toList());
		JobTracker.Response response = new JobTracker.Response();
		response.setTrackers(jobs.stream().map(Job::asJobTracker)
				.peek(jt -> jt.setLog(null)).collect(Collectors.toList()));
		return response;
	}

	@Override
	public JobTracker pollJobStatus(String id, boolean cancel) {
		Job job = Job.byId(Long.parseLong(id));
		if (job == null) {
			return null;
		}
		if (cancel) {
			job.cancel();
			Transaction.commit();
		}
		JobTracker tracker = job.asJobTracker();
		tracker.setLog(null);
		return tracker;
	}

	public PublicationResult
			publish(ContentRequestBase<? extends ContentDefinition> cr)
					throws WebException {
		return cr.publish();
	}

	private void sanitiseClrString(ClientLogRecord clr) {
		clr.setMessage(
				CommonUtils.nullToEmpty(clr.getMessage()).replace('\0', ' '));
	}

	@Override
	public SearchResultsBase search(SearchDefinition def) {
		return Registry.impl(CommonPersistenceProvider.class)
				.getCommonPersistence().search(def);
	}

	protected void setLogger(Logger logger) {
		this.logger = logger;
	}

	public EntityLocator submitPublication(
			ContentRequestBase<? extends ContentDefinition> publicationRequest)
			throws WebException {
		TaskPublish task = new TaskPublish();
		task.setPublicationRequest(publicationRequest);
		Job job = task.schedule();
		Transaction.commit();
		return job.toLocator();
	}

	@Override
	@WebMethod
	public Response suggest(BoundSuggestOracleRequest request) {
		try {
			LooseContext.set(DomainSearcher.CONTEXT_HINT, request.getHint());
			Class<? extends BoundSuggestOracleResponseElement> clazz = (Class<? extends BoundSuggestOracleResponseElement>) Class
					.forName(request.getTargetClassName());
			Registry.Query<BoundSuggestOracleRequestHandler> query = Registry
					.query(BoundSuggestOracleRequestHandler.class);
			query.addKeys(clazz);
			BoundSuggestOracleModel model = request.getModel();
			if (model != null) {
				query.addKeys(model.getClass());
			}
			return query.impl().handleRequest(clazz, request,
					request.getHint());
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	@Override
	@WebMethod(readonlyPermitted = false)
	public DomainTransformResponse transform(DomainTransformRequest request)
			throws DomainTransformRequestException {
		try {
			for (DomainTransformEvent transform : request.allTransforms()) {
				if (transform.getObjectClass()
						.getAnnotation(DomainTransformPersistable.class) != null
						|| (transform.getValueClass() != null
								&& transform.getValueClass().getAnnotation(
										DomainTransformPersistable.class) != null)) {
					throw new PermissionsException(Ax.format(
							"Illegal class for client modification: %s",
							transform));
				}
			}
			return TransformCommit.get().transform(request, false, false,
					true).response;
		} catch (DomainTransformRequestException dtre) {
			throw dtre;
		} catch (Exception e) {
			DomainTransformResponse domainTransformResponse = new DomainTransformResponse();
			domainTransformResponse.setRequest(request);
			DomainTransformException transformException = new DomainTransformException(
					e.getMessage(), e);
			transformException.setRequest(request);
			transformException.setType(DomainTransformExceptionType.UNKNOWN);
			domainTransformResponse.getTransformExceptions()
					.add(transformException);
			throw new DomainTransformRequestException(domainTransformResponse);
		}
	}

	@Override
	public List<ServerValidator>
			validateOnServer(List<ServerValidator> validators) {
		List<ServerValidator> results = new ArrayList<ServerValidator>();
		for (ServerValidator validator : validators) {
			ServerValidatorHandler handler = Registry
					.impl(ServerValidatorHandler.class, validator.getClass());
			handler.handle(validator);
			results.add(validator);
		}
		return results;
	}

	@Override
	@WebMethod(
		readonlyPermitted = true,
		customPermission = @Permission(access = AccessLevel.EVERYONE))
	public DomainUpdate
			waitForTransforms(DomainTransformCommitPosition position)
					throws PermissionsException {
		if (!waitForTransformsEnabled()) {
			throw new PermissionsException();
		}
		Long clientInstanceId = AuthenticationManager
				.provideAuthenticatedClientInstanceId();
		if (clientInstanceId == null) {
			throw new PermissionsException();
		}
		return new TransformCollector().waitForTransforms(position,
				clientInstanceId);
	}

	protected boolean waitForTransformsEnabled() {
		return false;
	}

	/**
	 * Obfuscate any fields or classes marked with @ObfuscateOnLog
	 */
	public static class ObfuscateParametersFieldFilter
			implements GraphProjectionFieldFilter {
		@Override
		public Boolean permitClass(Class clazz) {
			// Check for the ObfuscateOnLog annotation on the class
			// If present, filter out the class
			if (clazz.isAnnotationPresent(ObfuscateOnLog.class)) {
				return false;
			} else {
				// Otherwise, delegate to default permission-based filtering
				return null;
			}
		}

		@Override
		public boolean permitField(Field field,
				Set<Field> perObjectPermissionFields, Class clazz) {
			// Check for the ObfuscateOnLog annotation on the field
			// If present, filter out the field
			if (field.isAnnotationPresent(ObfuscateOnLog.class)) {
				return false;
			} else {
				return true;
			}
		}

		@Override
		public boolean permitTransient(Field field) {
			// Transient fields treated as normal
			return false;
		}
	}
}
