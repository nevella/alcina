package cc.alcina.framework.servlet.component.romcom.server;

import javax.servlet.http.HttpServletRequest;

import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol;
import cc.alcina.framework.servlet.dom.Environment;
import cc.alcina.framework.servlet.dom.PathrefDom;
import cc.alcina.framework.servlet.dom.PathrefDom.Credentials;
import cc.alcina.framework.servlet.dom.RemoteUi;

public interface RemoteComponent {
	default RemoteComponentProtocol.Session createEnvironment(HttpServletRequest request) {
		Credentials credentials = Credentials.createUnique();
		RemoteUi ui = getUiInstance();
		Environment environment = PathrefDom.get().register(ui,
				credentials);
		RemoteComponentProtocol.Session session = new RemoteComponentProtocol.Session();
		session.id = credentials.id;
		session.auth = credentials.auth;
		session.url = request.getRequestURL().toString();
		session.componentClassName = ui.getClass().getName();
		return session;
	}

	String getPath();

	default RemoteUi getUiInstance() {
		return Reflections.newInstance(getUiType());
	}

	Class<? extends RemoteUi> getUiType();
}