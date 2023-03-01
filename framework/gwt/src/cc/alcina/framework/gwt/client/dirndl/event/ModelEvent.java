package cc.alcina.framework.gwt.client.dirndl.event;

import java.util.List;
import java.util.Optional;

import com.google.gwt.event.shared.EventHandler;
import com.google.web.bindery.event.shared.SimpleEventBus;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.serializer.ClassSerialization;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;
import cc.alcina.framework.gwt.client.dirndl.model.Choices;
import cc.alcina.framework.gwt.client.dirndl.model.HasNode;

/**
 * <h2>Gotchas</h2>
 * <p>
 * Event reemission: if a model reeemits an event (say it receives a Selected
 * event and, in consequence, emits a Selected event - see {@link Choices}) then
 * it must check for reemission to avoid an infinite loop with a call to
 * {@link ModelEvent#wasReemitted}. See {@link Choices.Single#onSelected}
 *
 * <p>
 * Naming note. Use present/imperative when the event is "a command UI gesture
 * was made" - e.g. clicking on the 'x' button somewhere commands 'Close', so
 * emit a {@code CloseEvent}. But when a UI <i>change</i> event occurs - such as
 * 'the popup closed', use the past simple - {@code ClosedEvent}.
 *
 * <p>
 * Naming note: 'dispatch' - emit/enqueue an event. 'Fire' call an event handler
 * with an event instance. There may be some lingering misuse...and there's
 * confusion all over the web. But if you "dispatch" a physical package, that's
 * really the beginning of a process - not a step - so I think this usage is
 * reasonable. Note that GWT event bus sort of has it the other way round - sad.
 *
 * <p>
 * usage note: there are many ways to reemit or emit an event from code -
 * preferred are the fluent ones ({@code DirectedLayout.Node.dispatch_, {@code
 * NodeEvent.reemitAs})
 *
 * @author nick@alcina.cc
 *
 * @param <T>
 * @param <H>
 */
@Registration(ClassSerialization.class)
public abstract class ModelEvent<T, H extends NodeEvent.Handler>
		extends NodeEvent<H>
		implements NodeEvent.WithoutDomBinding, Registration.Ensure {
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
						modelEvent, context.node, List.of(handler.get()));
				return;
			}
			Optional<TopLevelCatchallHandler> catchallHandler = Registry
					.optional(TopLevelCatchallHandler.class);
			if (catchallHandler.isPresent()) {
				catchallHandler.get().handle(modelEvent);
				return;
			}
		}
	}

	public static String staticDisplayName(Class<? extends ModelEvent> clazz) {
		return Ax.friendly(clazz.getSimpleName().replaceFirst("Event$", ""));
	}

	private boolean handled;

	public ModelEvent() {
	}

	public boolean checkReemitted(HasNode hasNode) {
		if (wasReemitted(hasNode.provideNode())) {
			getContext().markCauseEventAsNotHandled();
			return true;
		} else {
			return false;
		}
	}

	public <T0 extends T> T0 getModel() {
		return (T0) this.model;
	}

	public String getName() {
		return staticDisplayName(getClass());
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

	// normally call checkreemitted, since there's not much else to do
	public boolean wasReemitted(Node node) {
		return getContext().getPrevious() != null
				&& getContext().getPrevious().reemission == node;
	}

	public static interface TopLevelCatchallHandler {
		void handle(ModelEvent unhandledEvent);
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