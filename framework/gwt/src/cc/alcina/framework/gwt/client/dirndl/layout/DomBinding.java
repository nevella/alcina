package cc.alcina.framework.gwt.client.dirndl.layout;

import com.google.common.base.Preconditions;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node.NodeEventBinding;

public abstract class DomBinding<E extends NodeEvent> implements EventHandler {
	protected HandlerRegistration handlerRegistration;

	NodeEventBinding nodeEventBinding;

	public void bind(Widget widget, Object model, boolean bind) {
		if (!bind) {
			Preconditions.checkState(handlerRegistration != null);
			handlerRegistration.removeHandler();
			handlerRegistration = null;
		} else {
			Preconditions.checkState(handlerRegistration == null);
			handlerRegistration = bind0(widget, model);
		}
	}

	// most bindings won't require the model - but bindings the vary based on
	// model characteristics (e.g. IsModal) will
	protected HandlerRegistration bind0(Widget widget, Object model) {
		return bind1(widget);
	}

	protected abstract HandlerRegistration bind1(Widget widget);

	protected void fireEvent(GwtEvent gwtEvent) {
		nodeEventBinding.onEvent(gwtEvent);
	}
}