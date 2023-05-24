package cc.alcina.extras.dev.component.remote.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.projection.GraphProjection;

@Registration.Singleton(RemoteComponentRemote.class)
public class RemoteComponentRemote {
	public static final transient String CONTEXT_CALLER_CLIENT_INSTANCE_UID = RemoteComponentRemote.class
			.getName() + ".CONTEXT_CALLER_CLIENT_INSTANCE_UID";

	public static RemoteComponentRemote get() {
		return Registry.impl(RemoteComponentRemote.class);
	}

	Object outputReadyNotifier = new Object();

	private List<ConsoleRecord> records = new ArrayList<>();

	public synchronized boolean hasRecords(String clientInstanceUid) {
		throw new UnsupportedOperationException();
	}

	synchronized List<ConsoleRecord> takeRecords(String clientInstanceUid) {
		throw new UnsupportedOperationException();
	}

	class ConsoleRecord {
		String text = "";

		boolean clear;

		String commandText;

		boolean errWriter;

		String callerClientInstanceUid;

		public ConsoleRecord() {
			putCallerId();
		}

		public ConsoleRecord(String text, boolean errWriter) {
			this.text = text;
			this.errWriter = errWriter;
			putCallerId();
		}

		public boolean matchesCaller(String clientInstanceUid) {
			return callerClientInstanceUid == null || Objects
					.equals(callerClientInstanceUid, clientInstanceUid);
		}

		@Override
		public String toString() {
			return GraphProjection.fieldwiseToStringOneLine(this);
		}

		private void putCallerId() {
			this.callerClientInstanceUid = LooseContext
					.get(CONTEXT_CALLER_CLIENT_INSTANCE_UID);
		}
	}
}
