package cc.alcina.framework.gwt.client.logic;

import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.event.logical.shared.AttachEvent.Handler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.InsertPanel.ForIsWidget;
import com.google.gwt.user.client.ui.Widget;

public class WidgetByElementTracker implements Handler {
	private WidgetByElementTracker() {
		super();
	}

	private static WidgetByElementTracker theInstance;

	public static WidgetByElementTracker get() {
		if (theInstance == null) {
			theInstance = new WidgetByElementTracker();
		}
		return theInstance;
	}

	public void appShutdown() {
		theInstance = null;
	}

	Map<Element, Widget> perElementWidgets = new LinkedHashMap<Element, Widget>();

	Map<Element, HandlerRegistration> perElementHandlers = new LinkedHashMap<Element, HandlerRegistration>();

	public void register(Widget w) {
		registerOrUnregister(w, true);
	}

	public Widget getWidget(Element e) {
		while (e != null) {
			Widget w = perElementWidgets.get(e);
			if (w != null) {
				return w;
			}
			e = e.getParentElement();
		}
		return null;
	}

	private void registerOrUnregister(Widget source, boolean register) {
		Element element = source.getElement();
		if (register) {
			if (!perElementWidgets.containsKey(element)) {
				perElementWidgets.put(element, source);
				HandlerRegistration handler = source.addAttachHandler(this);
				perElementHandlers.put(element, handler);
			}
		} else {
			perElementWidgets.remove(element);
			perElementHandlers.get(element).removeHandler();
			perElementHandlers.remove(element);
		}
	}

	@Override
	public void onAttachOrDetach(AttachEvent event) {
		Widget source = (Widget) event.getSource();
		registerOrUnregister(source, event.isAttached());
	}

	public Widget findBestWidgetForElement(ForIsWidget fiw, Element elt) {
		for (int i = 0; i < fiw.getWidgetCount(); i++) {
			Widget w = fiw.getWidget(i);
			if (elt == w.getElement()) {
				return w;
			}
		}
		return (Widget) fiw;
	}
}
