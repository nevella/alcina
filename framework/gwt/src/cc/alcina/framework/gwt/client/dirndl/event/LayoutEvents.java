package cc.alcina.framework.gwt.client.dirndl.event;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.Model.NodeEventTypeValidator;

/**
 * <p>
 * The nested Event classes are special - they're not called by standard (Node,
 * Model) event pipelines, rather during render. But because the call site looks
 * the same, the NodeEvent class is reused
 * 
 * <p>
 * Because bindings work better with Bound/Unbound (rather the Bind with a
 * boolean bound field), there are two flavors of Bind events - Handler - for
 * 'do x if bound, y if not' - using onBind - or Binding - on(Bound.class),
 * on(Unbound.class)
 * 
 * <p>
 * Note that models don't need to explictly register for onBind - but do for
 * LayoutEvents.Bound, LayoutEvents.Unbound - via say
 * ,LayoutEvents.Bound.Binding. They're fundamentally different mechanisms - the
 * latter is standard event dispatch, the former is imperative code called
 * during render. Use whichever is cleanest for your code
 */
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

		public NodeEventTypeValidator nodeEventTypeValidator;

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

		public static Bind exTreeUnbindEvent() {
			return new Bind(null, false);
		}

		public static Bind exTreeBindEvent() {
			return new Bind(null, true);
		}
	}

	/*
	 * Note that this will be called with a null context (it doesn't propagate
	 * etc)
	 */
	public static class Bound extends ModelEvent<Object, Bound.Handler> {
		@Override
		public void dispatch(Bound.Handler handler) {
			handler.onBound(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onBound(Bound event);
		}

		public interface Binding extends Handler, NodeEvent.Binding {
			@Override
			default void onBound(Bound event) {
				((Model) this).bindings().onNodeEvent(event);
			}
		}
	}

	/*
	 * Note that this will be called with a null context (it doesn't propagate
	 * etc)
	 */
	public static class Unbound extends ModelEvent<Object, Unbound.Handler> {
		@Override
		public void dispatch(Unbound.Handler handler) {
			handler.onUnbound(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onUnbound(Unbound event);
		}

		public interface Binding extends Handler, NodeEvent.Binding {
			@Override
			default void onUnbound(Unbound event) {
				((Model) this).bindings().onNodeEvent(event);
			}
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

	/**
	 * How to call this? Cross layer event broadcast? Short-circuiting the
	 * classic management idiom? When two models are separated by &gt;1
	 * intermediate models but have a direct logical binding, this initiialises
	 * communication from the _ancestor_
	 */
	public static class EmitDescent extends LayoutEvent<EmitDescent.Handler> {
		public Object model;

		public Node node;

		public EmitDescent(DirectedLayout.Node node, Object model) {
			this.model = model;
			this.node = node;
			setContext(Context.fromNode(node));
		}

		@Override
		public void dispatch(EmitDescent.Handler handler) {
			handler.onEmitDescent(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onEmitDescent(EmitDescent event);
		}
	}
}
