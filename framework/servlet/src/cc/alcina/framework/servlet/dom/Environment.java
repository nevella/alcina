package cc.alcina.framework.servlet.dom;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Document.RemoteType;
import com.google.gwt.dom.client.DocumentPathref;
import com.google.gwt.dom.client.DomEventData;
import com.google.gwt.dom.client.LocalDom;
import com.google.gwt.dom.client.Pathref;
import com.google.gwt.dom.client.mutations.LocationMutation;
import com.google.gwt.dom.client.mutations.MutationRecord;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.util.EventCollator;
import cc.alcina.framework.servlet.component.remote.protocol.EventSystemMutation;
import cc.alcina.framework.servlet.component.remote.protocol.ProtocolMessage;
import cc.alcina.framework.servlet.component.remote.protocol.ProtocolMessage.InvalidClientUidException;
import cc.alcina.framework.servlet.component.remote.protocol.RemoteComponentRequest.Session;
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
	/*
	 * Sent by the client to mark which environment it's communicating with.
	 * This allows the client to switch environments after say a dev rebuild
	 * (equal id/auth, unequal uid will force a refresh)
	 */
	public final String uid;

	RemoteUi ui;

	ClientProtocolMessageQueue queue;

	/*
	 * The uid of the most recent client to send a startup packet. All others
	 * will receive a 'reload' message on rpc calls
	 */
	String connectedClientUid;

	Logger logger = LoggerFactory.getLogger(getClass());

	Document document;

	MutationProxyImpl mutationProxy = new MutationProxyImpl();

	ProtocolMessage.Mutations mutations = null;

	EventCollator<Object> eventCollator;

	Client client;

	History history;

	Window.Location location;

	final Credentials credentials;

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

	public void initialiseClient(Session session) {
		if (queue == null) {
			startQueue();
		}
		connectedClientUid = session.clientInstanceUid;
		try {
			LooseContext.push();
			// the order - location, history, client, document - is necessary
			location = Window.Location.contextProvider.createFrame(null);
			history = History.contextProvider.createFrame(null);
			History.addValueChangeHandler(this::onHistoryChange);
			client = Client.contextProvider.createFrame(ui);
			client.getPlaceController();
			client.setupPlaceMapping();
			document = Document.contextProvider.createFrame(RemoteType.PATHREF);
			document.createDocumentElement("html");
			document.implAccess().pathrefRemote().mutationProxy = mutationProxy;
			LocalDom.initalizeDetachedSync();
			ui.init();
		} finally {
			LooseContext.pop();
		}
	}

	public synchronized void
			registerRemoteMessageConsumer(Consumer<ProtocolMessage> consumer) {
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

	public void validateSession(Session session,
			boolean validateClientInstanceUid) throws Exception {
		Preconditions.checkArgument(
				Objects.equals(session.environmentAuth, credentials.auth),
				"Invalid auth");
		if (validateClientInstanceUid) {
			if (!Objects.equals(session.clientInstanceUid,
					connectedClientUid)) {
				logger.warn("Expired client (tab) : {}",
						session.clientInstanceUid);
				throw new InvalidClientUidException();
			}
		}
	}

	private void runInClientFrame(Runnable runnable) {
		try {
			LooseContext.push();
			Client.contextProvider.registerFrame(client);
			History.contextProvider.registerFrame(history);
			Window.Location.contextProvider.registerFrame(location);
			Document.contextProvider.registerFrame(document);
			runnable.run();
			LocalDom.flush();
		} finally {
			LooseContext.pop();
		}
	}

	private void startQueue() {
		queue = new ClientProtocolMessageQueue();
		String threadName = Ax.format("remcom-env-%s", credentials.id);
		Thread thread = new Thread(queue, threadName);
		thread.setDaemon(true);
		thread.start();
	}

	// see class doc re sync
	synchronized void emitMutations() {
		if (mutations != null) {
			queue.send(mutations);
			mutations = null;
		}
	}

	void onHistoryChange(ValueChangeEvent<String> event) {
		mutationProxy.onLocationMutation(
				ProtocolMessage.Mutations.ofLocation().locationMutation);
	}

	class ClientProtocolMessageQueue implements Runnable {
		BlockingQueue<ProtocolMessage> queue = new LinkedBlockingQueue<>();

		boolean finished = false;

		Consumer<ProtocolMessage> consumer = null;

		public void registerConsumer(Consumer<ProtocolMessage> consumer) {
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
					ProtocolMessage message = queue.take();
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
		public void send(ProtocolMessage message) {
			queue.add(message);
		}

		public <R extends ProtocolMessage> R
				sendAndReceive(ProtocolMessage message) {
			throw new UnsupportedOperationException();
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
				mutations = new ProtocolMessage.Mutations();
			}
			runnable.run();
			eventCollator.eventOccurred();
		}
	}
}