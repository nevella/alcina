package cc.alcina.extras.dev.component.remote.server;

import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import cc.alcina.extras.dev.component.remote.protocol.ProtocolMessage;
import cc.alcina.extras.dev.component.remote.protocol.ProtocolMessage.AwaitRemote;
import cc.alcina.extras.dev.component.remote.protocol.ProtocolMessage.Startup;
import cc.alcina.extras.dev.component.remote.protocol.RemoteComponentRequest;
import cc.alcina.extras.dev.component.remote.protocol.RemoteComponentResponse;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.util.Ax;
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

		class MessageConsumer implements Consumer<ProtocolMessage> {
			// will be on a different thread to the parent instance handle
			// method
			@Override
			public void accept(ProtocolMessage message) {
				AwaitRemoteHandler.this.message = message;
				env.registerRemoteMessageConsumer(null);
				latch.countDown();
			}
		}
	}

	public static class StartupHandler
			extends ProtocolMessageHandlerServer<ProtocolMessage.Startup> {
		@Override
		public void handle(RemoteComponentRequest request,
				RemoteComponentResponse response, Environment env,
				Startup message) {
			env.initialiseClient(request.session);
			env.applyMutations(message.domMutations);
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