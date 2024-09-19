package cc.alcina.framework.servlet.environment;

import java.util.concurrent.CountDownLatch;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message.AwaitRemote;
import cc.alcina.framework.servlet.component.romcom.server.RemoteComponentProtocolServer.MessageToken;

@Registration.NonGenericSubtypes(MessageHandlerServer.class)
public abstract class MessageHandlerServer<PM extends Message>
		implements MessageToken.Handler<Environment, PM> {
	public abstract void handle(MessageToken token, Environment env,
			PM message);

	public boolean isValidateClientInstanceUid() {
		return true;
	}

	public void onAfterMessageHandled(PM message) {
	}

	public void onBeforeMessageHandled(PM message) {
	}

	public static class AwaitRemoteHandler
			extends MessageHandlerServer<Message.AwaitRemote>
			implements FromClientMessageAcceptor {
		CountDownLatch latch;

		MessageToken token;

		@Override
		public void onBeforeMessageHandled(Message.AwaitRemote message) {
			// required for reentrant handling of messages
			message.sync = true;
		}

		@Override
		public void handle(MessageToken token, Environment env,
				AwaitRemote message) {
			latch = new CountDownLatch(1);
			this.token = token;
			// NOOP (will be called-back once there's a message)
		}

		@Override
		public void onAfterMessageHandled(Message.AwaitRemote message) {
			try {
				latch.await();
			} catch (InterruptedException e) {
				Ax.simpleExceptionOut(e);// and exit, client will retry
			}
		}

		@Override
		public void accept(Message message) {
			token.response.protocolMessage = message;
			latch.countDown();
		}
	}

	public interface FromClientMessageAcceptor {
		void accept(Message message);
	}

	public static class DomEventMessageHandler
			extends MessageHandlerServer<Message.DomEventMessage> {
		@Override
		public void handle(MessageToken token, Environment env,
				Message.DomEventMessage message) {
			message.events.forEach(env::applyEvent);
		}
	}

	public static class InvokeResponseHandler
			extends MessageHandlerServer<Message.InvokeResponse> {
		@Override
		public void handle(MessageToken token, Environment env,
				Message.InvokeResponse message) {
			env.onInvokeResponse(message);
		}
	}

	public static class MutationsHandler
			extends MessageHandlerServer<Message.Mutations> {
		@Override
		public void handle(MessageToken token, Environment env,
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
		@Override
		public void handle(MessageToken token, Environment env,
				Message.Startup message) {
			env.initialiseClient(token.request.session);
			env.applyMutations(message.domMutations);
			env.applyLocationMutation(message.locationMutation, true);
			env.initialiseSettings(message.settings);
			env.startClient();
			token.response.protocolMessage = new Message.BeginAwaitLoop();
		}

		// rather than throwing if different to current (like other packets), a
		// new clientInstanceUid for this packet clobbers other sessions
		@Override
		public boolean isValidateClientInstanceUid() {
			return false;
		}
	}
}