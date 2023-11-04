package cc.alcina.framework.gwt.client.util;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Event.NativePreviewHandler;

import cc.alcina.framework.common.client.util.Ax;

public class GlobalKeyboardShortcuts implements NativePreviewHandler {
	private List<GlobalKeyboardShortcuts.Handler> handlers = new ArrayList<>();

	public void deltaHandler(GlobalKeyboardShortcuts.Handler handler,
			boolean add) {
		if (add) {
			handlers.add(handler);
		} else {
			handlers.remove(handler);
		}
	}

	@Override
	public void onPreviewNativeEvent(NativePreviewEvent event) {
		// FIXME - just the nativeEvent, since LocalDom caches all the
		// properties (or should)
		NativeEvent nativeEvent = event.getNativeEvent();
		String type = nativeEvent.getType();
		boolean altKey = nativeEvent.getAltKey();
		boolean shiftKey = nativeEvent.getShiftKey();
		int keyCode = nativeEvent.getKeyCode();
		int charCode = nativeEvent.getCharCode();
		EventTarget eventTarget = nativeEvent.getEventTarget();
		switch (type) {
		case "keypress":
		case "keydown":
		case "keyup":
			if (Element.is(eventTarget)) {
				try {
					Element elem = Element.as(eventTarget);
					String name = elem.getTagName();
					switch (name.toLowerCase()) {
					case "input":
					case "checkbox":
					case "select":
					case "textarea":
						return;
					}
					if (elem.asDomNode().ancestors().orSelf()
							.has(a -> a.has("contenteditable"))) {
						return;
					}
				} catch (Exception e) {
					// if under webdriver, possibly event source has already
					// been
					// removed
					// FIXME - dirndl 1x3 - narrow the catch [devex, dirndl
					// exception if not WD][via topic]
					Ax.simpleExceptionOut(e);
				}
			}
			break;
		default:
			return;
		}
		handlers.forEach(handler -> handler.checkShortcut(event, nativeEvent,
				type, altKey, shiftKey, keyCode));
	}

	@FunctionalInterface
	public static interface Handler {
		public void checkShortcut(NativePreviewEvent event,
				NativeEvent nativeEvent, String type, boolean altKey,
				boolean shiftKey, int keyCode);
	}
}