package cc.alcina.framework.servlet.servlet.remote;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager.LoginState;
import cc.alcina.framework.common.client.logic.permissions.UserlandProvider;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.entity.KryoUtils;
import cc.alcina.framework.entity.MetricLogging;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.logic.EntityLayerUtils;
import cc.alcina.framework.entity.logic.permissions.ThreadedPermissionsManager;
import cc.alcina.framework.entity.persistence.AuthenticationPersistence;
import cc.alcina.framework.entity.persistence.CommonPersistenceProvider;
import cc.alcina.framework.entity.persistence.domain.DomainLinker;
import cc.alcina.framework.entity.persistence.domain.DomainStore;
import cc.alcina.framework.entity.persistence.metric.InternalMetrics;
import cc.alcina.framework.entity.persistence.metric.InternalMetrics.InternalMetricTypeAlcina;
import cc.alcina.framework.entity.persistence.mvcc.KryoSupport;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.entity.persistence.transform.TransformCommit;
import cc.alcina.framework.entity.persistence.transform.TransformPersisterInPersistenceContext;
import cc.alcina.framework.entity.transform.DomainTransformLayerWrapper;
import cc.alcina.framework.entity.transform.EntityLocatorMap;
import cc.alcina.framework.entity.transform.ThreadlocalTransformManager;
import cc.alcina.framework.entity.transform.TransformPersistenceToken;
import cc.alcina.framework.servlet.ThreadedPmClientInstanceResolverImpl;

public abstract class RemoteInvocationServlet extends HttpServlet {
	public static final String REMOTE_INVOCATION_PARAMETERS = "remoteInvocationParameters";

	private static final String CONTEXT_IN = RemoteInvocationServlet.class
			.getName() + ".CONTEXT_IN";

	public static boolean in() {
		return LooseContext.is(CONTEXT_IN);
	}

	protected void customiseContextBeforePayloadWrite() {
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse res)
			throws ServletException, IOException {
		try {
			LooseContext.pushWithBoolean(
					KryoUtils.CONTEXT_USE_COMPATIBLE_FIELD_SERIALIZER, false);
			LooseContext.setTrue(CONTEXT_IN);
			LooseContext.set(KryoUtils.CONTEXT_USE_UNSAFE_FIELD_SERIALIZER,
					true);
			LooseContext.setTrue(
					TransformPersisterInPersistenceContext.CONTEXT_NOT_REALLY_SERIALIZING_ON_THIS_VM);
			LooseContext.setTrue(KryoUtils.CONTEXT_BYPASS_POOL);
			LooseContext.setTrue(KryoSupport.CONTEXT_FORCE_ENTITY_SERIALIZER);
			Transaction.begin();
			maybeToReadonlyTransaction();
			LooseContext
					.setTrue(TransformCommit.CONTEXT_FORCE_COMMIT_AS_ONE_CHUNK);
			doPost0(req, res);
		} catch (Exception e) {
			System.out.println(
					String.format("RemoteInvocationServlet: user:%s - url: %s",
							PermissionsManager.get().getUserName(),
							req.getRequestURI()));
			if (e instanceof ServletException) {
				throw (ServletException) e;
			}
			throw new ServletException(e);
		} finally {
			ThreadlocalTransformManager.get().resetTltm(null);
			// ensure correct phase
			maybeToReadonlyTransaction();
			Transaction.end();
			LooseContext.pop();
		}
	}

	protected void doPost0(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		String encodedParams = request
				.getParameter(REMOTE_INVOCATION_PARAMETERS);
		RemoteInvocationParameters params = KryoUtils.deserializeFromBase64(
				encodedParams, RemoteInvocationParameters.class);
		String remoteAddress = request.getRemoteAddr();
		String permittedAddressPattern = Configuration.get(
				RemoteInvocationServlet.class,
				Ax.format("%s.permittedAddresses", params.api));
		if (!remoteAddress.matches(permittedAddressPattern)) {
			throw Ax.runtimeException(
					"Remote invocation access: address %s does not match permitted %s for API %s",
					remoteAddress, permittedAddressPattern, params.api);
		}
		boolean pushedUser = false;
		try {
			ClientInstance clientInstance = AuthenticationPersistence.get()
					.getClientInstance(params.clientInstanceId);
			if (clientInstance == null
					&& params.api.isAllowWithoutClientInstance()) {
				clientInstance = ClientInstance.self();
			} else {
				Preconditions.checkArgument(
						clientInstance.getAuth() == params.clientInstanceAuth);
			}
			boolean asRoot = UserlandProvider.get()
					.getSystemUser() == clientInstance
							.getAuthenticationSession().getUser();
			PermissionsManager.get().pushUser(
					clientInstance.getAuthenticationSession().getUser(),
					LoginState.LOGGED_IN, asRoot);
			pushedUser = true;
			PermissionsManager.get().setClientInstance(clientInstance);
			Object invocationTarget = getInvocationTarget(params);
			Class<? extends Object> targetClass = invocationTarget.getClass();
			Object[] args = params.args;
			String methodName = params.methodName;
			Method method = new SEUtilities.MethodFinder()
					.findMethod(targetClass, args, methodName);
			Object out = null;
			boolean transformMethod = method.getName()
					.equals("transformInPersistenceContext");
			boolean getUserByNameMethod = method.getName()
					.equals("getUserByName");
			String key = Ax.format("RemoteInvocation::%s::%s.%s",
					clientInstance.getId(), targetClass.getSimpleName(),
					methodName);
			try {
				if (!methodName.equals("callRpc")) {
					MetricLogging.get().start(key);
				}
				InternalMetrics.get().startTracker(request,
						() -> "remote-invocation:" + method.toString(),
						InternalMetricTypeAlcina.remote_invocation,
						Thread.currentThread().getName(), () -> true);
				if (transformMethod) {
					Transaction.endAndBeginNew();
					TransformPersistenceToken token = (TransformPersistenceToken) args[1];
					Integer highestPersistedRequestId = CommonPersistenceProvider
							.get().getCommonPersistence()
							.getHighestPersistedRequestIdForClientInstance(
									clientInstance.getId());
					/*
					 * no...at least, for consle -> webapp, this crushes
					 * console/root transforms
					 */
					// token.getRequest().setClientInstance(clientInstance);
					if (token.getRequest().getClientInstance() == null) {
						token.getRequest().setClientInstance(clientInstance);
					}
					EntityLocatorMap locatorMap = Registry
							.impl(TransformCommit.class)
							.getLocatorMapForClient(token.getRequest());
					token.setLocatorMap(locatorMap);
					token.getRequest().setRequestId(
							CommonUtils.iv(highestPersistedRequestId) + 1);
					ThreadedPermissionsManager tpm = ThreadedPermissionsManager
							.cast();
					if (params.asRoot && params.api.isAllowAsRoot()) {
						tpm.pushSystemUser();
						token.setIgnoreClientAuthMismatch(true);
					} else {
						tpm.pushUser(clientInstance.provideUser(),
								LoginState.LOGGED_IN);
					}
				}
				for (int idx = 0; idx < args.length; idx++) {
					Object arg = args[idx];
					if (arg != null && arg.getClass().getName()
							.equals("org.slf4j.impl.Log4jLoggerAdapter")) {
						args[idx] = null;
					}
				}
				try {
					LooseContext.pushWithBoolean(
							KryoUtils.CONTEXT_USE_UNSAFE_FIELD_SERIALIZER,
							false);
					LooseContext.set(
							ThreadedPmClientInstanceResolverImpl.CONTEXT_CLIENT_INSTANCE,
							clientInstance);
					params.context.forEach((k, v) -> {
						if (!k.matches(
								"cc.alcina.framework.entity.KryoUtils.*")) {
							LooseContext.set(k, v);
						}
					});
					out = method.invoke(invocationTarget, args);
				} finally {
					LooseContext.pop();
				}
			} catch (Exception e) {
				e.printStackTrace();
				if (e instanceof InvocationTargetException) {
					e = new Exception(
							"Invocation target exception = see server logs");
				}
				out = e;
			} finally {
				if (transformMethod) {
					PermissionsManager.get().popUser();
				}
				InternalMetrics.get().endTracker(request);
				if (!methodName.equals("callRpc")) {
					MetricLogging.get().end(key);
				}
			}
			Object result = out;
			ArrayList resultHolder = new ArrayList();
			resultHolder.add(out);
			if (transformMethod) {
				Preconditions.checkState(EntityLayerUtils.isTestServer());
				ThreadlocalTransformManager.get().resetTltm(null);
				DomainStore.writableStore().getPersistenceEvents().getQueue()
						.refreshPositions();
				if (resultHolder
						.get(0) instanceof DomainTransformLayerWrapper) {
					DomainTransformLayerWrapper wrapper = (DomainTransformLayerWrapper) resultHolder
							.get(0);
					wrapper.snapshotEntityLocatorMap();
				}
			}
			if (params.api.isLinkToDomain(method.getName())
					&& params.mayLinkToDomain) {
				resultHolder = DomainLinker.linkToDomain(resultHolder);
			}
			ArrayList f_resultHolder = resultHolder;
			try {
				LooseContext.push();
				customiseContextBeforePayloadWrite();
				byte[] outBytes = KryoUtils
						.serializeToByteArray(f_resultHolder);
				Io.Streams.copy(
						new ByteArrayInputStream(outBytes),
						response.getOutputStream());
			} finally {
				LooseContext.pop();
			}
			return;
		} catch (Exception e) {
			throw new ServletException(e);
		} finally {
			if (pushedUser) {
				PermissionsManager.get().popUser();
			}
		}
	}

	protected abstract Object getInvocationTarget(
			RemoteInvocationParameters params) throws Exception;

	protected void maybeToReadonlyTransaction() {
		/*
		 * By default, remote invocations are readonly
		 */
		Transaction.current().toReadonly();
	}
}
