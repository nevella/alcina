package cc.alcina.framework.gwt.client.dirndl.behaviour;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Widget;

public class DomEvents {
	public static class Click extends NodeEvent {
		@Override
		protected HandlerRegistration bind0(Widget widget) {
			return widget.addDomHandler(this::fireEvent, ClickEvent.getType());
		}
	};
	
	public static class Change extends NodeEvent {
		@Override
		protected HandlerRegistration bind0(Widget widget) {
			return widget.addDomHandler(this::fireEvent, ChangeEvent.getType());
		}
	};
}
