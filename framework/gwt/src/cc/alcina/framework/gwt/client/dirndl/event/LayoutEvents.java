package cc.alcina.framework.gwt.client.dirndl.event;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;

public class LayoutEvents {
	/**
	 * <p>
	 * Fired before a model is rendered by the layout algorithm. This is the
	 * correct point at which to populate child models.
	 *
	 * <p>
	 * In most circumstances this is only called once per model.
	 *
	 * <p>
	 * Model fields can normally be populated during the constructor - in which
	 * case the model does not need to react to this event. The primary
	 * exception is model instances that are received via RPC - often they'll
	 * have some fields sent via RPC, others that are only populated (here)
	 * client-side
	 *
	 *
	 *
	 */
	public static class BeforeRender extends LayoutEvent<BeforeRender.Handler> {
		public Object model;

		public Node node;

		public BeforeRender(DirectedLayout.Node node, Object model) {
			this.model = model;
			this.node = node;
			setContext(Context.fromNode(node));
		}

		@Override
		public void dispatch(BeforeRender.Handler handler) {
			handler.onBeforeRender(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onBeforeRender(BeforeRender event);
		}
	}

	/**
	 * <p>
	 * Fired at the bind/unbind points of the layout algorithm as it lays out
	 * this model. See {@link DirectedLayout.RendererInput#render} for details
	 *
	 *
	 *
	 *
	 */
	public static class Bind extends LayoutEvent<Bind.Handler> {
		private final boolean bound;

		public Bind(DirectedLayout.Node node, boolean bound) {
			setContext(Context.fromNode(node));
			this.bound = bound;
		}

		@Override
		public void dispatch(Bind.Handler handler) {
			handler.onBind(this);
		}

		public boolean isBound() {
			return this.bound;
		}

		@Override
		public String toString() {
			return Ax.format("%s - bound: %s", super.toString(), bound);
		}

		public interface Handler extends NodeEvent.Handler {
			/**
			 * Do not modify the model (properties) here or create bindings -
			 * instead, use BeforeRender.onBeforeHandler. This occurs after
			 * widget and child node layout
			 */
			void onBind(Bind event);
		}
	}

	/**
	 * The respective Handler methods are called directly during layout. Note
	 * that getContext() only provides the Node corresponding to the model
	 *
	 *
	 *
	 */
	public static abstract class LayoutEvent<H extends NodeEvent.Handler>
			extends NodeEvent<H>
			implements NodeEvent.WithoutDomBinding, NodeEvent.DirectlyInvoked {
	}
}
