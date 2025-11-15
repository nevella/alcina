package cc.alcina.framework.gwt.client.dirndl.layout;

import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.NestedName;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent.Emitter;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent.Context;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;
import cc.alcina.framework.gwt.client.dirndl.model.HasNode;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

class ModelEventDispatch {
	static void dispatchAscent(ModelEvent modelEvent) {
		/*
		 * Bubble until event is handled or we've reached the top of the node
		 * tree
		 */
		Node cursor = modelEvent.getContext().node;
		while (cursor != null) {
			cursor.fireEvent(modelEvent);
			if (modelEvent.isHandled()) {
				break;
			}
			boolean rerouted = false;
			{
				/*
				 * this logic supports the DOM requirement that popups
				 * (overlays) be outside the dom containment of the parent (in
				 * general) - while maintining the event bubbling relationship
				 * of a popup model to its logical model parent
				 *
				 */
				if (cursor.model instanceof Model.RerouteBubbledEvents) {
					Model rerouteTo = ((Model.RerouteBubbledEvents) cursor.model)
							.rerouteBubbledEventsTo();
					if (rerouteTo != null && rerouteTo instanceof HasNode) {
						Node rerouteToNode = ((HasNode) rerouteTo)
								.provideNode();
						if (rerouteToNode != null) {
							cursor = rerouteToNode;
							rerouted = true;
						}
					}
				}
			}
			if (!rerouted) {
				cursor = cursor.parent;
			}
		}
	}

	static void dispatchDescent(ModelEvent modelEvent) {
		Node cursor = modelEvent.getContext().node;
		Class<? extends ModelEvent> eventType = modelEvent.getClass();
		Emitter emitter = cursor.findEmitter(eventType);
		if (emitter == null) {
			throw new IllegalStateException(Ax.format(
					"Type %s has no ancestor that implements the %s Emitter",
					NestedName.get(cursor.model), NestedName.get(eventType)));
		}
		Node emitterNode = ((Model) emitter).provideNode();
		if (emitterNode != cursor) {
			Object model = modelEvent.getModel();
			Context newContext = NodeEvent.Context
					.fromContext(modelEvent.getContext(), emitterNode);
			modelEvent = Reflections.newInstance(eventType);
			newContext.setNodeEvent(modelEvent);
			modelEvent.setModel(model);
		}
		emitterNode.getEventBinding(eventType).dispatchDescent(modelEvent);
	}
}