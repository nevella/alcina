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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
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
import cc.alcina.framework.common.client.gwittir.validator.ServerValidator;
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
import cc.alcina.framework.common.client.logic.permissions.AnnotatedPermissible;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.permissions.Permissions;
import cc.alcina.framework.common.client.logic.permissions.PermissionsException;
import cc.alcina.framework.common.client.logic.permissions.ReadOnlyException;
import cc.alcina.framework.common.client.logic.permissions.WebMethod;
import cc.alcina.framework.common.client.logic.reflection.Permission;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.process.ProcessObservers;
import cc.alcina.framework.common.client.publication.ContentDefinition;
import cc.alcina.framework.common.client.publication.request.ContentRequestBase;
import cc.alcina.framework.common.client.publication.request.PublicationResult;
import cc.alcina.framework.common.client.remote.CommonRemoteService;
import cc.alcina.framework.common.client.remote.ReflectiveRemoteServiceAsync.ReflectiveRemoteServicePayload;
import cc.alcina.framework.common.client.remote.ReflectiveRemoteServiceHandler;
import cc.alcina.framework.common.client.remote.ReflectiveRpcRemoteService;
import cc.alcina.framework.common.client.remote.SearchRemoteService;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.persistence.AppPersistenceBase;
import cc.alcina.framework.entity.persistence.CommonPersistenceBase;
import cc.alcina.framework.entity.persistence.CommonPersistenceLocal;
import cc.alcina.framework.entity.persistence.CommonPersistenceProvider;
import cc.alcina.framework.entity.persistence.domain.DomainStore;
import cc.alcina.framework.entity.persistence.metric.InternalMetrics;
import cc.alcina.framework.entity.persistence.metric.InternalMetrics.InternalMetricTypeAlcina;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.entity.persistence.transform.TransformCommit;
import cc.alcina.framework.entity.projection.GraphProjection.GraphProjectionFieldFilter;
import cc.alcina.framework.entity.projection.GraphProjections;
import cc.alcina.framework.entity.util.JacksonJsonObjectSerializer;
import cc.alcina.framework.entity.util.MethodContext;
import cc.alcina.framework.gwt.client.gwittir.widget.BoundSuggestBox.BoundSuggestOracleRequest;
import cc.alcina.framework.gwt.client.gwittir.widget.BoundSuggestOracleResponseElement;
import cc.alcina.framework.gwt.client.logic.process.ProcessMetric;
import cc.alcina.framework.gwt.client.logic.process.ProcessMetric.Observer;
import cc.alcina.framework.gwt.client.logic.process.ProcessMetrics;
import cc.alcina.framework.gwt.client.rpc.AlcinaRpcRequestBuilder;
import cc.alcina.framework.gwt.client.rpc.OutOfBandMessage;
import cc.alcina.framework.gwt.client.rpc.OutOfBandMessage.ExceptionMessage;
import cc.alcina.framework.servlet.ServletLayerUtils;
import cc.alcina.framework.servlet.SessionProvider;
import cc.alcina.framework.servlet.authentication.AuthenticationManager;
import cc.alcina.framework.servlet.authentication.AuthenticationManager.ExpiredClientInstanceException;
import cc.alcina.framework.servlet.job.JobRegistry;
import cc.alcina.framework.servlet.misc.ReadonlySupportServletLayer;
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
public abstract class CommonRemoteServiceServlet extends RemoteServiceServlet
		implements CommonRemoteService, SearchRemoteService,
		ReflectiveRpcRemoteService, ReflectiveRemoteServiceHandler {
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

	public static final String CONTEXT_METRIC_OBSERVER = CommonRemoteServiceServlet.class
			.getName() + ".CONTEXT_METRIC_OBSERVER";

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

	protected void afterAlcinaServletContextInitialisation() {
		// for subclasses
	}

	@Override
	public String callRpc(String encodedRpcPayload) {
		try {
			ReflectiveRemoteServicePayload payload = ReflectiveSerializer
					.deserialize(encodedRpcPayload);
			ReflectiveRemoteServiceHandler handler = Registry.impl(
					ReflectiveRemoteServiceHandler.class,
					payload.getAsyncInterfaceClass());
			if (handler.getClass() == getClass()) {
				handler = this;
			}
			Class[] methodArgumentTypes = (Class[]) payload
					.getMethodArgumentTypes().toArray(
							new Class[payload.getMethodArgumentTypes().size()]);
			Object[] methodArguments = (Object[]) payload.getMethodArguments()
					.toArray(new Object[payload.getMethodArguments().size()]);
			String methodName = payload.getMethodName();
			Method method = handler.getClass().getMethod(methodName,
					methodArgumentTypes);
			method.setAccessible(true);
			WebMethod webMethod = method.getAnnotation(WebMethod.class);
			if (webMethod != null) {
				String checkResult = checkWebMethod(webMethod, method);
				if (checkResult != null) {
					throw new RuntimeException(checkResult);
				}
			}
			String key = Ax.format("callRpc::%s.%s",
					handler.getClass().getSimpleName(), method.getName());
			long start = System.currentTimeMillis();
			Object result = method.invoke(handler, methodArguments);
			String serialized = ReflectiveRemoteServiceHandler
					.serializeForClient(result);
			if (methodName
					.matches(Configuration.get("logRpcMetricMethodRegex"))) {
				logger.info("Metric - {} - {} ms - {} bytes", methodName,
						System.currentTimeMillis() - start,
						serialized.length());
			}
			return serialized;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	private void configureProcessObserver(HttpServletRequest threadLocalRequest,
			Object[] parameters, boolean start) {
		if (threadLocalRequest == null) {
			return;
		}
		String metricRpcId = threadLocalRequest
				.getHeader(ProcessMetrics.HEADER_RPC_METRIC_ID);
		if (metricRpcId == null) {
			return;
		}
		if (start) {
			String requestId = ProcessMetric.getContextName();
			if (Objects.equals(requestId, "callRpc")) {
				// minor performance hit, but consistent and only in metrics
				// context
				ReflectiveRemoteServicePayload rpcPayload = ReflectiveSerializer
						.deserialize((String) parameters[0]);
				requestId = rpcPayload.getMethodName();
			}
			requestId += "." + metricRpcId;
			ProcessMetric.setContextName(requestId);
			Observer observer = new ProcessMetric.Observer();
			observer.setSequenceId(metricRpcId);
			LooseContext.set(CONTEXT_METRIC_OBSERVER, observer);
			ProcessObservers.observe(observer, start);
		} else {
			Observer observer = LooseContext.get(CONTEXT_METRIC_OBSERVER);
			ProcessObservers.observe(observer, start);
			OutOfBandMessages.get().addMessage(observer);
		}
	}

	protected <T> T defaultProjection(T t) {
		return GraphProjections.defaultProjections().project(t);
	}

	protected String describeRpcRequest(RPCRequest rpcRequest, String msg) {
		msg += "Method: " + rpcRequest.getMethod().getName() + "\n";
		msg += "User: " + Permissions.get().getUserString() + "\n";
		msg += "Types: " + CommonUtils.joinWithNewlineTab(
				Arrays.asList(rpcRequest.getMethod().getParameters()));
		msg += "\nParameters: \n";
		Object[] parameters = rpcRequest.getParameters();
		// Check for the ObfuscateOnLog annotation and
		// get parameter indicies to obfuscate if present
		List<Integer> parametersToObfuscate = new ArrayList<>();
		try {
			Method rpcMethod = this.getClass().getMethod(
					rpcRequest.getMethod().getName(),
					rpcRequest.getMethod().getParameterTypes());
			ObfuscateOnLog obfuscationAnnotation = rpcMethod
					.getAnnotation(ObfuscateOnLog.class);
			if (obfuscationAnnotation != null) {
				parametersToObfuscate = Arrays
						.stream(obfuscationAnnotation
								.parameterIndiciesToRemove())
						.boxed().collect(Collectors.toList());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (rpcRequest.getMethod().getName().equals("transform")) {
		} else {
			for (int idx = 0; idx < parameters.length; idx++) {
				try {
					Object requestParamterValue = parameters[idx];
					String serializedParameter = null;
					// If the given parameter index is in the parameters to
					// obfuscate,
					// don't print it
					if (parametersToObfuscate.contains(idx)) {
						serializedParameter = "<obfuscated>";
					} else {
						Object projectedParameterValue = GraphProjections
								.defaultProjections()
								.fieldFilter(
										new ObfuscateParametersFieldFilter())
								.project(requestParamterValue);
						serializedParameter = new JacksonJsonObjectSerializer()
								.withIdRefs()
								.serializeNoThrow(projectedParameterValue);
					}
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
				/*
				 * don't reset - want to emit headers (and there's nothing salty
				 * in the response)
				 */
				// response.reset();
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

	@Override
	@WebMethod(
		readonlyPermitted = true,
		customPermission = @Permission(access = AccessLevel.EVERYONE))
	public String getJobLog(long jobId) {
		Job job = Job.byId(jobId).domain().ensurePopulated();
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
		try {
			exceptionToString = CommonUtils.nullToEmpty(exceptionToString)
					.replace('\0', ' ');
			LooseContext.pushWithKey(
					CommonPersistenceBase.CONTEXT_CLIENT_IP_ADDRESS,
					remoteAddr);
			if (exceptionToString.matches(
					".*ReflectiveSearchDefinitionSerializer.FlatTreeException.*")) {
				Ax.out("Client exception:\n%s", exceptionToString);
			}
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

	protected int nextTransformRequestId() {
		return Registry.impl(TransformCommit.class).nextTransformRequestId();
	}

	protected void onAfterAlcinaAuthentication(String methodName) {
	}

	@Override
	protected void onAfterResponseSerialized(String serializedResponse) {
		LooseContext.confirmDepth(looseContextDepth.get());
		super.onAfterResponseSerialized(serializedResponse);
	}

	@Override
	protected void onBeforeRequestDeserialized(String serializedRequest) {
		super.onBeforeRequestDeserialized(serializedRequest);
		looseContextDepth.set(LooseContext.depth());
		getThreadLocalResponse().setHeader("Cache-Control", "no-cache");
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
		List<Job> jobs = MethodContext.instance().withContextTrue(
				DomainStore.CONTEXT_DO_NOT_POPULATE_LAZY_PROPERTY_VALUES)
				.call(() -> request.getIds().stream().map(Job::byId)
						.filter(Objects::nonNull).collect(Collectors.toList()));
		JobTracker.Response response = new JobTracker.Response();
		response.setTrackers(jobs.stream().map(Job::asJobTracker)
				.peek(jt -> jt.setLog(null)).collect(Collectors.toList()));
		return response;
	}

	@Override
	public JobTracker pollJobStatus(String id, boolean cancel) {
		Job job0 = MethodContext.instance().withContextTrue(
				DomainStore.CONTEXT_DO_NOT_POPULATE_LAZY_PROPERTY_VALUES)
				.call(() -> {
					Job job = Job.byId(Long.parseLong(id));
					if (job == null) {
						return null;
					}
					if (cancel) {
						job.cancel();
						Transaction.commit();
					}
					return job;
				});
		if (job0 == null) {
			return null;
		} else {
			JobTracker tracker = job0.asJobTracker();
			tracker.setLog(null);
			return tracker;
		}
	}

	@Override
	public final /*
					 * Cannot be overridden - since it does some complex context
					 * arrangements. To change behaviour, add subclass hooks
					 * (e.g. afterAlcinaServletContextInitialisation)
					 */
	/*
	 * Does not create a LooseContext instance, relies on creation via
	 * alcinaServletContext.begin
	 */
	String processCall(String payload) throws SerializationException {
		long start = System.currentTimeMillis();
		RPCRequest rpcRequest = null;
		boolean alcinaServletContextInitCalled = false;
		AlcinaServletContext alcinaServletContext = null;
		HttpServletRequest threadLocalRequest = getThreadLocalRequest();
		int encodedLength = 0;
		String resolvedName = null;
		try {
			rpcRequest = RPC.decodeRequest(payload, this.getClass(), this);
			String suffix = getRpcHandlerThreadNameSuffix(rpcRequest);
			String name = rpcRequest.getMethod().getName();
			String threadName = Ax.format("gwt-rpc:%s:%s%s", name,
					callCounter.incrementAndGet(), suffix);
			alcinaServletContext = new AlcinaServletContext();
			Method method = null;
			try {
				method = this.getClass().getMethod(name,
						rpcRequest.getMethod().getParameterTypes());
				// bail early, since some early checks require a method
			} catch (Exception ex) {
				ex.printStackTrace();
				return RPC.encodeResponseForFailure(null, ex);
			}
			Map<String, Object> initialContext = new LinkedHashMap<>();
			WebMethod webMethod = method.getAnnotation(WebMethod.class);
			initialContext.put(
					AuthenticationManager.CONTEXT_ALLOW_EXPIRED_ANONYMOUS_AUTHENTICATION_SESSION,
					webMethod != null && webMethod
							.allowExpiredAnonymousAuthenticationSession());
			try {
				alcinaServletContextInitCalled = true;
				alcinaServletContext.begin(threadLocalRequest,
						getThreadLocalResponse(), threadName, initialContext);
			} finally {
				// if context initialisation fails (say an expired auth
				// session), these will still be required (and there'll be a
				// loose context)
				LooseContext.set(CONTEXT_THREAD_LOCAL_HTTP_REQUEST,
						threadLocalRequest);
				LooseContext.set(CONTEXT_THREAD_LOCAL_HTTP_RESPONSE,
						getThreadLocalResponse());
			}
			afterAlcinaServletContextInitialisation();
			ProcessMetric.setContextName(name);
			configureProcessObserver(threadLocalRequest,
					rpcRequest.getParameters(), true);
			ProcessMetric.publish(start, ProcessMetric.ServerType.rpc,
					payload.length());
			threadLocalRequest.setAttribute(
					CONTEXT_THREAD_LOCAL_HTTP_RESPONSE_HEADERS,
					new StringMap());
			if (rpcRequest
					.getSerializationPolicy() instanceof LegacySerializationPolicy) {
				throw new IncompatibleRemoteServiceException();
			}
			threadLocalRequest.setAttribute(THRD_LOCAL_RPC_RQ, rpcRequest);
			threadLocalRequest.setAttribute(THRD_LOCAL_RPC_PAYLOAD, payload);
			LooseContext.set(CommonPersistenceBase.CONTEXT_CLIENT_IP_ADDRESS,
					ServletLayerUtils
							.robustGetRemoteAddress(threadLocalRequest));
			LooseContext.set(CommonPersistenceBase.CONTEXT_CLIENT_INSTANCE_ID,
					AuthenticationManager
							.provideAuthenticatedClientInstanceId());
			RPCRequest f_rpcRequest = rpcRequest;
			onAfterAlcinaAuthentication(name);
			LooseContext.set(CONTEXT_RPC_USER_ID,
					Permissions.get().getUserId());
			InternalMetrics.get().startTracker(rpcRequest,
					() -> describeRpcRequest(f_rpcRequest, ""),
					InternalMetricTypeAlcina.client,
					Thread.currentThread().getName(), () -> true);
			TransformCommit.prepareHttpRequestCommitContext(
					CommonUtils.lv(Permissions.get().getClientInstanceId()),
					Permissions.get().getUserId(), ServletLayerUtils
							.robustGetRemoteAddress(threadLocalRequest));
			try {
				if (webMethod != null) {
					String checkResult = checkWebMethod(webMethod, method);
					if (checkResult != null) {
						return checkResult;
					}
				}
			} catch (SecurityException ex) {
				return RPC.encodeResponseForFailure(null, ex);
			}
			ProcessMetric.end(ProcessMetric.ServerType.rpc_prepare);
			String response = invokeAndEncodeResponse(rpcRequest);
			encodedLength = response.length();
			return response;
		} catch (IncompatibleRemoteServiceException ex) {
			getServletContext().log(
					"An IncompatibleRemoteServiceException was thrown while processing this call.",
					ex);
			return RPC.encodeResponseForFailure(null, ex);
		} catch (ExpiredClientInstanceException ex) {
			ex.printStackTrace();
			// empty response body. client will handle the headers
			return "";
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
			ProcessMetric.end(ProcessMetric.ServerType.rpc_process,
					encodedLength);
			try {
				HttpServletResponse threadLocalResponse = getThreadLocalResponse();
				if (threadLocalResponse != null) {
					configureProcessObserver(threadLocalRequest, null, false);
					OutOfBandMessages.get().addToResponse(threadLocalResponse);
					/*
					 * save the username for metric logging - the user will be
					 * cleared before the metrics are output
					 */
					threadLocalRequest.setAttribute(THRD_LOCAL_USER_NAME,
							Permissions.get().getUserName());
				}
				if (rpcRequest != null) {
					InternalMetrics.get().endTracker(rpcRequest);
				}
			} finally {
				if (alcinaServletContextInitCalled) {
					alcinaServletContext.end();
				}
			}
		}
	}

	String checkWebMethod(WebMethod webMethod, Method method)
			throws SerializationException {
		AnnotatedPermissible ap = new AnnotatedPermissible(
				webMethod.customPermission());
		if (!Permissions.isPermitted(ap)) {
			WebException wex = new WebException(
					"Action not permitted: " + method.toString());
			logRpcException(wex,
					LogMessageType.PERMISSIONS_EXCEPTION.toString());
			return RPC.encodeResponseForFailure(null, wex);
		}
		if (!webMethod.readonlyPermitted()) {
			try {
				AppPersistenceBase.checkNotReadOnly();
			} catch (ReadOnlyException e) {
				ExceptionMessage exceptionMessage = new OutOfBandMessage.ExceptionMessage();
				exceptionMessage.setMessageHtml(ReadonlySupportServletLayer
						.get().getNotPerformedBecauseReadonlyMessage());
				OutOfBandMessages.get().addMessage(exceptionMessage);
				throw e;
			}
		}
		return null;
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
	public Response suggest(BoundSuggestOracleRequest request) {
		try {
			LooseContext.set(DomainSearcher.CONTEXT_HINT, request.getHint());
			Class<? extends BoundSuggestOracleResponseElement> clazz = (Class<? extends BoundSuggestOracleResponseElement>) Class
					.forName(request.getTargetClassName());
			return Registry.impl(BoundSuggestOracleRequestHandler.class, clazz)
					.handleRequest(clazz, request, request.getHint());
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
			logRpcException(e);
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

	@Registration.Singleton
	public static class OutOfBandMessages {
		public static final String ATTR = OutOfBandMessages.class.getName()
				+ ".ATTR";

		public static final Topic<List<OutOfBandMessage>> topicAppendMessages = Topic
				.create();

		public static CommonRemoteServiceServlet.OutOfBandMessages get() {
			return Registry
					.impl(CommonRemoteServiceServlet.OutOfBandMessages.class);
		}

		protected Logger logger = LoggerFactory.getLogger(getClass());

		public void addMessage(OutOfBandMessage message) {
			HttpServletRequest threadLocalRequest = getContextThreadLocalRequest();
			if (threadLocalRequest == null) {
				logger.info("No request - not publishing {}", message);
				return;
			}
			ensureMessageList().add(message);
		}

		public void addToResponse(HttpServletResponse threadLocalResponse) {
			List<OutOfBandMessage> messageList = ensureMessageList();
			topicAppendMessages.publish(messageList);
			if (messageList.size() > 0) {
				threadLocalResponse.setHeader(
						AlcinaRpcRequestBuilder.RESPONSE_HEADER_OUT_OF_BAND_MESSAGES,
						TransformManager.serialize(messageList));
			}
		}

		private List<OutOfBandMessage> ensureMessageList() {
			List<OutOfBandMessage> result = (List<OutOfBandMessage>) getContextThreadLocalRequest()
					.getAttribute(ATTR);
			if (result == null) {
				result = new ArrayList<>();
				getContextThreadLocalRequest().setAttribute(ATTR, result);
			}
			return result;
		}
	}
}
