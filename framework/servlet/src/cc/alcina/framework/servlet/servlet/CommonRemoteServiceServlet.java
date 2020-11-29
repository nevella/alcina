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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
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

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.actions.ActionLogItem;
import cc.alcina.framework.common.client.actions.RemoteAction;
import cc.alcina.framework.common.client.actions.SynchronousAction;
import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.common.client.csobjects.JobTracker;
import cc.alcina.framework.common.client.csobjects.LogMessageType;
import cc.alcina.framework.common.client.csobjects.SearchResultsBase;
import cc.alcina.framework.common.client.csobjects.WebException;
import cc.alcina.framework.common.client.domain.search.DomainSearcher;
import cc.alcina.framework.common.client.entity.ClientLogRecord;
import cc.alcina.framework.common.client.entity.ClientLogRecord.ClientLogRecords;
import cc.alcina.framework.common.client.entity.WrapperPersistable;
import cc.alcina.framework.common.client.gwittir.validator.ServerValidator;
import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.log.ILogRecord;
import cc.alcina.framework.common.client.logic.domain.DomainTransformPersistable;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.DeltaApplicationRecord;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException.DomainTransformExceptionType;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequestException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformResponse;
import cc.alcina.framework.common.client.logic.domaintransform.DomainUpdate;
import cc.alcina.framework.common.client.logic.domaintransform.DomainUpdate.DomainTransformCommitPosition;
import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.permissions.AnnotatedPermissible;
import cc.alcina.framework.common.client.logic.permissions.PermissionsException;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.permissions.ReadOnlyException;
import cc.alcina.framework.common.client.logic.permissions.WebMethod;
import cc.alcina.framework.common.client.logic.reflection.Permission;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.publication.ContentDefinition;
import cc.alcina.framework.common.client.publication.request.ContentRequestBase;
import cc.alcina.framework.common.client.publication.request.PublicationResult;
import cc.alcina.framework.common.client.remote.CommonRemoteService;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.persistence.AppPersistenceBase;
import cc.alcina.framework.entity.persistence.CommonPersistenceBase;
import cc.alcina.framework.entity.persistence.CommonPersistenceLocal;
import cc.alcina.framework.entity.persistence.CommonPersistenceProvider;
import cc.alcina.framework.entity.persistence.ServerValidatorHandler;
import cc.alcina.framework.entity.persistence.cache.DomainStore;
import cc.alcina.framework.entity.persistence.metric.InternalMetrics;
import cc.alcina.framework.entity.persistence.metric.InternalMetrics.InternalMetricTypeAlcina;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.entity.persistence.transform.TransformCommit;
import cc.alcina.framework.entity.projection.GraphProjections;
import cc.alcina.framework.entity.util.AlcinaBeanSerializerS;
import cc.alcina.framework.entity.util.JacksonJsonObjectSerializer;
import cc.alcina.framework.gwt.client.gwittir.widget.BoundSuggestBox.BoundSuggestOracleRequest;
import cc.alcina.framework.gwt.client.gwittir.widget.BoundSuggestOracleResponseType;
import cc.alcina.framework.gwt.client.gwittir.widget.BoundSuggestOracleResponseType.BoundSuggestOracleModel;
import cc.alcina.framework.gwt.client.gwittir.widget.BoundSuggestOracleResponseType.BoundSuggestOracleSuggestion;
import cc.alcina.framework.servlet.ServletLayerUtils;
import cc.alcina.framework.servlet.ServletLayerValidatorHandler;
import cc.alcina.framework.servlet.SessionProvider;
import cc.alcina.framework.servlet.authentication.AuthenticationManager;
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
		implements CommonRemoteService {
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

	public static HttpContext getHttpContext() {
		HttpContext context = new HttpContext();
		context.request = getContextThreadLocalRequest();
		context.response = getContextThreadLocalResponse();
		return context;
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

	private ThreadLocal<Integer> looseContextDepth = new ThreadLocal<>();

	private AtomicInteger callCounter = new AtomicInteger(0);

	private AtomicInteger rpcExceptionLogCounter = new AtomicInteger();

	private AlcinaServletContext alcinaServletContext = new AlcinaServletContext()
			.withRootPermissions(false);

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
		return JobRegistry.get().getLogsForAction(action, count);
	}

	@Override
	@WebMethod(readonlyPermitted = true)
	// FIXME - mvcc.jobs - remove
	public List<String> listRunningJobs() {
		throw new UnsupportedOperationException();
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
		AlcinaServlet.topicApplicationThrowables().publish(ex);
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
	public String performAction(RemoteAction action) {
		if (action instanceof SynchronousAction) {
			return JobRegistry.get().perform(action).getResultMessage();
		} else {
			Job job = JobRegistry.createBuilder().withTask(action).create();
			Transaction.commit();
			DomainStore.waitUntilCurrentRequestsProcessed();
			String idString = String.valueOf(job.getId());
			return idString;
		}
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

	@Override
	public void persistOfflineTransforms(
			List<DeltaApplicationRecord> uncommitted) throws WebException {
		TransformCommit.commitBulkTransforms(uncommitted, true, false);
	}

	@Override
	public void ping() {
	}

	@Override
	public JobTracker pollJobStatus(String id, boolean cancel) {
		Job job = Job.byId(Long.parseLong(id));
		if (job == null) {
			return null;
		}
		if (cancel) {
			job.cancel();
		}
		return job.asJobTracker();
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
							.robustGetRemoteAddress(getThreadLocalRequest()));
			LooseContext.set(CommonPersistenceBase.CONTEXT_CLIENT_INSTANCE_ID,
					AuthenticationManager
							.provideAuthenticatedClientInstanceId());
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
							.robustGetRemoteAddress(getThreadLocalRequest()));
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

	public abstract PublicationResult
			publish(ContentRequestBase<? extends ContentDefinition> cr)
					throws WebException;

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
		Long clientInstanceId = AuthenticationManager
				.provideAuthenticatedClientInstanceId();
		if (clientInstanceId == null) {
			throw new PermissionsException();
		}
		return new TransformCollector().waitForTransforms(position,
				clientInstanceId);
	}

	private void sanitiseClrString(ClientLogRecord clr) {
		clr.setMessage(
				CommonUtils.nullToEmpty(clr.getMessage()).replace('\0', ' '));
	}

	protected <T> T defaultProjection(T t) {
		return GraphProjections.defaultProjections().project(t);
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

	@Override
	protected void onBeforeRequestDeserialized(String serializedRequest) {
		super.onBeforeRequestDeserialized(serializedRequest);
		looseContextDepth.set(LooseContext.depth());
		getThreadLocalResponse().setHeader("Cache-Control", "no-cache");
	}

	protected void setLogger(Logger logger) {
		this.logger = logger;
	}

	protected boolean waitForTransformsEnabled() {
		return false;
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
}
