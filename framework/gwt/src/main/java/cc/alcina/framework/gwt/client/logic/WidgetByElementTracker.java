package cc.alcina.framework.gwt.client.logic;

import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.event.logical.shared.AttachEvent.Handler;
import com.google.gwt.user.client.ui.InsertPanel.ForIsWidget;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

@Registration.Singleton
public class WidgetByElementTracker implements Handler {
	public static WidgetByElementTracker get() {
		return Registry.impl(WidgetByElementTracker.class);
	}

	Map<Element, Widget> perElementWidgets = new LinkedHashMap<Element, Widget>();

	public Widget findBestWidgetForElement(ForIsWidget fiw, Element elt) {
		for (int i = 0; i < fiw.getWidgetCount(); i++) {
			Widget w = fiw.getWidget(i);
			if (elt == w.getElement()) {
				return w;
			}
		}
		return (Widget) fiw;
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

	@Override
	public void onAttachOrDetach(AttachEvent event) {
		Widget source = (Widget) event.getSource();
		Element element = source.getElement();
		if (event.isAttached()) {
			perElementWidgets.put(element, source);
		} else {
			perElementWidgets.remove(element);
		}
	}

	public void register(Widget w) {
		new WidgetAttachHandler(this, w);
	}

	private static class WidgetAttachHandler implements Handler {
		private final WidgetByElementTracker tracker;

		public WidgetAttachHandler(
				WidgetByElementTracker widgetByElementTracker, Widget w) {
			this.tracker = widgetByElementTracker;
			w.addAttachHandler(this);
		}

		@Override
		public void onAttachOrDetach(AttachEvent event) {
			tracker.onAttachOrDetach(event);
		}
	}
}
