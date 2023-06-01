package cc.alcina.extras.dev.component.remote.protocol;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.dom.client.LocalDom;
import com.google.gwt.dom.client.Pathref;
import com.google.gwt.dom.client.mutations.MutationRecord;
import com.google.gwt.user.client.Event;

import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.reflection.Reflections;

@Bean(PropertySource.FIELDS)
public abstract class ProtocolMessage {
	/*
	 * Sent by the client to allow the server to send it messages
	 */
	public static class AwaitRemote extends ProtocolMessage {
	}

	/*
	 * Sent by the server to instruct the client to begin the await loop
	 */
	public static class BeginAwaitLoop extends ProtocolMessage {
	}

	/*
	 * Not an album by Beck.
	 */
	public static class DomEventMessage extends ProtocolMessage {
		public Pathref firstReceiver;

		// But wait - is this serializable? Yes, it is!
		public Event event;
	}

	public static class InvalidClientUidException extends Exception {
	}

	/*
	 * An album by Beck. Amazing.
	 */
	public static class Mutations extends ProtocolMessage {
		public List<MutationRecord> domMutations = new ArrayList<>();

		public List<EventSystemMutation> eventMutations = new ArrayList<>();
	}

	/*
	 * Models an exception during message processing;
	 */
	public static class ProcessingException extends ProtocolMessage {
		public String exceptionMessage;

		public String exceptionClassName;

		public Class<? extends Exception> exceptionClass() {
			try {
				return Reflections.forName(exceptionClassName);
			} catch (Exception e) {
				return null;
			}
		}
	}

	/*
	 * Sent by the client on startup, to initialise the server dom
	 */
	public static class Startup extends ProtocolMessage {
		public static Startup forClient() {
			Startup result = new Startup();
			result.maxCharsPerTextNode = LocalDom.maxCharsPerTextNode;
			result.domMutations = LocalDom.pathRefRepresentations()
					.domAsMutations();
			return result;
		}

		public List<MutationRecord> domMutations = new ArrayList<>();

		public int maxCharsPerTextNode;
	}
}