package cc.alcina.framework.servlet.component.romcom.server;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.servlet.servlet.AlcinaServlet;

public class RemoteComponentServlet extends AlcinaServlet {
	RemoteComponentHandler handler;

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		String componentClassName = config
				.getInitParameter("componentClassName");
		String featurePath = config.getInitParameter("featurePath");
		RemoteComponent component = Reflections.newInstance(componentClassName);
		handler = new RemoteComponentHandler(component, featurePath);
	}

	@Override
	protected void handleRequest(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		handler.handle(request, response);
	}
}
