package cc.alcina.framework.gwt.client.dirndl.event;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.ElementRemote;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Event.NativePreviewHandler;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.gwt.client.dirndl.layout.DomBinding;
import cc.alcina.framework.gwt.client.util.WidgetUtils;

public class InferredDomEvents {
	public static class ClickOutside extends NodeEvent<ClickOutside.Handler> {
		@Override
		public void dispatch(ClickOutside.Handler handler) {
			handler.onClickOutside(this);
		}

		@Override
		public Class<ClickOutside.Handler> getHandlerClass() {
			return ClickOutside.Handler.class;
		}

		@Registration({ DomBinding.class, ClickOutside.class })
		public static class BindingImpl extends DomBinding
				implements NativePreviewHandler {
			private Widget widget;

			@Override
			public HandlerRegistration bind0(Widget widget) {
				this.widget = widget;
				return Event.addNativePreviewHandler(this);
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
		}

		public interface Handler extends NodeEvent.Handler {
			void onClickOutside(ClickOutside event);
		}
	}

	public static class CtrlEnterPressed
			extends NodeEvent<CtrlEnterPressed.Handler> {
		@Override
		public void dispatch(CtrlEnterPressed.Handler handler) {
			handler.onCtrlEnterPressed(this);
		}

		@Override
		public Class<CtrlEnterPressed.Handler> getHandlerClass() {
			return CtrlEnterPressed.Handler.class;
		}

		@Registration({ DomBinding.class, CtrlEnterPressed.class })
		public static class BindingImpl extends DomBinding
				implements KeyUpHandler {
			@Override
			public HandlerRegistration bind0(Widget widget) {
				return widget.addDomHandler(this::onKeyUp,
						KeyUpEvent.getType());
			}

			@Override
			public void onKeyUp(KeyUpEvent event) {
				int keyCode = event.getNativeEvent().getKeyCode();
				if (keyCode == KeyCodes.KEY_ENTER) {
					if (event.isControlKeyDown() || event.isMetaKeyDown()) {
						fireEvent(event);
					}
				}
			}
		}

		public interface Handler extends NodeEvent.Handler {
			void onCtrlEnterPressed(CtrlEnterPressed event);
		}
	}

	public static class EnterPressed extends NodeEvent<EnterPressed.Handler> {
		@Override
		public void dispatch(EnterPressed.Handler handler) {
			handler.onEnterPressed(this);
		}

		@Override
		public Class<EnterPressed.Handler> getHandlerClass() {
			return EnterPressed.Handler.class;
		}

		@Registration({ DomBinding.class, EnterPressed.class })
		public static class BindingImpl extends DomBinding
				implements KeyUpHandler {
			@Override
			public HandlerRegistration bind0(Widget widget) {
				return widget.addDomHandler(this::onKeyUp,
						KeyUpEvent.getType());
			}

			@Override
			public void onKeyUp(KeyUpEvent event) {
				int keyCode = event.getNativeEvent().getKeyCode();
				if (keyCode == KeyCodes.KEY_ENTER) {
					if (!event.isControlKeyDown() && !event.isMetaKeyDown()) {
						fireEvent(event);
					}
				}
			}
		}

		public interface Handler extends NodeEvent.Handler {
			void onEnterPressed(EnterPressed event);
		}
	}

	public static class EscapePressed extends NodeEvent<EscapePressed.Handler> {
		@Override
		public void dispatch(EscapePressed.Handler handler) {
			handler.onEscapePressed(this);
		}

		@Override
		public Class<EscapePressed.Handler> getHandlerClass() {
			return EscapePressed.Handler.class;
		}

		@Registration({ DomBinding.class, EscapePressed.class })
		public static class BindingImpl extends DomBinding
				implements KeyUpHandler {
			@Override
			public HandlerRegistration bind0(Widget widget) {
				return widget.addDomHandler(this::onKeyUp,
						KeyUpEvent.getType());
			}

			@Override
			public void onKeyUp(KeyUpEvent event) {
				int keyCode = event.getNativeEvent().getKeyCode();
				if (keyCode == KeyCodes.KEY_ESCAPE) {
					fireEvent(event);
				}
			}
		}

		public interface Handler extends NodeEvent.Handler {
			void onEscapePressed(EscapePressed event);
		}
	}

	/*
	 * This class is basically to support "[enter] means next/submit"
	 *
	 * FIXME - dirndl 1x2 - even better, wrap the input in a form and intercept
	 * submit
	 */
	public static class InputEnterCommit
			extends NodeEvent<InputEnterCommit.Handler> {
		@Override
		public void dispatch(InputEnterCommit.Handler handler) {
			handler.onInputEnterCommit(this);
		}

		@Override
		public Class<InputEnterCommit.Handler> getHandlerClass() {
			return InputEnterCommit.Handler.class;
		}

		@Registration({ DomBinding.class, InputEnterCommit.class })
		public static class BindingImpl extends DomBinding
				implements ChangeHandler, KeyUpHandler {
			private boolean enterReceived = false;

			private boolean changeReceivedWhileFocussedElement = false;

			@Override
			public HandlerRegistration bind0(Widget widget) {
				MultiHandlerRegistration multiHandlerRegistration = new MultiHandlerRegistration();
				multiHandlerRegistration.add(widget
						.addDomHandler(this::onChange, ChangeEvent.getType()));
				multiHandlerRegistration.add(widget.addDomHandler(this::onKeyUp,
						KeyUpEvent.getType()));
				return multiHandlerRegistration;
			}

			@Override
			public void onChange(ChangeEvent event) {
				handleChangeEvent(event);
			}

			@Override
			public void onKeyUp(KeyUpEvent event) {
				enterReceived |= event.getNativeKeyCode() == KeyCodes.KEY_ENTER;
				checkFire(event);
			}

			private void checkFire(GwtEvent event) {
				if (changeReceivedWhileFocussedElement && enterReceived) {
					fireEvent(event);
				}
			}

			private void handleChangeEvent(ChangeEvent event) {
				// if the document focus is still the source element, and it's
				// <input type='text'>, its value was cxommitted via [enter]
				//
				// except if set by autocomplete. but in that case, an *input*
				// eventwill never have been fired on the element
				EventTarget eventTarget = event.getNativeEvent()
						.getEventTarget();
				if (Element.is(eventTarget)) {
					Element focussedElement = WidgetUtils
							.getFocussedDocumentElement();
					if (Element.as(eventTarget) == focussedElement) {
						changeReceivedWhileFocussedElement = true;
						checkFire(event);
					}
				}
			}
		}

		public interface Handler extends NodeEvent.Handler {
			void onInputEnterCommit(InputEnterCommit event);
		}
	}

	public static class IntersectionObserved
			extends NodeEvent<IntersectionObserved.Handler> {
		private boolean intersecting;

		@Override
		public void dispatch(Handler handler) {
			handler.onIntersectionObserved(this);
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

		@Registration({ DomBinding.class, IntersectionObserved.class })
		public static class BindingImpl extends DomBinding {
			private IntersectionObserver intersectionObserver;

			@Override
			public HandlerRegistration bind0(Widget widget) {
				widget.addAttachHandler(evt -> {
					if (evt.isAttached()) {
						intersectionObserver = IntersectionObserver
								.observerFor(this, widget.getElement()
										.implAccess().ensureRemote());
					} else {
						intersectionObserver.disconnect();
					}
				});
				return null;
			}

			public void fireEvent(boolean visible) {
				IntersectionObserved event = new IntersectionObserved();
				event.setIntersecting(visible);
				super.fireEvent(event);
			}

			public static final class IntersectionObserver
					extends JavaScriptObject {
				public static final native IntersectionObserver observerFor(
						IntersectionObserved.BindingImpl intersectionObserved,
						ElementRemote elt) /*-{
          var callback = $entry(function(entries, observer) {
            for ( var k in entries) {
              intersectionObserved.@cc.alcina.framework.gwt.client.dirndl.event.InferredDomEvents.IntersectionObserved.BindingImpl::fireEvent(Z)(entries[k].isIntersecting);
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
            //fire as soon as 1 pixel is visible
            threshold : 0.0
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

		public interface Handler extends NodeEvent.Handler {
			void onIntersectionObserved(IntersectionObserved event);
		}
	}

	public static class LeftClick extends NodeEvent<LeftClick.Handler> {
		@Override
		public void dispatch(LeftClick.Handler handler) {
			handler.onLeftClick(this);
		}

		@Override
		public Class<LeftClick.Handler> getHandlerClass() {
			return LeftClick.Handler.class;
		}

		@Registration({ DomBinding.class, LeftClick.class })
		public static class BindingImpl extends DomBinding
				implements ClickHandler {
			@Override
			public HandlerRegistration bind0(Widget widget) {
				return widget.addDomHandler(this::onClick,
						ClickEvent.getType());
			}

			@Override
			public void onClick(ClickEvent event) {
				int nativeButton = event.getNativeButton();
				switch (nativeButton) {
				case NativeEvent.BUTTON_MIDDLE:
				case NativeEvent.BUTTON_RIGHT:
					break;
				default:
					// fire on touch as well
					fireEvent(event);
					break;
				}
			}
		}

		public interface Handler extends NodeEvent.Handler {
			void onLeftClick(LeftClick event);
		}
	}

	// as per clickoutside
	public static class MouseUpOutside
			extends NodeEvent<MouseUpOutside.Handler> {
		@Override
		public void dispatch(MouseUpOutside.Handler handler) {
			handler.onMouseUpOutside(this);
		}

		@Override
		public Class<MouseUpOutside.Handler> getHandlerClass() {
			return MouseUpOutside.Handler.class;
		}

		@Registration({ DomBinding.class, MouseUpOutside.class })
		public static class BindingImpl extends DomBinding
				implements NativePreviewHandler {
			private Widget widget;

			@Override
			public HandlerRegistration bind0(Widget widget) {
				this.widget = widget;
				return Event.addNativePreviewHandler(this);
			}

			@Override
			public void onPreviewNativeEvent(NativePreviewEvent event) {
				if (Event.as(event.getNativeEvent())
						.getTypeInt() != Event.ONMOUSEUP) {
					return;
				}
				if (!eventTargetsWidget(event)) {
					// could do some jiggery-pokery to get
					// DomEvent.fireNativeEvent to get us a
					// mouseUpEvent...if
					// needed
					//
					// FIXME - can we scheduleFinally, not deferred?
					Scheduler.get().scheduleDeferred(() -> {
						fireEvent(null);
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
		}

		public interface Handler extends NodeEvent.Handler {
			void onMouseUpOutside(MouseUpOutside event);
		}
	}

	public static class MultiHandlerRegistration
			implements HandlerRegistration {
		List<HandlerRegistration> registrations = new ArrayList<>();

		public void add(HandlerRegistration registration) {
			registrations.add(registration);
		}

		@Override
		public void removeHandler() {
			registrations.forEach(HandlerRegistration::removeHandler);
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
		@Override
		public void dispatch(Handler handler) {
			handler.onResizeObserved(this);
		}

		@Override
		public Class<Handler> getHandlerClass() {
			return ResizeObserved.Handler.class;
		}

		@Registration({ DomBinding.class, ResizeObserved.class })
		public static class BindingImpl extends DomBinding {
			private ResizeObserver resizeObserver;

			@Override
			public HandlerRegistration bind0(Widget widget) {
				widget.addAttachHandler(evt -> {
					if (evt.isAttached()) {
						resizeObserver = ResizeObserver.observerFor(this, widget
								.getElement().implAccess().ensureRemote());
					} else {
						resizeObserver.disconnect();
					}
				});
				return null;
			}

			public void fireEvent() {
				super.fireEvent(null);
			}
		}

		public interface Handler extends NodeEvent.Handler {
			void onResizeObserved(ResizeObserved event);
		}

		public static class RequestAnimation extends ResizeObserved {
			@Registration({ DomBinding.class, RequestAnimation.class })
			public static class BindingImpl extends ResizeObserved.BindingImpl {
				private RequestAnimationFrameGate gate = new RequestAnimationFrameGate();

				@Override
				protected void fireEvent(GwtEvent gwtEvent) {
					gate.schedule(super::fireEvent);
				}
			}
		}

		public static final class ResizeObserver extends JavaScriptObject {
			public static final native ResizeObserver observerFor(
					ResizeObserved.BindingImpl resizeObserved,
					ElementRemote elt) /*-{
        var callback = $entry(function(entries, observer) {
          for ( var k in entries) {
            //there's info in the entry (a contentBox or contentRect, browser-dependent) - but not interested
            resizeObserved.@cc.alcina.framework.gwt.client.dirndl.event.InferredDomEvents.ResizeObserved.BindingImpl::fireEvent()();
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
