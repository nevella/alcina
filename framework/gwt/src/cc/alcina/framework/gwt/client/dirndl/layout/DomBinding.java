package cc.alcina.framework.gwt.client.dirndl.layout;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node.NodeEventBinding;

public abstract class DomBinding<E extends NodeEvent> implements EventHandler {
	protected HandlerRegistration handlerRegistration;

	NodeEventBinding nodeEventBinding;

	public void bind(Widget widget, boolean bind) {
		if (!bind) {
			if (handlerRegistration != null) {
				handlerRegistration.removeHandler();
				handlerRegistration = null;
			}
		} else {
			if (handlerRegistration != null) {
				return;
			}
			handlerRegistration = bind0(widget);
		}
	}

	public abstract HandlerRegistration bind0(Widget widget);

	protected void fireEvent(GwtEvent gwtEvent) {
		nodeEventBinding.onEvent(gwtEvent);
	}
}