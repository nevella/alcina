package cc.alcina.framework.gwt.client.dirndl.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.ElementJso;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.LocalDom;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.mutations.MutationRecord;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasNativeEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Event.NativePreviewHandler;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.util.Al;
import cc.alcina.framework.common.client.util.TopicListener;
import cc.alcina.framework.gwt.client.dirndl.layout.DomBinding;
import cc.alcina.framework.gwt.client.util.WidgetUtils;

public class InferredDomEvents {
	public static class ActionOutside extends NodeEvent<ActionOutside.Handler> {
		@Override
		public void dispatch(ActionOutside.Handler handler) {
			handler.onActionOutside(this);
		}

		// TODO - this extra registration is caused by buildtimereflection not
		// handling the generic parameter correctly
		// it's patched because there are only a few cases (where there's two
		// layers of inheritance - DomBinding -> EventRelativeBinding ->
		// BindingImpl), but it should be
		// fixed
		@Registration({ DomBinding.class, ActionOutside.class })
		public static class BindingImpl
				extends EventRelativeBinding<ActionOutside> {
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

		@Registration({ DomBinding.class, ClickOutside.class })
		public static class BindingImpl
				extends EventRelativeBinding<ClickOutside> {
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

	/**
	 * This effectively models a DOM selection changed on the element (or
	 * subtree)
	 */
	public static class SelectionChanged
			extends NodeEvent<SelectionChanged.Handler> {
		@Override
		public void dispatch(SelectionChanged.Handler handler) {
			handler.onSelectionChanged(this);
		}

		@Registration({ DomBinding.class, SelectionChanged.class })
		public static class BindingImpl
				extends NativePreviewBinding<SelectionChanged> {
			@Override
			protected boolean isObservedEvent(NativePreviewEvent event) {
				return Objects.equals(
						Event.as(event.getNativeEvent()).getType(),
						BrowserEvents.SELECTIONCHANGE);
			}
		}

		public interface Handler extends NodeEvent.Handler {
			void onSelectionChanged(SelectionChanged event);
		}
	}

	public static class CtrlEnterPressed
			extends NodeEvent<CtrlEnterPressed.Handler> {
		@Override
		public void dispatch(CtrlEnterPressed.Handler handler) {
			handler.onCtrlEnterPressed(this);
		}

		public static class BindingImpl extends DomBinding<CtrlEnterPressed>
				implements KeyUpHandler {
			@Override
			protected HandlerRegistration bind1(Element element) {
				return element.addDomHandler(this::onKeyUp,
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

		public static class BindingImpl extends DomBinding<EnterPressed>
				implements KeyUpHandler {
			@Override
			protected HandlerRegistration bind1(Element element) {
				return element.addDomHandler(this::onKeyUp,
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

		public static class BindingImpl extends DomBinding<EscapePressed>
				implements KeyUpHandler {
			@Override
			protected HandlerRegistration bind1(Element element) {
				return element.addDomHandler(this::onKeyUp,
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

	static abstract class NativePreviewBinding<E extends NodeEvent>
			extends DomBinding<E> implements NativePreviewHandler {
		@Override
		protected HandlerRegistration bind0(Element element, Object model) {
			return Event.addNativePreviewHandler(this);
		}

		@Override
		protected HandlerRegistration bind1(Element element) {
			// never called
			throw new UnsupportedOperationException();
		}

		protected abstract boolean isObservedEvent(NativePreviewEvent event);

		@Override
		public void onPreviewNativeEvent(NativePreviewEvent event) {
			if (!isObservedEvent(event)) {
				return;
			}
			Event nativeEvent = Event.as(event.getNativeEvent());
			Scheduler.get().scheduleFinally(() -> {
				if (handlerRegistration == null) {
					// was unbound by a prior finally event
					return;
				}
				fireEvent(new NativePreviewEventAsync(nativeEvent));
			});
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
	static abstract class EventRelativeBinding<E extends NodeEvent>
			extends DomBinding<E> implements NativePreviewHandler {
		protected boolean modal;

		Element element;

		@Override
		protected HandlerRegistration bind0(Element element, Object model) {
			this.element = element;
			this.modal = model instanceof IsModal
					&& ((IsModal) model).provideModal();
			return Event.addNativePreviewHandler(this);
		}

		@Override
		protected HandlerRegistration bind1(Element element) {
			// never called
			throw new UnsupportedOperationException();
		}

		private boolean eventTargetsWidget(NativePreviewEvent event) {
			EventTarget target = event.getNativeEvent().getEventTarget();
			if (Element.is(target)) {
				return element.isOrHasChild(Element.as(target));
			}
			return false;
		}

		/*
		 * true = outside; false = inside
		 */
		protected boolean fireIfOutside() {
			return true;
		}

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
			boolean mobile = EventFrame.get().mobile;
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

		protected abstract boolean isObservedEvent(NativePreviewEvent event);

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
			// copy from NativePreviewEvent singleton
			NativeEvent nativeEvent = event.getNativeEvent();
			boolean fire = eventTargetsWidget ^ fireIfOutside();
			if (fire) {
				Scheduler.get().scheduleFinally(() -> {
					if (handlerRegistration == null) {
						// was unbound by a prior finally event
						return;
					}
					fireEvent(new NativePreviewEventAsync(nativeEvent));
				});
			}
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

		public static class BindingImpl extends DomBinding<InputEnterCommit>
				implements ChangeHandler, KeyUpHandler {
			private boolean enterReceived = false;

			private boolean changeReceivedWhileFocussedElement = false;

			@Override
			protected HandlerRegistration bind1(Element element) {
				MultiHandlerRegistration multiHandlerRegistration = new MultiHandlerRegistration();
				multiHandlerRegistration.add(element
						.addDomHandler(this::onChange, ChangeEvent.getType()));
				multiHandlerRegistration.add(element
						.addDomHandler(this::onKeyUp, KeyUpEvent.getType()));
				return multiHandlerRegistration;
			}

			private void checkFire(GwtEvent event) {
				if (changeReceivedWhileFocussedElement && enterReceived) {
					enterReceived = false;
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
			public void onChange(ChangeEvent event) {
				handleChangeEvent(event);
			}

			@Override
			public void onKeyUp(KeyUpEvent event) {
				enterReceived |= event.getNativeKeyCode() == KeyCodes.KEY_ENTER;
				checkFire(event);
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

		/*
		 * Must defer connect (since requires element connected to the dom) --
		 * so must also check connect occurred on disconnect
		 */
		public static class BindingImpl
				extends DomBinding<IntersectionObserved> {
			private IntersectionObserver intersectionObserver;

			boolean removed = false;

			@Override
			protected HandlerRegistration bind1(Element element) {
				// FIXME - romcom.emul -
				if (Al.isBrowser()) {
					Scheduler.get().scheduleFinally(() -> {
						if (!removed) {
							intersectionObserver = IntersectionObserver
									.observerFor(this, element.implAccess()
											.ensureJsoRemote());
						}
					});
				}
				return new HandlerRegistration() {
					@Override
					public void removeHandler() {
						removed = true;
						if (intersectionObserver != null) {
							intersectionObserver.disconnect();
						}
					}
				};
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
						ElementJso elt) /*-{
          var callback = $entry(function(entries, observer) {
			for ( var k in entries) {
              intersectionObserved.@cc.alcina.framework.gwt.client.dirndl.event.InferredDomEvents.IntersectionObserved.BindingImpl::fireEvent(Z)(entries[k].isIntersecting);
            }
          });
          var	 scrollCursor = elt;
          while (scrollCursor != $doc.body) {
            var style = $wnd.getComputedStyle(scrollCursor);
            if (style.overflow == 'scroll' || style.overflowX == 'scroll'
                || style.overflowY == 'scroll') {
              break;
            }
            scrollCursor = scrollCursor.parentElement;
          }
		  var root = scrollCursor==$doc.body?null:scrollCursor;
          var observer = new IntersectionObserver(callback, {
            root : root,
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

		public static class BindingImpl extends DomBinding<LeftClick>
				implements ClickHandler {
			@Override
			protected HandlerRegistration bind1(Element element) {
				return element.addDomHandler(this::onClick,
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
	public static class MouseDownOutside
			extends NodeEvent<MouseDownOutside.Handler> {
		@Override
		public void dispatch(MouseDownOutside.Handler handler) {
			handler.onMouseDownOutside(this);
		}

		@Registration({ DomBinding.class, MouseDownOutside.class })
		public static class BindingImpl
				extends EventRelativeBinding<MouseDownOutside> {
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

	public static class Mutation extends NodeEvent<Mutation.Handler> {
		public List<MutationRecord> records;

		@Override
		public NodeEvent clone() {
			Mutation mutation = (Mutation) super.clone();
			mutation.records = records;
			return mutation;
		}

		@Override
		public void dispatch(Mutation.Handler handler) {
			handler.onMutation(this);
		}

		public static class BindingImpl extends DomBinding<Mutation> {
			TopicListener<List<MutationRecord>> mutationListener = this::onMutations;

			Element mutationRoot;

			@Override
			protected HandlerRegistration bind1(Element element) {
				this.mutationRoot = element;
				LocalDom.getLocalMutations().topicMutations
						.add(mutationListener);
				return new HandlerRegistration() {
					@Override
					public void removeHandler() {
						LocalDom.getLocalMutations().topicMutations
								.remove(mutationListener);
					}
				};
			}

			void onMutations(List<MutationRecord> mutations) {
				List<MutationRecord> applicableMutations = mutations.stream()
						.filter(m -> mutationRoot.isOrHasChild(
								(com.google.gwt.dom.client.Node) m.target.w3cNode))
						.collect(Collectors.toList());
				if (applicableMutations.isEmpty()) {
					return;
				}
				Mutation mutation = new Mutation();
				mutation.records = applicableMutations;
				super.fireEvent(mutation);
			}
		}

		public interface Handler extends NodeEvent.Handler {
			void onMutation(Mutation event);
		}
	}

	public static class NativePreviewEventAsync
			extends NodeEvent<NativePreviewEventAsync.Handler>
			implements HasNativeEvent {
		private final NativeEvent nativeEvent;

		public NativePreviewEventAsync(NativeEvent nativeEvent) {
			this.nativeEvent = nativeEvent;
		}

		@Override
		public void dispatch(NativePreviewEventAsync.Handler handler) {
			handler.onNativePreviewEventAsync(this);
		}

		@Override
		public NativeEvent getNativeEvent() {
			return this.nativeEvent;
		}

		public interface Handler extends NodeEvent.Handler {
			void onNativePreviewEventAsync(NativePreviewEventAsync event);
		}
	}

	/**
	 * Unless it's guaranteed that the callback will be inexpensive, use the
	 * RequestAnimation subclass, which ensures smoothness
	 *
	 *
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

		public static class BindingImpl extends DomBinding<ResizeObserved> {
			private ResizeObserver resizeObserver;

			boolean removed = false;

			@Override
			protected HandlerRegistration bind1(Element element) {
				HandlerRegistration registration = new HandlerRegistration() {
					@Override
					public void removeHandler() {
						removed = true;
						if (resizeObserver != null) {
							resizeObserver.disconnect();
						}
					}
				};
				// FIXME - romcom.emul -
				if (Al.isBrowser()) {
					Scheduler.get().scheduleFinally(() -> {
						if (!removed) {
							resizeObserver = ResizeObserver.observerFor(this,
									element.implAccess().ensureJsoRemote());
						}
					});
				}
				return registration;
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
					ResizeObserved.BindingImpl resizeObserved, ElementJso elt) /*-{
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
