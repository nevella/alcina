package cc.alcina.framework.gwt.client.dirndl.event;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.BrowserEvents;
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
import cc.alcina.framework.gwt.client.browsermod.BrowserMod;
import cc.alcina.framework.gwt.client.dirndl.layout.DomBinding;
import cc.alcina.framework.gwt.client.util.WidgetUtils;

public class InferredDomEvents {
	public static class ActionOutside extends NodeEvent<ActionOutside.Handler> {
		@Override
		public void dispatch(ActionOutside.Handler handler) {
			handler.onActionOutside(this);
		}

		@Override
		public Class<ActionOutside.Handler> getHandlerClass() {
			return ActionOutside.Handler.class;
		}

		@Registration({ DomBinding.class, ActionOutside.class })
		public static class BindingImpl extends EventRelativeBinding {
			@Override
			protected boolean isObservedEvent(NativePreviewEvent event) {
				return Event.as(event.getNativeEvent())
						.getTypeInt() == Event.ONCLICK;
			}
		}

		public interface Handler extends NodeEvent.Handler {
			void onActionOutside(ActionOutside event);
		}
	}

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
		public static class BindingImpl extends EventRelativeBinding {
			@Override
			protected boolean isObservedEvent(NativePreviewEvent event) {
				return Event.as(event.getNativeEvent())
						.getTypeInt() == Event.ONCLICK;
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
			public void onKeyUp(KeyUpEvent event) {
				int keyCode = event.getNativeEvent().getKeyCode();
				if (keyCode == KeyCodes.KEY_ENTER) {
					if (event.isControlKeyDown() || event.isMetaKeyDown()) {
						fireEvent(event);
					}
				}
			}

			@Override
			protected HandlerRegistration bind1(Widget widget) {
				return widget.addDomHandler(this::onKeyUp,
						KeyUpEvent.getType());
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
			public void onKeyUp(KeyUpEvent event) {
				int keyCode = event.getNativeEvent().getKeyCode();
				if (keyCode == KeyCodes.KEY_ENTER) {
					if (!event.isControlKeyDown() && !event.isMetaKeyDown()) {
						fireEvent(event);
					}
				}
			}

			@Override
			protected HandlerRegistration bind1(Widget widget) {
				return widget.addDomHandler(this::onKeyUp,
						KeyUpEvent.getType());
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
			public void onKeyUp(KeyUpEvent event) {
				int keyCode = event.getNativeEvent().getKeyCode();
				if (keyCode == KeyCodes.KEY_ESCAPE) {
					fireEvent(event);
				}
			}

			@Override
			protected HandlerRegistration bind1(Widget widget) {
				return widget.addDomHandler(this::onKeyUp,
						KeyUpEvent.getType());
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

			@Override
			protected HandlerRegistration bind1(Widget widget) {
				MultiHandlerRegistration multiHandlerRegistration = new MultiHandlerRegistration();
				multiHandlerRegistration.add(widget
						.addDomHandler(this::onChange, ChangeEvent.getType()));
				multiHandlerRegistration.add(widget.addDomHandler(this::onKeyUp,
						KeyUpEvent.getType()));
				return multiHandlerRegistration;
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
					: ((IntersectionObserved) getContext()
							.getGwtEvent()).intersecting;
		}

		public void setIntersecting(boolean intersecting) {
			this.intersecting = intersecting;
		}

		@Registration({ DomBinding.class, IntersectionObserved.class })
		public static class BindingImpl extends DomBinding {
			private IntersectionObserver intersectionObserver;

			public void fireEvent(boolean visible) {
				IntersectionObserved event = new IntersectionObserved();
				event.setIntersecting(visible);
				super.fireEvent(event);
			}

			@Override
			protected HandlerRegistration bind1(Widget widget) {
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

			@Override
			protected HandlerRegistration bind1(Widget widget) {
				return widget.addDomHandler(this::onClick,
						ClickEvent.getType());
			}
		}

		public interface Handler extends NodeEvent.Handler {
			void onLeftClick(LeftClick event);
		}
	}

	// as per clickoutside
	public static class MouseDownOutside
			extends NodeEvent<MouseDownOutside.Handler> {
		@Override
		public void dispatch(MouseDownOutside.Handler handler) {
			handler.onMouseDownOutside(this);
		}

		@Override
		public Class<MouseDownOutside.Handler> getHandlerClass() {
			return MouseDownOutside.Handler.class;
		}

		@Registration({ DomBinding.class, MouseDownOutside.class })
		public static class BindingImpl extends EventRelativeBinding {
			@Override
			protected boolean isObservedEvent(NativePreviewEvent event) {
				return Event.as(event.getNativeEvent())
						.getTypeInt() == Event.ONMOUSEDOWN;
			}
		}

		public interface Handler extends NodeEvent.Handler {
			void onMouseDownOutside(MouseDownOutside event);
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

			public void fireEvent() {
				super.fireEvent(null);
			}

			@Override
			protected HandlerRegistration bind1(Widget widget) {
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

	/*
	 * Fires an event based on whether the previewed event is inside or outside
	 * the element
	 *
	 * This class also manages the nativepreviewevent [consumed, canceled]
	 * states that are managed by GWT PopupPanel.
	 *
	 * FIXME - dirndl 1x1dz - check on mobile (and maybe remove)
	 */
	static abstract class EventRelativeBinding extends DomBinding
			implements NativePreviewHandler {
		static boolean mobile = BrowserMod.isMobile();

		private Widget widget;

		protected boolean modal;

		/*
		 * follows com.google.gwt.user.client.ui.PopupPanel.previewNativeEvent(
		 * NativePreviewEvent)
		 *
		 * see also OverlayContainer.previewNativeEvent in alcina/e82f44f7e
		 *
		 * TODO - there are several thins going on that should be split:
		 *
		 * @formatter:off
		 * - what does consumed vs canceled *really* mean?
		 * - modal handling (event cancellation/consumption) should be split out from event emission
		 * - 'exit' events (which this binding mostly supports) should be emitted on the event bus *after* native events are consumed
		 * - (but before the scheduled DOM event)
		 *
		 *
		 * @formatter:on
		 */
		@Override
		public void onPreviewNativeEvent(NativePreviewEvent event) {
			if (!isObservedEvent(event)) {
				return;
			}
			// if (handleConsumed(event)) {
			// return;
			// }
			boolean eventTargetsWidget = eventTargetsWidget(event);
			boolean fire = eventTargetsWidget ^ fireIfOutside();
			if (fire) {
				Scheduler.get().scheduleDeferred(() -> {
					fireEvent(event);
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
		protected HandlerRegistration bind0(Widget widget, Object model) {
			this.widget = widget;
			this.modal = model instanceof IsModal
					&& ((IsModal) model).provideModal();
			return Event.addNativePreviewHandler(this);
		}

		@Override
		protected HandlerRegistration bind1(Widget widget) {
			// never called
			throw new UnsupportedOperationException();
		}

		/*
		 * true = outside; false = inside
		 */
		protected boolean fireIfOutside() {
			return true;
		}

		protected abstract boolean isObservedEvent(NativePreviewEvent event);

		/*
		 * Returns true if event should not be fired due to consumption
		 */
		boolean handleConsumed(NativePreviewEvent event) {
			// If the event has been canceled or consumed, ignore it
			if (event.isCanceled() || event.isConsumed()) {
				// We need to ensure that we cancel the event even if its been
				// consumed so
				// that popups lower on the stack do not auto hide
				if (modal) {
					event.cancel();
				}
				return true;
			}
			if (event.isCanceled()) {
				return true;
			}
			boolean eventTargetsWidget = eventTargetsWidget(event);
			// If the event targets the popup or the partner, consume it
			Event nativeEvent = Event.as(event.getNativeEvent());
			EventTarget eTarget = nativeEvent.getEventTarget();
			boolean eventTargetsScrollBar = Element.is(eTarget) && Element
					.as(eTarget).getTagName().equalsIgnoreCase("html");
			boolean wasTouchMaybeDrag = mobile && (BrowserEvents.TOUCHSTART
					.equals(nativeEvent.getType())
					|| BrowserEvents.TOUCHEND.equals(nativeEvent.getType())
					|| BrowserEvents.TOUCHMOVE.equals(nativeEvent.getType())
					|| BrowserEvents.GESTURECHANGE.equals(nativeEvent.getType())
					|| BrowserEvents.GESTUREEND.equals(nativeEvent.getType())
					|| BrowserEvents.GESTURESTART.equals(nativeEvent.getType())
					|| BrowserEvents.SCROLL.equals(nativeEvent.getType()));
			if (eventTargetsWidget || eventTargetsScrollBar
					|| wasTouchMaybeDrag) {
				event.consume();
			}
			return false;
		}
	}
}
