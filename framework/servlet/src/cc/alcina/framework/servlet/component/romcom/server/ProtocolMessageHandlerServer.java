package cc.alcina.framework.servlet.component.romcom.server;

import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.servlet.component.romcom.protocol.ProtocolMessage;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentRequest;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentResponse;
import cc.alcina.framework.servlet.component.romcom.protocol.ProtocolMessage.AwaitRemote;
import cc.alcina.framework.servlet.dom.Environment;

/*
 * FIXME - beans1x5 - package protected
 */
@Registration.NonGenericSubtypes(ProtocolMessageHandlerServer.class)
public abstract class ProtocolMessageHandlerServer<PM extends ProtocolMessage> {
	public abstract void handle(RemoteComponentRequest request,
			RemoteComponentResponse response, Environment env, PM message);

	public boolean isValidateClientInstanceUid() {
		return true;
	}

	/*
	 * Most handlers block on the environment - AwaitRemoteHandler is one that
	 * has more complex sync logic
	 */
	Object provideMonitor(Environment env) {
		return env;
	}

	public static class AwaitRemoteHandler
			extends ProtocolMessageHandlerServer<ProtocolMessage.AwaitRemote> {
		private CountDownLatch latch;

		private Environment env;

		public ProtocolMessage message;

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

		class MessageConsumer implements Consumer<ProtocolMessage> {
			// will be called on a different thread to the parent instance
			// handle
			// method (the calling frame owns the env monitor)
			@Override
			public void accept(ProtocolMessage message) {
				AwaitRemoteHandler.this.message = message;
				env.registerRemoteMessageConsumer(null);
				latch.countDown();
			}
		}
	}

	public static class DomEventMessageHandler extends
			ProtocolMessageHandlerServer<ProtocolMessage.DomEventMessage> {
		@Override
		public void handle(RemoteComponentRequest request,
				RemoteComponentResponse response, Environment env,
				ProtocolMessage.DomEventMessage message) {
			env.applyEvent(message.data);
		}
	}

	public static class MutationsHandler
			extends ProtocolMessageHandlerServer<ProtocolMessage.Mutations> {
		@Override
		public void handle(RemoteComponentRequest request,
				RemoteComponentResponse response, Environment env,
				ProtocolMessage.Mutations message) {
			Preconditions.checkState(message.domMutations.isEmpty());
			Preconditions.checkState(message.eventMutations.isEmpty());
			env.applyLocationMutation(message.locationMutation, false);
		}
	}

	public static class StartupHandler
			extends ProtocolMessageHandlerServer<ProtocolMessage.Startup> {
		@Override
		public void handle(RemoteComponentRequest request,
				RemoteComponentResponse response, Environment env,
				ProtocolMessage.Startup message) {
			env.initialiseClient(request.session);
			env.applyMutations(message.domMutations);
			env.applyLocationMutation(message.locationMutation, true);
			/*
			 * will enqueue a mutations event in the to-client queue
			 */
			env.renderInitialUi();
			response.protocolMessage = new ProtocolMessage.BeginAwaitLoop();
			Ax.out("startup");
		}

		// rather than throwing if different to current (like other packets), a
		// new clientInstanceUid for this packet clobbers other sessions
		@Override
		public boolean isValidateClientInstanceUid() {
			return false;
		}
	}
}