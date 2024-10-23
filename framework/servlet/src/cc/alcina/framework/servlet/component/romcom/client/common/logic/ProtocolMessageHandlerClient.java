package cc.alcina.framework.servlet.component.romcom.client.common.logic;

import java.util.Objects;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.AttachId;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.DomEventData;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.HrefElement;
import com.google.gwt.dom.client.LocalDom;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeJso;
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

/*
 * FIXME - beans1x5 - package protected
 */
@Registration.NonGenericSubtypes(ProtocolMessageHandlerClient.class)
public abstract class ProtocolMessageHandlerClient<PM extends Message>
		implements Message.Handler<PM> {
	public abstract void handle(HandlerContext handlerContext, PM message);

	public interface HandlerContext {
	}

	public static class BeginAwaitLoopHandler
			extends ProtocolMessageHandlerClient<Message.BeginAwaitLoop> {
		@Override
		public void handle(HandlerContext handlerContext,
				Message.BeginAwaitLoop message) {
			ClientRpc.beginAwaitLoop();
		}
	}

	public static class InvokeHandler
			extends ProtocolMessageHandlerClient<Message.Invoke> {
		@Override
		public void handle(HandlerContext handlerContext,
				Message.Invoke message) {
			AttachId path = message.path;
			Node node = path == null ? null : path.node();
			Message.InvokeResponse responseMessage = new Message.InvokeResponse();
			responseMessage.id = message.id;
			// if the server requested sync, return that in the response (since
			// the protocol is stateless)
			responseMessage.sync = message.sync;
			Object result = null;
			try {
				if (message.methodName != null) {
					result = Reflections.at(node).invoke(node,
							message.methodName, message.argumentTypes,
							message.arguments, message.flags);
				} else {
					Result scriptResult = new Result();
					String script = message.javascript;
					invokeJs(scriptResult, message.jsResponseType.name(), node,
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
					return AttachId.forNode(node.node());
				} else if (string != null) {
					return string;
				} else {
					return null;
				}
			}
		}

		static final native void invokeJs(Result scriptResult,
				String responseType, Node node, String script) /*-{
			var arg = node;
			var ret = eval(script);
			switch(responseType){
				case "_void":
				break;
				case "node_jso":
				scriptResult.@cc.alcina.framework.servlet.component.romcom.client.common.logic.ProtocolMessageHandlerClient.InvokeHandler.Result::node = ret;
				break; 
				case "string":
				scriptResult.@cc.alcina.framework.servlet.component.romcom.client.common.logic.ProtocolMessageHandlerClient.InvokeHandler.Result::string = ret;
				break; 
				default:
				throw "unsupported responseType "+responseType;
			}
			}-*/;
	}

	static DomEventMessage currentEventMessage = null;

	static void dispatchEventMessage(Event event, Element listenerElement,
			boolean preview) {
		/*
		 * FIXME - shouldn't need to dedpue
		 */
		if (preview) {
			switch (event.getType().toLowerCase()) {
			case "mouseout":
			case "mouseenter":
			case "mouseleave":
			case "mousemove":
			case "mouseover":
				return;
			}
		}
		if (event.getEventTarget().isDetachedElement()) {
			return;
		}
		if (currentEventMessage == null) {
			currentEventMessage = new Message.DomEventMessage();
			Scheduler.get().scheduleDeferred(() -> {
				// may have been removed by a 'fire-now'
				if (currentEventMessage == null) {
					return;
				}
				ClientRpc.send(currentEventMessage);
				currentEventMessage = null;
			});
		}
		DomEventData eventData = new DomEventData();
		currentEventMessage.events.add(eventData);
		eventData.event = event.serializableForm();
		eventData.preview = preview;
		/*
		 * Unused, informative only. This is the element that has a browser
		 * listener - but those will mostly be coalesced.
		 * 
		 * The element that the event will be fired from (server-side) is the
		 * eventData.eventTarget
		 */
		eventData.firstReceiver = listenerElement == null ? null
				: AttachId.forNode(listenerElement);
		String eventType = event.getType();
		if (Element.is(event.getEventTarget())) {
			Element elem = Element.as(event.getEventTarget());
			/*
			 * Cancel a few events if they're in a form (assume form auto-submit
			 * just not wanted). This is because - since event handling is async
			 * - we can't wait for the remote handler to call preventDefault
			 * 
			 * 
			 */
			boolean focusEvent = false;
			switch (eventType) {
			case BrowserEvents.FOCUS:
			case BrowserEvents.BLUR:
			case BrowserEvents.FOCUSIN:
			case BrowserEvents.FOCUSOUT:
				focusEvent = true;
				break;
			}
			if (!focusEvent) {
				if (elem.asDomNode().ancestors().has("form")) {
					boolean explicitPrevent = false;
					if (eventType.equals("keydown") && event.getKeyCode() == 13
							&& !elem.hasTagName("textarea")) {
						explicitPrevent = true;
					}
					if (elem instanceof HrefElement
							&& !((HrefElement) elem).hasLinkHref()) {
						explicitPrevent = true;
					}
					if (explicitPrevent) {
						event.preventDefault();
					}
				}
				// prevent default action on <a> with no href
				// TODO - space on <a> with no href?
				if (eventType.equals("click") || eventType.equals("keydown")) {
					if (Ax.isBlank(elem.getAttribute("href"))
							&& elem.hasTagName("a")) {
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
		if (event.getType().equals(BrowserEvents.PAGEHIDE)) {
			// immediate dispatch
			ClientRpc.send(currentEventMessage);
			currentEventMessage = null;
		}
	}

	public static class MutationsHandler
			extends ProtocolMessageHandlerClient<Message.Mutations> {
		@Override
		public void handle(HandlerContext handlerContext,
				Message.Mutations message) {
			LocalDom.attachIdRepresentations()
					.applyMutations(message.domMutations, true);
			message.eventSystemMutations.forEach(m -> {
				try {
					Element elem = (Element) m.nodeId.node();
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
				dispatchEventMessage(event, elem, false);
			}
		}
	}

	public static class ProcessingExceptionHandler
			extends ProtocolMessageHandlerClient<Message.ProcessingException> {
		@Override
		public boolean isHandleOutOfBand() {
			return true;
		}

		@Override
		public void handle(HandlerContext handlerContext,
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
		public void handle(HandlerContext handlerContext,
				Message.PersistSettings message) {
			RemoteComponentSettings.setSettings(message.value);
		}
	}
}