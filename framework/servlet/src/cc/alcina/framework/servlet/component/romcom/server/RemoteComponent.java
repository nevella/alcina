package cc.alcina.framework.servlet.component.romcom.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.service.InstanceOracle;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.TimeConstants;
import cc.alcina.framework.entity.persistence.domain.DomainStore;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol;
import cc.alcina.framework.servlet.environment.EnvironmentManager;
import cc.alcina.framework.servlet.environment.EnvironmentManager.Credentials;
import cc.alcina.framework.servlet.environment.RemoteUi;
import cc.alcina.framework.servlet.servlet.AlcinaServletContext;

public interface RemoteComponent {
	default boolean isUseContextIdentity() {
		return false;
	}

	default RemoteComponentProtocol.Session createEnvironment(
			HttpServletRequest request, HttpServletResponse response) {
		if (isUseContextIdentity()) {
			InstanceOracle.query(DomainStore.class).await();
			AlcinaServletContext alcinaContext = new AlcinaServletContext();
			try {
				alcinaContext.begin(request, response,
						Thread.currentThread().getName());
				return createEnvironmentAuthenticated(request);
			} finally {
				alcinaContext.end();
			}
		} else {
			try {
				LooseContext.push();
				return createEnvironmentAuthenticated(request);
			} finally {
				LooseContext.pop();
			}
		}
	}

	default RemoteComponentProtocol.Session
			createEnvironmentAuthenticated(HttpServletRequest request) {
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
		EnvironmentManager.get().register(ui, session,
				getNonInteractionTimeout(session));
		return session;
	}

	default long
			getNonInteractionTimeout(RemoteComponentProtocol.Session session) {
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