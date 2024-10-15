package cc.alcina.framework.servlet.environment;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message.AwaitRemote;
import cc.alcina.framework.servlet.component.romcom.server.RemoteComponentProtocolServer.MessageToken;
import cc.alcina.framework.servlet.environment.Environment.Access;

@Registration.NonGenericSubtypes(MessageHandlerServer.class)
public abstract class MessageHandlerServer<PM extends Message>
		implements Message.Handler<PM> {
	public abstract void handle(MessageToken token, Environment.Access env,
			PM message);

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
		public void handle(MessageToken token, Access env,
				AwaitRemote message) {
			/*
			 * Noop
			 */
		}
	}

	public static class DomEventMessageHandler
			extends MessageHandlerServer<Message.DomEventMessage> {
		@Override
		public void handle(MessageToken token, Environment.Access env,
				Message.DomEventMessage message) {
			message.events.forEach(env::applyEvent);
		}
	}

	public static class InvokeResponseHandler
			extends MessageHandlerServer<Message.InvokeResponse> {
		@Override
		public void handle(MessageToken token, Environment.Access env,
				Message.InvokeResponse message) {
			env.onInvokeResponse(message);
		}

		@Override
		public boolean isSynchronous() {
			return true;
		}
	}

	public static class MutationsHandler
			extends MessageHandlerServer<Message.Mutations> {
		@Override
		public void handle(MessageToken token, Environment.Access env,
				Message.Mutations message) {
			/*
			 * Currently romcom doesn't handle non-localdom browser .js mutation
			 * of the dom - it's planned that it will
			 */
			Preconditions.checkState(message.domMutations.isEmpty());
			Preconditions.checkState(message.eventSystemMutations.isEmpty());
			env.applyLocationMutation(message.locationMutation, false);
		}
	}

	public static class StartupHandler
			extends MessageHandlerServer<Message.Startup> {
		public boolean isSynchronous() {
			return true;
		}

		@Override
		public void handle(MessageToken token, Environment.Access env,
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