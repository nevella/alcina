package cc.alcina.framework.gwt.client.dirndl.behaviour;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.ElementRemote;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyEvent;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
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

	public static class EnterPressed extends NodeEvent<EnterPressed.Handler>
			implements KeyPressHandler {
		@Override
		public void dispatch(EnterPressed.Handler handler) {
			handler.onEnterPressed(this);
		}

		@Override
		public Class<EnterPressed.Handler> getHandlerClass() {
			return EnterPressed.Handler.class;
		}

		@Override
		public void onKeyPress(KeyPressEvent event) {
			handleEvent(event);
		}

		private void handleEvent(KeyEvent event) {
			char charCode = event instanceof KeyPressEvent
					? ((KeyPressEvent) event).getCharCode()
					: '0';
			int keyCode = event.getNativeEvent().getKeyCode();
			if (charCode == KeyCodes.KEY_ENTER
					|| keyCode == KeyCodes.KEY_ENTER) {
				fireEvent(event);
			}
		}

		@Override
		protected HandlerRegistration bind0(Widget widget) {
			return widget.addDomHandler(this::handleEvent,
					KeyPressEvent.getType());
		}

		public interface Handler extends NodeEvent.Handler {
			void onEnterPressed(EnterPressed event);
		}
	}

	public static class IntersectionObserved
			extends NodeEvent<IntersectionObserved.Handler> {
		private IntersectionObserver intersectionObserver;

		private boolean intersecting;

		@Override
		public void dispatch(Handler handler) {
			handler.onIntersectionObserved(this);
		}

		public void fireEvent(boolean visible) {
			IntersectionObserved event = new IntersectionObserved();
			event.setIntersecting(visible);
			super.fireEvent(event);
		}

		@Override
		public Class<Handler> getHandlerClass() {
			return IntersectionObserved.Handler.class;
		}

		public boolean isIntersecting() {
			return getContext() == null ? this.intersecting
					: ((IntersectionObserved) getContext().gwtEvent).intersecting;
		}

		public void setIntersecting(boolean intersecting) {
			this.intersecting = intersecting;
		}

		@Override
		protected HandlerRegistration bind0(Widget widget) {
			widget.addAttachHandler(evt -> {
				if (evt.isAttached()) {
					intersectionObserver = IntersectionObserver.observerFor(
							this,
							widget.getElement().implAccess().ensureRemote());
				} else {
					intersectionObserver.disconnect();
				}
			});
			return null;
		}

		public interface Handler extends NodeEvent.Handler {
			void onIntersectionObserved(IntersectionObserved event);
		}

		public static final class IntersectionObserver
				extends JavaScriptObject {
			public static final native IntersectionObserver observerFor(
					IntersectionObserved intersectionObserved,
					ElementRemote elt) /*-{
        var callback = $entry(function(entries, observer) {
          for ( var k in entries) {
            intersectionObserved.@cc.alcina.framework.gwt.client.dirndl.behaviour.InferredDomEvents.IntersectionObserved::fireEvent(Z)(entries[k].isIntersecting);
          }
        });
        var scrollCursor = elt;
        while (scrollCursor != document.body) {
          var style = $wnd.getComputedStyle(scrollCursor);
          if (style.overflow == 'scroll' || style.overflowX == 'scroll'
              || style.overflowY == 'scroll') {
            break;
          }
          scrollCursor = scrollCursor.parentElement;
        }
        var observer = new IntersectionObserver(callback, {
          root : scrollCursor,
          threshold : 1.0
        });
        observer.observe(elt);
        return observer;
			}-*/;

			protected IntersectionObserver() {
			}

			final native void disconnect() /*-{
        this.disconnect();
			}-*/;
		}
	}

	/**
	 * Unless it's guaranteed that the callback will be inexpensive, use the
	 * RequestAnimation subclass, which ensures smoothness
	 *
	 * @author nick@alcina.cc
	 *
	 */
	public static class ResizeObserved
			extends NodeEvent<ResizeObserved.Handler> {
		private ResizeObserver resizeObserver;

		@Override
		public void dispatch(Handler handler) {
			handler.onResizeObserved(this);
		}

		public void fireEvent() {
			ResizeObserved event = new ResizeObserved();
			super.fireEvent(event);
		}

		@Override
		public Class<Handler> getHandlerClass() {
			return ResizeObserved.Handler.class;
		}

		@Override
		protected HandlerRegistration bind0(Widget widget) {
			widget.addAttachHandler(evt -> {
				if (evt.isAttached()) {
					resizeObserver = ResizeObserver.observerFor(this,
							widget.getElement().implAccess().ensureRemote());
				} else {
					resizeObserver.disconnect();
				}
			});
			return null;
		}

		public interface Handler extends NodeEvent.Handler {
			void onResizeObserved(ResizeObserved event);
		}

		public static class RequestAnimation extends ResizeObserved {
			private RequestAnimationFrameGate gate = new RequestAnimationFrameGate();

			@Override
			public void fireEvent() {
				gate.schedule(super::fireEvent);
			}
		}

		public static final class ResizeObserver extends JavaScriptObject {
			public static final native ResizeObserver observerFor(
					ResizeObserved resizeObserved, ElementRemote elt) /*-{
        var callback = $entry(function(entries, observer) {
          for ( var k in entries) {
            //there's info in the entry (a contentBox or contentRect, browser-dependent) - but not interested
            resizeObserved.@cc.alcina.framework.gwt.client.dirndl.behaviour.InferredDomEvents.ResizeObserved::fireEvent()();
          }
        });
        var observer = new ResizeObserver(callback);
        observer.observe(elt);
        return observer;
			}-*/;

			protected ResizeObserver() {
			}

			final native void disconnect() /*-{
        this.disconnect();
			}-*/;
		}
	}
}
