package cc.alcina.framework.servlet.dom;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import cc.alcina.extras.dev.component.remote.protocol.RemoteComponentRequest.Session;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;

/*
 * PathrefDom DOM(s) are a server-side dom pair (local, pathref) coupled to an
 * in-browser dom pair (local, remote) via rpc calls - the relationship is:
 * 
 * Server.NodeLocal <--> Server.NodePathRef <==> Client.NodeLocal <-->
 * Client.NodeRemote (Client.NodeRemote being the 'real' browser dom)
 * 
 * 'PathRef' because the server has no object refs to client nodes, instead
 * using node (x.y.z) paths to transmit references
 */
@Registration.Singleton
public class PathrefDom {
	public static PathrefDom get() {
		return Registry.impl(PathrefDom.class);
	}

	private ConcurrentMap<String, Environment> environments = new ConcurrentHashMap<>();

	public Environment getEnvironment(Session session,
			boolean validateClientInstanceUid) {
		Environment environment = environments.get(session.environmentId);
		if (environment == null && Ax.isTest()) {
			try {
				Thread.sleep(1000);
				return getEnvironment(session, validateClientInstanceUid);
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
		environment.validateSession(session, validateClientInstanceUid);
		return environment;
	}

	public Environment register(RemoteUi ui) {
		Environment environment = new Environment(ui);
		environments.put(environment.id, environment);
		return environment;
	}
}
