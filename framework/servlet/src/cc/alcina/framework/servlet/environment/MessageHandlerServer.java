package cc.alcina.framework.servlet.environment;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.gwt.client.logic.handshake.SetupAfterObjectsPlayer;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message.AwaitRemote;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message.ServerDebugProtocolRequest;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message.ServerDebugProtocolResponse;
import cc.alcina.framework.servlet.component.romcom.server.RemoteComponentProtocolServer.MessageProcessingToken;
import cc.alcina.framework.servlet.environment.Environment.Access;

@Registration.NonGenericSubtypes(MessageHandlerServer.class)
public abstract class MessageHandlerServer<PM extends Message>
		implements Message.Handler<PM> {
	public abstract void handle(MessageProcessingToken token,
			Environment.Access env, PM message);

	public boolean isValidateClientInstanceUid() {
		return true;
	}

	public static class AwaitRemoteHandler
			extends MessageHandlerServer<Message.AwaitRemote> {
		@Override
		public boolean isSynchronous() {
			return true;
		}

		@Override
		public void handle(MessageProcessingToken token, Access env,
				AwaitRemote message) {
			/*
			 * Noop
			 */
		}
	}

	public static class ServerDebugProtocolRequestHandler
			extends MessageHandlerServer<Message.ServerDebugProtocolRequest> {
		@Override
		public boolean isSynchronous() {
			return true;
		}

		@Override
		public void handle(MessageProcessingToken token, Access env,
				ServerDebugProtocolRequest message) {
			env.emitServerDebugProtocolResponse(message);
		}
	}

	public static class DomEventMessageHandler
			extends MessageHandlerServer<Message.DomEventMessage> {
		@Override
		public void handle(MessageProcessingToken token, Environment.Access env,
				Message.DomEventMessage message) {
			env.onDomEventMessage(message);
		}
	}

	public static class InvokeResponseHandler
			extends MessageHandlerServer<Message.InvokeResponse> {
		@Override
		public void handle(MessageProcessingToken token, Environment.Access env,
				Message.InvokeResponse message) {
			env.onInvokeResponse(message);
		}

		@Override
		public boolean isSynchronous() {
			return true;
		}
	}

	public static class WindowStateUpdateHandler
			extends MessageHandlerServer<Message.WindowStateUpdate> {
		@Override
		public void handle(MessageProcessingToken token, Environment.Access env,
				Message.WindowStateUpdate message) {
			env.applyWindowState(message.windowState);
			env.applySelectionRecord(message.selectionRecord);
		}

		@Override
		public boolean isSynchronous() {
			return true;
		}
	}

	public static class MutationsHandler
			extends MessageHandlerServer<Message.Mutations> {
		@Override
		public void handle(MessageProcessingToken token, Environment.Access env,
				Message.Mutations message) {
			env.applyDomMutations(message.domMutations);
			Preconditions.checkState(message.eventSystemMutations.isEmpty());
			if (message.locationMutation != null) {
				env.applyLocationMutation(message.locationMutation, false);
			}
			env.applySelectionRecord(message.selectionMutation);
		}
	}

	public static class ProcessingExceptionHandler
			extends MessageHandlerServer<Message.ProcessingException> {
		@Override
		public void handle(MessageProcessingToken token, Environment.Access env,
				Message.ProcessingException message) {
			env.onClientProcessingException(message);
		}
	}

	public static class StartupHandler
			extends MessageHandlerServer<Message.Startup> {
		@Override
		public boolean isSynchronous() {
			return true;
		}

		@Override
		public void handle(MessageProcessingToken token, Environment.Access env,
				Message.Startup message) {
			/*
			 * the startup message handler will send a BeginAwaitLoop message to
			 * the server at the end of processing
			 */
			env.startup(token, message);
			// token.response.protocolMessage = new Message.BeginAwaitLoop();
		}

		// rather than throwing if different to current (like other packets), a
		// new clientInstanceUid for this packet clobbers other sessions
		@Override
		public boolean isValidateClientInstanceUid() {
			return false;
		}
	}

	public static MessageHandlerServer<?> forMessage(Message message) {
		return Registry.impl(MessageHandlerServer.class, message.getClass());
	}

	/*
	 * handle synchronously (from the POV of the incoming Http/WS request)(not
	 * in the CEQT)
	 */
	public boolean isSynchronous() {
		return false;
	}
}