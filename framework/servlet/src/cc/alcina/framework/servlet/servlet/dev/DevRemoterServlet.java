package cc.alcina.framework.servlet.servlet.dev;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager.LoginState;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.KryoUtils;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.domaintransform.ThreadlocalTransformManager;
import cc.alcina.framework.entity.domaintransform.TransformPersistenceToken;
import cc.alcina.framework.entity.domaintransform.event.DomainTransformPersistenceEvent;
import cc.alcina.framework.entity.domaintransform.event.DomainTransformPersistenceListener;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceLocal;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceProvider;
import cc.alcina.framework.entity.entityaccess.cache.AlcinaMemCache;
import cc.alcina.framework.entity.logic.permissions.ThreadedPermissionsManager;
import cc.alcina.framework.entity.projection.EntityUtils;

public abstract class DevRemoterServlet extends HttpServlet {
	public static final String DEV_REMOTER_PARAMS = "devRemoterParams";

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
			if (!ResourceUtilities.getBoolean(DevRemoterServlet.class,
					"enabled")) {
				throw new Exception("DevRemoterServlet disabled");
			}
			if (ResourceUtilities.getBoolean(DevRemoterServlet.class,
					"restrictToLocalhost")) {
				String host = req.getRemoteAddr();
				NetworkInterface intf = NetworkInterface
						.getByInetAddress(InetAddress.getByName(host));
				if (intf == null) {
					throw new Exception(
							"DevRemoterServlet only enabled for local callers");
				}
			}
			doPost0(req, res);
		} catch (Exception e) {
			System.out.println(
					String.format("DevRemoterServlet info: user:%s - url: %s",
							PermissionsManager.get().getUserName(),
							req.getRequestURI()));
			if (e instanceof ServletException) {
				throw (ServletException) e;
			}
			throw new ServletException(e);
		}
	}

	protected void doPost0(HttpServletRequest req, HttpServletResponse res)
			throws Exception {
		String encodedParams = req.getParameter(DEV_REMOTER_PARAMS);
		DevRemoterParams params = KryoUtils.deserializeFromBase64(encodedParams,
				DevRemoterParams.class);
		CommonPersistenceLocal up = Registry
				.impl(CommonPersistenceProvider.class).getCommonPersistence();
		IUser user = up.getUserByName(params.username, true);
		try {
			PermissionsManager.get().pushUser(user, LoginState.LOGGED_IN);
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
					ClientInstance clientInstance = CommonPersistenceProvider
							.get().getCommonPersistence().getClientInstance(
									String.valueOf(params.clientInstanceId));
					Integer highestPersistedRequestId = CommonPersistenceProvider
							.get().getCommonPersistence()
							.getHighestPersistedRequestIdForClientInstance(
									clientInstance.getId());
					token.getRequest().setClientInstance(clientInstance);
					token.getRequest().setRequestId(
							CommonUtils.iv(highestPersistedRequestId) + 1);
					ThreadedPermissionsManager tpm = ThreadedPermissionsManager
							.cast();
					if (params.asRoot) {
						tpm.pushSystemUser();
						token.setIgnoreClientAuthMismatch(true);
					} else {
						tpm.pushUser(clientInstance.getUser(),
								LoginState.LOGGED_IN);
					}
					params.cleanEntities = true;
				}
				if (getUserByNameMethod) {
					params.cleanEntities = true;
				}
				out = method.invoke(api, params.args);
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
			if (params.cleanEntities) {
				out = new EntityUtils().detachedClone(out);
			}
			ArrayList resultHolder = new ArrayList();
			resultHolder.add(out);
			if (transformMethod) {
				ThreadlocalTransformManager.get().resetTltm(null);
				resultHolder.add(ThreadlocalTransformManager.get()
						.getPostTransactionEntityResolver());
				/**
				 * This caused double-processing of requests - and is
				 * unneccesary if both apps are linked by kafka transform
				 * publishing queue
				 */
				// Method mcmethod = AlcinaMemCache.class.getDeclaredMethod(
				// "postProcess",
				// new Class[] { DomainTransformPersistenceEvent.class });
				// mcmethod.setAccessible(true);
				// // create an "event" to publish in the queue
				// TransformPersistenceToken persistenceToken =
				// (TransformPersistenceToken) params.args[1];
				// DomainTransformLayerWrapper wrapper =
				// (DomainTransformLayerWrapper) params.args[2];
				// DomainTransformPersistenceEvent persistenceEvent = new
				// DomainTransformPersistenceEvent(
				// persistenceToken, wrapper, true);
				// mcmethod.invoke(AlcinaMemCache.get(),
				// new Object[] { persistenceEvent });
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

	protected abstract Object getApi(DevRemoterParams params, Object api)
			throws ClassNotFoundException, IllegalAccessException,
			InvocationTargetException;

	protected boolean isAssignableRelaxed(Class c1, Class c2) {
		Class nc1 = normaliseClass(c1);
		Class nc2 = normaliseClass(c2);
		return nc1.isAssignableFrom(nc2)
				|| (nc2 == Void.class && !c1.isPrimitive());
	}

	static class MemCachePersistenceRouterListener
			implements DomainTransformPersistenceListener {
		@Override
		public void onDomainTransformRequestPersistence(
				DomainTransformPersistenceEvent evt) {
			try {
				Method method = AlcinaMemCache.class.getDeclaredMethod(
						"postProcess",
						new Class[] { DomainTransformPersistenceEvent.class });
				method.setAccessible(true);
				method.invoke(AlcinaMemCache.get(), new Object[] { evt });
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
	}
}
