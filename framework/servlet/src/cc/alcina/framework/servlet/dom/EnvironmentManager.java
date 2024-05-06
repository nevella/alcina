package cc.alcina.framework.servlet.dom;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.google.common.base.Preconditions;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;

import cc.alcina.framework.common.client.context.ContextProvider;
import cc.alcina.framework.common.client.flight.FlightEvent;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.process.ProcessObserver;
import cc.alcina.framework.common.client.process.ProcessObservers;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.Timeout;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.logic.EntityLayerObjects;
import cc.alcina.framework.entity.logic.EntityLayerUtils;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol;
import cc.alcina.framework.servlet.component.romcom.server.RemoteComponent;
import cc.alcina.framework.servlet.component.romcom.server.RemoteComponentEvent;

/*
 * The EnvironmentManager maintains mappings of credentials (per-browser-tab) to
 * Environment instances (essentially server-side client apps with an associated
 * DOM + trimmings). An Environment instance is essentially the OM in ROM - it's
 * remote from the POV of the client
 * 
 * The EnvironmentManager also maintains a list of named environment
 * <i>sources</i>
 */
@Registration.Singleton
public class EnvironmentManager {
	public static EnvironmentManager get() {
		return Registry.impl(EnvironmentManager.class);
	}

	private ConcurrentMap<String, Environment> environments = new ConcurrentHashMap<>();

	/*
	 * Since this is awaited, it's easier to manage synchronization manually
	 */
	private Map<String, EnvironmentSource> environmentSources = new LinkedHashMap<>();

	public static class EnvironmentSource {
		String path;

		public EnvironmentSource(String path) {
			this.path = path;
		}
	}

	public void registerSource(EnvironmentSource source) {
		synchronized (environmentSources) {
			environmentSources.put(source.path, source);
			environmentSources.notifyAll();
		}
	}

	public void deregisterSource(EnvironmentSource source) {
		synchronized (environmentSources) {
			environmentSources.remove(source.path);
			environmentSources.notifyAll();
		}
	}

	public EnvironmentManager() {
		// initialise the primary contexts for each server-hosted 'client app'
		Client.contextProvider = ContextProvider.createProvider(
				ctx -> ((RemoteUi) ctx).createClient(), null, null,
				Client.class, true);
		History.contextProvider = ContextProvider.createProvider(
				ctx -> new History(), History::init, null, History.class, true);
		Window.Location.contextProvider = ContextProvider.createProvider(
				ctx -> new Window.Location(), null, null, Window.Location.class,
				true);
		Window.Navigator.contextProvider = ContextProvider.createProvider(
				ctx -> new Window.Navigator(), null, null,
				Window.Navigator.class, true);
		flightRecordingEnabled = Configuration.is("flightRecordingEnabled");
		if (flightRecordingEnabled) {
			startFlightRecording();
		}
	}

	boolean flightRecordingEnabled;

	void startFlightRecording() {
		ProcessObservers.observe(new RemoteComponentEventObserver(), true);
	}

	class RemoteComponentEventObserver
			implements ProcessObserver<RemoteComponentEvent> {
		@Override
		public void topicPublished(RemoteComponentEvent message) {
			new FlightEvent(message).publish();
		}
	}

	public Environment getEnvironment(RemoteComponentProtocol.Session session) {
		return environments.get(session.id);
	}

	public boolean hasEnvironment(Class<? extends RemoteUi> uiType) {
		return environments.values().stream()
				.anyMatch(env -> env.ui.getClass() == uiType);
	}

	public boolean isSingletonEnvironmentInitialised() {
		Preconditions.checkState(environments.size() <= 1);
		return environments.size() == 1;
	}

	public Environment register(RemoteUi ui, Credentials credentials) {
		Environment environment = new Environment(ui, credentials);
		environments.put(credentials.id, environment);
		ui.setEnvironment(environment);
		return environment;
	}

	public Environment singletonEnvironment() {
		Preconditions.checkState(environments.size() == 1);
		return environments.values().iterator().next();
	}

	public static class Credentials {
		/*
		 * For single-client/environment jdks, this can be used to define the
		 * client uuid
		 */
		public static String uuid;

		public static Credentials createUnique() {
			ClientInstance serverAsClientInstance = EntityLayerObjects.get()
					.getServerAsClientInstance();
			return new Credentials(
					uuid != null ? uuid
							: Ax.format("%s-%s-%s",
									EntityLayerUtils.getLocalHostName(),
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

	public void deregister(RemoteUi remoteUi) {
		environments.remove(remoteUi.getEnvironment().credentials.id);
	}

	public void await(RemoteComponent component, String path)
			throws InterruptedException {
		String awaitPath = Ax.format("%s/%s", component.getPath(), path);
		Timeout timeout = new Timeout(60000);
		while (timeout.check(true)) {
			synchronized (environmentSources) {
				if (environmentSources.containsKey(awaitPath)) {
					return;
				}
				environmentSources.wait(timeout.remaining());
			}
		}
	}
}
