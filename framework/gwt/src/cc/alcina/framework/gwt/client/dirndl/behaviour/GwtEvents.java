package cc.alcina.framework.gwt.client.dirndl.behaviour;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Widget;

public class GwtEvents {
	public static class Attach extends NodeEvent {
		@Override
		protected HandlerRegistration bind0(Widget widget) {
			return widget.addAttachHandler(this::fireEvent);
		}
	};
	public static class ValueChange extends NodeEvent {
		@Override
		protected HandlerRegistration bind0(Widget widget) {
			return widget.addHandler(this::fireEvent, ValueChangeEvent.getType());
		}
	};
}
