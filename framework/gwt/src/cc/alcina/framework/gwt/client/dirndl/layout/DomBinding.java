package cc.alcina.framework.gwt.client.dirndl.layout;

import com.google.common.base.Preconditions;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.process.ProcessObservers;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node.NodeEventBinding;

@Registration.NonGenericSubtypes(DomBinding.class)
public abstract class DomBinding<E extends NodeEvent> implements EventHandler {
	protected HandlerRegistration handlerRegistration;

	NodeEventBinding nodeEventBinding;

	public void bind(Widget widget, Object model, boolean bind) {
		if (!bind) {
			Preconditions.checkState(handlerRegistration != null);
			ProcessObservers.publish(DirndlProcess.DomBindingUnbind.class,
					() -> new DirndlProcess.DomBindingUnbind(nodeEventBinding));
			handlerRegistration.removeHandler();
			handlerRegistration = null;
		} else {
			Preconditions.checkState(handlerRegistration == null);
			ProcessObservers.publish(DirndlProcess.DomBindingBind.class,
					() -> new DirndlProcess.DomBindingBind(nodeEventBinding));
			handlerRegistration = bind0(widget, model);
			Preconditions.checkState(handlerRegistration != null);
		}
	}

	// most bindings won't require the model - but bindings that vary based on
	// model characteristics (e.g. IsModal) will
	protected HandlerRegistration bind0(Widget widget, Object model) {
		return bind1(widget);
	}

	/*
	 * Some subclasses bind to non-handler-registration sources (e.g.
	 * intersectionobserver), so HandlerRegistration is somewhat abused (what is
	 * really bound is an Observer, and the return is an ObserverRegistration)
	 */
	protected abstract HandlerRegistration bind1(Widget widget);

	protected void fireEvent(GwtEvent gwtEvent) {
		ProcessObservers.publish(DirndlProcess.DomBindingFire.class,
				() -> new DirndlProcess.DomBindingFire(nodeEventBinding,
						gwtEvent));
		nodeEventBinding.onEvent(gwtEvent);
	}
}