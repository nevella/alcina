package cc.alcina.framework.servlet.dom;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Document.RemoteType;
import com.google.gwt.dom.client.DocumentPathref;
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

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.Ref;
import cc.alcina.framework.common.client.util.Timer;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.gwt.headless.SchedulerFrame;
import cc.alcina.framework.entity.util.TimerJvm;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.util.EventCollator;
import cc.alcina.framework.servlet.component.romcom.protocol.EventSystemMutation;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message.InvokeResponse;
import cc.alcina.framework.servlet.dom.PathrefDom.Credentials;

/*
 * Sync note - most methods will be called already synced on the environment
 * (since called downstream from a servlet call which looks up the environment
 * and syncs)
 *
 * An exception is the eventCollator action - it occurs on a timer thread so is
 * responsible for its own sync
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

	class ClientProtocolMessageQueue implements Runnable {
		BlockingQueue<Message> queue = new LinkedBlockingQueue<>();

		boolean finished = false;

		Consumer<Message> consumer = null;

		public void registerConsumer(Consumer<Message> consumer) {
			this.consumer = consumer;
			if (consumer != null) {
				synchronized (this) {
					notify();
				}
			}
		}

		@Override
		public void run() {
			while (!finished) {
				while (consumer == null) {
					synchronized (this) {
						try {
							wait();
						} catch (InterruptedException e) {
							Ax.simpleExceptionOut(e);
						}
					}
				}
				try {
					Message message = queue.take();
					consumer.accept(message);
				} catch (Throwable e) {
					logger.warn("Queue handler issue");
					throw WrappedRuntimeException.wrap(e);
				}
			}
		}

		/*
		 * Does not await receipt
		 */
		public void send(Message message) {
			queue.add(message);
		}

		public <R extends Message> R sendAndReceive(Message message) {
			throw new UnsupportedOperationException();
		}
	}

	class InvokeProxyImpl implements DocumentPathref.InvokeProxy {
		int invokeCounter = 0;

		Map<Integer, AsyncCallback> callbacks = new LinkedHashMap<>();

		@Override
		public void invoke(NodePathref node, String methodName,
				List<Class> argumentTypes, List<?> arguments,
				AsyncCallback<?> callback) {
			// always emit mutations before proxy invoke
			emitMutations();
			runInClientFrame(() -> {
				Message.Invoke invoke = new Message.Invoke();
				invoke.path = node == null ? null
						: Pathref.forNode(node.node());
				invoke.id = ++invokeCounter;
				invoke.methodName = methodName;
				invoke.argumentTypes = argumentTypes == null ? List.of()
						: argumentTypes;
				invoke.arguments = arguments == null ? List.of() : arguments;
				callbacks.put(invoke.id, callback);
				queue.send(invoke);
			});
		}

		public void onInvokeResponse(InvokeResponse response) {
			AsyncCallback callback = callbacks.remove(response.id);
			if (response.exception == null) {
				callback.onSuccess(response.response);
			} else {
				callback.onSuccess(response.exception);
			}
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

	private static final transient String CONTEXT_ENVIRONMENT = Environment.class
			.getName() + ".CONTEXT_ENVIRONMENT";

	public static Environment get() {
		return LooseContext.get(CONTEXT_ENVIRONMENT);
	}

	/*
	 * Sent by the client to mark which environment it's communicating with.
	 * This allows the client to switch environments after say a dev rebuild
	 * (equal id/auth, unequal uid will force a refresh)
	 */
	public final String uid;

	public final RemoteUi ui;

	ClientProtocolMessageQueue queue;

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

	final Credentials credentials;

	SchedulerFrame scheduler;

	Environment(RemoteUi ui, Credentials credentials) {
		this.ui = ui;
		this.credentials = credentials;
		this.uid = SEUtilities.generatePrettyUuid();
		this.eventCollator = new EventCollator<Object>(5, this::emitMutations);
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
			Ax.out("Navigate %s:: -> %s", startup ? "(startup) " : "", token);
			History.newItem(token, !startup);
		});
	}

	public void applyMutations(List<MutationRecord> mutations) {
		runInClientFrame(() -> LocalDom.pathRefRepresentations()
				.applyMutations(mutations, false));
	}

	/**
	 * Executes the runnable within the environment's context
	 */
	public void dispatch(Runnable runnable) {
		runInClientFrame(runnable);
	}

	public void initialiseClient(RemoteComponentProtocol.Session session) {
		if (queue == null) {
			startQueue();
		}
		try {
			LooseContext.push();
			LooseContext.set(CONTEXT_ENVIRONMENT, this);
			// the order - location, history, client, document - is necessary
			location = Window.Location.contextProvider.createFrame(null);
			navigator = Window.Navigator.contextProvider.createFrame(null);
			history = History.contextProvider.createFrame(null);
			History.addValueChangeHandler(this::onHistoryChange);
			client = Client.contextProvider.createFrame(ui);
			client.getPlaceController();
			client.setupPlaceMapping();
			scheduler = SchedulerFrame.contextProvider.createFrame(null);
			scheduler.commandExecutor = this::performScheduledCommand;
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
				scheduler.pump(false);
				scheduler.scheduleNextEntry(() -> runInClientFrame(new Noop()));
			}
		} catch (RuntimeException e) {
			e.printStackTrace();
			// TODO - allow exception catch (uncaught exception handler) here
			throw e;
		}
	}

	class Noop implements Runnable {
		@Override
		public void run() {
			// used to trigger a timed schedule event within the main event pump
			// loop
		}
	}

	public boolean isInitialised() {
		return client != null;
	}

	public boolean performScheduledCommand(Scheduler.Command command) {
		Ref<Boolean> resultRef = Ref.of(false);
		runInClientFrame(() -> {
			if (command instanceof RepeatingCommand) {
				boolean result = ((RepeatingCommand) command).execute();
				resultRef.set(result);
			} else {
				((ScheduledCommand) command).execute();
			}
		});
		return resultRef.get();
	}

	public void onInvokeResponse(InvokeResponse response) {
		runInClientFrame(() -> invokeProxy.onInvokeResponse(response));
	}

	public synchronized void
			registerRemoteMessageConsumer(Consumer<Message> consumer) {
		queue.registerConsumer(consumer);
	}

	public void renderInitialUi() {
		runInClientFrame(() -> ui.render());
	}

	@Override
	public String toString() {
		return Ax.format("env::%s [%s/%s]", uid, credentials.id,
				credentials.auth);
	}

	public void validateSession(RemoteComponentProtocol.Session session,
			boolean validateClientInstanceUid) throws Exception {
		if (!Objects.equals(session.auth, credentials.auth)) {
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

	static boolean has() {
		return LooseContext.has(CONTEXT_ENVIRONMENT);
	}

	synchronized void runInClientFrame(Runnable runnable) {
		if (has()) {
			runnable.run();
		} else {
			try {
				LooseContext.push();
				LooseContext.set(CONTEXT_ENVIRONMENT, this);
				Client.contextProvider.registerFrame(client);
				History.contextProvider.registerFrame(history);
				Window.Location.contextProvider.registerFrame(location);
				Window.Navigator.contextProvider.registerFrame(navigator);
				SchedulerFrame.contextProvider.registerFrame(scheduler);
				Document.contextProvider.registerFrame(document);
				Runnable cmd = () -> {
					runnable.run();
					// TODO - romcom - use normal scheduler call
					LocalDom.flush();
				};
				enter(cmd);
			} finally {
				LooseContext.pop();
			}
		}
	}

	private void startQueue() {
		queue = new ClientProtocolMessageQueue();
		String threadName = Ax.format("remcom-env-%s", credentials.id);
		Thread thread = new Thread(queue, threadName);
		thread.setDaemon(true);
		thread.start();
	}
}
