package cc.alcina.framework.gwt.client.logic;

import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.event.logical.shared.AttachEvent.Handler;
import com.google.gwt.event.shared.HandlerRegistration;
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
		Element element = w.getElement();
		perElementWidgets.put(element, w);
		HandlerRegistration handler = w.addAttachHandler(this);
		perElementHandlers.put(element, handler);
	}

	public Widget getWidget(Element e) {
		while(e!=null){
			Widget w = perElementWidgets.get(e);
			if(w!=null){
				return w;
			}
			e=e.getParentElement();
		}
		return null;
	}

	@Override
	public void onAttachOrDetach(AttachEvent event) {
		if (!event.isAttached()) {
			Widget source = (Widget) event.getSource();
			Element element = source.getElement();
			perElementWidgets.remove(element);
			perElementHandlers.get(element).removeHandler();
			perElementHandlers.remove(element);
		}
	}
}
