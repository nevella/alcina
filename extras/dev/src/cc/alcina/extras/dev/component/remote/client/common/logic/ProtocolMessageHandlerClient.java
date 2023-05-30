package cc.alcina.extras.dev.component.remote.client.common.logic;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.LocalDom;
import com.google.gwt.user.client.Window;

import cc.alcina.extras.dev.component.remote.client.RemoteComponentState;
import cc.alcina.extras.dev.component.remote.protocol.ProtocolMessage;
import cc.alcina.extras.dev.component.remote.protocol.ProtocolMessage.InvalidClientUidException;
import cc.alcina.extras.dev.component.remote.protocol.RemoteComponentResponse;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.util.Ax;

/*
 * FIXME - beans1x5 - package protected
 */
@Registration.NonGenericSubtypes(ProtocolMessageHandlerClient.class)
public abstract class ProtocolMessageHandlerClient<PM extends ProtocolMessage> {
	public abstract void handle(RemoteComponentResponse response, PM message);

	public static class BeginAwaitLoopHandler extends
			ProtocolMessageHandlerClient<ProtocolMessage.BeginAwaitLoop> {
		@Override
		public void handle(RemoteComponentResponse response,
				ProtocolMessage.BeginAwaitLoop message) {
			Document.get().setTitle("start");
			Ax.out("bruce");
			RemoteComponentClientRpc.send(new ProtocolMessage.AwaitRemote());
		}
	}

	public static class MutationsHandler
			extends ProtocolMessageHandlerClient<ProtocolMessage.Mutations> {
		@Override
		public void handle(RemoteComponentResponse response,
				ProtocolMessage.Mutations message) {
			LocalDom.pathRefRepresentations()
					.applyMutations(message.domMutations, true);
			Document.get().setTitle("bruce");
			Ax.err("jaaaaa");
		}
	}

	public static class ProcessingExceptionHandler extends
			ProtocolMessageHandlerClient<ProtocolMessage.ProcessingException> {
		@Override
		public void handle(RemoteComponentResponse response,
				ProtocolMessage.ProcessingException message) {
			RemoteComponentState.get().finished = true;
			String clientMessage = Ax.format(
					"Exception occurred - ui stopped: %s",
					message.exceptionMessage);
			if (message.exceptionClass() == InvalidClientUidException.class) {
				clientMessage = "This component client (tab) has ben superseded "
						+ "by a newer access to this component url. \n\nPlease use the newer tab";
			}
			// FIXME - remcon - prettier?
			Window.alert(clientMessage);
		}
	}
}