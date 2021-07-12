package cc.alcina.framework.gwt.client.dirndl.behaviour;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Event.NativePreviewHandler;
import com.google.gwt.user.client.ui.Widget;

public class InferredDomEvents {
	public static class ClickOutside extends NodeEvent<ClickOutside.Handler>
			implements NativePreviewHandler {
		private Widget widget;

		@Override
		public void dispatch(ClickOutside.Handler handler) {
			handler.onClickOutside(this);
		}

		@Override
		public Class<ClickOutside.Handler> getHandlerClass() {
			return ClickOutside.Handler.class;
		}

		@Override
		public void onPreviewNativeEvent(NativePreviewEvent event) {
			if (Event.as(event.getNativeEvent())
					.getTypeInt() != Event.ONCLICK) {
				return;
			}
			if (!eventTargetsWidget(event)) {
				// could do some jiggery-pokery to get
				// DomEvent.fireNativeEvent to get us a
				// clickEvent...if
				// needed
				Scheduler.get().scheduleDeferred(() -> {
					fireEvent(null);
					unbind();
				});
			}
		}

		private boolean eventTargetsWidget(NativePreviewEvent event) {
			EventTarget target = event.getNativeEvent().getEventTarget();
			if (Element.is(target)) {
				return widget.getElement().isOrHasChild(Element.as(target));
			}
			return false;
		}

		@Override
		protected HandlerRegistration bind0(Widget widget) {
			this.widget = widget;
			widget.addAttachHandler(evt -> {
				if (!evt.isAttached()) {
					unbind();
				}
			});
			return Event.addNativePreviewHandler(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onClickOutside(ClickOutside event);
		}
	}
}
