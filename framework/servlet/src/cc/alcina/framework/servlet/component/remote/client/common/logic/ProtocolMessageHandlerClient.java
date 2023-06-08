package cc.alcina.framework.servlet.component.remote.client.common.logic;

import com.google.gwt.dom.client.DomEventData;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.LocalDom;
import com.google.gwt.dom.client.Pathref;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.servlet.component.remote.client.RemoteComponentState;
import cc.alcina.framework.servlet.component.remote.protocol.ProtocolMessage;
import cc.alcina.framework.servlet.component.remote.protocol.ProtocolMessage.DomEventMessage;
import cc.alcina.framework.servlet.component.remote.protocol.ProtocolMessage.InvalidClientUidException;
import cc.alcina.framework.servlet.component.remote.protocol.RemoteComponentResponse;

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
			ClientRpc.send(new ProtocolMessage.AwaitRemote());
		}
	}

	public static class MutationsHandler
			extends ProtocolMessageHandlerClient<ProtocolMessage.Mutations> {
		@Override
		public void handle(RemoteComponentResponse response,
				ProtocolMessage.Mutations message) {
			LocalDom.pathRefRepresentations()
					.applyMutations(message.domMutations, true);
			message.eventMutations.forEach(m -> {
				Element elem = (Element) m.path.node();
				if (m.eventBits == -1) {
					DOM.sinkBitlessEvent(elem, m.eventTypeName);
				} else {
					DOM.sinkEvents(elem, m.eventBits);
				}
				elem.uiObjectListener = new DispatchListener(elem);
			});
			if (message.locationMutation != null) {
				History.newItem(message.locationMutation.hash);
			}
		}

		static class DispatchListener implements EventListener {
			private Element elem;

			public DispatchListener(Element elem) {
				this.elem = elem;
			}

			@Override
			public void onBrowserEvent(Event event) {
				// just send the lowest event receiver - things will bubble from
				// here
				DomEventMessage message = new ProtocolMessage.DomEventMessage();
				message.data = new DomEventData();
				message.data.event = event.serializableForm();
				message.data.firstReceiver = Pathref.forNode(elem);
				event.stopPropagation();
				event.preventDefault();
				ClientRpc.send(message, true);
			}
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