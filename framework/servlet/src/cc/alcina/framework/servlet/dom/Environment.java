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
import com.google.gwt.dom.client.LocalDom;
import com.google.gwt.dom.client.mutations.MutationRecord;

import cc.alcina.extras.dev.component.remote.protocol.ProtocolMessage;
import cc.alcina.extras.dev.component.remote.protocol.ProtocolMessage.InvalidClientUidException;
import cc.alcina.extras.dev.component.remote.protocol.RemoteComponentRequest.Session;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.gwt.client.util.EventCollator;

/*
 * Sync note - most methods will be called already synced on the environment
 * (since called downstream from a servlet call which looks up the environment
 * and syncs)
 * 
 * An exception is the eventCollator action - it occurs on a timer thread so is
 * responsible for its own sync
 */
public class Environment {
	public static final transient String CONTEXT_TEST_CREDENTIALS = Environment.class
			.getName() + ".CONTEXT_TEST_CREDENTIALS";

	/*
	 * Sent by the client to mark which environment it's communicating with.
	 * This allows the client to switch environments after say a dev rebuild
	 * (equal id/auth, unequal uid will force a refresh)
	 */
	public final String uid;

	public final String id;

	public final String auth;

	RemoteUi ui;

	ClientProtocolMessageQueue queue;

	/*
	 * The uid of the most recent client to send a startup packet. All others
	 * sent a "
	 */
	String connectedClientUid;

	Logger logger = LoggerFactory.getLogger(getClass());

	private Document document;

	MutationProxyImpl mutationProxy = new MutationProxyImpl();

	ProtocolMessage.Mutations mutations = null;

	private EventCollator<Object> eventCollator;

	Environment(RemoteUi ui) {
		this.ui = ui;
		uid = SEUtilities.generatePrettyUuid();
		if (LooseContext.is(CONTEXT_TEST_CREDENTIALS)) {
			id = "test";
			auth = "test";
		} else {
			id = SEUtilities.generatePrettyUuid();
			auth = SEUtilities.generatePrettyUuid();
		}
		this.eventCollator = new EventCollator<Object>(5, this::emitMutations);
	}

	public void applyMutations(List<MutationRecord> mutations) {
		runInDocumentFrame(() -> LocalDom.pathRefRepresentations()
				.applyMutations(mutations, false));
	}

	public void initialiseClient(Session session) {
		if (queue == null) {
			startQueue();
		}
		connectedClientUid = session.clientInstanceUid;
		try {
			LooseContext.push();
			document = Document.contextProvider.createFrame(RemoteType.PATHREF);
			document.createDocumentElement("html");
			document.implAccess().pathrefRemote().mutationProxy = mutationProxy;
			LocalDom.initalizeDetachedSync();
			ui.init();
		} finally {
			LooseContext.pop();
		}
	}

	public void
			registerRemoteMessageConsumer(Consumer<ProtocolMessage> consumer) {
		queue.registerConsumer(consumer);
	}

	public void renderInitialUi() {
		runInDocumentFrame(() -> ui.render());
	}

	@Override
	public String toString() {
		return Ax.format("env::%s [%s/%s]", uid, id, auth);
	}

	public void validateSession(Session session,
			boolean validateClientInstanceUid) throws Exception {
		Preconditions.checkArgument(
				Objects.equals(session.environmentAuth, auth), "Invalid auth");
		if (validateClientInstanceUid) {
			if (!Objects.equals(session.clientInstanceUid,
					connectedClientUid)) {
				logger.warn("Expired client (tab) : {}",
						session.clientInstanceUid);
				throw new InvalidClientUidException();
			}
		}
	}

	private void runInDocumentFrame(Runnable runnable) {
		try {
			LooseContext.push();
			Document.contextProvider.registerFrame(document);
			runnable.run();
			LocalDom.flush();
		} finally {
			LooseContext.pop();
		}
	}

	private void startQueue() {
		queue = new ClientProtocolMessageQueue();
		String threadName = Ax.format("remcom-env-%s", id);
		Thread thread = new Thread(queue, threadName);
		thread.setDaemon(true);
		thread.start();
	}

	// see class doc re sync
	synchronized void emitMutations() {
		queue.send(mutations);
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
		public void onMutation(MutationRecord mutationRecord) {
			if (mutations == null) {
				mutations = new ProtocolMessage.Mutations();
			}
			mutations.domMutations.add(mutationRecord);
			eventCollator.eventOccurred();
		}
	}
}
