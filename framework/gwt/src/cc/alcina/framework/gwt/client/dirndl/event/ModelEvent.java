package cc.alcina.framework.gwt.client.dirndl.event;

import java.util.List;
import java.util.Optional;

import com.google.gwt.event.shared.EventHandler;
import com.google.web.bindery.event.shared.SimpleEventBus;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout;
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
		extends NodeEvent<H> implements NodeEvent.WithoutDomBinding {
	public static void dispatch(Context context,
			Class<? extends ModelEvent> type, Object model) {
		ModelEvent modelEvent = Reflections.newInstance(type);
		context.setNodeEvent(modelEvent);
		modelEvent.setModel(model);
		DirectedLayout.dispatchModelEvent(modelEvent);
		if (!modelEvent.handled) {
			Optional<TopLevelHandler> handler = Registry
					.optional(TopLevelHandler.class, type);
			if (handler.isPresent()) {
				((SimpleEventBus) Client.eventBus()).fireEventFromSource(
						modelEvent, context.node, List.of(handler));
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

	public Class<? extends ModelEvent> getReceiverType() {
		return getClass();
	}

	public boolean isHandled() {
		return handled;
	}

	public void setHandled(boolean handled) {
		this.handled = handled;
	}

	public boolean wasReemitted(Node node) {
		return getContext().getPrevious() != null
				&& getContext().getPrevious().node == node;
	}

	/**
	 * Marker for top-level handlers of unhandled ModelEvents (basically
	 * links/actions where the context doesn't matter, such as 'logout' or
	 * 'privacy')
	 *
	 * @author nick@alcina.cc
	 *
	 */
	public static interface TopLevelHandler extends EventHandler {
		void handle(ModelEvent unhandledEvent);
	}
}