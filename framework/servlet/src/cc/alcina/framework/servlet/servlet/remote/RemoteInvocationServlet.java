package cc.alcina.framework.servlet.servlet.remote;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.EntityLocatorMap;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager.LoginState;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.KryoUtils;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.logic.permissions.ThreadedPermissionsManager;
import cc.alcina.framework.entity.persistence.AuthenticationPersistence;
import cc.alcina.framework.entity.persistence.CommonPersistenceProvider;
import cc.alcina.framework.entity.persistence.cache.DomainLinker;
import cc.alcina.framework.entity.persistence.cache.DomainStore;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.entity.persistence.transform.TransformCommit;
import cc.alcina.framework.entity.persistence.transform.TransformPersisterInPersistenceContext;
import cc.alcina.framework.entity.transform.ThreadlocalTransformManager;
import cc.alcina.framework.entity.transform.TransformPersistenceToken;
import cc.alcina.framework.servlet.ThreadedPmClientInstanceResolverImpl;

public abstract class RemoteInvocationServlet extends HttpServlet {
	public static final String REMOTE_INVOCATION_PARAMETERS = "remoteInvocationParameters";

	private Class normaliseClass(Class c1) {
		if (c1.isPrimitive()) {
			if (c1 == void.class) {
				return Void.class;
			}
			if (c1 == boolean.class) {
				return Boolean.class;
			}
			return Number.class;
		}
		if (Number.class.isAssignableFrom(c1)) {
			return Number.class;
		}
		return c1;
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse res)
			throws ServletException, IOException {
		try {
			LooseContext.pushWithBoolean(
					KryoUtils.CONTEXT_USE_COMPATIBLE_FIELD_SERIALIZER, false);
			LooseContext.set(KryoUtils.CONTEXT_USE_UNSAFE_FIELD_SERIALIZER,
					true);
			LooseContext.setTrue(
					TransformPersisterInPersistenceContext.CONTEXT_NOT_REALLY_SERIALIZING_ON_THIS_VM);
			Transaction.begin();
			Transaction.current().toNoActiveTransaction();
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
			Transaction.current().toNoActiveTransaction();
			Transaction.end();
			LooseContext.pop();
		}
	}

	protected void doPost0(HttpServletRequest req, HttpServletResponse res)
			throws Exception {
		String encodedParams = req.getParameter(REMOTE_INVOCATION_PARAMETERS);
		RemoteInvocationParameters params = KryoUtils.deserializeFromBase64(
				encodedParams, RemoteInvocationParameters.class);
		String remoteAddress = req.getRemoteAddr();
		String permittedAddressPattern = ResourceUtilities.get(
				RemoteInvocationServlet.class,
				Ax.format("%s.permittedAddresses", params.api));
		if (!remoteAddress.matches(permittedAddressPattern)) {
			throw Ax.runtimeException(
					"Remote invocation access: address %s does not match permitted %s for API %s",
					remoteAddress, permittedAddressPattern, params.api);
		}
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
			PermissionsManager.get().pushUser(
					clientInstance.getAuthenticationSession().getUser(),
					LoginState.LOGGED_IN);
			PermissionsManager.get().setClientInstance(clientInstance);
			Object api = null;
			api = getApi(params, api);
			List<Class> argTypes = new ArrayList<Class>();
			Method method = null;
			methodLoop: for (Method m : api.getClass().getMethods()) {
				if (m.getName().equals(params.methodName)
						&& params.args.length == m.getParameterTypes().length) {
					for (int i = 0; i < m.getParameterTypes().length; i++) {
						Class c1 = m.getParameterTypes()[i];
						Class c2 = params.args[i] == null ? void.class
								: params.args[i].getClass();
						if (!isAssignableRelaxed(c1, c2)) {
							continue methodLoop;
						}
					}
					method = m;
					break;
				}
			}
			Object out = null;
			boolean transformMethod = method.getName()
					.equals("transformInPersistenceContext");
			boolean getUserByNameMethod = method.getName()
					.equals("getUserByName");
			try {
				System.out.format("DevRemoter - %s.%s\n",
						api.getClass().getSimpleName(), method.getName());
				if (transformMethod) {
					// assume as root
					TransformPersistenceToken token = (TransformPersistenceToken) params.args[1];
					Integer highestPersistedRequestId = CommonPersistenceProvider
							.get().getCommonPersistence()
							.getHighestPersistedRequestIdForClientInstance(
									clientInstance.getId());
					token.getRequest().setClientInstance(clientInstance);
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
				for (int idx = 0; idx < params.args.length; idx++) {
					Object arg = params.args[idx];
					if (arg != null && arg.getClass().getName()
							.equals("org.slf4j.impl.Log4jLoggerAdapter")) {
						params.args[idx] = null;
					}
				}
				try {
					LooseContext.pushWithBoolean(
							KryoUtils.CONTEXT_USE_UNSAFE_FIELD_SERIALIZER,
							false);
					LooseContext.set(
							ThreadedPmClientInstanceResolverImpl.CONTEXT_CLIENT_INSTANCE,
							clientInstance);
					out = method.invoke(api, params.args);
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
			}
			Object result = out;
			ArrayList resultHolder = new ArrayList();
			resultHolder.add(out);
			if (transformMethod) {
				ThreadlocalTransformManager.get().resetTltm(null);
				resultHolder.add(ThreadlocalTransformManager.get()
						.getPostTransactionEntityResolver(
								DomainStore.writableStore()));
			}
			if (params.api.isLinkToDomain()) {
				resultHolder = DomainLinker.linkToDomain(resultHolder);
			}
			byte[] outBytes = KryoUtils.serializeToByteArray(resultHolder);
			ResourceUtilities.writeStreamToStream(
					new ByteArrayInputStream(outBytes), res.getOutputStream());
			return;
		} catch (Exception e) {
			throw new ServletException(e);
		} finally {
			PermissionsManager.get().popUser();
		}
	}

	protected abstract Object getApi(RemoteInvocationParameters params,
			Object api) throws ClassNotFoundException, IllegalAccessException,
			InvocationTargetException;

	protected boolean isAssignableRelaxed(Class c1, Class c2) {
		Class nc1 = normaliseClass(c1);
		Class nc2 = normaliseClass(c2);
		return nc1.isAssignableFrom(nc2)
				|| (nc2 == Void.class && !c1.isPrimitive());
	}
}
