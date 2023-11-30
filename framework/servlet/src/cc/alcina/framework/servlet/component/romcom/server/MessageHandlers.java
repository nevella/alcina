package cc.alcina.framework.servlet.component.romcom.server;

import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message.AwaitRemote;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentRequest;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentResponse;
import cc.alcina.framework.servlet.dom.Environment;

public class MessageHandlers {
	public static class AwaitRemoteHandler extends
			RemoteComponentProtocolServer.MessageHandlerServer<Message.AwaitRemote> {
		private CountDownLatch latch;

		private Environment env;

		public Message message;

		@Override
		public void handle(RemoteComponentRequest request,
				RemoteComponentResponse response, Environment env,
				AwaitRemote message) {
			this.env = env;
			this.latch = new CountDownLatch(1);
			env.registerRemoteMessageConsumer(new MessageConsumer());
			try {
				latch.await();
				response.protocolMessage = this.message;
			} catch (InterruptedException e) {
				throw new WrappedRuntimeException(e);
			}
		}

		@Override
		Object provideMonitor(Environment env) {
			return this;
		}

		class MessageConsumer implements Consumer<Message> {
			// will be called on a different thread to the parent instance
			// handle
			// method (the calling frame owns the env monitor)
			@Override
			public void accept(Message message) {
				AwaitRemoteHandler.this.message = message;
				env.registerRemoteMessageConsumer(null);
				latch.countDown();
			}
		}
	}

	public static class DomEventMessageHandler extends
			RemoteComponentProtocolServer.MessageHandlerServer<Message.DomEventMessage> {
		@Override
		public void handle(RemoteComponentRequest request,
				RemoteComponentResponse response, Environment env,
				Message.DomEventMessage message) {
			env.applyEvent(message.data);
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
			/*
			 * will enqueue a mutations event in the to-client queue
			 */
			env.renderInitialUi();
			response.protocolMessage = new Message.BeginAwaitLoop();
		}

		// rather than throwing if different to current (like other packets), a
		// new clientInstanceUid for this packet clobbers other sessions
		@Override
		public boolean isValidateClientInstanceUid() {
			return false;
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
}
