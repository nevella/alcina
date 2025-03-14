package cc.alcina.framework.gwt.client.dirndl.layout;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.web.bindery.event.shared.SimpleEventBus;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.process.ProcessObservers;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.AlcinaCollections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent.Context;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

/**
 * <p>
 * Note that dom/inferred-dom events (NodeEvent *not* subclass ModelEvent) and
 * model events have quite different event propagation mechanisms, so there's
 * essentially two event propagation mechanisms:
 *
 * <p>
 * DOM: model -> widget -> element.addListener(x) -- Model implements the
 * handler mechanism, event propagation is DOM propagation, so up the widget
 * tree (which mostly corresponds to the DL node tree). (TODO - actually explain
 * this - possibly in javadoc)
 *
 * <p>
 * Model: ModelEvent.fire(...) - event fires on the current Model if it
 * implements the Model.Handler class, and propagation finishes at the first
 * Node that handles the event (implements the Handler class) unless explicitly
 * permitted via NodeEvent.Context.markCauseEventAsNotHandled()
 *
 * <h3>An example</h3>
 *
 * <pre>
 * <code>
 *
 *
 * &#64;Override
 * public void onSubmitted(Submitted event) {
 * 	// this occurs when enter is clicked, so handle here, but also propagate
 * 	// to the containing form
 * 	event.getContext().markCauseEventAsNotHandled();
 * 	String value = textArea.getValue();
 * 	setDirty(!Objects.equals(originalOrLastSubmittedValue, value));
 * 	originalOrLastSubmittedValue = value;
 * }
 *
 * </code>
 * </pre>
 *
 * <p>
 * FIXME - dirndl 1x1h - should these in fact be two different bindings - say a
 * base class and subclass?
 *
 * <h3>Descendant bindings</h3>
 * <p>
 * Ascent binding/dispatch doesn't require optimisation, since the
 * find-receivier algorithm is O(n) where n is the depth of the dispatcher in
 * the dirndl node tree.
 * <p>
 * Descent binding requires that descendants which receive Descent events
 * register themselves with the emitting ancestor on bind (and unbind
 * appropriately)
 */
class NodeEventBinding {
	static boolean isDescendantBinding(Class<? extends NodeEvent> clazz) {
		return Reflections.isAssignableFrom(ModelEvent.DescendantEvent.class,
				clazz);
	}

	final DirectedLayout.Node node;

	/*
	 * This may be a superclass of the event type (see ActionEvent). TODO - Doc
	 * (it makes event binding slightly more complicated, but allows a really
	 * useful inversion for one-off event handling
	 */
	final Class<? extends NodeEvent> type;

	DomBinding domBinding;

	DescendantBindings descendantBindings;

	Class<? extends ModelEvent> reemitAs;

	public NodeEventBinding(DirectedLayout.Node node,
			Class<? extends NodeEvent> type) {
		this.node = node;
		this.type = type;
	}

	void addDescendantBinding(NodeEventBinding descendantBinding) {
		ensureDescendantBindings().addHandler(descendantBinding);
	}

	/*
	 * this method contains checks that a binding exists (if the event type does
	 * not implement WithoutDomBinding), and that the DomBinding subclass is an
	 * inner class of the NodeEvent subclass
	 */
	void bind() {
		if (isDomBinding()) {
			domBinding = Registry.impl(DomBinding.class, type);
			Preconditions.checkState(domBinding.getClass().getName()
					.indexOf(type.getName()) == 0);
			domBinding.nodeEventBinding = this;
			if (node.rendered == null) {
				Ax.err(node.toParentStack());
				throw new IllegalStateException(Ax.format(
						"No widget for model binding dom event %s - possibly delegating",
						node.model));
			}
			domBinding.bind(getBindingRendered().as(Element.class), node.model,
					true);
		} else {
			// model event
			Preconditions.checkState(Reflections
					.isAssignableFrom(NodeEvent.WithoutDomBinding.class, type));
			if (isDescendantBinding(type)) {
				// find emitter attached to a parent node which emits events of
				// Type type
				ModelEvent.Emitter emitter = node.findEmitter(type);
				if (emitter != null) {
					((Model) emitter).provideNode().getEventBinding(type)
							.addDescendantBinding(this);
				}
			}
			return;
		}
	}

	void dispatchDescent(ModelEvent modelEvent) {
		ensureDescendantBindings().dispatch(modelEvent);
	}

	DescendantBindings ensureDescendantBindings() {
		if (descendantBindings == null) {
			descendantBindings = new DescendantBindings();
		}
		return descendantBindings;
	}

	/*
	 * FIXME - dirndl 1x1h - receive/reemit merging - document how to reemit
	 * from StringInput (annotation merge should fail if receive/reemit pair -
	 * instead, add just receipt and manually reemit)
	 *
	 * FIXME - dirndl 1x1h - Also: warn if a model implements a handler but has
	 * no receive (reverse will be a ClassCast, so no need to check)
	 *
	 * What this (opaque) comment is saying is that there *was* an issue with
	 * the StringInput @Directed annotation - or a subclass? Anyway, merging
	 * receive [z] to superclass receive[x]/reemit[y] is obviously problematic.
	 * Solution is to disallow that with an informative exception (as above)
	 *
	 * FIXME - dirndl 1x1h - doc - note that a new NodeEvent is at each point in
	 * the model ancestor chain that it's fired - since an event is immutable
	 * and we want to fire additional (context) information at each point. Hence
	 * the cloning of the originating event, which should copy any additional
	 * payload fields of the originating event
	 *
	 */
	void fireEvent(Class<? extends NodeEvent> actualEventType, Context context,
			Object model) {
		NodeEvent nodeEvent = context.getNodeEvent();
		if (nodeEvent == null) {
			GwtEvent gwtEvent = context.getOriginatingGwtEvent();
			if (gwtEvent instanceof NodeEvent) {
				nodeEvent = (NodeEvent) gwtEvent;
			}
		}
		if (nodeEvent != null && nodeEvent.getClass() == actualEventType) {
			nodeEvent = nodeEvent.clone();
		} else {
			nodeEvent = Reflections.newInstance(actualEventType);
		}
		context.setNodeEvent(nodeEvent);
		nodeEvent.setModel(model);
		ProcessObservers.publish(DirectedLayout.EventObservable.class,
				() -> new DirectedLayout.EventObservable(actualEventType,
						context, model));
		Class<? extends EventHandler> handlerClass = Reflections
				.at(actualEventType).templateInstance().getHandlerClass();
		NodeEvent.Handler handler = null;
		if (reemitAs == null) {
			handler = (NodeEvent.Handler) context.node.model;
			if (Client.has()) {
				SimpleEventBus eventBus = (SimpleEventBus) Client.eventBus();
				eventBus.fireEventFromSource(nodeEvent, context.node,
						List.of(handler));
			} else {
				// pure-server
				nodeEvent.dispatch(handler);
			}
		} else {
			// dispatch a new ModelEvent, compute its type [receive,
			// reemit] tuple in Directed.reemits
			Context eventContext = NodeEvent.Context.fromContext(context, node);
			eventContext.dispatch(reemitAs, node.getModel());
		}
	}

	void fireEventIfType(ModelEvent event) {
		if (event.getClass() == type) {
			Context context = NodeEvent.Context.fromContext(event.getContext(),
					node);
			// set before we dispatch to the handler, so the handler can
			// unset
			event.setHandled(true);
			fireEvent(event.getClass(), context, event.getModel());
		}
	}

	DirectedLayout.Rendered getBindingRendered() {
		return node.verifySingleRendered();
	}

	Node getNode() {
		return node;
	}

	boolean isDomBinding() {
		return Registry.query(DomBinding.class).addKeys(type)
				.hasImplementation();
	}

	public void onEvent(GwtEvent event) {
		Context context = NodeEvent.Context.fromEvent(event, node);
		fireEvent(type, context, node.getModel());
	}

	@Override
	public String toString() {
		return Ax.format("%s :: %s", node.model.getClass().getSimpleName(),
				type.getSimpleName());
	}

	void unbind() {
		if (domBinding != null) {
			domBinding.bind(null, null, false);
		}
		if (descendantBindings != null) {
			descendantBindings.unbind(this);
		}
	}

	/**
	 * <p>
	 * One essential feature of descendant bindings is that they record the last
	 * dispatched event, since an 'initialisation event' is often fired by an
	 * ancestor before the descendant even exists, let alone is attached
	 * 
	 * <p>
	 * So the last fired event is fired (if it exists) to any handlers on attach
	 * *as a finally* - note the risk of memory leaks is really pretty remote
	 * here, since whatever's reachable from the event should be reachable to
	 * the firing model
	 */
	class DescendantBindings {
		Set<NodeEventBinding> handlers = GWT.isScript()
				? AlcinaCollections.newUniqueSet()
				// easier debugging
				: new LinkedHashSet<>();

		NodeEventBinding ancestorEmitter;

		ModelEvent lastDispatched;

		public void addHandler(NodeEventBinding descendantBinding) {
			handlers.add(descendantBinding);
			descendantBinding
					.ensureDescendantBindings().ancestorEmitter = NodeEventBinding.this;
			if (lastDispatched != null) {
				ModelEvent lastDispatchedRef = lastDispatched;
				Client.eventBus().queued().lambda(() -> this
						.fireAttachEvent(descendantBinding, lastDispatchedRef))
						.dispatch();
			}
		}

		void fireAttachEvent(NodeEventBinding descendantBinding,
				ModelEvent lastDispatchedRef) {
			if (lastDispatchedRef == lastDispatched) {
				descendantBinding.fireEventIfType(lastDispatchedRef);
			}
		}

		void dispatch(ModelEvent modelEvent) {
			lastDispatched = modelEvent;
			/*
			 * it's guaranteed that the handler NodeEventBinding is the right
			 * type
			 */
			handlers.forEach(h -> h.fireEventIfType(modelEvent));
		}

		public void removeHandler(NodeEventBinding descendantBinding) {
			handlers.remove(descendantBinding);
		}

		void unbind(NodeEventBinding nodeEventBinding) {
			if (ancestorEmitter != null) {
				ancestorEmitter.descendantBindings
						.removeHandler(NodeEventBinding.this);
			}
		}
	}
}