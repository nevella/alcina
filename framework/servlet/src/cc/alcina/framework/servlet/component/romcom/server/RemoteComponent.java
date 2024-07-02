package cc.alcina.framework.servlet.component.romcom.server;

import javax.servlet.http.HttpServletRequest;

import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.TimeConstants;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol;
import cc.alcina.framework.servlet.dom.Environment;
import cc.alcina.framework.servlet.dom.EnvironmentManager;
import cc.alcina.framework.servlet.dom.EnvironmentManager.Credentials;
import cc.alcina.framework.servlet.dom.RemoteUi;

public interface RemoteComponent {
	default RemoteComponentProtocol.Session
			createEnvironment(HttpServletRequest request) {
		Credentials credentials = Credentials.createUnique();
		RemoteUi ui = getUiInstance();
		RemoteComponentProtocol.Session session = new RemoteComponentProtocol.Session();
		session.id = credentials.id;
		session.auth = credentials.auth;
		session.url = request.getRequestURL().toString();
		session.remoteAddress = request.getRemoteAddr();
		session.startTime = System.currentTimeMillis();
		if (Ax.notBlank(request.getQueryString())) {
			session.url += "?" + request.getQueryString();
		}
		session.componentClassName = ui.getClass().getName();
		Environment environment = EnvironmentManager.get().register(ui,
				session);
		environment.setNonInteractionTimeout(
				getNonInteractionTimeout(environment));
		return session;
	}

	default long getNonInteractionTimeout(Environment environment) {
		return 5 * TimeConstants.ONE_MINUTE_MS;
	}

	String getPath();

	default RemoteUi getUiInstance() {
		return Reflections.newInstance(getUiType());
	}

	Class<? extends RemoteUi> getUiType();

	public static class EnvironmentPath extends Model.All {
		public String id;
	}
}