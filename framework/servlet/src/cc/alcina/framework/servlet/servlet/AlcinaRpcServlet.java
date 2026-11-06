package cc.alcina.framework.servlet.servlet;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.csobjects.LogMessageType;
import cc.alcina.framework.common.client.csobjects.WebException;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.permissions.AnnotatedPermissible;
import cc.alcina.framework.common.client.logic.permissions.Permissions;
import cc.alcina.framework.common.client.logic.permissions.PermissionsException;
import cc.alcina.framework.common.client.logic.permissions.ReadOnlyException;
import cc.alcina.framework.common.client.logic.permissions.WebMethod;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.remote.AlcinaRpc;
import cc.alcina.framework.common.client.remote.AlcinaRpc.Request;
import cc.alcina.framework.common.client.remote.ReflectiveRemoteServiceHandler;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.persistence.AppPersistenceBase;
import cc.alcina.framework.entity.persistence.CommonPersistenceBase;
import cc.alcina.framework.entity.persistence.CommonPersistenceLocal;
import cc.alcina.framework.entity.persistence.CommonPersistenceProvider;
import cc.alcina.framework.entity.persistence.transform.TransformCommit;
import cc.alcina.framework.gwt.client.rpc.AlcinaRpcRequestBuilder;
import cc.alcina.framework.gwt.client.rpc.OutOfBandMessage;
import cc.alcina.framework.gwt.client.rpc.OutOfBandMessage.ExceptionMessage;
import cc.alcina.framework.servlet.ServletLayerUtils;
import cc.alcina.framework.servlet.authentication.AuthenticationManager;
import cc.alcina.framework.servlet.misc.ReadonlySupportServletLayer;

/*
 * TODO - common service inherits from this, rework rpc,
 */
public abstract class AlcinaRpcServlet extends AlcinaServlet {
	public static LooseContext.Key<HttpServletRequest> CONTEXT_REQUEST = LooseContext
			.key(AlcinaRpcServlet.class, "CONTEXT_REQUEST");

	public static LooseContext.Key<HttpServletResponse> CONTEXT_RESPONSE = LooseContext
			.key(AlcinaRpcServlet.class, "CONTEXT_RESPONSE");

	static final String UA_NULL_SERVER = "null/server";

	AtomicInteger rpcExceptionLogCounter = new AtomicInteger();

	@Override
	protected void handleRequest(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		if (!Objects.equals(request.getMethod(), "POST")) {
			throw new IllegalArgumentException(
					Ax.format("Illegal method: %s", request.getMethod()));
		}
		try {
			LooseContext.push();
			CONTEXT_REQUEST.set(request);
			CONTEXT_RESPONSE.set(response);
			String rpcPayload = Io.read().fromStream(request.getInputStream())
					.asString();
			String responsePayload = callRpc(rpcPayload);
			Io.write().string(responsePayload)
					.toStream(response.getOutputStream());
		} finally {
			LooseContext.pop();
		}
	}

	public String callRpc(String encodedRpcPayload) {
		AlcinaRpc.Response response = new AlcinaRpc.Response();
		AlcinaRpc.Request rpcRequest = null;
		try {
			long preDeserializeInstant = System.currentTimeMillis();
			rpcRequest = ReflectiveSerializer.deserialize(encodedRpcPayload);
			ReflectiveRemoteServiceHandler handler = Registry.impl(
					ReflectiveRemoteServiceHandler.class,
					rpcRequest.getAsyncInterfaceClass());
			if (handler.getClass() == getClass()) {
				handler = (ReflectiveRemoteServiceHandler) this;
			}
			Class[] methodArgumentTypes = (Class[]) rpcRequest
					.getMethodArgumentTypes().toArray(new Class[rpcRequest
							.getMethodArgumentTypes().size()]);
			Object[] methodArguments = (Object[]) rpcRequest
					.getMethodArguments().toArray(
							new Object[rpcRequest.getMethodArguments().size()]);
			String methodName = rpcRequest.getMethodName();
			Method method = handler.getClass().getMethod(methodName,
					methodArgumentTypes);
			method.setAccessible(true);
			WebMethod webMethod = method.getAnnotation(WebMethod.class);
			checkWebMethod(webMethod, method);
			String key = Ax.format("callRpc::%s.%s",
					handler.getClass().getSimpleName(), method.getName());
			String suffix = webMethod.rpcHandlerThreadNameSuffix();
			String threadName = Ax.format("gwt-rpc:%s:%s%s", methodName,
					callCounter.incrementAndGet(), suffix);
			long start = System.currentTimeMillis();
			CommonPersistenceBase.CONTEXT_CLIENT_IP_ADDRESS
					.set(ServletLayerUtils.robustGetRemoteAddress(
							CONTEXT_REQUEST.getTyped()));
			CommonPersistenceBase.CONTEXT_CLIENT_INSTANCE_ID
					.set(AuthenticationManager
							.provideAuthenticatedClientInstanceId());
			TransformCommit.prepareHttpRequestCommitContext(
					CommonUtils.lv(Permissions.get().getClientInstanceId()),
					Permissions.get().getUserId(),
					ServletLayerUtils.robustGetRemoteAddress(
							CONTEXT_REQUEST.getTyped()));
			response.payload = method.invoke(handler, methodArguments);
			response.success = true;
		} catch (Exception e) {
			Throwable throwable = SEUtilities
					.unwrapInvocationTargetException(e);
			throwable.printStackTrace();
			if (rpcRequest != null) {
				logRpcException(throwable, rpcRequest);
			}
			response.payload = new AlcinaRpc.RpcException(throwable);
		}
		return ReflectiveRemoteServiceHandler.serializeForClient(response);
	}

	protected void logRpcException(Throwable throwable, Request rpcRequest) {
		String exceptionType = LogMessageType.RPC_EXCEPTION.toString();
		if (rpcExceptionLogCounter.incrementAndGet() > 10000) {
			Ax.err("Not logging rpc exception %s : %s - too many exceptions",
					exceptionType,
					CommonUtils.toSimpleExceptionMessage(throwable));
			return;
		}
		AlcinaServlet.topicApplicationThrowables().publish(throwable);
		CommonPersistenceBase.CONTEXT_CLIENT_IP_ADDRESS.runWithValue(() -> {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			throwable.printStackTrace(pw);
			String msg = "RPC exception:\n";
			if (rpcRequest != null) {
				msg = rpcRequest.toDebugString();
			}
			msg += "\nStacktrace:\t " + sw.toString();
			Ax.err(msg);
			CommonPersistenceLocal cpl = Registry
					.impl(CommonPersistenceProvider.class)
					.getCommonPersistence();
			cpl.log(msg, exceptionType);
		}, getRemoteAddress());
	}

	public static String getUserAgent(HttpServletRequest rq) {
		return rq == null ? UA_NULL_SERVER : rq.getHeader("User-Agent");
	}

	protected String getRemoteAddress() {
		return CONTEXT_REQUEST.unset() ? null
				: CONTEXT_REQUEST.getTyped().getRemoteAddr();
	}

	void checkWebMethod(WebMethod webMethod, Method method) throws Exception {
		if (webMethod == null) {
			throw new PermissionsException(
					Ax.format("No WebMethod annotation for %s - %s",
							method.getName(), method));
		}
		AnnotatedPermissible ap = new AnnotatedPermissible(
				webMethod.customPermission());
		if (!Permissions.isPermitted(ap)) {
			WebException wex = new WebException(
					"Action not permitted: " + method.toString());
			throw wex;
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
	}

	AtomicInteger callCounter = new AtomicInteger(0);

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
			HttpServletRequest threadLocalRequest = CONTEXT_REQUEST.getTyped();
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
			List<OutOfBandMessage> result = (List<OutOfBandMessage>) CONTEXT_REQUEST
					.getTyped().getAttribute(ATTR);
			if (result == null) {
				result = new ArrayList<>();
				CONTEXT_REQUEST.getTyped().setAttribute(ATTR, result);
			}
			return result;
		}
	}
}
