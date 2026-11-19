package cc.alcina.framework.gwt.client.dirndl.event;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.place.shared.Place;

import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.Click;
import cc.alcina.framework.gwt.client.place.BasePlace;
import cc.alcina.framework.gwt.client.place.RegistryHistoryMapper;
import cc.alcina.framework.gwt.client.util.WidgetUtils;

public interface PushStateHrefHandler extends DomEvents.Click.Handler {
	@Override
	default void onClick(Click event) {
		ClickEvent gwtEvent = (ClickEvent) event.getContext()
				.getOriginatingGwtEvent();
		EventTarget eventTarget = gwtEvent.getNativeEvent().getEventTarget();
		if (gwtEvent.getNativeButton() == NativeEvent.BUTTON_LEFT
				&& Element.is(eventTarget)) {
			Element elem = Element.as(eventTarget);
			if (elem.getTagName().equalsIgnoreCase("a")) {
				String href = elem.getAttribute("href");
				if (href.startsWith("/")) {
					Place place = RegistryHistoryMapper.get()
							.getPlaceIfParseable(href).orElse(null);
					if (place instanceof BasePlace) {
						WidgetUtils.squelchCurrentEvent();
						((BasePlace) place).go();
					}
				}
			}
		}
	}
}
