package cc.alcina.framework.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;

@RegistryLocation(registryPoint = SessionProvider.class, implementationType = ImplementationType.SINGLETON)
public class SessionProvider {
	public HttpSession getSession(HttpServletRequest request,
			HttpServletResponse response) {
		return request == null ? null : request.getSession();
	}
}
