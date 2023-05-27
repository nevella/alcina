package cc.alcina.extras.dev.component.remote.server;

import cc.alcina.extras.dev.component.remote.protocol.ProtocolMessage;
import cc.alcina.extras.dev.component.remote.protocol.ProtocolMessage.Startup;
import cc.alcina.extras.dev.component.remote.protocol.RemoteComponentRequest;
import cc.alcina.extras.dev.component.remote.protocol.RemoteComponentResponse;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.servlet.dom.Environment;

/*
 * FIXME - beans1x5 - package
 */
@Registration.NonGenericSubtypes(ProtocolMessageHandler.class)
public abstract class ProtocolMessageHandler<PM extends ProtocolMessage> {
	public abstract void handle(RemoteComponentRequest request,
			RemoteComponentResponse response, Environment env, PM message);

	public boolean isValidateClientInstanceUid() {
		return true;
	}

	public static class StartupHandler
			extends ProtocolMessageHandler<ProtocolMessage.Startup> {
		@Override
		public void handle(RemoteComponentRequest request,
				RemoteComponentResponse response, Environment env,
				Startup message) {
			env.initialiseClient(request.session);
			env.applyMutations(message.mutations);
			/*
			 * init envoironment (po
			 * 
			 * 
			 * 
			 * replay dom
			 */
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