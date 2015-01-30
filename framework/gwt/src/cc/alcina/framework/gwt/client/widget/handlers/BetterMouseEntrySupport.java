package cc.alcina.framework.gwt.client.widget.handlers;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.event.dom.client.HasMouseMoveHandlers;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.event.logical.shared.AttachEvent.Handler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Event.NativePreviewHandler;
import com.google.gwt.user.client.ui.Widget;

public class BetterMouseEntrySupport implements Handler, NativePreviewHandler,
		MouseMoveHandler {
	private HandlerRegistration nativePreviewHandlerRegistration;

	private Widget widget;

	public BetterMouseEntrySupport(Widget widget) {
		this.widget = widget;
		widget.addAttachHandler(this);
		((HasMouseMoveHandlers) widget).addMouseMoveHandler(this);
	}

	private void ensurePreview() {
		if (this.nativePreviewHandlerRegistration == null) {
			this.nativePreviewHandlerRegistration = Event
					.addNativePreviewHandler(this);
		}
	}

	@Override
	public void onAttachOrDetach(AttachEvent event) {
		if (!event.isAttached()) {
			removePreviewHandler();
		}
	}

	private void removePreviewHandler() {
		if (this.nativePreviewHandlerRegistration != null) {
			this.nativePreviewHandlerRegistration.removeHandler();
			this.nativePreviewHandlerRegistration = null;
		}
	}

	@Override
	public void onPreviewNativeEvent(NativePreviewEvent event) {
		Event nativeEvent = Event.as(event.getNativeEvent());
		EventTarget target = nativeEvent.getEventTarget();
		if (Element.is(target)) {
			if (!widget.getElement().isOrHasChild(Element.as(target))) {
				removePreviewHandler();
			}
		}
	}

	public boolean isOver() {
		return nativePreviewHandlerRegistration != null;
	}

	@Override
	public void onMouseMove(MouseMoveEvent event) {
		ensurePreview();
	}
}
