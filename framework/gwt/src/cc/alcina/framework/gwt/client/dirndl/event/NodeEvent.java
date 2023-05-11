package cc.alcina.framework.gwt.client.dirndl.event;

import java.lang.annotation.Annotation;

import com.google.common.base.Preconditions;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

/**
 * <p>
 * See dirndl-events.md in this folder for the Dirndl event system overview
 *
 * @formatter:on
 *
 */
@Reflected
public abstract class NodeEvent<H extends NodeEvent.Handler>
		extends GwtEvent<H> {
	private Context context;

	protected Object model;

	@Override
	public abstract void dispatch(H handler);

	@Override
	public final Type<H> getAssociatedType() {
		throw new UnsupportedOperationException();
	}

	public Context getContext() {
		return this.context;
	}

	public abstract Class<H> getHandlerClass();

	// special case when re-emitting an event from its handler - marks the event
	// as reemitted (from the node) so as to not loop
	public void reemit() {
		context.reemit();
	}

	public <O extends ModelEvent> void reemitAs(Model from,
			Class<O> eventClass) {
		reemitAs(from, eventClass, from);
	}

	public <O extends ModelEvent> void reemitAs(Model from, Class<O> eventClass,
			Object eventModel) {
		NodeEvent.Context.fromEvent(this, from.provideNode())
				.dispatch(eventClass, eventModel);
	}

	public void setContext(Context context) {
		this.context = context;
	}

	public void setModel(Object model) {
		this.model = model;
	}

	@Override
	public String toString() {
		return Ax.format("%s : %s", getClass().getSimpleName(), model);
	}

	//
	public static class Context {
		public static Context fromContext(Context previous, Node node) {
			Context context = new Context(node == null ? previous.node : node);
			context.previous = previous;
			return context;
		}

		public static Context fromEvent(GwtEvent event, Node node) {
			Context context = new Context(node);
			if (event instanceof NodeEvent
					&& ((NodeEvent) event).context != null) {
				context.previous = ((NodeEvent) event).context;
			} else {
				context.gwtEvent = event;
			}
			return context;
		}

		public static Context fromNode(Node node) {
			Context context = new Context(node);
			return context;
		}

		private Context previous;

		public final Node node;

		private NodeEvent nodeEvent;

		private GwtEvent gwtEvent;

		Node reemission;

		public Context(Node node) {
			this.node = node;
		}

		public <A extends Annotation> A annotation(Class<A> clazz) {
			return node.annotation(clazz);
		}

		/*
		 * Mark the event causing this one as not handled - which causes it to
		 * fire on ancestor handlers
		 */
		public void bubble() {
			((ModelEvent) getPrevious().getNodeEvent()).setHandled(false);
		}

		public void dispatch(Class<? extends ModelEvent> modelEventClass,
				Object model) {
			ModelEvent.dispatch(this, modelEventClass, model);
		}

		public GwtEvent getGwtEvent() {
			return gwtEvent;
		}

		public NodeEvent getNodeEvent() {
			return nodeEvent;
		}

		public GwtEvent getOriginatingGwtEvent() {
			Context cursor = this;
			while (cursor.previous != null) {
				cursor = cursor.previous;
			}
			return cursor.gwtEvent;
		}

		public Context getPrevious() {
			return previous;
		}

		public boolean hasPrevious(Class<? extends NodeEvent> eventClass) {
			if (eventClass == nodeEvent.getClass()) {
				return true;
			}
			if (getPrevious() == null) {
				return false;
			}
			return getPrevious().hasPrevious(eventClass);
		}

		public void setNodeEvent(NodeEvent nodeEvent) {
			Preconditions.checkState(this.nodeEvent == null);
			this.nodeEvent = nodeEvent;
			nodeEvent.context = this;
		}

		void reemit() {
			Context newContext = fromContext(this, node);
			newContext.reemission = node;
			newContext.dispatch(
					(Class<? extends ModelEvent>) nodeEvent.getClass(), null);
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

	// Marker interface - otherwise a Registry-supplied DomBinding is expected
	public interface WithoutDomBinding {
	}
}
