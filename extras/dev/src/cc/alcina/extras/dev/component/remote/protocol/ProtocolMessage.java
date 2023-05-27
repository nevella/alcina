package cc.alcina.extras.dev.component.remote.protocol;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.dom.client.LocalDom;
import com.google.gwt.dom.client.mutations.MutationRecord;

import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;

@Bean(PropertySource.FIELDS)
public abstract class ProtocolMessage {
	/*
	 * Sent by the server if an old client is accessing a server rc environment
	 */
	public static class InvalidClientUid extends ProtocolMessage {
	}

	/*
	 * Sent by the client on startup, to initialise the server dom
	 */
	public static class Startup extends ProtocolMessage {
		public static Startup forClient() {
			Startup result = new Startup();
			result.maxCharsPerTextNode = LocalDom.maxCharsPerTextNode;
			result.mutations = LocalDom.pathRefRepresentations()
					.domAsMutations();
			return result;
		}

		public List<MutationRecord> mutations = new ArrayList<>();

		public int maxCharsPerTextNode;
	}
}