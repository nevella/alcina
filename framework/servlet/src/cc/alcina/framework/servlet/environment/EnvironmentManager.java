package cc.alcina.framework.servlet.environment;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;

import cc.alcina.framework.common.client.context.ContextProvider;
import cc.alcina.framework.common.client.flight.FlightEvent;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.common.client.process.ProcessObserver;
import cc.alcina.framework.common.client.process.ProcessObservers;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.NestedName;
import cc.alcina.framework.common.client.util.Timeout;
import cc.alcina.framework.common.client.util.Timer;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.logic.EntityLayerObjects;
import cc.alcina.framework.entity.logic.EntityLayerUtils;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.EventFrame;
import cc.alcina.framework.gwt.client.dirndl.model.Heading;
import cc.alcina.framework.gwt.client.dirndl.model.Link;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.InvalidClientException;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.InvalidClientException.Action;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Session;
import cc.alcina.framework.servlet.component.romcom.server.RemoteComponent;
import cc.alcina.framework.servlet.component.romcom.server.RemoteComponentEvent;
import cc.alcina.framework.servlet.component.romcom.server.RemoteComponentProtocolServer.MessageProcessingToken;
import cc.alcina.framework.servlet.component.romcom.server.RemoteComponentProtocolServer.RequestToken;
import cc.alcina.framework.servlet.dom.Feature_EnvironmentManager;

/**
 * <p>
 * The EnvironmentManager maintains mappings of credentials (per-browser-tab) to
 * Environment instances (essentially server-side client apps with an associated
 * DOM + trimmings). An Environment instance is essentially the OM in ROM - it's
 * remote from the POV of the client
 * 
 * <p>
 * The EnvironmentManager also maintains a list of named environment
 * <i>sources</i>
 * 
 * <p>
 * And, thirdly, the EnvironmentManager is the access point for reaping of
 * environments
 */
@Registration.Singleton
@Feature.Ref(Feature_EnvironmentManager.class)
public class EnvironmentManager {
	public static EnvironmentManager get() {
		return Registry.impl(EnvironmentManager.class);
	}

	/*
	 * Environments by id
	 */
	ConcurrentMap<String, Environment> environments = new ConcurrentHashMap<>();

	/*
	 * Since this is awaited, it's easier to manage synchronization manually
	 */
	private Map<String, EnvironmentSource> environmentSources = new LinkedHashMap<>();

	public static class EnvironmentSource {
		String path;

		public EnvironmentSource(String path, String href) {
			this.path = path;
			this.href = href;
		}

		String href;
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
		Window.Resources.contextProvider = ContextProvider.createProvider(
				ctx -> new Window.Resources(), null, null,
				Window.Resources.class, true);
		EventFrame.contextProvider = ContextProvider.createProvider(
				ctx -> new EventFrame(), null, null, EventFrame.class, true);
		flightRecordingEnabled = Configuration.is("flightRecordingEnabled");
		if (flightRecordingEnabled) {
			startFlightRecording();
		}
		new EnvironmentReaper().start();
	}

	boolean flightRecordingEnabled;

	void startFlightRecording() {
		ProcessObservers.observe(new RemoteComponentEventObserver(), true);
	}

	@Reflected
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
				.anyMatch(env -> env.access().getUi().getClass() == uiType);
	}

	public boolean isSingletonEnvironmentInitialised() {
		Preconditions.checkState(environments.size() <= 1);
		return environments.size() == 1;
	}

	public Environment register(RemoteUi ui, Session session,
			long nonInteractionTimeout) {
		Environment environment = new Environment(ui, session);
		environments.put(session.id, environment);
		ui.setEnvironment(environment);
		environment.access().setNonInteractionTimeout(nonInteractionTimeout);
		return environment;
	}

	Environment singletonEnvironment() {
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

	public void deregister(Environment environment) {
		environments.remove(environment.access().getSession().id);
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

	/*
	 * For servlet rendering
	 */
	public EnvironmentList getEnvironmentList() {
		return new EnvironmentList();
	}

	@Directed
	public class EnvironmentList extends Model.All {
		class Entry extends Model.All {
			Link id;

			Entry(EnvironmentSource env) {
				id = new Link().withText(env.path).withHref(env.href);
			}
		}

		Heading heading = new Heading("Environment list");

		@Directed.Wrap("entries")
		List<Entry> entries;

		EnvironmentList() {
			entries = environmentSources.values().stream().map(Entry::new)
					.collect(Collectors.toList());
		}
	}

	public void handleMessage(MessageProcessingToken token) throws Exception {
		/*
		 * ROMCOM - messages are offered to the CEQ in order (off CEQ thread) -
		 * it can either process em (sync) or add to queue (async)
		 * 
		 * message return to client is 'send message' - with some debouncing
		 */
		// RemoteComponentRequest request = token.request;
		// Environment env = getEnvironment(request.session);
		// if (env == null) {
		// throw buildInvalidClientException(
		// request.session.componentClassName);
		// }
		// MessageHandlerServer messageHandler = Registry.impl(
		// MessageHandlerServer.class, request.protocolMessage.getClass());
		// token.messageHandler = messageHandler;
		// /*
		// * tmp = this will all be queue calls
		// */
		// // http thread
		// messageHandler.onBeforeMessageHandled(request.protocolMessage);
		// // unless the message is a sync response, on the env thread
		// env.access().handleFromClientMessage(token);
		// // http thread
		// messageHandler.onAfterMessageHandled(request.protocolMessage);
	}

	InvalidClientException
			buildInvalidClientException(String componentClassName) {
		Class<? extends RemoteUi> uiType = Reflections
				.forName(componentClassName);
		boolean singleInstance = RemoteUi.SingleInstance.class
				.isAssignableFrom(uiType);
		boolean existingInstance = singleInstance
				&& EnvironmentManager.get().hasEnvironment(uiType);
		String message = null;
		InvalidClientException.Action action = Action.REFRESH;
		if (existingInstance) {
			action = Action.EXPIRED;
			message = "This component client (tab) has ben superseded "
					+ "by a newer access to this component. \n\nPlease use the newer client, "
					+ "or refresh to switch rendering to this client";
		}
		return new InvalidClientException(message, action,
				NestedName.get(uiType));
	}

	public static void registerEnvironmentSensitiveTimerProvider() {
		Registry.register().singleton(Timer.Provider.class,
				new Environment.TimerProvider());
	}

	public void invokeInSingletonEnvironment(Runnable runnable) {
		singletonEnvironment().access().invoke(runnable);
	}

	public void acceptRequest(RequestToken token) throws Exception {
		Environment env = getEnvironment(token.request.session);
		if (env == null) {
			LoggerFactory.getLogger(getClass()).info(
					"Invalid client session :: {}", token.request.session);
			throw buildInvalidClientException(
					token.request.session.componentClassName);
		}
		env.access().handleRequest(token);
	}

	public class Debug {
		public void triggerDebugRomcomServerException() {
			singletonEnvironment().access()
					.dispatchDebugRomcomProcessingException();
		}
	}

	public Debug debug() {
		return new Debug();
	}
}
