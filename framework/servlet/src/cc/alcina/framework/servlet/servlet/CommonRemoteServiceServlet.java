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

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gwt.user.client.rpc.IncompatibleRemoteServiceException;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.ui.SuggestOracle.Response;
import com.google.gwt.user.server.rpc.RPC;
import com.google.gwt.user.server.rpc.RPCRequest;
import com.google.gwt.user.server.rpc.RPCServletUtils;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.google.gwt.user.server.rpc.UnexpectedException;
import com.google.gwt.user.server.rpc.impl.LegacySerializationPolicy;
import com.totsp.gwittir.client.beans.Converter;
import com.totsp.gwittir.client.validator.ValidationException;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.WrappedRuntimeException.SuggestedAction;
import cc.alcina.framework.common.client.actions.ActionLogItem;
import cc.alcina.framework.common.client.actions.ActionResult;
import cc.alcina.framework.common.client.actions.RemoteAction;
import cc.alcina.framework.common.client.actions.RemoteActionPerformer;
import cc.alcina.framework.common.client.actions.RemoteActionWithParameters;
import cc.alcina.framework.common.client.actions.RemoteParametersValidator;
import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.common.client.csobjects.JobTracker;
import cc.alcina.framework.common.client.csobjects.LogMessageType;
import cc.alcina.framework.common.client.csobjects.ObjectDeltaResult;
import cc.alcina.framework.common.client.csobjects.ObjectDeltaSpec;
import cc.alcina.framework.common.client.csobjects.SearchResultsBase;
import cc.alcina.framework.common.client.csobjects.WebException;
import cc.alcina.framework.common.client.domain.search.DomainSearcher;
import cc.alcina.framework.common.client.entity.ClientLogRecord;
import cc.alcina.framework.common.client.entity.ClientLogRecord.ClientLogRecords;
import cc.alcina.framework.common.client.entity.WrapperPersistable;
import cc.alcina.framework.common.client.gwittir.validator.ServerValidator;
import cc.alcina.framework.common.client.log.ILogRecord;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.AlcinaPersistentEntityImpl;
import cc.alcina.framework.common.client.logic.domaintransform.DeltaApplicationRecord;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException.DomainTransformExceptionType;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequestException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformResponse;
import cc.alcina.framework.common.client.logic.domaintransform.DomainUpdate;
import cc.alcina.framework.common.client.logic.domaintransform.DomainUpdate.DomainTransformCommitPosition;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.permissions.AnnotatedPermissible;
import cc.alcina.framework.common.client.logic.permissions.PermissionsException;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.permissions.ReadOnlyException;
import cc.alcina.framework.common.client.logic.permissions.WebMethod;
import cc.alcina.framework.common.client.logic.reflection.DomainTransformPersistable;
import cc.alcina.framework.common.client.logic.reflection.Permission;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.remote.CommonRemoteServiceExt;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CancelledException;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.common.client.util.TopicPublisher.TopicListener;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.actions.RequiresHttpSession;
import cc.alcina.framework.entity.domaintransform.ThreadlocalTransformManager;
import cc.alcina.framework.entity.entityaccess.AppPersistenceBase;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceBase;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceLocal;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceProvider;
import cc.alcina.framework.entity.entityaccess.ServerValidatorHandler;
import cc.alcina.framework.entity.entityaccess.metric.InternalMetricData;
import cc.alcina.framework.entity.entityaccess.metric.InternalMetrics;
import cc.alcina.framework.entity.entityaccess.metric.InternalMetrics.InternalMetricTypeAlcina;
import cc.alcina.framework.entity.entityaccess.transform.TransformCommit;
import cc.alcina.framework.entity.entityaccess.transform.TransformCommit.TransformPriorityStd;
import cc.alcina.framework.entity.logic.EntityLayerLogging;
import cc.alcina.framework.entity.projection.GraphProjections;
import cc.alcina.framework.entity.util.AlcinaBeanSerializerS;
import cc.alcina.framework.entity.util.AlcinaChildRunnable;
import cc.alcina.framework.entity.util.JacksonJsonObjectSerializer;
import cc.alcina.framework.gwt.client.gwittir.widget.BoundSuggestBox.BoundSuggestOracleRequest;
import cc.alcina.framework.gwt.client.gwittir.widget.BoundSuggestOracleResponseType;
import cc.alcina.framework.gwt.client.gwittir.widget.BoundSuggestOracleResponseType.BoundSuggestOracleModel;
import cc.alcina.framework.gwt.client.gwittir.widget.BoundSuggestOracleResponseType.BoundSuggestOracleSuggestion;
import cc.alcina.framework.servlet.ServletLayerObjects;
import cc.alcina.framework.servlet.ServletLayerUtils;
import cc.alcina.framework.servlet.ServletLayerValidatorHandler;
import cc.alcina.framework.servlet.SessionHelper;
import cc.alcina.framework.servlet.SessionProvider;
import cc.alcina.framework.servlet.job.JobRegistry;

/**
 *
 * Tests (todo) for transform persistence: invalid clientauth multiple
 * simultaneous (identical clientinstance, non-) cross-server-restart
 *
 * <p>
 * Readonly: most checks happen of simple methods happen at the persistence
 * layer so not needed here
 * </p>
 *
 * @author nick@alcina.cc
 *
 */
public abstract class CommonRemoteServiceServlet extends RemoteServiceServlet
		implements CommonRemoteServiceExt {
	public static final String UA_NULL_SERVER = "null/server";

	public static final String THRD_LOCAL_RPC_RQ = "THRD_LOCAL_RPC_RQ";

	public static final String THRD_LOCAL_USER_NAME = "THRD_LOCAL_USER_NAME";

	public static final String THRD_LOCAL_RPC_PAYLOAD = "THRD_LOCAL_RPC_PAYLOAD";

	public static final String CONTEXT_RPC_USER_ID = CommonRemoteServiceServlet.class
			.getName() + ".CONTEXT_RPC_USER_ID";

	public static final String CONTEXT_OVERRIDE_CONTEXT = CommonRemoteServiceServlet.class
			.getName() + ".CONTEXT_OVERRIDE_CONTEXT";

	public static final String CONTEXT_THREAD_LOCAL_HTTP_REQUEST = CommonRemoteServiceServlet.class
			.getName() + ".CONTEXT_THREAD_LOCAL_HTTP_REQUEST";

	public static final String CONTEXT_THREAD_LOCAL_HTTP_RESPONSE = CommonRemoteServiceServlet.class
			.getName() + ".CONTEXT_THREAD_LOCAL_HTTP_RESPONSE";

	public static final String CONTEXT_NO_ACTION_LOG = CommonRemoteServiceServlet.class
			.getName() + ".CONTEXT_NO_ACTION_LOG";

	public static final String CONTEXT_THREAD_LOCAL_HTTP_RESPONSE_HEADERS = CommonRemoteServiceServlet.class
			.getName() + ".CONTEXT_THREAD_LOCAL_HTTP_RESPONSE_HEADERS";

	public static boolean DUMP_STACK_TRACE_ON_OOM = true;

	public static HttpServletRequest getContextThreadLocalRequest() {
		return LooseContext.get(
				CommonRemoteServiceServlet.CONTEXT_THREAD_LOCAL_HTTP_REQUEST);
	}

	public static HttpServletResponse getContextThreadLocalResponse() {
		return LooseContext.get(
				CommonRemoteServiceServlet.CONTEXT_THREAD_LOCAL_HTTP_RESPONSE);
	}

	public static String getUserAgent(HttpServletRequest rq) {
		return rq == null ? UA_NULL_SERVER : rq.getHeader("User-Agent");
	}

	public static void setContextThreadLocalRequestResponse(
			HttpServletRequest request, HttpServletResponse response) {
		LooseContext.set(CONTEXT_THREAD_LOCAL_HTTP_REQUEST, request);
		LooseContext.set(CONTEXT_THREAD_LOCAL_HTTP_RESPONSE, response);
	}

	/*
	 * Because GWT unexpected exception code resets the headers, we handle this
	 * special
	 */
	protected static void setHeader(String key, String value) {
		getContextThreadLocalResponse().setHeader(key, value);
		StringMap cachedForUnexpectedException = (StringMap) getContextThreadLocalRequest()
				.getAttribute(
						CommonRemoteServiceServlet.CONTEXT_THREAD_LOCAL_HTTP_RESPONSE_HEADERS);
		cachedForUnexpectedException.put(key, value);
	}

	protected Logger logger = LoggerFactory.getLogger(getClass());

	private AtomicInteger actionCounter = new AtomicInteger();

	private ThreadLocal<Integer> looseContextDepth = new ThreadLocal<>();

	private AtomicInteger callCounter = new AtomicInteger(0);

	private AtomicInteger rpcExceptionLogCounter = new AtomicInteger();

	private AlcinaServletContext alcinaServletContext = new AlcinaServletContext()
			.withRootPermissions(false);

	@Override
	@WebMethod(readonlyPermitted = true)
	public void dumpData(String data) {
		if (!ResourceUtilities.getBoolean(CommonRemoteServiceServlet.class,
				"dumpPermitted")) {
			throw new RuntimeException("Dump not permitted");
		}
		String key = String.valueOf(System.currentTimeMillis());
		File dir = getDataDumpsFolder();
		String path = Ax.format("%s/%s.dat", dir.getPath(), key);
		File file = new File(path);
		try {
			ResourceUtilities.writeStringToFile(data, file);
			System.out.println("Client db dumped - key: " + key);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	@WebMethod(readonlyPermitted = true)
	public <T extends Entity> T getItemById(String className, Long id)
			throws WebException {
		try {
			Class<T> clazz = (Class<T>) Class.forName(className);
			return Registry.impl(CommonPersistenceProvider.class)
					.getCommonPersistence().getItemById(clazz, id, true, false);
		} catch (Exception e) {
			logRpcException(e);
			throw new WebException(e.getMessage());
		}
	}

	@Override
	@WebMethod(readonlyPermitted = true)
	public List<ActionLogItem> getLogsForAction(RemoteAction action,
			Integer count) {
		checkAnnotatedPermissions(action);
		return Registry.impl(CommonPersistenceProvider.class)
				.getCommonPersistence()
				.listLogItemsForClass(action.getClass().getName(), count);
	}

	@Override
	@WebMethod(readonlyPermitted = true)
	public List<ObjectDeltaResult> getObjectDelta(List<ObjectDeltaSpec> specs)
			throws WebException {
		try {
			return Registry.impl(CommonPersistenceProvider.class)
					.getCommonPersistence().getObjectDelta(specs);
		} catch (Exception e) {
			logRpcException(e);
			throw new WebException(e);
		}
	}

	@Override
	@WebMethod(readonlyPermitted = true)
	public List<String> listRunningJobs() {
		return JobRegistry.get().getRunningJobs();
	}

	@Override
	public String loadData(String key) {
		if (!ResourceUtilities.getBoolean(CommonRemoteServiceServlet.class,
				"loadDumpPermitted")) {
			throw new RuntimeException("Load dump not permitted");
		}
		File dir = getDataDumpsFolder();
		String path = Ax.format("%s/%s.dat", dir.getPath(), key);
		File file = new File(path);
		try {
			return ResourceUtilities.readFileToString(file);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	@Override
	@WebMethod(readonlyPermitted = true, customPermission = @Permission(access = AccessLevel.EVERYONE))
	public Long log(ILogRecord logRecord) {
		return Registry.impl(CommonPersistenceProvider.class)
				.getCommonPersistence().persistLogRecord(logRecord);
	}

	@Override
	@WebMethod(readonlyPermitted = true, customPermission = @Permission(access = AccessLevel.EVERYONE))
	public Long logClientError(String exceptionToString) {
		return logClientError(exceptionToString,
				LogMessageType.CLIENT_EXCEPTION.toString());
	}

	@Override
	@WebMethod(readonlyPermitted = true, customPermission = @Permission(access = AccessLevel.EVERYONE))
	public Long logClientError(String exceptionToString, String exceptionType) {
		String remoteAddr = getRemoteAddress();
		try {
			exceptionToString = CommonUtils.nullToEmpty(exceptionToString)
					.replace('\0', ' ');
			LooseContext.pushWithKey(
					CommonPersistenceBase.CONTEXT_CLIENT_IP_ADDRESS,
					remoteAddr);
			return Registry.impl(CommonPersistenceProvider.class)
					.getCommonPersistence()
					.log(exceptionToString, exceptionType);
		} finally {
			LooseContext.pop();
		}
	}

	@Override
	public void logClientRecords(String serializedLogRecords) {
		Converter<String, ClientLogRecords> converter = new Converter<String, ClientLogRecord.ClientLogRecords>() {
			@Override
			public ClientLogRecords convert(String original) {
				try {
					return new AlcinaBeanSerializerS().deserialize(original);
				} catch (Exception e) {
					System.out.format(
							"problem deserializing clientlogrecord:\n%s\n",
							original);
					e.printStackTrace();
					if (ResourceUtilities.getBoolean(
							CommonRemoteServiceServlet.class,
							"throwLogClientRecordExceptions")) {
						throw new WrappedRuntimeException(e);
					}
					return null;
				}
			}
		};
		List<String> lines = Arrays.asList(serializedLogRecords.split("\n"));
		List<ClientLogRecords> records = CollectionFilters.convert(lines,
				converter);
		while (records.remove(null)) {
		}
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

	public void logRpcException(Exception ex) {
		logRpcException(ex, LogMessageType.RPC_EXCEPTION.toString());
	}

	public void logRpcException(Exception ex, String exceptionType) {
		String remoteAddr = getRemoteAddress();
		if (rpcExceptionLogCounter.incrementAndGet() > 10000) {
			Ax.err("Not logging rpc exception %s : %s - too many exceptions",
					exceptionType, CommonUtils.toSimpleExceptionMessage(ex));
			return;
		}
		try {
			LooseContext.pushWithKey(
					CommonPersistenceBase.CONTEXT_CLIENT_IP_ADDRESS,
					remoteAddr);
			RPCRequest rpcRequest = getThreadRpcRequest();
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			ex.printStackTrace(pw);
			String msg = "RPC exception:\n";
			if (rpcRequest != null) {
				msg = describeRpcRequest(rpcRequest, msg);
			}
			msg += "\nStacktrace:\t " + sw.toString();
			Ax.err(msg);
			CommonPersistenceLocal cpl = Registry
					.impl(CommonPersistenceProvider.class)
					.getCommonPersistence();
			cpl.log(msg, exceptionType);
		} finally {
			LooseContext.pop();
		}
	}

	/**
	 * Note - don't (normally) call this server-side, particularly in a loop,
	 * since it spawns a potentially unlimited number of performers
	 */
	@Override
	public String performAction(final RemoteAction action) {
		checkAnnotatedPermissions(action);
		final RemoteActionPerformer performer = (RemoteActionPerformer) Registry
				.get().instantiateSingle(RemoteActionPerformer.class,
						action.getClass());
		if (performer instanceof RemoteParametersValidator) {
			try {
				((RemoteParametersValidator) performer).validate(
						((RemoteActionWithParameters) action).getParameters());
			} catch (ValidationException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}
		// because we're spawning the thread, we use this pattern to allow for
		// getting to the countDown() in the spawned thread before the await()
		// in the launcher
		ActionLauncherAsync async = new ActionLauncherAsync(
				performer.getClass().getSimpleName() + "-"
						+ (actionCounter.incrementAndGet()),
				action);
		JobTracker tracker = async.launchAndWaitForTracker();
		return tracker.getId();
	}

	@Override
	public ActionLogItem performActionAndWait(final RemoteAction action)
			throws WebException {
		return new ActionLauncher().performActionAndWait(action).actionLogItem;
	}

	public <T> ActionResult<T> performActionAndWaitForObject(
			final RemoteAction action) throws WebException {
		return new ActionLauncher().performActionAndWait(action);
	}

	@Override
	public <G extends WrapperPersistable> Long persist(G gwpo)
			throws WebException {
		try {
			Long id = Registry.impl(CommonPersistenceProvider.class)
					.getCommonPersistence().persist(gwpo);
			TransformCommit.get().handleWrapperTransforms();
			return id;
		} catch (Exception e) {
			logger.warn("Exception in persist wrappable", e);
			logRpcException(e);
			throw new WebException(e.getMessage());
		}
	}

	// TODO - well, lock sync
	@Override
	public void persistOfflineTransforms(
			List<DeltaApplicationRecord> uncommitted) throws WebException {
		TransformCommit.commitBulkTransforms(uncommitted, logger, true, false);
	}

	public int persistOfflineTransforms(
			List<DeltaApplicationRecord> uncommitted, Logger logger)
			throws WebException {
		return TransformCommit.commitBulkTransforms(uncommitted, logger, null,
				false);
	}

	@Override
	public void ping() {
	}

	@Override
	public JobTracker pollJobStatus(String id, boolean cancel) {
		if (cancel) {
			JobRegistry.get().cancel(id);
		}
		JobTracker tracker = JobRegistry.get().getTracker(id);
		if (tracker == null) {
			return null;
		}
		return JobRegistry.exportableForm(tracker);
	}

	@Override
	public String processCall(String payload) throws SerializationException {
		RPCRequest rpcRequest = null;
		try {
			LooseContext.set(CONTEXT_THREAD_LOCAL_HTTP_REQUEST,
					getThreadLocalRequest());
			LooseContext.set(CONTEXT_THREAD_LOCAL_HTTP_RESPONSE,
					getThreadLocalResponse());
			getThreadLocalRequest().setAttribute(
					CONTEXT_THREAD_LOCAL_HTTP_RESPONSE_HEADERS,
					new StringMap());
			rpcRequest = RPC.decodeRequest(payload, this.getClass(), this);
			String suffix = getRpcHandlerThreadNameSuffix(rpcRequest);
			String name = rpcRequest.getMethod().getName();
			String threadName = Ax.format("gwt-rpc:%s:%s%s", name,
					callCounter.incrementAndGet(), suffix);
			alcinaServletContext.begin(getThreadLocalRequest(),
					getThreadLocalResponse(), threadName);
			if (rpcRequest
					.getSerializationPolicy() instanceof LegacySerializationPolicy) {
				throw new IncompatibleRemoteServiceException();
			}
			getThreadLocalRequest().setAttribute(THRD_LOCAL_RPC_RQ, rpcRequest);
			getThreadLocalRequest().setAttribute(THRD_LOCAL_RPC_PAYLOAD,
					payload);
			LooseContext.set(CommonPersistenceBase.CONTEXT_CLIENT_IP_ADDRESS,
					ServletLayerUtils
							.robustGetRemoteAddr(getThreadLocalRequest()));
			LooseContext.set(CommonPersistenceBase.CONTEXT_CLIENT_INSTANCE_ID,
					SessionHelper.getAuthenticatedSessionClientInstanceId(
							getThreadLocalRequest()));
			RPCRequest f_rpcRequest = rpcRequest;
			onAfterAlcinaAuthentication(name);
			LooseContext.set(CONTEXT_RPC_USER_ID,
					PermissionsManager.get().getUserId());
			InternalMetrics.get().startTracker(rpcRequest,
					() -> describeRpcRequest(f_rpcRequest, ""),
					InternalMetricTypeAlcina.client,
					Thread.currentThread().getName(), () -> true);
			TransformCommit.prepareHttpRequestCommitContext(
					PermissionsManager.get().getClientInstance().getId(),
					PermissionsManager.get().getUserId(), ServletLayerUtils
							.robustGetRemoteAddr(getThreadLocalRequest()));
			Method method;
			try {
				method = this.getClass().getMethod(name,
						rpcRequest.getMethod().getParameterTypes());
				if (method.isAnnotationPresent(WebMethod.class)) {
					WebMethod webMethod = method.getAnnotation(WebMethod.class);
					AnnotatedPermissible ap = new AnnotatedPermissible(
							webMethod.customPermission());
					if (!PermissionsManager.get().isPermissible(ap)) {
						WebException wex = new WebException(
								"Action not permitted: " + name);
						logRpcException(wex,
								LogMessageType.PERMISSIONS_EXCEPTION
										.toString());
						return RPC.encodeResponseForFailure(null, wex);
					}
					if (!webMethod.readonlyPermitted()) {
						AppPersistenceBase.checkNotReadOnly();
					}
				}
			} catch (SecurityException ex) {
				RPC.encodeResponseForFailure(null, ex);
			} catch (NoSuchMethodException ex) {
				RPC.encodeResponseForFailure(null, ex);
			}
			return invokeAndEncodeResponse(rpcRequest);
		} catch (IncompatibleRemoteServiceException ex) {
			getServletContext().log(
					"An IncompatibleRemoteServiceException was thrown while processing this call.",
					ex);
			return RPC.encodeResponseForFailure(null, ex);
		} catch (UnexpectedException ex) {
			logRpcException(ex);
			throw ex;
		} catch (OutOfMemoryError e) {
			handleOom(payload, e);
			throw e;
		} catch (RuntimeException rex) {
			logRpcException(rex);
			throw rex;
		} finally {
			/*
			 * save the username for metric logging - the user will be cleared
			 * before the metrics are output
			 */
			getThreadLocalRequest().setAttribute(THRD_LOCAL_USER_NAME,
					PermissionsManager.get().getUserName());
			InternalMetrics.get().endTracker(rpcRequest);
			alcinaServletContext.end();
		}
	}

	@Override
	public SearchResultsBase search(SearchDefinition def, int pageNumber) {
		return Registry.impl(CommonPersistenceProvider.class)
				.getCommonPersistence().search(def, pageNumber);
	}

	@Override
	public Response suggest(BoundSuggestOracleRequest request) {
		try {
			LooseContext.set(DomainSearcher.CONTEXT_HINT, request.hint);
			Class<? extends BoundSuggestOracleResponseType> clazz = (Class<? extends BoundSuggestOracleResponseType>) Class
					.forName(request.targetClassName);
			return Registry.impl(BoundSuggestOracleRequestHandler.class, clazz)
					.handleRequest(clazz, request, request.hint);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	@Override
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
			logRpcException(e);
			throw new DomainTransformRequestException(domainTransformResponse);
		}
	}

	@Override
	public List<ServerValidator>
			validateOnServer(List<ServerValidator> validators) {
		List<ServerValidator> entityLayer = new ArrayList<ServerValidator>();
		List<ServerValidator> results = new ArrayList<ServerValidator>();
		for (ServerValidator validator : validators) {
			Class clazz = Registry.get().lookupSingle(ServerValidator.class,
					validator.getClass());
			ServerValidatorHandler handler = null;
			if (ServerValidatorHandler.class.isAssignableFrom(clazz)) {
				handler = (ServerValidatorHandler) Registry.get()
						.instantiateSingle(ServerValidator.class,
								validator.getClass());
			}
			if (handler instanceof ServletLayerValidatorHandler) {
				handler.handle(validator, null);
				results.add(validator);
			} else {
				results.addAll(Registry.impl(CommonPersistenceProvider.class)
						.getCommonPersistence()
						.validate(Collections.singletonList(validator)));
			}
		}
		return results;
	}

	@Override
	@WebMethod(readonlyPermitted = true, customPermission = @Permission(access = AccessLevel.EVERYONE))
	public DomainUpdate
			waitForTransforms(DomainTransformCommitPosition position)
					throws PermissionsException {
		if (!waitForTransformsEnabled()) {
			throw new PermissionsException();
		}
		Long clientInstanceId = Registry.impl(SessionHelper.class)
				.getAuthenticatedClientInstanceId(getThreadLocalRequest());
		if (clientInstanceId == null) {
			throw new PermissionsException();
		}
		return new TransformCollector().waitForTransforms(position,
				clientInstanceId);
	}

	private File getDataDumpsFolder() {
		File dataFolder = ServletLayerObjects.get().getDataFolder();
		File dir = new File(
				dataFolder.getPath() + File.separator + "client-dumps");
		dir.mkdirs();
		return dir;
	}

	private void sanitiseClrString(ClientLogRecord clr) {
		clr.setMessage(
				CommonUtils.nullToEmpty(clr.getMessage()).replace('\0', ' '));
	}

	protected void checkAnnotatedPermissions(Object o) {
		WebMethod ara = o.getClass().getAnnotation(WebMethod.class);
		if (ara != null) {
			if (!PermissionsManager.get().isPermissible(
					new AnnotatedPermissible(ara.customPermission()))) {
				WrappedRuntimeException e = new WrappedRuntimeException(
						"Permission denied for action " + o,
						SuggestedAction.NOTIFY_WARNING);
				EntityLayerLogging.log(LogMessageType.TRANSFORM_EXCEPTION,
						"Domain transform permissions exception", e);
				throw e;
			}
		}
	}

	protected String describeRpcRequest(RPCRequest rpcRequest, String msg) {
		msg += "Method: " + rpcRequest.getMethod().getName() + "\n";
		msg += "User: " + PermissionsManager.get().getUserString() + "\n";
		msg += "Types: " + CommonUtils.joinWithNewlineTab(
				Arrays.asList(rpcRequest.getMethod().getParameters()));
		msg += "\nParameters: \n";
		Object[] parameters = rpcRequest.getParameters();
		if (rpcRequest.getMethod().getName().equals("transform")) {
		} else {
			for (int idx = 0; idx < parameters.length; idx++) {
				try {
					String serializedParameter = new JacksonJsonObjectSerializer()
							.withIdRefs().withMaxLength(100000)
							.withTruncateAtMaxLength(true)
							.serializeNoThrow(parameters[idx]);
					msg += Ax.format("%s: %s\n", idx, serializedParameter);
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
		}
		return msg;
	}

	@Override
	protected void doUnexpectedFailure(Throwable e) {
		if (e.getClass().getName()
				.equals("org.apache.catalina.connector.ClientAbortException")) {
			getLogger().debug("Client RPC call aborted by client");
			return;
		}
		if (e instanceof ReadOnlyException) {
			try {
				HttpServletResponse response = getThreadLocalResponse();
				response.reset();
				ServletContext servletContext = getServletContext();
				response.setContentType("text/plain");
				response.setStatus(
						HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				response.getOutputStream()
						.write(e.toString().getBytes("UTF-8"));
			} catch (Exception e2) {
				throw new WrappedRuntimeException(e2);
			}
		} else {
			e.printStackTrace();
			try {
				getThreadLocalResponse().reset();
				StringMap cachedForUnexpectedException = (StringMap) getThreadLocalRequest()
						.getAttribute(
								CONTEXT_THREAD_LOCAL_HTTP_RESPONSE_HEADERS);
				cachedForUnexpectedException.forEach(
						(k, v) -> getThreadLocalResponse().setHeader(k, v));
			} catch (IllegalStateException ex) {
				/*
				 * If we can't reset the request, the only way to signal that
				 * something has gone wrong is to throw an exception from here.
				 * It should be the case that we call the user's implementation
				 * code before emitting data into the response, so the only time
				 * that gets tripped is if the object serialization code blows
				 * up.
				 */
				throw new RuntimeException("Unable to report failure", e);
			}
			ServletContext servletContext = getServletContext();
			RPCServletUtils.writeResponseForUnexpectedFailure(servletContext,
					getThreadLocalResponse(), e);
		}
	}

	protected Logger getLogger() {
		return logger;
	}

	protected String getRemoteAddress() {
		return getThreadLocalRequest() == null ? null
				: getThreadLocalRequest().getRemoteAddr();
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
		return getSession(getThreadLocalRequest());
	}

	protected HttpSession getSession(HttpServletRequest request) {
		return getSession(request, getThreadLocalResponse());
	}

	protected HttpSession getSession(HttpServletRequest request,
			HttpServletResponse response) {
		return Registry.impl(SessionProvider.class).getSession(request,
				response);
	}

	protected RPCRequest getThreadRpcRequest() {
		return getThreadLocalRequest() == null ? null
				: (RPCRequest) getThreadLocalRequest()
						.getAttribute(THRD_LOCAL_RPC_RQ);
	}

	protected String getUserAgent() {
		return getUserAgent(getThreadLocalRequest());
	}

	protected void handleOom(String payload, OutOfMemoryError e) {
		if (DUMP_STACK_TRACE_ON_OOM) {
			System.out.println("Payload:");
			System.out.println(payload);
			e.printStackTrace();
			SEUtilities.dumpAllThreads();
		}
	}

	protected String invokeAndEncodeResponse(RPCRequest rpcRequest)
			throws SerializationException {
		return RPC.invokeAndEncodeResponse(this, rpcRequest.getMethod(),
				rpcRequest.getParameters(),
				rpcRequest.getSerializationPolicy());
	}

	protected boolean isPersistOfflineTransforms() {
		return true;
	}

	protected int nextTransformRequestId() {
		return Registry.impl(TransformCommit.class).nextTransformRequestId();
	}

	protected void onAfterAlcinaAuthentication(String methodName) {
	}

	@Override
	protected void onAfterResponseSerialized(String serializedResponse) {
		LooseContext.confirmDepth(looseContextDepth.get());
		PermissionsManager.get().setUser(null);
		super.onAfterResponseSerialized(serializedResponse);
	}

	protected void onAfterSpawnedThreadRun(Map properties) {
	}

	@Override
	protected void onBeforeRequestDeserialized(String serializedRequest) {
		super.onBeforeRequestDeserialized(serializedRequest);
		looseContextDepth.set(LooseContext.depth());
		getThreadLocalResponse().setHeader("Cache-Control", "no-cache");
	}

	protected void onBeforeSpawnedThreadRun(Map properties) {
	}

	protected void setLogger(Logger logger) {
		this.logger = logger;
	}

	protected boolean waitForTransformsEnabled() {
		return false;
	}

	String describeRemoteAction(RemoteAction remoteAction, String msg) {
		msg += "Clazz: " + remoteAction.getClass().getName() + "\n";
		msg += "User: " + PermissionsManager.get().getUserString() + "\n";
		msg += "\nParameters: \n";
		try {
			msg += new JacksonJsonObjectSerializer().withIdRefs()
					.withMaxLength(1000000).serializeNoThrow(remoteAction);
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return msg;
	}

	public class ActionLauncher<T> {
		private JobTracker actionTracker;

		TopicListener<JobTracker> startListener = new TopicListener<JobTracker>() {
			boolean processed = false;

			@Override
			public void topicPublished(String key, JobTracker message) {
				if (processed) {
				} else {
					processed = true;
					actionTracker = message;
				}
			}
		};

		protected ActionLogItem trackerToResult(final RemoteAction action) {
			ActionLogItem logItem = AlcinaPersistentEntityImpl
					.getNewImplementationInstance(ActionLogItem.class);
			logItem.setActionClass(action.getClass());
			logItem.setActionDate(new Date());
			logItem.setShortDescription(CommonUtils
					.trimToWsChars(actionTracker.getJobResult(), 220));
			if (!LooseContext.is(CONTEXT_NO_ACTION_LOG)) {
				logItem.setActionLog(actionTracker.getLog());
			}
			return logItem;
		}

		protected ActionResult<T> trackerToResult(final RemoteAction action,
				boolean nonPersistent) {
			ActionResult<T> result = new ActionResult<T>();
			if (actionTracker != null) {
				ActionLogItem logItem = trackerToResult(action);
				if (!actionTracker.provideIsRoot() || nonPersistent) {
				} else {
					try {
						Registry.impl(CommonPersistenceProvider.class)
								.getCommonPersistence().logActionItem(logItem);
					} catch (RuntimeException e) {
						e.printStackTrace();
					}
				}
				result.actionLogItem = logItem;
				result.resultObject = (T) actionTracker.getJobResultObject();
			}
			return result;
		}

		ActionResult<T> performActionAndWait(final RemoteAction action)
				throws WebException {
			checkAnnotatedPermissions(action);
			RemoteActionPerformer performer = (RemoteActionPerformer) Registry
					.get().instantiateSingle(RemoteActionPerformer.class,
							action.getClass());
			if (performer instanceof RequiresHttpSession) {
				RequiresHttpSession rhs = (RequiresHttpSession) performer;
				rhs.setHttpSession(getSession());
			}
			boolean nonPersistent = LooseContext
					.is(JobRegistry.CONTEXT_NON_PERSISTENT) || Ax.isTest();
			TransformManager transformManager = TransformManager.get();
			try {
				if (transformManager instanceof ThreadlocalTransformManager) {
					ThreadlocalTransformManager.get().resetTltm(null);
				}
				LooseContext.push();
				if (!LooseContext.has(CONTEXT_THREAD_LOCAL_HTTP_REQUEST)) {
					ActionPerformerMetricFilter filter = Registry
							.impl(ActionPerformerMetricFilter.class);
					InternalMetrics.get().startTracker(action,
							() -> describeRemoteAction(action, ""),
							InternalMetricTypeAlcina.service,
							action.getClass().getSimpleName(), () -> true);
				}
				LooseContext.getContext().addTopicListener(
						JobRegistry.TOPIC_JOB_STARTED, startListener);
				performer.performAction(action);
				return trackerToResult(action, nonPersistent);
			} catch (Throwable t) {
				Exception e = (Exception) ((t instanceof Exception) ? t
						: new WrappedRuntimeException(t));
				if (actionTracker != null && !actionTracker.isComplete()) {
					JobRegistry.get().jobError(e);
					trackerToResult(action, nonPersistent);
				}
				boolean log = true;
				if (e instanceof WrappedRuntimeException) {
					WrappedRuntimeException ire = (WrappedRuntimeException) e;
					log = ire
							.getSuggestedAction() != SuggestedAction.EXPECTED_EXCEPTION;
				}
				if (log) {
					if (CommonUtils.extractCauseOfClass(e,
							CancelledException.class) != null) {
					} else {
						logRpcException(e);
					}
				}
				throw new WebException(e);
			} finally {
				if (!LooseContext.has(CONTEXT_THREAD_LOCAL_HTTP_REQUEST)) {
					InternalMetrics.get().endTracker(action);
				}
				LooseContext.pop();
				if (transformManager instanceof ThreadlocalTransformManager) {
					ThreadlocalTransformManager.get().resetTltm(null);
				}
			}
		}
	}

	@RegistryLocation(registryPoint = ActionPerformerMetricFilter.class, implementationType = ImplementationType.SINGLETON)
	public static class ActionPerformerMetricFilter
			implements Predicate<InternalMetricData> {
		@Override
		public boolean test(InternalMetricData imd) {
			return false;
		}
	}

	public static abstract class BoundSuggestOracleRequestHandler<T extends BoundSuggestOracleResponseType> {
		public Response handleRequest(Class<T> clazz,
				BoundSuggestOracleRequest request, String hint) {
			Response response = new Response();
			List<T> responses = getResponses(request.getQuery(), request.model,
					hint);
			response.setSuggestions(responses.stream()
					.map(BoundSuggestOracleSuggestion::new)
					.limit(getSuggestionLimit()).collect(Collectors.toList()));
			if (offerNullSuggestion()) {
				((List) response.getSuggestions()).add(0,
						BoundSuggestOracleSuggestion.nullSuggestion());
			}
			return GraphProjections.defaultProjections().project(response);
		}

		protected abstract List<T> getResponses(String query,
				BoundSuggestOracleModel model, String hint);

		protected long getSuggestionLimit() {
			return 50;
		}

		protected boolean offerNullSuggestion() {
			return true;
		}
	}

	class ActionLauncherAsync extends AlcinaChildRunnable {
		private CountDownLatch latch;

		private RemoteAction action;

		private TopicListener startListener;

		volatile JobTracker tracker;

		private Map properties = new LinkedHashMap();

		ActionLauncherAsync(String name, RemoteAction action) {
			super(name);
			this.latch = new CountDownLatch(2);
			this.action = action;
			this.startListener = new TopicListener<JobTracker>() {
				@Override
				public void topicPublished(String key, JobTracker tracker) {
					ActionLauncherAsync.this.tracker = tracker;
					latch.countDown();
				}
			};
		}

		public JobTracker launchAndWaitForTracker() {
			Thread thread = new Thread(this);
			onBeforeSpawnedThreadRun(properties);
			thread.start();
			latch.countDown();
			try {
				latch.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return tracker;
		}

		@Override
		protected void run0() throws Exception {
			onAfterSpawnedThreadRun(properties);
			LooseContext.getContext().addTopicListener(
					JobRegistry.TOPIC_JOB_STARTED, startListener);
			TransformCommit.setPriority(TransformPriorityStd.Job);
			performActionAndWait(this.action);
		}
	}
}
