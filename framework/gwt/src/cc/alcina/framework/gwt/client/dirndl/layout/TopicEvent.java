package cc.alcina.framework.gwt.client.dirndl.layout;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.gwt.client.dirndl.behaviour.NodeEvent;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node.NodeEventBinding;

//FIXME - dirndl 1.2 - can TopicEvent be combined with ModelEvent? Why rendered.preRenderListeners?
public abstract class TopicEvent<T, H extends NodeEvent.Handler>
		extends NodeEvent.ModelEvent<T, H> {
	// FIXME - dirndl 1.1 - fire on GWT/Scheduler event pump
	public static void fire(Context context, Class<? extends TopicEvent> type,
			Object model) {
		TopicEvent topicEvent = Reflections.newInstance(type);
		context.setNodeEvent(topicEvent);
		topicEvent.setModel(model);
		context.topicListeners.eventBindings
				.forEach(bb -> bb.onTopicEvent(topicEvent));
		/*
		 * Bubble
		 */
		Node cursor = context.node;
		while (cursor != null && !topicEvent.handled) {
			cursor.fireEvent(topicEvent);
			cursor = cursor.parent;
		}
	}

	private boolean handled;

	public TopicEvent() {
	}

	public boolean isHandled() {
		return handled;
	}

	public void setHandled(boolean handled) {
		this.handled = handled;
	}

	@Override
	protected HandlerRegistration bind0(Widget widget) {
		return widget.addAttachHandler(evt -> {
			if (!evt.isAttached()) {
				unbind();
			}
		});
	}

	public static class TopicListeners {
		List<NodeEventBinding> eventBindings = new ArrayList<>();

		public void addListener(NodeEventBinding binding) {
			eventBindings.add(binding);
			binding.getBindingWidget().addAttachHandler(evt -> {
				if (!evt.isAttached()) {
					eventBindings.remove(binding);
				}
			});
		}
	}
}