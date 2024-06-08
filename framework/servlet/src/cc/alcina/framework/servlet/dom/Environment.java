package cc.alcina.framework.servlet.dom;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Document.RemoteType;
import com.google.gwt.dom.client.DocumentPathref;
import com.google.gwt.dom.client.DocumentPathref.InvokeProxy;
import com.google.gwt.dom.client.DomEventData;
import com.google.gwt.dom.client.LocalDom;
import com.google.gwt.dom.client.NodePathref;
import com.google.gwt.dom.client.Pathref;
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
import cc.alcina.framework.entity.gwt.headless.SchedulerFrame;
import cc.alcina.framework.entity.util.TimerJvm;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.dirndl.event.EventFrame;
import cc.alcina.framework.gwt.client.util.EventCollator;
import cc.alcina.framework.servlet.component.romcom.protocol.EventSystemMutation;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message.ExceptionTransport;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message.InvokeResponse;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Session;
import cc.alcina.framework.servlet.component.romcom.server.RemoteComponentProtocolServer.MessageHandlingToken;

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
 */
public class Environment {
	public static class TimerProvider implements Timer.Provider {
		TimerJvm.Provider exEnvironmentDelegate = new TimerJvm.Provider();

		public TimerProvider() {
		}

		@Override
		public Timer getTimer(Runnable runnable) {
			if (has()) {
				return get().scheduler.createTimer(runnable);
			} else {
				return exEnvironmentDelegate.getTimer(runnable);
			}
		}
	}

	/*
	 * FIXME - tricky - what to do with timeouts here - does this interact with
	 * keep-alives, environmentmanager
	 */
	class InvokeProxyImpl implements DocumentPathref.InvokeProxy {
		int invokeCounter = 0;

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

			public void handle(InvokeResponse response) {
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

		Map<Integer, ResponseHandler> responseHandlers = new LinkedHashMap<>();

		@Override
		public void invoke(NodePathref node, String methodName,
				List<Class> argumentTypes, List<?> arguments,
				List<InvokeProxy.Flag> flags, AsyncCallback<?> callback) {
			invoke0(node, methodName, argumentTypes, arguments, flags,
					callback);
		}

		ResponseHandler invoke0(NodePathref node, String methodName,
				List<Class> argumentTypes, List<?> arguments, List<Flag> flags,
				AsyncCallback<?> callback) {
			// always emit mutations before proxy invoke
			emitMutations();
			ResponseHandler handler = new ResponseHandler(callback);
			runInClientFrame(() -> {
				Message.Invoke invoke = new Message.Invoke();
				invoke.path = node == null ? null
						: Pathref.forNode(node.node());
				invoke.id = ++invokeCounter;
				invoke.methodName = methodName;
				invoke.argumentTypes = argumentTypes == null ? List.of()
						: argumentTypes;
				invoke.arguments = arguments == null ? List.of() : arguments;
				invoke.flags = flags == null ? List.of() : flags;
				invoke.sync = callback == null;
				responseHandlers.put(invoke.id, handler);
				queue.send(invoke);
			});
			return handler;
		}

		public void onInvokeResponse(InvokeResponse response) {
			ResponseHandler handler = responseHandlers.remove(response.id);
			handler.handle(response);
		}

		/*
		 * Unsurprisingly similar to the GWT devmode message send/await
		 */
		@Override
		public <T> T invokeSync(NodePathref node, String methodName,
				List<Class> argumentTypes, List<?> arguments,
				List<InvokeProxy.Flag> flags) {
			ResponseHandler handler = invoke0(node, methodName, argumentTypes,
					arguments, flags, null);
			if (!clientStarted) {
				return null;
			}
			/*
			 * Special sauce - the queue will return the client's http call with
			 * an 'invoke' message, and await the invokeResponse. Synchronous!
			 * 2006 is back!
			 */
			queue.onInvokedSync();
			boolean timedOut = false;
			long start = System.currentTimeMillis();
			do {
				try {
					timedOut = !handler.latch.await(1, TimeUnit.SECONDS);
				} catch (Exception e) {
					Ax.simpleExceptionOut(e);
				}
				if (timedOut) {
					Ax.out("invokesync - timedout");
				}
			} while (timedOut && TimeConstants.within(start,
					30 * TimeConstants.ONE_SECOND_MS));
			if (handler.response.exception == null) {
				return (T) handler.response.response;
			} else {
				throw new InvokeException(handler.response.exception);
			}
		}
	}

	static class InvokeException extends RuntimeException {
		InvokeException(ExceptionTransport exception) {
			super(exception.toExceptionString());
		}

		InvokeException(String message) {
			super(message);
		}
	}

	class MutationProxyImpl implements DocumentPathref.MutationProxy {
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
		public void onSinkBitlessEvent(Pathref from, String eventTypeName) {
			runWithMutations(() -> mutations.eventMutations
					.add(new EventSystemMutation(from, eventTypeName)));
		}

		@Override
		public void onSinkEvents(Pathref from, int eventBits) {
			runWithMutations(() -> mutations.eventMutations
					.add(new EventSystemMutation(from, eventBits)));
		}

		// run the runnable in a mutation-processing context
		void runWithMutations(Runnable runnable) {
			if (mutations == null) {
				mutations = new Message.Mutations();
			}
			runnable.run();
			eventCollator.eventOccurred();
		}
	}

	class Noop implements Runnable {
		@Override
		public void run() {
			// used to trigger a timed schedule event within the main event pump
			// loop
		}
	}

	private static final transient String CONTEXT_ENVIRONMENT = Environment.class
			.getName() + ".CONTEXT_ENVIRONMENT";

	public static Environment get() {
		return LooseContext.get(CONTEXT_ENVIRONMENT);
	}

	static boolean has() {
		return LooseContext.has(CONTEXT_ENVIRONMENT);
	}

	/*
	 * Sent by the client to mark which environment it's communicating with.
	 * This allows the client to switch environments after say a dev rebuild
	 * (equal id/auth, unequal uid will force a refresh)
	 */
	public final String uid;

	public final RemoteUi ui;

	ClientExecutionQueue queue;

	/*
	 * The uid of the most recent client to send a startup packet. All others
	 * will receive a 'reload' message on rpc calls
	 */
	String connectedClientUid;

	Logger logger = LoggerFactory.getLogger(getClass());

	Document document;

	MutationProxyImpl mutationProxy = new MutationProxyImpl();

	InvokeProxyImpl invokeProxy = new InvokeProxyImpl();

	Message.Mutations mutations = null;

	// FIXME - romcom - use an event pump rather than a timer
	EventCollator<Object> eventCollator;

	Client client;

	History history;

	Window.Location location;

	Window.Navigator navigator;

	Window.Resources windowResources;

	EventFrame eventFrame;

	final Session session;

	SchedulerFrame scheduler;

	boolean clientStarted;

	EnvironmentRegistry environmentRegistry;

	/*
	 * Instruction from the queue that serial execution is controlled by
	 * invokeSync (so execute the next invokeInClientFrame without synchronizing
	 * on 'this')
	 */
	boolean runInFrameWithoutSync;

	AtomicInteger serverClientMessageCounter = new AtomicInteger();

	long lastPacketsReceived;

	public void clientStarted() {
		clientStarted = true;
		scheduler.setClientStarted(true);
	}

	Environment(RemoteUi ui, Session session) {
		this.ui = ui;
		this.session = session;
		this.uid = SEUtilities.generatePrettyUuid();
		this.eventCollator = new EventCollator<Object>(5, this::emitMutations);
		startQueue();
	}

	public void applyEvent(DomEventData eventData) {
		runInClientFrame(
				() -> LocalDom.pathRefRepresentations().applyEvent(eventData));
	}

	public void applyLocationMutation(LocationMutation locationMutation,
			boolean startup) {
		runInClientFrame(() -> {
			if (startup) {
				Window.Location.init(locationMutation.protocol,
						locationMutation.host, locationMutation.port,
						locationMutation.path, locationMutation.queryString);
				Window.Navigator.init(locationMutation.navigator.appCodeName,
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

	// TODO - threading - this should only occur on the ClientExecutionQueue, so
	// probably dispatch should go via that
	public void applyMutations(List<MutationRecord> mutations) {
		runInClientFrame(() -> LocalDom.pathRefRepresentations()
				.applyMutations(mutations, false));
	}

	/**
	 * Executes the runnable within the environment's context. Called from
	 * outside the queuing system (so simply add to the execution queue)
	 * 
	 * Note that probably the queue should wrap the runnable (rather than here)
	 */
	public void dispatch(Runnable runnable) {
		queue.submit(() -> runInClientFrame(runnable));
	}

	public void initialiseClient(RemoteComponentProtocol.Session session) {
		Preconditions.checkState(Objects.equals(session.id, this.session.id));
		try {
			LooseContext.push();
			LooseContext.set(CONTEXT_ENVIRONMENT, this);
			environmentRegistry = new EnvironmentRegistry();
			EnvironmentRegistry.enter(environmentRegistry);
			// the order - location, history, client, document - is necessary
			location = Window.Location.contextProvider.createFrame(null);
			navigator = Window.Navigator.contextProvider.createFrame(null);
			history = History.contextProvider.createFrame(null);
			windowResources = Window.Resources.contextProvider
					.createFrame(null);
			eventFrame = EventFrame.contextProvider.createFrame(null);
			History.addValueChangeHandler(this::onHistoryChange);
			client = Client.contextProvider.createFrame(ui);
			client.getPlaceController();
			client.setupPlaceMapping();
			scheduler = SchedulerFrame.contextProvider.createFrame(null);
			scheduler.commandExecutor = new CommandExecutorImpl();
			document = Document.contextProvider.createFrame(RemoteType.PATHREF);
			document.createDocumentElement("<html/>");
			document.implAccess().pathrefRemote().mutationProxy = mutationProxy;
			document.implAccess().pathrefRemote().invokeProxy = invokeProxy;
			LocalDom.initalizeDetachedSync();
			enter(ui::init);
		} finally {
			LooseContext.pop();
		}
	}

	class CommandExecutorImpl implements SchedulerFrame.CommandExecutor {
		@Override
		public void execute(SchedulerFrame.Task task) {
			queue.submit(() -> {
				runInClientFrame(() -> {
					task.executeCommand();
				});
			});
		}
	}

	public boolean isInitialised() {
		return client != null;
	}

	public void onInvokeResponse(InvokeResponse response) {
		Runnable runnable = () -> invokeProxy.onInvokeResponse(response);
		if (response.sync) {
			// effectively reentering locked section on another thread
			runnable.run();
		} else {
			runInClientFrame(runnable);
		}
	}

	public void renderInitialUi() {
		runInClientFrame(() -> ui.render());
	}

	@Override
	public String toString() {
		return Ax.format("env::%s [%s/%s]", uid, session.id, session.auth);
	}

	public void validateSession(RemoteComponentProtocol.Session session,
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

	/*
	 * Corresponds to com.google.gwt.core.client.impl.Impl.entry0(Object
	 * jsFunction, Object thisObj, Object args)
	 */
	void enter(Runnable runnable) {
		try {
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
				scheduler.scheduleNextEntry(() -> dispatch(new Noop()));
			}
		} catch (RuntimeException e) {
			e.printStackTrace();
			// TODO - allow exception catch (uncaught exception handler) here
			throw e;
		}
	}

	// see class doc re sync
	synchronized void emitMutations() {
		if (mutations != null) {
			runInClientFrame(() -> {
				Document.get().pathrefRemote().flushSinkEventsQueue();
				queue.send(mutations);
				mutations = null;
			});
		}
	}

	void onHistoryChange(ValueChangeEvent<String> event) {
		mutationProxy.onLocationMutation(
				Message.Mutations.ofLocation().locationMutation);
	}

	void runInClientFrame(Runnable runnable) {
		if (runInFrameWithoutSync) {
			runInClientFrameUnsynced(runnable);
		} else {
			synchronized (this) {
				runInClientFrameUnsynced(runnable);
			}
		}
	}

	void runInClientFrameUnsynced(Runnable runnable) {
		if (has()) {
			runnable.run();
		} else {
			try {
				LooseContext.push();
				LooseContext.set(CONTEXT_ENVIRONMENT, this);
				EnvironmentRegistry.enter(environmentRegistry);
				Client.contextProvider.registerFrame(client);
				History.contextProvider.registerFrame(history);
				Window.Location.contextProvider.registerFrame(location);
				Window.Navigator.contextProvider.registerFrame(navigator);
				Window.Resources.contextProvider.registerFrame(windowResources);
				SchedulerFrame.contextProvider.registerFrame(scheduler);
				EventFrame.contextProvider.registerFrame(eventFrame);
				Document.contextProvider.registerFrame(document);
				Runnable cmd = () -> {
					runnable.run();
					// TODO - romcom - use normal scheduler call
					LocalDom.flush();
				};
				ui.onBeforeEnterFrame();
				enter(cmd);
			} finally {
				ui.onExitFrame();
				LooseContext.pop();
			}
		}
	}

	void startQueue() {
		queue = new ClientExecutionQueue(this);
		queue.start();
	}

	public void handleFromClientMessage(MessageHandlingToken token)
			throws Exception {
		validateSession(token.request.session,
				token.messageHandler.isValidateClientInstanceUid());
		lastPacketsReceived = System.currentTimeMillis();
		queue.handleFromClientMessage(token);
	}

	public void flush() {
		emitMutations();
	}

	public void initialiseSettings(String settings) {
		ui.initialiseSettings(settings);
	}

	public void addLifecycleHandlers() {
		runInClientFrame(() -> ui.addLifecycleHandlers());
	}

	public int nextServerClientMessageId() {
		return serverClientMessageCounter.getAndIncrement();
	}

	public String getSessionPath() {
		Url url = Url.parse(session.url);
		return url.queryParameters.get("path");
	}

	void end(String reason) {
		logger.info("Stopping env [{}] :: {}", reason, session.id);
		runInClientFrame(() -> ui.end());
		queue.stop();
		EnvironmentManager.get().deregister(this);
	}
}
