package cc.alcina.framework.gwt.client.dirndl.behaviour;

import java.lang.annotation.Annotation;
import java.util.logging.Level;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.log.AlcinaLogUtils;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.gwt.client.dirndl.annotation.Behaviour;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.NodeEventReceiver;
import cc.alcina.framework.gwt.client.dirndl.layout.TopicEvent;
import cc.alcina.framework.gwt.client.dirndl.layout.TopicEvent.TopicListeners;

@ClientInstantiable
/*
 * FIXME - dirndl 1.1 - don't like the interplay between NodeEvent,
 * BehaviourBinding and NodeEventReceiver...
 * 
 * but...maybe...it's right? It's an event fired on a node, so as long as
 * there's one nodeevent per logical 'event', per node, maybe it's right...?
 * weird...
 */
public abstract class NodeEvent {
	public static class Context {
		public Context() {
		}

		public Behaviour behaviour;

		public DirectedLayout.Node node;

		public NodeEvent nodeEvent;

		public GwtEvent gwtEvent;

		public TopicEvent<?> topicEvent;

		public TopicListeners topicListeners;

		public <A extends Annotation> A annotation(Class<A> clazz) {
			return node.annotation(clazz);
		}

		public <B extends Bindable> B typedModel() {
			return (B) node.getModel();
		}

		public <E extends GwtEvent> E typedEvent() {
			return (E) gwtEvent;
		}
	}

	public interface Handler {
		public abstract void onEvent(NodeEvent.Context eventContext);
	}

	@ClientInstantiable
	public static abstract class AbstractHandler implements Handler {
	}

	private NodeEventReceiver eventReceiver;

	public void setReceiver(NodeEventReceiver eventReceiver) {
		this.eventReceiver = eventReceiver;
	}

	static Logger logger = LoggerFactory.getLogger(NodeEvent.class);
	static {
		AlcinaLogUtils.sysLogClient(NodeEvent.class, Level.OFF);
	}

	protected void fireEvent(GwtEvent gwtEvent) {
		eventReceiver.onEvent(gwtEvent);
	}

	protected HandlerRegistration handlerRegistration;

	public void bind(Widget widget, boolean bind) {
		logger.info("bind: {} {} {}",
				widget == null ? "(unbind)" : widget.getClass().getSimpleName(),
				getClass().getSimpleName(), bind);
		if (!bind) {
			if (handlerRegistration != null) {
				handlerRegistration.removeHandler();
				handlerRegistration = null;
			}
		} else {
			if (handlerRegistration != null) {
				return;
			}
			handlerRegistration = bind0(widget);
		}
	}

	protected void unbind() {
		bind(null, false);
	}

	protected abstract HandlerRegistration bind0(Widget widget);
}
