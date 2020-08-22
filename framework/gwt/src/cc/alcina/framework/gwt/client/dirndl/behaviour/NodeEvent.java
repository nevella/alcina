package cc.alcina.framework.gwt.client.dirndl.behaviour;

import java.lang.annotation.Annotation;
import java.util.logging.Level;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.log.AlcinaLogUtils;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.gwt.client.dirndl.annotation.Behaviour;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.NodeEventReceiver;

@ClientInstantiable
public abstract class NodeEvent {
	public static class Context {
		public Behaviour behaviour;

		public DirectedLayout.Node node;

		public NodeEvent nodeEvent;

		public GwtEvent gwtEvent;

		public Widget resolveHandlerTarget() {
			return node.resolveWidget(behaviour.handlerTargetPath());
		}

		public <A extends Annotation> A annotation(Class<A> clazz) {
			return node.annotation(clazz);
		}
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
