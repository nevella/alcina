package cc.alcina.framework.servlet.environment;

import java.util.concurrent.CountDownLatch;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message.AwaitRemote;
import cc.alcina.framework.servlet.component.romcom.server.RemoteComponentProtocolServer.MessageToken;

@Registration.NonGenericSubtypes(MessageHandlerServer.class)
public abstract class MessageHandlerServer<PM extends Message>
		implements MessageToken.Handler<Environment.Access, PM> {
	public abstract void handle(MessageToken token, Environment.Access env,
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
			implements ToClientMessageAcceptor {
		CountDownLatch latch;

		MessageToken token;

		@Override
		public void onBeforeMessageHandled(Message.AwaitRemote message) {
			// handle this message on the receiving servlet thread
			message.sync = true;
		}

		/*
		 * handled in the servlet thread, not the client execution thread
		 */
		@Override
		public void handle(MessageToken token, Environment.Access env,
				AwaitRemote message) {
			latch = new CountDownLatch(1);
			this.token = token;
			env.registerToClientMessageAcceptor(this);
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
			// token.response.protocolMessage = message;
			// latch.countDown();
		}
	}

	/*
	 * Accepts messages to the client (and causes them to be passed to the
	 * client)
	 */
	interface ToClientMessageAcceptor {
		void accept(Message message);
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
		@Override
		public void onBeforeMessageHandled(Message.Startup message) {
			// handle this message on the receiving servlet thread
			message.sync = true;
		}

		public boolean isSync() {
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
	 * handle synchronously (not in the CEQT)
	 */
	public boolean isSync() {
		return false;
	}
}