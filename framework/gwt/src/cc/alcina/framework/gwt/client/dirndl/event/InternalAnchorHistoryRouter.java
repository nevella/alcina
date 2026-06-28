package cc.alcina.framework.gwt.client.dirndl.event;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.Url;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.Click;

public interface InternalAnchorHistoryRouter extends Click.Handler {
	@Override
	default void onClick(Click event) {
		NativeEvent nativeEvent = event.getContext()
				.getOriginatingNativeEvent();
		EventTarget eventTarget = nativeEvent.getEventTarget();
		if (Element.is(eventTarget)) {
			Element elem = Element.as(eventTarget);
			String href = elem.asDomNode().ancestors().orSelf()
					.match(n -> n.has("href")).map(n -> n.attr("href"))
					.orElse("");
			if (Ax.isBlank(href)) {
				return;
			}
			Url url = Url.parse(Window.Location.getHref());
			String strUrlEndingAtPath = url.strUrlEndingAtPath();
			if (href.startsWith(strUrlEndingAtPath)) {
				href = href.substring(strUrlEndingAtPath.length());
			}
			if (href.startsWith("/")) {
				History.newItem(href);
			}
		}
	}
}