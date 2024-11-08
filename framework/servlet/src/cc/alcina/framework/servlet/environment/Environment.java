package cc.alcina.framework.servlet.environment;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.gwt.dom.client.AttachId;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Document.RemoteType;
import com.google.gwt.dom.client.DocumentAttachId;
import com.google.gwt.dom.client.DocumentAttachId.InvokeProxy;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.LocalDom;
import com.google.gwt.dom.client.NodeAttachId;
import com.google.gwt.dom.client.mutations.LocationMutation;
import com.google.gwt.dom.client.mutations.MutationRecord;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.common.client.logic.reflection.registry.EnvironmentRegistry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.TimeConstants;
import cc.alcina.framework.common.client.util.Timer;
import cc.alcina.framework.common.client.util.Url;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.gwt.headless.GWTBridgeHeadless;
import cc.alcina.framework.entity.gwt.headless.SchedulerFrame;
import cc.alcina.framework.entity.util.TimerJvm;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.dirndl.event.EventFrame;
import cc.alcina.framework.gwt.client.util.EventCollator;
import cc.alcina.framework.servlet.component.romcom.protocol.EventSystemMutation;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message.DomEventMessage;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message.ExceptionTransport;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message.Invoke.JsResponseType;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message.InvokeResponse;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message.Startup;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Session;
import cc.alcina.framework.servlet.component.romcom.server.RemoteComponentProtocolServer.MessageToken;
import cc.alcina.framework.servlet.component.romcom.server.RemoteComponentProtocolServer.RequestToken;

/*
 * Sync note - most methods will be called already synced on the environment
 * (since called downstream from a servlet call which looks up the environment
 * and syncs)
 *
 * An exception is the eventCollator action - it occurs on a timer thread so is
 * responsible for its own sync
 * 
 * Environment affecting code is not run on a single thread but it is (where
 * needed) synchronized. This will probably change to 'yes, single-threaded' -
 * on the ClientExecutionQueue loop/dispatch thread (update - yes, it's
 * single-threaded on the queue thread)
 * 
 * Other notes: dirndl event binding causes changes propagation to run in the
 * appropriate environment context via RemoteResolver
 * 
 * TODO - the syncing could (should) be simplified by only allowing
 * clientexecutionqueue to call runinclientframe (all messages run in the client
 * frame)
 * 
 * TOPICS Message loop and exception handling - see ClientExecutionQueue.loop
 * 
 * ------* ------* ------* ------* ------* ------
 * 
 * v2
 * 
 * Thread-safety - all external (non-Environment) access should go via the
 * Access object, which ensures thread-safety, mostly by dispatching on the ui
 * thread
 */
class Environment {
	static class TimerProvider implements Timer.Provider {
		TimerJvm.Provider exEnvironmentDelegate = new TimerJvm.Provider();

		TimerProvider() {
		}

		@Override
		public Timer getTimer(Runnable runnable) {
			if (Environment.has()) {
				return Environment.get().scheduler.createTimer(runnable);
			} else {
				return exEnvironmentDelegate.getTimer(runnable);
			}
		}
	}

	/*
	 * FIXME - tricky - what to do with timeouts here - does this interact with
	 * keep-alives, environmentmanager
	 */
	class InvokeProxyImpl implements DocumentAttachId.InvokeProxy {
		class ResponseHandler {
			InvokeResponse response;

			AsyncCallback callback;

			CountDownLatch latch;

			ResponseHandler(AsyncCallback<?> callback) {
				this.callback = callback;
				if (callback == null) {
					latch = new CountDownLatch(1);
				}
			}

			void handle(InvokeResponse response) {
				this.response = response;
				if (callback != null) {
					if (response.exception == null) {
						callback.onSuccess(response.response);
					} else {
						callback.onSuccess(response.exception);
					}
				} else {
					latch.countDown();
				}
			}
		}

		int invokeCounter = 0;

		Map<Integer, ResponseHandler> responseHandlers = new LinkedHashMap<>();

		@Override
		public void invoke(NodeAttachId node, String methodName,
				List<Class> argumentTypes, List<?> arguments,
				List<InvokeProxy.Flag> flags, AsyncCallback<?> callback) {
			invoke0(node, methodName, argumentTypes, arguments, flags, null,
					callback);
		}

		/*
		 * Unsurprisingly similar to the GWT devmode message send/await
		 */
		@Override
		public <T> T invokeSync(NodeAttachId node, String methodName,
				List<Class> argumentTypes, List<?> arguments,
				List<InvokeProxy.Flag> flags) {
			/*
			 * Special sauce - the invoke0 will create a handler, the handler's
			 * latch will block until the response message returns from the
			 * client .
			 * 
			 * Synchronous! 2006 is back!
			 */
			ResponseHandler handler = invoke0(node, methodName, argumentTypes,
					arguments, flags, null, null);
			queue.flush();
			return awaitResponse(node, methodName, handler);
		}

		<T> T awaitResponse(NodeAttachId node, String methodName,
				ResponseHandler handler) {
			boolean timedOut = false;
			boolean hadTimeOut = false;
			long start = System.currentTimeMillis();
			do {
				try {
					timedOut = !handler.latch.await(1, TimeUnit.SECONDS);
				} catch (Exception e) {
					Ax.simpleExceptionOut(e);
				}
				if (timedOut) {
					hadTimeOut = true;
					Ax.out("invokesync - [retry]");
				}
			} while (timedOut && !queue.finished && TimeConstants.within(start,
					30 * TimeConstants.ONE_SECOND_MS));
			if (timedOut) {
				throw new InvokeException("Timed out");
			} else {
				if (hadTimeOut) {
					Ax.out("invokesync - [retry.success]");
				}
				if (handler.response.exception == null) {
					return (T) handler.response.response;
				} else {
					String context = Ax.format(
							"invoke-remote - node %s - method %s", node.node(),
							methodName);
					throw new InvokeException(context,
							handler.response.exception);
				}
			}
		}

		@Override
		public <T> T invokeScript(Class clazz, String methodName,
				List<Class> argumentTypes, List<?> arguments) {
			String methodBody = new JsInvokeBuilder().build(clazz, methodName,
					argumentTypes, arguments);
			ResponseHandler handler = invoke0(null, null, null, null, List.of(),
					methodBody, null);
			// currently non-sync
			return null;
		}

		ResponseHandler invoke0(NodeAttachId node, String methodName,
				List<Class> argumentTypes, List<?> arguments, List<Flag> flags,
				String javascript, AsyncCallback<?> callback) {
			// check this is valid
			if (node != null && AttachId.forNode(node.node()).id == 0) {
				throw new IllegalStateException(Ax.format(
						"node %s is detached, cannot be remote-invoked",
						node.node().getNodeName()));
			}
			// always emit mutations before proxy invoke
			access().flush();
			ResponseHandler handler = new ResponseHandler(callback);
			Message.Invoke invoke = new Message.Invoke();
			invoke.path = node == null ? null : AttachId.forNode(node.node());
			invoke.targetName = node == null ? null : node.getNodeName();
			invoke.id = ++invokeCounter;
			invoke.methodName = methodName;
			invoke.argumentTypes = argumentTypes == null ? List.of()
					: argumentTypes;
			invoke.arguments = arguments == null ? List.of() : arguments;
			invoke.flags = flags == null ? List.of() : flags;
			invoke.sync = callback == null;
			if (javascript != null) {
				invoke.javascript = javascript;
				invoke.jsResponseType = JsResponseType._void;
			}
			responseHandlers.put(invoke.id, handler);
			queue.sendToClient(invoke);
			return handler;
		}

		void onInvokeResponse(InvokeResponse response) {
			ResponseHandler handler = responseHandlers.remove(response.id);
			handler.handle(response);
		}
	}

	static class InvokeException extends RuntimeException {
		InvokeException(String context, ExceptionTransport exception) {
			super(Ax.format("%s\n======================\n%s", context,
					exception.toExceptionString()));
		}

		InvokeException(String message) {
			super(message);
		}
	}

	class MutationProxyImpl implements DocumentAttachId.MutationProxy {
		@Override
		public void onLocationMutation(LocationMutation locationMutation) {
			runWithMutations(
					() -> mutations.locationMutation = locationMutation);
		}

		@Override
		public void onMutation(MutationRecord mutationRecord) {
			runWithMutations(() -> mutations.domMutations.add(mutationRecord));
		}

		@Override
		public void onSinkBitlessEvent(AttachId from, String eventTypeName) {
			addEventMutation(new EventSystemMutation(from, eventTypeName));
		}

		@Override
		public void onSinkEvents(AttachId from, int eventBits) {
			addEventMutation(new EventSystemMutation(from, eventBits));
		}

		// run the runnable in a mutation-processing context
		void runWithMutations(Runnable runnable) {
			if (mutations == null) {
				mutations = new Message.Mutations();
			}
			// if there's leakage of singletons, the mutationRecord may be
			// firing in the wrong environment's context
			if (Environment.get() != Environment.this) {
				throw new IllegalStateException(Ax.format(
						"Emitting a mutation in an invalid environment context. "
								+ "This is possibly caused by singleton leakage (a missing @Registration.EnvironmentSingleton) - mutation env: %s - context env: %s ",
						Environment.get(), Environment.this));
			}
			runnable.run();
			eventCollator.eventOccurred();
		}

		void addEventMutation(EventSystemMutation eventSystemMutation) {
			runWithMutations(() -> {
				Element elem = (Element) eventSystemMutation.nodeId.node();
				mutations.eventSystemMutations.add(eventSystemMutation);
			});
		}
	}

	class Noop implements Runnable {
		@Override
		public void run() {
			// used to trigger a timed schedule event within the main event pump
			// loop
		}
	}

	class CommandExecutorImpl implements SchedulerFrame.CommandExecutor {
		@Override
		public void execute(SchedulerFrame.Task task) {
			queue.invoke(task::executeCommand);
		}
	}

	/*
	 * Access *from* the execution thread (so DOM etc access permitted)
	 */
	class ClientExecutionThreadAccess {
		void beforeEnterContext() {
			ui.onBeforeEnterContext();
		}

		void enterContext() {
			Environment.this.enterContext();
		}

		void enterIteration() {
			Environment.this.enterIteration();
		}

		void exitIteration() {
			Environment.this.exitIteration();
		}

		void exitContext() {
			Environment.this.exitContext();
		}

		void enter(Runnable runnable) {
			Environment.this.enter(runnable);
		}
	}

	/*
	 * Provides thread-safe access to the Environment. Don't directly access the
	 * Environment is
	 */
	class Access {
		Access() {
		}

		public void handleRequest(RequestToken token) {
			queue.transportLayer.onReceivedToken(token);
		}

		String getConnectedClientUid() {
			return connectedClientUid;
		}

		void setNonInteractionTimeout(long nonInteractionTimeout) {
			Environment.this.nonInteractionTimeout.set(nonInteractionTimeout);
		}

		Session getSession() {
			return session;
		}

		void dispatchToClient(Message message) {
			queue.sendToClient(message);
		}

		void applyLocationMutation(LocationMutation locationMutation,
				boolean startup) {
			queue.invoke(() -> {
				if (startup) {
					Window.Location.init(locationMutation.protocol,
							locationMutation.host, locationMutation.port,
							locationMutation.path,
							locationMutation.queryString);
					Window.Navigator.init(
							locationMutation.navigator.appCodeName,
							locationMutation.navigator.appName,
							locationMutation.navigator.appVersion,
							locationMutation.navigator.platform,
							locationMutation.navigator.userAgent,
							locationMutation.navigator.cookieEnabled);
				}
				String token = locationMutation.hash.startsWith("#")
						? locationMutation.hash.substring(1)
						: locationMutation.hash;
				Ax.logEvent("Navigate %s:: -> %s", startup ? "(startup) " : "",
						token);
				History.newItem(token, !startup);
			});
		}

		void end(String reason) {
			logger.info("Stopping env [{}] :: {}", reason, session.id);
			EnvironmentManager.get().deregister(Environment.this);
			if (queue.finished) {
				access().invoke(() -> ui.end());
			}
			queue.stop();
		}

		void applyMutations(List<MutationRecord> mutations) {
			queue.invoke(() -> LocalDom.attachIdRepresentations()
					.applyMutations(mutations, false));
		}

		/*
		 * Called from off the CEQT
		 */
		void onInvokeResponse(InvokeResponse response) {
			Runnable runnable = () -> invokeProxy.onInvokeResponse(response);
			if (response.sync) {
				/*
				 * This will cause the response value to be applied to the
				 * handler, and the handler's latch unlocked (so the client
				 * execution thread will continue)
				 */
				runnable.run();
			} else {
				queue.invoke(runnable);
			}
		}

		void startup(MessageToken token, Startup message) {
			Preconditions.checkState(
					Objects.equals(session.id, Environment.this.session.id));
			Runnable startupRunnable = () -> Environment.this.startup(token,
					message);
			queue.start();
			queue.invoke(startupRunnable);
		}

		/**
		 * Executes the runnable within the environment's context. Called from
		 * outside the queuing system (so simply add to the execution queue)
		 * 
		 * Note that probably the queue should wrap the runnable (rather than
		 * here)
		 */
		void invoke(Runnable runnable) {
			queue.invoke(runnable::run);
		}

		RemoteUi getUi() {
			return ui;
		}

		String getUid() {
			return uid;
		}

		// currently only called by app reload - which makes sense, flush final
		// messages ('reloading') before reload
		void flush() {
			invoke(Environment.this::emitMutations);
		}

		String getSessionPath() {
			Url url = Url.parse(session.url);
			return url.queryParameters.get("path");
		}

		Date getLastPacketsReceived() {
			return queue.transportLayer.getLastEnvelopeReceived();
		}

		AtomicLong getNonInteractionTimeout() {
			return nonInteractionTimeout;
		}

		void onDomEventMessage(DomEventMessage message) {
			queue.invoke(() -> {
				document.attachIdRemote()
						.onRemoteUiContextReceived(message.eventContext);
				message.events.forEach(eventData -> LocalDom
						.attachIdRepresentations().applyEvent(eventData));
			});
		}
	}

	static final transient String CONTEXT_ENVIRONMENT = Environment.class
			.getName() + ".CONTEXT_ENVIRONMENT";

	static Environment get() {
		return LooseContext.get(Environment.CONTEXT_ENVIRONMENT);
	}

	static boolean has() {
		return LooseContext.has(Environment.CONTEXT_ENVIRONMENT);
	}

	/*
	 * Sent by the client to mark which environment it's communicating with.
	 * This allows the client to switch environments after say a dev rebuild
	 * (equal id/auth, unequal uid will force a refresh)
	 */
	private final String uid;

	private final RemoteUi ui;

	private ClientExecutionQueue queue;

	/*
	 * The uid of the most recent client to send a startup packet. All others
	 * will receive a 'reload' message on rpc calls
	 */
	private String connectedClientUid;

	private Logger logger = LoggerFactory.getLogger(getClass());

	private Document document;

	private MutationProxyImpl mutationProxy = new MutationProxyImpl();

	private InvokeProxyImpl invokeProxy = new InvokeProxyImpl();

	private Message.Mutations mutations = null;

	// FIXME - romcom - use an event pump rather than a timer
	private EventCollator<Object> eventCollator;

	private Client client;

	private History history;

	private Window.Location location;

	private Window.Navigator navigator;

	private Window.Resources windowResources;

	private EventFrame eventFrame;

	private final Session session;

	private SchedulerFrame scheduler;

	private EnvironmentRegistry environmentRegistry;

	private AtomicLong lastPacketsReceived = new AtomicLong();

	private AtomicLong nonInteractionTimeout = new AtomicLong();

	private Access access;

	private ClientExecutionThreadAccess clientExecutionThreadAccess;

	Environment(RemoteUi ui, Session session) {
		this.ui = ui;
		this.session = session;
		this.uid = SEUtilities.generatePrettyUuid();
		this.eventCollator = new EventCollator<Object>(5,
				this::flushNonClientMutations);
		this.access = new Access();
		this.clientExecutionThreadAccess = new ClientExecutionThreadAccess();
		queue = new ClientExecutionQueue(this);
	}

	void flushNonClientMutations() {
		access().invoke(() -> {
			emitMutations();
			access().flush();
		});
	}

	@Override
	public String toString() {
		return Ax.format("env::%s [%s/%s]", uid, session.id, session.auth);
	}

	ClientExecutionThreadAccess fromClientExecutionThreadAccess() {
		return clientExecutionThreadAccess;
	}

	void exitIteration() {
		ui.onExitIteration();
	}

	void enterIteration() {
		ui.onEnterIteration();
	}

	Access access() {
		return access;
	}

	private void enterContext() {
		LooseContext.set(CONTEXT_ENVIRONMENT, this);
		environmentRegistry = new EnvironmentRegistry();
		EnvironmentRegistry.enter(environmentRegistry);
		// the order - location, history, client, document - is necessary
		location = Window.Location.contextProvider.createFrame(null);
		navigator = Window.Navigator.contextProvider.createFrame(null);
		history = History.contextProvider.createFrame(null);
		windowResources = Window.Resources.contextProvider.createFrame(null);
		eventFrame = EventFrame.contextProvider.createFrame(null);
		History.addValueChangeHandler(this::onHistoryChange);
		client = Client.contextProvider.createFrame(ui);
		client.getPlaceController();
		client.setupPlaceMapping();
		scheduler = SchedulerFrame.contextProvider.createFrame(null);
		scheduler.commandExecutor = new CommandExecutorImpl();
		document = Document.contextProvider.createFrame(RemoteType.REF_ID);
		document.createDocumentElement("<html/>", true);
		document.implAccess().attachIdRemote().mutationProxy = mutationProxy;
		document.implAccess().attachIdRemote()
				.registerToRemoteInvokeProxy(invokeProxy);
		LocalDom.initalizeDetachedSync();
		GWTBridgeHeadless.inClient.set(true);
		EnvironmentRegistry.enter(environmentRegistry);
		Client.contextProvider.registerFrame(client);
		History.contextProvider.registerFrame(history);
		Window.Location.contextProvider.registerFrame(location);
		Window.Navigator.contextProvider.registerFrame(navigator);
		Window.Resources.contextProvider.registerFrame(windowResources);
		SchedulerFrame.contextProvider.registerFrame(scheduler);
		EventFrame.contextProvider.registerFrame(eventFrame);
		Document.contextProvider.registerFrame(document);
		GWTBridgeHeadless.inClient.set(true);
	}

	private void exitContext() {
		GWTBridgeHeadless.inClient.set(false);
	}

	private void validateSession(RemoteComponentProtocol.Session session,
			boolean validateClientInstanceUid) throws Exception {
		if (!Objects.equals(session.auth, session.auth)) {
			throw new RemoteComponentProtocol.InvalidAuthenticationException();
		}
		connectedClientUid = session.id;
		if (validateClientInstanceUid) {
			// FIXME - romcom - throw various exceptions if expired etc - see
			// package javadoc
			// if (!Objects.equals(session.clientInstanceUid,
			// connectedClientUid)) {
			// if (connectedClientUid == null) {
			// logger.warn(
			// "Call against new (dev) server with no connected client : {}",
			// session.clientInstanceUid);
			// } else {
			// logger.warn("Expired client (tab) : {}",
			// session.clientInstanceUid);
			// }
			// throw new InvalidClientUidException();
			// }
		}
	}

	private void initialiseSettings(String settings) {
		ui.initialiseSettings(settings);
	}

	private void startClient() {
		/*
		 * will cause mutations event (the initial render) to be queued in the
		 * queue.toClientQueue
		 */
		ui.init();
		ui.render();
		ui.addLifecycleHandlers();
	}

	/*
	 * Corresponds to com.google.gwt.core.client.impl.Impl.entry0(Object
	 * jsFunction, Object thisObj, Object args)
	 */
	private void enter(Runnable runnable) {
		try {
			ui.onEnterIteration();
			try {
				scheduler.pump(true);
				runnable.run();
			} finally {
				//
				scheduler.pump(false);
				/**
				 * If there are future timer events, this will queue a
				 * noop-runnable (which will flush the scheduler) at the
				 * earliest event
				 */
				scheduler.scheduleNextEntry(() -> access().invoke(new Noop()));
			}
		} catch (RuntimeException e) {
			e.printStackTrace();
			// TODO - allow exception catch (uncaught exception handler) here
			throw e;
		} finally {
			ui.onExitIteration();
		}
	}

	void emitMutations() {
		if (mutations != null) {
			Document.get().attachIdRemote().flushSinkEventsQueue();
			queue.sendToClient(mutations);
			mutations = null;
		}
	}

	private void onHistoryChange(ValueChangeEvent<String> event) {
		mutationProxy.onLocationMutation(
				Message.Mutations.ofLocation().locationMutation);
	}

	private void startup(MessageToken token, Startup message) {
		access().applyMutations(message.domMutations);
		access().applyLocationMutation(message.locationMutation, true);
		initialiseSettings(message.settings);
		startClient();
	}
}
