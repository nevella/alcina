package cc.alcina.framework.gwt.client.dirndl.event;

import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout;

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
	 * @author nick@alcina.cc
	 *
	 */
	public static class BeforeRender extends LayoutEvent<BeforeRender.Handler> {
		public BeforeRender(DirectedLayout.Node node) {
			setContext(Context.fromNode(node));
		}

		@Override
		public void dispatch(BeforeRender.Handler handler) {
			handler.onBeforeRender(this);
		}

		@Override
		public Class<BeforeRender.Handler> getHandlerClass() {
			return BeforeRender.Handler.class;
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
	 * FIXME - dirndl 1x1d.0 - remove unneeded usages (since generally better
	 * via early binding().add() if possible
	 *
	 * @author nick@alcina.cc
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

		@Override
		public Class<Bind.Handler> getHandlerClass() {
			return Bind.Handler.class;
		}

		public boolean isBound() {
			return this.bound;
		}

		public interface Handler extends NodeEvent.Handler {
			/**
			 * Do not modify the model (properties) here - instead, use
			 * BeforeRender.onBeforeHandler. This occurs after widget and child
			 * node layout
			 */
			void onBind(Bind event);
		}
	}

	/**
	 * Handlers do not need to configure binding event receipt
	 * via @Directed(receives) - the respective Handler methods are called
	 * directly during layout. Note that getContext() only provides the Node
	 * corresponding to the model
	 *
	 * @author nick@alcina.cc
	 *
	 */
	public static abstract class LayoutEvent<H extends NodeEvent.Handler>
			extends NodeEvent<H> implements NodeEvent.WithoutDomBinding {
	}
}
