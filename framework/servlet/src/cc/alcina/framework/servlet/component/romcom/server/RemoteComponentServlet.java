package cc.alcina.framework.servlet.component.romcom.server;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.servlet.servlet.AlcinaServlet;

/*
 * @formatter:off
 
web.xml configuration example:

	<servlet>
		<servlet-name>RemoteComponentServlet.Entity</servlet-name>
		<servlet-class>cc.alcina.framework.servlet.component.romcom.server.RemoteComponentServlet</servlet-class>
		<init-param>
			<param-name>componentClassName</param-name>
			<param-value>cc.alcina.framework.servlet.component.entity.EntityGraphView$Component</param-value>
		</init-param>
		<init-param>
			<param-name>featurePath</param-name>
			<param-value>/remote/entity</param-value>
		</init-param>
	</servlet>
	<servlet-mapping>
		<servlet-name>RemoteComponentServlet.Entity</servlet-name>
		<url-pattern>/remote/entity/*</url-pattern>
	</servlet-mapping>


 * @formatter:on
 */
public class RemoteComponentServlet extends AlcinaServlet {
	RemoteComponentHandler handler;

	private boolean requiresAdmin;

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		String componentClassName = config
				.getInitParameter("componentClassName");
		String featurePath = config.getInitParameter("featurePath");
		String requiresAdminStr = config.getInitParameter("requiresAdmin");
		this.requiresAdmin = requiresAdminStr == null
				|| Boolean.valueOf(requiresAdminStr);
		RemoteComponent component = Reflections.newInstance(componentClassName);
		handler = new RemoteComponentHandler(component, featurePath, false);
	}

	@Override
	protected void handleRequest(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		if (requiresAdmin) {
			if (!PermissionsManager.get().isAdmin()) {
				String message = "Administrator permissions required\n";
				writeTextResponse(response, message);
				throw new Exception(message);
			}
		}
		handler.handle(request, response);
	}
}
