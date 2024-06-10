package cc.alcina.framework.servlet.component.romcom.client.common.logic;

import java.util.Objects;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.DomEventData;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.LocalDom;
import com.google.gwt.dom.client.NodeJso;
import com.google.gwt.dom.client.Pathref;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.servlet.component.romcom.client.RemoteObjectModelComponentState;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.InvalidClientException;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message.DomEventMessage;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message.ExceptionTransport;
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
			ClientRpc.beginAwaitLoop();
		}
	}

	public static class InvokeHandler
			extends ProtocolMessageHandlerClient<Message.Invoke> {
		@Override
		public void handle(RemoteComponentResponse response,
				Message.Invoke message) {
			Pathref path = message.path;
			Element elem = path == null ? null : (Element) path.node();
			Message.InvokeResponse responseMessage = new Message.InvokeResponse();
			responseMessage.id = message.id;
			// if the server requested sync, return that in the response (since
			// the protocol is stateless)
			responseMessage.sync = message.sync;
			Object result = null;
			try {
				if (message.methodName != null) {
					result = Reflections.at(elem).invoke(elem,
							message.methodName, message.argumentTypes,
							message.arguments, message.flags);
				} else {
					Result scriptResult = new Result();
					String script = message.javascript;
					invokeJs(scriptResult, message.jsResponseType.name(), elem,
							script);
					result = scriptResult.asObject();
				}
				responseMessage.response = result;
			} catch (Throwable e) {
				Window.alert(CommonUtils.toSimpleExceptionMessage(e));
				e.printStackTrace();
				responseMessage.exception = new ExceptionTransport(e);
			}
			ClientRpc.send(responseMessage);
		}

		static class Result {
			NodeJso node;

			String string;

			public Object asObject() {
				if (node != null) {
					return Pathref.forNode(node.node());
				} else if (string != null) {
					return string;
				} else {
					return null;
				}
			}
		}

		static final native void invokeJs(Result scriptResult,
				String responseType, Element elem, String script) /*-{
			var arg = elem;
			var ret = eval(script);
			switch(responseTypeName){
				case "void":
				break;
				case "node_jso":
				scriptResult.@cc.alcina.framework.servlet.component.romcom.client.common.logic.ProtocolMessageHandlerClient.InvokeHandler.Result::node = ret;
				break; 
				case "string":
				scriptResult.@cc.alcina.framework.servlet.component.romcom.client.common.logic.ProtocolMessageHandlerClient.InvokeHandler.Result::string = ret;
				break; 
			}
			}-*/;
	}

	static DomEventMessage currentEventMessage = null;

	static void dispatchEventMessage(Event event, Element listenerElement,
			boolean preview, boolean window) {
		/*
		 * FIXME - shouldn't need to dedpue
		 */
		if (preview) {
			switch (event.getType().toLowerCase()) {
			case "mouseout":
			case "mousedown":
			case "mouseenter":
			case "mouseleave":
			case "mousemove":
			case "mouseover":
				return;
			}
		}
		if (currentEventMessage == null) {
			currentEventMessage = new Message.DomEventMessage();
			Scheduler.get().scheduleDeferred(() -> {
				// may have been removed by a 'fire-now'
				if (currentEventMessage == null) {
					return;
				}
				ClientRpc.send(currentEventMessage, true);
				currentEventMessage = null;
			});
		}
		DomEventData eventData = new DomEventData();
		currentEventMessage.events.add(eventData);
		eventData.event = event.serializableForm();
		eventData.preview = preview;
		eventData.window = window;
		/*
		 * Unused, informative only. This is the element that has a browser
		 * listener - but those will mostly be coalesced.
		 * 
		 * The element that the event will be fired from (server-side) is the
		 * eventData.eventTarget
		 */
		eventData.firstReceiver = listenerElement == null ? null
				: Pathref.forNode(listenerElement);
		String eventType = event.getType();
		if (Element.is(event.getEventTarget())) {
			Element elem = Element.as(event.getEventTarget());
			/*
			 * Cancel a few events if they're in a form (assume form auto-submit
			 * just not wanted)
			 */
			if (elem.asDomNode().ancestors().has("form")) {
				if (eventType.equals("keydown") && event.getKeyCode() == 13) {
					event.preventDefault();
				}
				if (eventType.equals("click")) {
					if (!(elem.hasAttribute("href") && elem.hasTagName("a"))) {
						event.preventDefault();
					}
				}
			}
			/*
			 * Propagate value + inputValue property changes
			 */
			if (Objects.equals(eventType, "change")) {
				eventData.value = elem.getPropertyString("value");
			}
			if (Objects.equals(eventType, "input")) {
				eventData.inputValue = elem.getPropertyString("value");
			}
		}
		if (window) {
			ClientRpc.send(currentEventMessage, true);
			currentEventMessage = null;
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
				try {
					Element elem = (Element) m.path.node();
					if (m.eventBits == -1) {
						DOM.sinkBitlessEvent(elem, m.eventTypeName);
					} else {
						DOM.sinkEvents(elem, m.eventBits);
					}
					elem.eventListener = new DispatchListener(elem);
				} catch (Throwable e) {
					e.printStackTrace();
				}
			});
			if (message.locationMutation != null) {
				try {
					RemoteObjectModelComponentState
							.get().firingLocationMutation = true;
					History.newItem(message.locationMutation.hash);
				} finally {
					RemoteObjectModelComponentState
							.get().firingLocationMutation = false;
				}
			}
		}

		static class DispatchListener implements EventListener {
			private Element elem;

			public DispatchListener(Element elem) {
				this.elem = elem;
			}

			@Override
			public void onBrowserEvent(Event event) {
				dispatchEventMessage(event, elem, false, false);
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

	public static class PersistSettingsHandler
			extends ProtocolMessageHandlerClient<Message.PersistSettings> {
		@Override
		public void handle(RemoteComponentResponse response,
				Message.PersistSettings message) {
			RemoteComponentSettings.setSettings(message.value);
		}
	}
}