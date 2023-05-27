package cc.alcina.framework.servlet.dom;

import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.LocalDom;
import com.google.gwt.dom.client.mutations.MutationRecord;

import cc.alcina.extras.dev.component.remote.protocol.ProtocolMessage;
import cc.alcina.extras.dev.component.remote.protocol.RemoteComponentRequest.Session;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.impl.DocumentContextProviderImpl;
import cc.alcina.framework.entity.impl.DocumentContextProviderImpl.DocumentFrame;

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

	DocumentFrame frame;

	RemoteUi ui;

	ClientProtocolMessageQueues queues = new ClientProtocolMessageQueues();

	/*
	 * The uid of the most recent client to send a startup packet. All others
	 * sent a "
	 */
	String connectedClientUid;

	Logger logger = LoggerFactory.getLogger(getClass());

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
		try {
			LooseContext.push();
			DocumentContextProviderImpl.get().registerNewContextFrame();
			Document document = Document.get();
			document.createDocumentElement("html");
			LocalDom.initalizeDetachedSync();
			frame = DocumentContextProviderImpl.get().contextInstance();
			ui.init();
		} finally {
			LooseContext.pop();
		}
	}

	public void applyMutations(List<MutationRecord> mutations) {
		runInDocumentFrame(() -> LocalDom.pathRefRepresentations()
				.applyMutations(mutations));
	}

	public void initialiseClient(Session session) {
		connectedClientUid = session.clientInstanceUid;
	}

	@Override
	public String toString() {
		return Ax.format("env::%s [%s/%s]", uid, id, auth);
	}

	public void validateSession(Session session,
			boolean validateClientInstanceUid) {
		Preconditions.checkArgument(
				Objects.equals(session.environmentAuth, auth), "Invalid auth");
		if (validateClientInstanceUid) {
			if (!Objects.equals(session.environmentAuth, auth)) {
				queues.send(session, new ProtocolMessage.InvalidClientUid());
				logger.warn("Expired client (tab) : {}",
						session.clientInstanceUid);
			}
		}
	}

	private void runInDocumentFrame(Runnable runnable) {
		try {
			LooseContext.push();
			DocumentContextProviderImpl.get().registerContextFrame(frame);
			runnable.run();
		} finally {
			LooseContext.pop();
		}
	}

	class ClientProtocolMessageQueues {
		public void send(Session session, ProtocolMessage message) {
			throw new UnsupportedOperationException();
		}
	}
}
