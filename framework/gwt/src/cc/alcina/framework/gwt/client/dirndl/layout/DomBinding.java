package cc.alcina.framework.gwt.client.dirndl.layout;

import com.google.common.base.Preconditions;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.Registration.AllSubtypesClient;
import cc.alcina.framework.common.client.process.ProcessObservers;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent;

@Registration.NonGenericSubtypes(DomBinding.class)
public abstract class DomBinding<E extends NodeEvent>
		implements EventHandler, AllSubtypesClient {
	protected HandlerRegistration handlerRegistration;

	NodeEventBinding nodeEventBinding;

	public void bind(Element element, Object model, boolean bind) {
		if (!bind) {
			Preconditions.checkState(handlerRegistration != null);
			ProcessObservers.publish(DirndlObservables.DomBindingUnbind.class,
					() -> new DirndlObservables.DomBindingUnbind(
							nodeEventBinding));
			handlerRegistration.removeHandler();
			handlerRegistration = null;
		} else {
			Preconditions.checkState(handlerRegistration == null);
			ProcessObservers.publish(DirndlObservables.DomBindingBind.class,
					() -> new DirndlObservables.DomBindingBind(
							nodeEventBinding));
			handlerRegistration = bind0(element, model);
			Preconditions.checkState(handlerRegistration != null);
		}
	}

	// most bindings won't require the model - but bindings that vary based on
	// model characteristics (e.g. IsModal) will
	protected HandlerRegistration bind0(Element element, Object model) {
		return bind1(element);
	}

	/*
	 * Some subclasses bind to non-handler-registration sources (e.g.
	 * intersectionobserver), so HandlerRegistration is somewhat abused (what is
	 * really bound is an Observer, and the return is an ObserverRegistration)
	 */
	protected abstract HandlerRegistration bind1(Element element);

	protected void fireEvent(GwtEvent gwtEvent) {
		ProcessObservers.publish(DirndlObservables.DomBindingFire.class,
				() -> new DirndlObservables.DomBindingFire(nodeEventBinding,
						gwtEvent));
		nodeEventBinding.onEvent(gwtEvent);
	}
}