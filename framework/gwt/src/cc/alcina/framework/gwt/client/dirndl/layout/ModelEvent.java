package cc.alcina.framework.gwt.client.dirndl.layout;

import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.gwt.client.dirndl.behaviour.NodeEvent;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;

public abstract class ModelEvent<T, H extends NodeEvent.Handler>
		extends NodeEvent<H> {
	// FIXME - dirndl 1.1 - fire on GWT/Scheduler event pump? or explain why not
	public static void fire(Context context, Class<? extends ModelEvent> type,
			Object model) {
		ModelEvent modelEvent = Reflections.newInstance(type);
		context.setNodeEvent(modelEvent);
		modelEvent.setModel(model);
		/*
		 * Bubble
		 */
		Node cursor = context.node;
		while (cursor != null && !modelEvent.handled) {
			cursor.fireEvent(modelEvent);
			cursor = cursor.parent;
		}
	}

	private boolean handled;

	public ModelEvent() {
	}

	public <T0 extends T> T0 getModel() {
		return (T0) this.model;
	}

	public boolean isHandled() {
		return handled;
	}

	public void setHandled(boolean handled) {
		this.handled = handled;
	}

	@Override
	// FIXME - dirndl 1x1b - model vs widget bindings. Look at the guarantees of
	// model binding, but I *think* we can move this into layout events. Also
	// pretty sure bind/unbind is a noop for model events - in fact,
	//
	// FIXME - dirndl 1x1a verify
	protected HandlerRegistration bind0(Widget widget) {
		return widget.addAttachHandler(evt -> {
			if (!evt.isAttached()) {
				unbind();
			}
		});
	}
}