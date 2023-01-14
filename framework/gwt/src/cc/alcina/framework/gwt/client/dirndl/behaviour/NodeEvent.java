package cc.alcina.framework.gwt.client.dirndl.behaviour;

import java.lang.annotation.Annotation;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.NodeEventReceiver;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelEvent;

@Reflected
/**
 * <p>
 * Event system notes
 *
 * <ul>
 * <li>Events do not bubble by design - once handled, unless explicitly
 * instructed to continue up the Node ancestor tree by
 * markCauseEventAsNotHandled(), they stop.
 * <li>Events are a 'recasting' of an existing point-in-time event, so an
 * EventBus is not used. This seems to be a better choice - (explain)
 *
 * <p>
 * FIXME - dirndl 1x1d - events - issue with 'Don't like' below is that
 * NodeEvent does too much. There should be an DomBinding nested class which
 * handles the nodeevent -> dom relation, not populated for ModelEvent [Note re
 * FIXME - pretty sure it's already implemented. Current TODO is give examples,
 * clean up docs.
 *
 * @author nick@alcina.cc
 *
 * @param <H>
 */
/*
 * FIXME - dirndl 1x1d - events - don't like the interplay between NodeEvent,
 * BehaviourBinding and NodeEventReceiver... see https://github.com/nevella/alcina/issues/24
 *
 * (later) - factor out into 'eventsource' - which only exists for gwt events
 *
 * but...maybe...it's right? It's an event fired on a node, so as long as
 * there's one nodeevent per logical 'event', per node, maybe it's right...?
 * weird...
 *
 * Actually (TODO: document better) - this is totally correct - and feature, not
 * bug - dirndl node events always only have one receiver (the node) -
 * event bubbling generates new events at the receiver node
 *
 * Implementation - extend nodeevent (to future-proof against using the gwt
 * event bus) but, because our propagation model is differnt, don't implement
 * the dispatch/TYPE mechanism
 *
 * 20220918 - Yes, correct. Although the ancestor propagation of NodeEvents
 * doesn't (and shouldn't) use the GWT event bus, events with no GWT event bus
 * root cause *may* want to be scheduled on that bus - but possibly just
 * schedule() on that bus is enough
 *
 * TODO: document the nodeevent bind/fire/unbind lifecycle.
 *
 * @formatter:off
 *
 * Sketch of current sequence:
 * ===========================
 *
 * - rendering algorithm in DirectedLayout renders Node
 *
 * @formatter:on
 *
 */
public abstract class NodeEvent<H extends NodeEvent.Handler>
		extends GwtEvent<H> {
	private NodeEventReceiver eventReceiver;

	protected HandlerRegistration handlerRegistration;

	private Context context;

	protected Object model;

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

	@Override
	/*
	 * Not (yet) dispatching via gwt event bus - so force public
	 */
	public abstract void dispatch(H handler);

	@Override
	public final Type<H> getAssociatedType() {
		throw new UnsupportedOperationException();
	}

	public Context getContext() {
		return this.context;
	}

	public abstract Class<H> getHandlerClass();

	public void setContext(Context context) {
		this.context = context;
	}

	public void setEventReceiver(NodeEventReceiver eventReceiver) {
		this.eventReceiver = eventReceiver;
	}

	public void setModel(Object model) {
		this.model = model;
	}

	@Override
	public String toString() {
		return Ax.format("%s : %s", getClass().getSimpleName(), model);
	}

	protected abstract HandlerRegistration bind0(Widget widget);

	protected void fireEvent(GwtEvent gwtEvent) {
		eventReceiver.onEvent(gwtEvent);
	}

	protected void unbind() {
		bind(null, false);
	}

	public static class Context {
		public static Context newModelContext(Context previous, Node node) {
			Context context = new Context();
			context.previous = previous;
			context.node = node == null ? previous.node : node;
			return context;
		}

		public static Context newModelContext(GwtEvent event, Node node) {
			Context context = new Context();
			context.gwtEvent = event;
			context.node = node;
			return context;
		}

		public static Context newModelContext(String hint, Node node) {
			Context context = new Context();
			context.hint = hint;
			context.node = node;
			return context;
		}

		public static Context newNodeContext(Node node) {
			Context context = new Context();
			context.node = node;
			return context;
		}

		// informational/debugging
		String hint;

		public Context previous;

		public DirectedLayout.Node node;

		private NodeEvent nodeEvent;

		public GwtEvent gwtEvent;

		public Context() {
		}

		public <A extends Annotation> A annotation(Class<A> clazz) {
			return node.annotation(clazz);
		}

		// Fluent event emission
		public void fire(Class<? extends ModelEvent> modelEvent) {
			ModelEvent.fire(this, modelEvent, null);
		}

		public NodeEvent getNodeEvent() {
			return nodeEvent;
		}

		public boolean hasPrevious(Class<? extends NodeEvent> eventClass) {
			if (eventClass == nodeEvent.getClass()) {
				return true;
			}
			if (previous == null) {
				return false;
			}
			return previous.hasPrevious(eventClass);
		}

		public void markCauseEventAsNotHandled() {
			((ModelEvent) previous.getNodeEvent()).setHandled(false);
		}

		public void setNodeEvent(NodeEvent nodeEvent) {
			this.nodeEvent = nodeEvent;
			nodeEvent.context = this;
		}
	}

	public interface Handler extends EventHandler {
	}

	public static class Type<H extends EventHandler> extends GwtEvent.Type<H> {
		private Class<H> handlerClass;

		public Type(Class<H> handlerClass) {
			super();
			this.handlerClass = handlerClass;
		}

		public Class<H> getHandlerClass() {
			return this.handlerClass;
		}
	}
}
