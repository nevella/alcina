package cc.alcina.framework.gwt.client.dirndl.event;

import java.lang.annotation.Annotation;

import com.google.common.base.Preconditions;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;

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

	public <O extends ModelEvent> void reemitAs(Class<O> clazz) {
		NodeEvent.Context.newModelContext(this, this.context.node).fire(clazz);
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

	protected void fireEvent(GwtEvent gwtEvent) {
		int debug = 3;
		// eventReceiver.onEvent(gwtEvent);
	}

	//
	public static class Context {
		public static Context newModelContext(Context previous, Node node) {
			Context context = new Context(node == null ? previous.node : node);
			context.previous = previous;
			return context;
		}

		public static Context newModelContext(GwtEvent event, Node node) {
			Context context = new Context(node);
			context.gwtEvent = event;
			return context;
		}

		public static Context newModelContext(String hint, Node node) {
			Context context = new Context(node);
			context.hint = hint;
			return context;
		}

		public static Context newNodeContext(Node node) {
			Context context = new Context(node);
			return context;
		}

		// informational/debugging
		@SuppressWarnings("unused")
		private String hint;

		private Context previous;

		public final Node node;

		private NodeEvent nodeEvent;

		private GwtEvent gwtEvent;

		public Context(Node node) {
			this.node = node;
		}

		public <A extends Annotation> A annotation(Class<A> clazz) {
			return node.annotation(clazz);
		}

		// Fluent event emission
		public void fire(Class<? extends ModelEvent> modelEvent) {
			fire(modelEvent, null);
		}

		public void fire(Class<? extends ModelEvent> modelEvent, Object model) {
			ModelEvent.dispatch(this, modelEvent, model);
		}

		public GwtEvent getGwtEvent() {
			return gwtEvent;
		}

		public NodeEvent getNodeEvent() {
			return nodeEvent;
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

		public void markCauseEventAsNotHandled() {
			((ModelEvent) getPrevious().getNodeEvent()).setHandled(false);
		}

		public void setNodeEvent(NodeEvent nodeEvent) {
			Preconditions.checkState(this.nodeEvent == null);
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

	// Marker interface - otherwise a Registry-supplied DomBinding is expected
	public interface WithoutDomBinding {
	}
}