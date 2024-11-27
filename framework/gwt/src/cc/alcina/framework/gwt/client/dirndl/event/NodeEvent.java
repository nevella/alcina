package cc.alcina.framework.gwt.client.dirndl.event;

import java.lang.annotation.Annotation;

import com.google.common.base.Preconditions;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.HasNativeEvent;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.reflection.Reflections;
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
@Registration(NodeEvent.class)
public abstract class NodeEvent<H extends NodeEvent.Handler>
		extends GwtEvent<H> {
	private Context context;

	protected Object model;

	@Override
	public NodeEvent clone() {
		return (NodeEvent) Reflections.newInstance(getClass());
	}

	@Override
	public abstract void dispatch(H handler);

	@Override
	public final Type<H> getAssociatedType() {
		throw new UnsupportedOperationException();
	}

	public Context getContext() {
		return this.context;
	}

	/*
	 * If a subclass overrides this, it must also have a no-args constructor
	 */
	public Class<H> getHandlerClass() {
		return Reflections.at(getClass()).firstGenericBound();
	}

	// special case when re-emitting an event from its handler - marks the event
	// as reemitted (from the node) so as to not loop
	public void reemit() {
		context.reemit();
	}

	public <O extends ModelEvent> void reemitAs(Model from,
			Class<O> eventClass) {
		reemitAs(from, eventClass, null);
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

	public Object sourceModel() {
		return context.getPrevious().node.getModel();
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

		public NativeEvent getOriginatingNativeEvent() {
			return ((HasNativeEvent) getOriginatingGwtEvent()).getNativeEvent();
		}

		/**
		 * A dispatched event (dispatched to a model) is a copy of the bubbling
		 * event, so the first call to getPrevious() within a handler will
		 * return that bubbling event. Subsequent calls will return the prior
		 * event(s) causal events
		 */
		public Context getPrevious() {
			return previous;
		}

		public <E extends NodeEvent> E getPreviousEvent(Class<E> eventClass) {
			Context cursor = this;
			while (cursor != null) {
				if (eventClass == cursor.nodeEvent.getClass()) {
					return (E) cursor.getNodeEvent();
				}
				cursor = cursor.getPrevious();
			}
			return null;
		}

		public boolean hasPrevious(Class<? extends NodeEvent> eventClass) {
			return getPreviousEvent(eventClass) != null;
		}

		void reemit() {
			Context newContext = fromContext(this, node);
			newContext.reemission = node;
			ModelEvent modelEvent = (ModelEvent) nodeEvent;
			newContext.dispatch(modelEvent.getClass(), modelEvent.getModel());
		}

		public void setNodeEvent(NodeEvent nodeEvent) {
			Preconditions.checkState(this.nodeEvent == null);
			this.nodeEvent = nodeEvent;
			nodeEvent.context = this;
		}
	}

	// Omit from Event binding
	public interface DirectlyInvoked {
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
