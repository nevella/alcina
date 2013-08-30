package cc.alcina.framework.servlet.servlet.dev;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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

import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager.LoginState;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceLocal;
import cc.alcina.framework.entity.logic.EntityLayerLocator;
import cc.alcina.framework.entity.projection.EntityUtils;
import cc.alcina.framework.gwt.client.util.Base64Utils;

public abstract class DevRemoterServlet extends HttpServlet {
	public static final String DEV_REMOTER_PARAMS = "devRemoterParams";

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
			System.out
					.println(String.format(
							"DevRemoterServlet info: user:%s - url: %s",
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
		byte[] bytes = Base64Utils.fromBase64(encodedParams);
		DevRemoterParams params = (DevRemoterParams) new ObjectInputStream(
				new ByteArrayInputStream(bytes)).readObject();
		CommonPersistenceLocal up = EntityLayerLocator.get()
				.commonPersistenceProvider().getCommonPersistence();
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
			try {
				System.out.format("DevRemoter - %s.%s\n", api.getClass()
						.getSimpleName(), method.getName());
				out = method.invoke(api, params.args);
			} catch (Exception e) {
				e.printStackTrace();
				out = e;
			}
			if (params.cleanEntities) {
				out = new EntityUtils().detachedClone(out);
			}
			new ObjectOutputStream(res.getOutputStream()).writeObject(out);
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
}
