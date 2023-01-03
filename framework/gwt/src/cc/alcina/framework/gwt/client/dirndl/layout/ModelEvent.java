package cc.alcina.framework.gwt.client.dirndl.layout;

import java.util.Optional;

import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.behaviour.NodeEvent;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;
import cc.alcina.framework.gwt.client.dirndl.model.Choices;

/**
 * <h2>Gotchas</h2>
 * <p>
 * Event reemission: if a model reeemits an event (say it receives a Selected
 * event and, in consequence, emits a Selected event - see {@link Choices}) then
 * it must check for reemission to avoid an infinite loop with a call to
 * {@link ModelEvent#wasReemitted}. See {@link Choices.Single#onSelected}
 *
 * @author nick@alcina.cc
 *
 * @param <T>
 * @param <H>
 */
public abstract class ModelEvent<T, H extends NodeEvent.Handler>
		extends NodeEvent<H> {
	// FIXME - dirndl 1x1h - fire on GWT/Scheduler event pump? nope, explain why
	// not
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
		if (!modelEvent.handled) {
			Optional<TopLevelHandler> handler = Registry
					.optional(TopLevelHandler.class, type);
			if (handler.isPresent()) {
				handler.get().handle(modelEvent);
			}
		}
	}

	private boolean handled;

	public ModelEvent() {
	}

	public <T0 extends T> T0 getModel() {
		return (T0) this.model;
	}

	public String getName() {
		return Ax.friendly(getClass().getSimpleName());
	}

	public boolean isHandled() {
		return handled;
	}

	public void setHandled(boolean handled) {
		this.handled = handled;
	}

	public boolean wasReemitted(Node node) {
		return getContext().previous != null
				&& getContext().previous.node == node;
	}

	@Override
	// FIXME - dirndl 1x1d - events - model vs widget bindings. Look at the
	// guarantees of
	// model binding, but I *think* we can move this into layout events. Also
	// pretty sure bind/unbind is a noop for model events - in fact,
	//
	protected HandlerRegistration bind0(Widget widget) {
		return widget.addAttachHandler(evt -> {
			if (!evt.isAttached()) {
				unbind();
			}
		});
	}

	/**
	 * Marker for top-level handlers of unhandled ModelEvents (basically
	 * links/actions where the context doesn't matter, such as 'logout' or
	 * 'privacy')
	 *
	 * @author nick@alcina.cc
	 *
	 */
	public static interface TopLevelHandler {
		void handle(ModelEvent unhandledEvent);
	}
}