package cc.alcina.framework.servlet.component.romcom.server;

import java.util.concurrent.CountDownLatch;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message.AwaitRemote;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentRequest;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentResponse;
import cc.alcina.framework.servlet.dom.Environment;

public class MessageHandlers {
	public static class AwaitRemoteHandler extends
			RemoteComponentProtocolServer.MessageHandlerServer<Message.AwaitRemote>
			implements FromClientMessageAcceptor {
		CountDownLatch latch;

		RemoteComponentResponse response;

		@Override
		public void onBeforeMessageHandled(Message.AwaitRemote message) {
			// required for reentrant handling of messages
			message.sync = true;
		}

		@Override
		public void handle(RemoteComponentRequest request,
				RemoteComponentResponse response, Environment env,
				AwaitRemote message) {
			latch = new CountDownLatch(1);
			this.response = response;
			// NOOP (will be called-back once there's a message)
		}

		@Override
		public void onAfterMessageHandled(Message.AwaitRemote message) {
			try {
				Ax.logEvent("before await latch - %s - message id %s",
						hashCode(), message.messageId);
				latch.await();
				Ax.logEvent("after await latch - %s - message id %s",
						hashCode(), message.messageId);
			} catch (InterruptedException e) {
				Ax.simpleExceptionOut(e);// and exit, client will retry
			}
		}

		@Override
		public void accept(Message message) {
			response.protocolMessage = message;
			latch.countDown();
			Ax.logEvent(
					"after await latch - consumed - %s - result message id %s",
					hashCode(), message == null ? "(null)" : message.messageId);
		}
	}

	public interface FromClientMessageAcceptor {
		void accept(Message message);
	}

	public static class DomEventMessageHandler extends
			RemoteComponentProtocolServer.MessageHandlerServer<Message.DomEventMessage> {
		@Override
		public void handle(RemoteComponentRequest request,
				RemoteComponentResponse response, Environment env,
				Message.DomEventMessage message) {
			message.events.forEach(env::applyEvent);
		}
	}

	public static class InvokeResponseHandler extends
			RemoteComponentProtocolServer.MessageHandlerServer<Message.InvokeResponse> {
		@Override
		public void handle(RemoteComponentRequest request,
				RemoteComponentResponse response, Environment env,
				Message.InvokeResponse message) {
			env.onInvokeResponse(message);
		}
	}

	public static class MutationsHandler extends
			RemoteComponentProtocolServer.MessageHandlerServer<Message.Mutations> {
		@Override
		public void handle(RemoteComponentRequest request,
				RemoteComponentResponse response, Environment env,
				Message.Mutations message) {
			Preconditions.checkState(message.domMutations.isEmpty());
			Preconditions.checkState(message.eventMutations.isEmpty());
			env.applyLocationMutation(message.locationMutation, false);
		}
	}

	public static class StartupHandler extends
			RemoteComponentProtocolServer.MessageHandlerServer<Message.Startup> {
		@Override
		public void handle(RemoteComponentRequest request,
				RemoteComponentResponse response, Environment env,
				Message.Startup message) {
			env.initialiseClient(request.session);
			env.applyMutations(message.domMutations);
			env.applyLocationMutation(message.locationMutation, true);
			env.initialiseSettings(message.settings);
			/*
			 * will enqueue a mutations event in the to-client queue
			 */
			env.renderInitialUi();
			env.addLifecycleHandlers();
			env.clientStarted();
			response.protocolMessage = new Message.BeginAwaitLoop();
		}

		// rather than throwing if different to current (like other packets), a
		// new clientInstanceUid for this packet clobbers other sessions
		@Override
		public boolean isValidateClientInstanceUid() {
			return false;
		}
	}
}
