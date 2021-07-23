package cc.alcina.framework.gwt.client.dirndl.behaviour;

import java.lang.annotation.Annotation;
import java.util.logging.Level;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.log.AlcinaLogUtils;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.NodeEventReceiver;
import cc.alcina.framework.gwt.client.dirndl.layout.TopicEvent;
import cc.alcina.framework.gwt.client.dirndl.layout.TopicEvent.TopicListeners;

@ClientInstantiable
/*
 * FIXME - dirndl 1.3 - don't like the interplay between NodeEvent,
 * BehaviourBinding and NodeEventReceiver...
 * 
 * but...maybe...it's right? It's an event fired on a node, so as long as
 * there's one nodeevent per logical 'event', per node, maybe it's right...?
 * weird...
 * 
 * Actually (TODO: document better) - this is totally correct - and feature, not
 * bug - dirndl node events always only have one receiver (the node) -
 * topic/message passing generates new events at the receiver node
 * 
 * Implementation - extend nodeevent (to future-proof against using the gwt
 * event bus) but, because our propagation model is differnt, don't implement
 * the dispatch/TYPE mechanism
 */
public abstract class NodeEvent<H extends NodeEvent.Handler>
		extends GwtEvent<H> {
	static Logger logger = LoggerFactory.getLogger(NodeEvent.class);
	static {
		AlcinaLogUtils.sysLogClient(NodeEvent.class, Level.OFF);
	}

	private NodeEventReceiver eventReceiver;

	protected HandlerRegistration handlerRegistration;

	private Context context;

	protected Object model;

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

	@Override
	/*
	 * Not (yet) dispatching via gwt event bus - so force public
	 */
	public abstract void dispatch(H handler);

	@Override
	public final Type<H> getAssociatedType() {
		throw new UnsupportedOperationException();
	}

	public Context getContext() {
		return this.context;
	}

	public abstract Class<H> getHandlerClass();

	public void setContext(Context context) {
		this.context = context;
	}

	public void setEventReceiver(NodeEventReceiver eventReceiver) {
		this.eventReceiver = eventReceiver;
	}

	public void setModel(Object model) {
		if (model instanceof NodeEvent) {
			int debug = 3;
		}
		this.model = model;
	}

	@Override
	public String toString() {
		return Ax.format("%s : %s", getClass().getSimpleName(), model);
	}

	protected abstract HandlerRegistration bind0(Widget widget);

	protected void fireEvent(GwtEvent gwtEvent) {
		eventReceiver.onEvent(gwtEvent);
	}

	protected void unbind() {
		bind(null, false);
	}

	public static class Context {
		public static Context newTopicContext(Context previous, Node node) {
			Context context = new Context();
			context.previous = previous;
			context.node = node == null ? previous.node : node;
			context.topicListeners = new TopicListeners();
			return context;
		}

		public static Context newTopicContext(GwtEvent event, Node node) {
			Context context = new Context();
			context.gwtEvent = event;
			context.node = node;
			context.topicListeners = new TopicListeners();
			return context;
		}

		public Context previous;

		public DirectedLayout.Node node;

		private NodeEvent nodeEvent;

		public GwtEvent gwtEvent;

		public TopicListeners topicListeners;

		public Context() {
		}

		public <A extends Annotation> A annotation(Class<A> clazz) {
			return node.annotation(clazz);
		}

		public NodeEvent getNodeEvent() {
			return nodeEvent;
		}

		public boolean hasPrevious(Class<? extends NodeEvent> eventClass) {
			if (eventClass == nodeEvent.getClass()) {
				return true;
			}
			if (previous == null) {
				return false;
			}
			return previous.hasPrevious(eventClass);
		}

		public void markCauseTopicAsNotHandled() {
			((TopicEvent) previous.getNodeEvent()).setHandled(false);
		}

		public void setNodeEvent(NodeEvent nodeEvent) {
			this.nodeEvent = nodeEvent;
			nodeEvent.context = this;
		}
	}

	public interface Handler extends EventHandler {
	}

	public static abstract class ModelEvent<T, H extends NodeEvent.Handler>
			extends NodeEvent<H> {
		public <T0 extends T> T0 getModel() {
			return (T0) this.model;
		}
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
}
