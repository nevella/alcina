package cc.alcina.framework.gwt.client.dirndl.layout;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.util.IdentityFunction;
import cc.alcina.framework.gwt.client.dirndl.annotation.Behaviour;
import cc.alcina.framework.gwt.client.dirndl.behaviour.NodeEvent;
import cc.alcina.framework.gwt.client.dirndl.behaviour.NodeTopic;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node.BehaviourBinding;

public class TopicEvent<T> extends NodeEvent {
	public static void fire(Context context, Class<? extends NodeTopic> topic,
			Class<? extends Function> payloadTransformer, Object payload,
			boolean programmatic) {
		TopicEvent topicEvent = new TopicEvent(context);
		topicEvent.topic = topic;
		if (payload == null) {
			if (payloadTransformer == IdentityFunction.class) {
				topicEvent.payload = context.node;
			} else {
				Function<Node, ?> transformerImpl = Reflections
						.newInstance(payloadTransformer);
				topicEvent.payload = transformerImpl.apply(context.node);
			}
		} else {
			topicEvent.payload = payload;
		}
		/*
		 * Code event, use listeners on the emitting behaviour binding
		 */
		if (programmatic) {
			Behaviour emitter = Behaviour.Util.getEmitter(context.node.directed,
					topic);
			context.topicListeners = context.node.rendered
					.behaviourBindingFor(emitter).topicListeners;
		}
		context.topicListeners.listeners
				.forEach(bb -> bb.onTopicEvent(topicEvent));
		/*
		 * Bubble
		 */
		Node cursor = context.node.parent;
		while (cursor != null && !topicEvent.cancelBubble) {
			cursor.fireEvent(topicEvent);
			cursor = cursor.parent;
		}
	}

	public Class<? extends NodeTopic> topic;

	public T payload;

	public Context context;

	public boolean cancelBubble;

	public TopicEvent() {
	}

	private TopicEvent(Context context) {
		this.context = context;
	}

	@Override
	protected HandlerRegistration bind0(Widget widget) {
		return widget.addAttachHandler(evt -> {
			if (!evt.isAttached()) {
				unbind();
			}
		});
	}

	/*
	 * Indicates topic event will be fired from code, not an annotation
	 */
	public static class CodeTopic extends NodeTopic {
	}

	public static class TopicListeners {
		List<BehaviourBinding> listeners = new ArrayList<>();

		public void addListener(BehaviourBinding behaviourBinding) {
			listeners.add(behaviourBinding);
			behaviourBinding.getBindingWidget().addAttachHandler(evt -> {
				if (!evt.isAttached()) {
					listeners.remove(behaviourBinding);
				}
			});
		}
	}
}