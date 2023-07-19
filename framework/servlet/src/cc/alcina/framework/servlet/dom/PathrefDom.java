package cc.alcina.framework.servlet.dom;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.google.common.base.Preconditions;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;

import cc.alcina.framework.common.client.context.ContextProvider;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.logic.EntityLayerObjects;
import cc.alcina.framework.entity.logic.EntityLayerUtils;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol;

/*
 * PathrefDom DOM(s) are a server-side dom pair (local, pathref) coupled to an
 * in-browser dom pair (local, remote) via rpc calls - the relationship is:
 *
 * Server.NodeLocal <--> Server.NodePathRef <==> Client.NodeLocal <-->
 * Client.NodeJso (Client.NodeJso being the 'real' browser dom)
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

	public PathrefDom() {
		// initialise the primary contexts for each server-hosted 'client app'
		Client.contextProvider = ContextProvider.createProvider(
				ctx -> ((RemoteUi) ctx).createClient(), null, null,
				Client.class, true);
		History.contextProvider = ContextProvider.createProvider(
				ctx -> new History(), History::init, null, History.class, true);
		Window.Location.contextProvider = ContextProvider.createProvider(
				ctx -> new Window.Location(), null, null, Window.Location.class,
				true);
	}

	public Environment getEnvironment(RemoteComponentProtocol.Session session) {
		return environments.get(session.id);
	}

	public boolean hasEnvironment(Class<? extends RemoteUi> uiType) {
		return environments.values().stream()
				.anyMatch(env -> env.ui.getClass() == uiType);
	}

	public Environment register(RemoteUi ui, Credentials credentials) {
		Environment environment = new Environment(ui, credentials);
		environments.put(credentials.id, environment);
		return environment;
	}

	public Environment singletonEnvironment() {
		Preconditions.checkState(environments.size() == 1);
		return environments.values().iterator().next();
	}

	public static class Credentials {
		public static Credentials createUnique() {
			ClientInstance serverAsClientInstance = EntityLayerObjects.get()
					.getServerAsClientInstance();
			return new Credentials(
					Ax.format("%s-%s-%s", EntityLayerUtils.getLocalHostName(),
							serverAsClientInstance == null ? 0
									: serverAsClientInstance.getId(),
							SEUtilities.generatePrettyUuid()),
					SEUtilities.generatePrettyUuid());
		}

		public final String id;

		public final String auth;

		Credentials(String id, String auth) {
			this.id = id;
			this.auth = auth;
		}
	}
}
