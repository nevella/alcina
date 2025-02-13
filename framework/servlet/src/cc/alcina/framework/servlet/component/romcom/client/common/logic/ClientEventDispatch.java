package cc.alcina.framework.servlet.component.romcom.client.common.logic;

import java.util.Objects;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.AttachId;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.DomEventData;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.HrefElement;
import com.google.gwt.user.client.Event;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.servlet.component.Feature_RemoteObjectComponent;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message.DomEventMessage;

class ClientEventDispatch {
	@Feature.Ref(Feature_RemoteObjectComponent.Feature_ClientEventThrottling.class)
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
			case "mousewheel":
			case "scroll":
				return;
			}
		}
		if (event.getEventTarget().isDetachedElement()) {
			return;
		}
		if (currentEventMessage == null) {
			currentEventMessage = new Message.DomEventMessage();
			Scheduler.get().scheduleDeferred(() -> {
				sendCurrentEventMessage();
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
			boolean mouseDownEvent = Objects.equals(eventType,
					BrowserEvents.MOUSEDOWN);
			switch (eventType) {
			case BrowserEvents.FOCUS:
			case BrowserEvents.BLUR:
			case BrowserEvents.FOCUSIN:
			case BrowserEvents.FOCUSOUT:
				focusEvent = true;
				break;
			}
			DomNode eventTargetDomNode = elem.asDomNode();
			if (!focusEvent) {
				if (eventTargetDomNode.ancestors().has("form")) {
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
					if (elem.hasTagName("a")) {
						String href = elem.getAttribute("href");
						if (Ax.isBlank(href) || href.equals("#")) {
							event.preventDefault();
						}
					}
				}
			}
			// TODO - why (why just this)? should probably be an attrbehavior.
			// In general, a.withoutLink shd probably preventdefault
			if (mouseDownEvent) {
				if (eventTargetDomNode.ancestors().has("choice-suggestions")) {
					event.preventDefault();
				}
			}
			/*
			 * Propagate value + inputValue property changes
			 */
			if (Objects.equals(eventType, "change")) {
				eventData.value = elem.getPropertyString("value");
				if (eventTargetDomNode.nameIs("select")) {
					eventData.selectedIndex = elem
							.getPropertyInt("selectedIndex");
				}
			}
			if (Objects.equals(eventType, "input")) {
				eventData.inputValue = elem.getPropertyString("value");
			}
		}
		if (event.getType().equals(BrowserEvents.PAGEHIDE)) {
			// immediate dispatch
			sendCurrentEventMessage();
		}
	}

	static DomEventMessage currentEventMessage = null;

	static void sendCurrentEventMessage() {
		// may have been removed by a 'fire-now'
		if (currentEventMessage == null) {
			return;
		}
		currentEventMessage.eventContext = new DomEventContextGenerator()
				.generate();
		Ax.out(currentEventMessage);
		ClientRpc.send(currentEventMessage);
		currentEventMessage = null;
	}
}
