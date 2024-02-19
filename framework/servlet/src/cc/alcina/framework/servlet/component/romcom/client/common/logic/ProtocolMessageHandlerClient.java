package cc.alcina.framework.servlet.component.romcom.client.common.logic;

import java.util.Objects;

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
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.servlet.component.romcom.client.RemoteObjectModelComponentState;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.InvalidClientException;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message.DomEventMessage;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentResponse;

/*
 * FIXME - beans1x5 - package protected
 */
@Registration.NonGenericSubtypes(ProtocolMessageHandlerClient.class)
public abstract class ProtocolMessageHandlerClient<PM extends Message> {
	public abstract void handle(RemoteComponentResponse response, PM message);

	public static class BeginAwaitLoopHandler
			extends ProtocolMessageHandlerClient<Message.BeginAwaitLoop> {
		@Override
		public void handle(RemoteComponentResponse response,
				Message.BeginAwaitLoop message) {
			ClientRpc.sendAwaitRemoteMessage();
		}
	}

	public static class InvokeHandler
			extends ProtocolMessageHandlerClient<Message.Invoke> {
		@Override
		public void handle(RemoteComponentResponse response,
				Message.Invoke message) {
			Element elem = (Element) message.path.node();
			Message.InvokeResponse responseMessage = new Message.InvokeResponse();
			responseMessage.id = message.id;
			try {
				Object result = Reflections.at(elem).invoke(elem,
						message.methodName, message.argumentTypes,
						message.arguments);
				responseMessage.response = result;
			} catch (Exception e) {
				e.printStackTrace();
				responseMessage.exception = e;
			}
			ClientRpc.send(responseMessage);
		}
	}

	public static class MutationsHandler
			extends ProtocolMessageHandlerClient<Message.Mutations> {
		@Override
		public void handle(RemoteComponentResponse response,
				Message.Mutations message) {
			LocalDom.pathRefRepresentations()
					.applyMutations(message.domMutations, true);
			message.eventMutations.forEach(m -> {
				Element elem = (Element) m.path.node();
				if (m.eventBits == -1) {
					DOM.sinkBitlessEvent(elem, m.eventTypeName);
				} else {
					DOM.sinkEvents(elem, m.eventBits);
				}
				elem.eventListener = new DispatchListener(elem);
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
				DomEventMessage message = new Message.DomEventMessage();
				Ax.err(event.getType());
				message.data = new DomEventData();
				message.data.event = event.serializableForm();
				message.data.firstReceiver = Pathref.forNode(elem);
				event.stopPropagation();
				if (Element.is(event.getEventTarget())) {
					Element elem = Element.as(event.getEventTarget());
					String eventType = event.getType();
					/*
					 * Cancel a few events if they're in a form (assume form
					 * auto-submit just not wanted)
					 */
					if (elem.asDomNode().ancestors().has("form")) {
						if (eventType.equals("keydown")
								&& event.getKeyCode() == 13) {
							event.preventDefault();
						}
						if (eventType.equals("click")) {
							if (!(elem.hasAttribute("href")
									&& elem.hasTagName("a"))) {
								event.preventDefault();
							}
						}
					}
					/*
					 * Propagate value property changes
					 */
					if (Objects.equals(eventType, "change")) {
						message.data.value = elem.getPropertyString("value");
					}
				}
				ClientRpc.send(message, true);
			}
		}
	}

	public static class ProcessingExceptionHandler
			extends ProtocolMessageHandlerClient<Message.ProcessingException> {
		@Override
		public void handle(RemoteComponentResponse response,
				Message.ProcessingException message) {
			RemoteObjectModelComponentState.get().finished = true;
			Exception protocolException = message.protocolException;
			String clientMessage = Ax.format(
					"Exception occurred - ui stopped: %s",
					message.exceptionMessage);
			if (protocolException instanceof InvalidClientException) {
				InvalidClientException invalidClientException = (InvalidClientException) protocolException;
				switch (invalidClientException.action) {
				case REFRESH:
					Window.Location.reload();
					return;
				}
			}
			// FIXME - remcon - prettier?
			Window.alert(clientMessage);
		}
	}
}