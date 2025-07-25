package cc.alcina.framework.gwt.client.dirndl.event;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.dom.client.LocalDom;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.NativeEvent.BeforeInputEventData;
import com.google.gwt.dom.client.TextAreaElement;
import com.google.gwt.event.dom.client.BeforeInputEvent;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusinEvent;
import com.google.gwt.event.dom.client.FocusoutEvent;
import com.google.gwt.event.dom.client.InputEvent;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.ScrollEvent;
import com.google.gwt.event.dom.client.SelectionChangedEvent;
import com.google.gwt.event.shared.HandlerRegistration;

import cc.alcina.framework.gwt.client.dirndl.layout.DomBinding;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

public class DomEvents {
	public static class BeforeInput extends NodeEvent<BeforeInput.Handler> {
		private BeforeInputEventData beforeInputEventData;

		public BeforeInputEventData getBeforeInputEventData() {
			return beforeInputEventData;
		}

		@Override
		public void dispatch(BeforeInput.Handler handler) {
			if (beforeInputEventData == null) {
				NativeEvent nativeEvent = ((BeforeInputEvent) getContext()
						.getGwtEvent()).getNativeEvent();
				this.beforeInputEventData = nativeEvent
						.getBeforeInputEventData();
			}
			handler.onBeforeInput(this);
		}

		public static class BindingImpl extends DomBinding<BeforeInput> {
			@Override
			protected HandlerRegistration bind1(Element element) {
				return element.addDomHandler(this::fireEvent,
						BeforeInputEvent.getType());
			}
		}

		public interface Handler extends NodeEvent.Handler {
			void onBeforeInput(BeforeInput event);
		}
	}

	public static class Blur extends NodeEvent<Blur.Handler> {
		@Override
		public void dispatch(Blur.Handler handler) {
			handler.onBlur(this);
		}

		public static class BindingImpl extends DomBinding<Blur> {
			@Override
			protected HandlerRegistration bind1(Element element) {
				return element.addDomHandler(this::fireEvent,
						BlurEvent.getType());
			}
		}

		public interface Handler extends NodeEvent.Handler {
			void onBlur(Blur event);
		}
	}

	/**
	 * Don't use for handling form [enter] events - use
	 * InferredDomEvents.InputEnterCommit
	 *
	 * 
	 *
	 */
	public static class Change extends NodeEvent<Change.Handler> {
		@Override
		public void dispatch(Change.Handler handler) {
			handler.onChange(this);
		}

		public static class BindingImpl extends DomBinding<Change> {
			@Override
			protected HandlerRegistration bind1(Element element) {
				return element.addDomHandler(this::fireEvent,
						ChangeEvent.getType());
			}
		}

		public interface Handler extends NodeEvent.Handler {
			void onChange(Change event);
		}
	}

	/**
	 * Note that this fires on any (not just left) click. For just leftclick,
	 * use InferredDomEvents.LeftClick
	 *
	 * 
	 *
	 */
	public static class Click extends NodeEvent<Click.Handler> {
		@Override
		public void dispatch(Click.Handler handler) {
			handler.onClick(this);
		}

		public static class BindingImpl extends DomBinding<Click> {
			@Override
			protected HandlerRegistration bind1(Element element) {
				return element.addDomHandler(this::fireEvent,
						ClickEvent.getType());
			}
		}

		public interface Handler extends NodeEvent.Handler {
			void onClick(Click event);
		}
	}

	public static class Focus extends NodeEvent<Focus.Handler> {
		@Override
		public void dispatch(Focus.Handler handler) {
			handler.onFocus(this);
		}

		public static class BindingImpl extends DomBinding<Focus> {
			@Override
			protected HandlerRegistration bind1(Element element) {
				return element.addDomHandler(this::fireEvent,
						FocusEvent.getType());
			}
		}

		public interface Handler extends NodeEvent.Handler {
			void onFocus(Focus event);
		}
	}

	public static class Focusin extends NodeEvent<Focusin.Handler> {
		@Override
		public void dispatch(Focusin.Handler handler) {
			handler.onFocusin(this);
		}

		public static class BindingImpl extends DomBinding<Focusin> {
			@Override
			protected HandlerRegistration bind1(Element element) {
				return element.addDomHandler(this::fireEvent,
						FocusinEvent.getType());
			}
		}

		public interface Handler extends NodeEvent.Handler {
			void onFocusin(Focusin event);
		}

		public interface Binding extends Handler {
			@Override
			default void onFocusin(Focusin event) {
				((Model) this).bindings().onNodeEvent(event);
			}
		}
	}

	public static class Focusout extends NodeEvent<Focusout.Handler> {
		@Override
		public void dispatch(Focusout.Handler handler) {
			handler.onFocusout(this);
		}

		public static class BindingImpl extends DomBinding<Focusout> {
			@Override
			protected HandlerRegistration bind1(Element element) {
				return element.addDomHandler(this::fireEvent,
						FocusoutEvent.getType());
			}
		}

		public interface Handler extends NodeEvent.Handler {
			void onFocusout(Focusout event);
		}

		public interface Binding extends Handler {
			@Override
			default void onFocusout(Focusout event) {
				((Model) this).bindings().onNodeEvent(event);
			}
		}
	}

	public static class Input extends NodeEvent<Input.Handler> {
		private boolean populated = false;

		private String value;

		@Override
		public void dispatch(Input.Handler handler) {
			if (!populated) {
				populated = true;
				EventTarget eventTarget = ((InputEvent) getContext()
						.getGwtEvent()).getNativeEvent().getEventTarget();
				Element element = Element.as(eventTarget);
				if (element.getTagName().equalsIgnoreCase("input")) {
					value = ((InputElement) element).getValue();
				} else if (element.getTagName().equalsIgnoreCase("textarea")) {
					value = ((TextAreaElement) element).getValue();
				} else {
					// contenteditable
					value = null;
				}
			}
			handler.onInput(this);
		}

		public String getValue() {
			return this.value;
		}

		public static class BindingImpl extends DomBinding<Input> {
			@Override
			protected HandlerRegistration bind1(Element element) {
				return element.addDomHandler(this::fireEvent,
						InputEvent.getType());
			}
		}

		public interface Handler extends NodeEvent.Handler {
			void onInput(Input event);
		}
	}

	public static class KeyDown extends NodeEvent<KeyDown.Handler> {
		@Override
		public void dispatch(KeyDown.Handler handler) {
			handler.onKeyDown(this);
		}

		public static class BindingImpl extends DomBinding<KeyDown> {
			@Override
			protected HandlerRegistration bind1(Element element) {
				return element.addDomHandler(this::fireEvent,
						KeyDownEvent.getType());
			}
		}

		public interface Handler extends NodeEvent.Handler {
			void onKeyDown(KeyDown event);
		}
	}

	public static class KeyPress extends NodeEvent<KeyPress.Handler> {
		@Override
		public void dispatch(KeyPress.Handler handler) {
			handler.onKeyPress(this);
		}

		public static class BindingImpl extends DomBinding<KeyPress> {
			@Override
			protected HandlerRegistration bind1(Element element) {
				return element.addDomHandler(this::fireEvent,
						KeyPressEvent.getType());
			}
		}

		public interface Handler extends NodeEvent.Handler {
			void onKeyPress(KeyPress event);
		}
	}

	/**
	 * <p>
	 * DO NOT USE (here for completeness)
	 * 
	 * <p>
	 * Since this event fires on the document, not the element containing the
	 * selection, routing can't be done here - use the corresponding
	 * {@link InferredDomEvents.SelectionChanged} event
	 */
	public static class SelectionChanged
			extends NodeEvent<SelectionChanged.Handler> {
		@Override
		public void dispatch(SelectionChanged.Handler handler) {
			handler.onSelectionChanged(this);
		}

		public static class BindingImpl extends DomBinding<SelectionChanged> {
			@Override
			protected HandlerRegistration bind1(Element element) {
				return element.addDomHandler(this::fireEvent,
						SelectionChangedEvent.getType());
			}
		}

		public interface Handler extends NodeEvent.Handler {
			void onSelectionChanged(SelectionChanged event);
		}
	}

	public static class KeyUp extends NodeEvent<KeyUp.Handler> {
		@Override
		public void dispatch(KeyUp.Handler handler) {
			handler.onKeyUp(this);
		}

		public static class BindingImpl extends DomBinding<KeyUp> {
			@Override
			protected HandlerRegistration bind1(Element element) {
				return element.addDomHandler(this::fireEvent,
						KeyUpEvent.getType());
			}
		}

		public interface Handler extends NodeEvent.Handler {
			void onKeyUp(KeyUp event);
		}
	}

	public static class MouseDown extends NodeEvent<MouseDown.Handler> {
		@Override
		public void dispatch(MouseDown.Handler handler) {
			handler.onMouseDown(this);
		}

		public static class BindingImpl extends DomBinding<MouseDown> {
			@Override
			protected HandlerRegistration bind1(Element element) {
				return element.addDomHandler(this::fireEvent,
						MouseDownEvent.getType());
			}
		}

		public interface Handler extends NodeEvent.Handler {
			void onMouseDown(MouseDown event);
		}
	}

	public static class MouseMove extends NodeEvent<MouseMove.Handler> {
		@Override
		public void dispatch(MouseMove.Handler handler) {
			handler.onMouseMove(this);
		}

		public static class BindingImpl extends DomBinding<MouseMove> {
			@Override
			protected HandlerRegistration bind1(Element element) {
				return element.addDomHandler(this::fireEvent,
						MouseMoveEvent.getType());
			}
		}

		public interface Handler extends NodeEvent.Handler {
			void onMouseMove(MouseMove event);
		}
	}

	public static class MouseOut extends NodeEvent<MouseOut.Handler> {
		@Override
		public void dispatch(MouseOut.Handler handler) {
			handler.onMouseOut(this);
		}

		public static class BindingImpl extends DomBinding<MouseOut> {
			@Override
			protected HandlerRegistration bind1(Element element) {
				return element.addDomHandler(this::fireEvent,
						MouseOutEvent.getType());
			}
		}

		public interface Handler extends NodeEvent.Handler {
			void onMouseOut(MouseOut event);
		}
	}

	public static class MouseOver extends NodeEvent<MouseOver.Handler> {
		@Override
		public void dispatch(MouseOver.Handler handler) {
			handler.onMouseOver(this);
		}

		public static class BindingImpl extends DomBinding<MouseOver> {
			@Override
			protected HandlerRegistration bind1(Element element) {
				return element.addDomHandler(this::fireEvent,
						MouseOverEvent.getType());
			}
		}

		public interface Handler extends NodeEvent.Handler {
			void onMouseOver(MouseOver event);
		}
	}

	public static class MouseUp extends NodeEvent<MouseUp.Handler> {
		@Override
		public void dispatch(MouseUp.Handler handler) {
			handler.onMouseUp(this);
		}

		public static class BindingImpl extends DomBinding<MouseUp> {
			@Override
			protected HandlerRegistration bind1(Element element) {
				return element.addDomHandler(this::fireEvent,
						MouseUpEvent.getType());
			}
		}

		public interface Handler extends NodeEvent.Handler {
			void onMouseUp(MouseUp event);
		}
	}

	public static class Scroll extends NodeEvent<Scroll.Handler> {
		@Override
		public void dispatch(Scroll.Handler handler) {
			handler.onScroll(this);
		}

		public static class BindingImpl extends DomBinding<Scroll> {
			@Override
			protected HandlerRegistration bind1(Element element) {
				return element.addDomHandler(this::fireEvent,
						ScrollEvent.getType());
			}
		}

		public interface Handler extends NodeEvent.Handler {
			void onScroll(Scroll event);
		}
	}

	/*
	 * Doesn't work because requires element to be attached to the dom - would
	 * require a bit of a rework of event system to get it to work (and just
	 * intercepting an[enter] key works just as well)
	 */
	public static class Submit extends NodeEvent<Submit.Handler> {
		@Override
		public void dispatch(Submit.Handler handler) {
			handler.onSubmit(this);
		}

		public static class BindingImpl extends DomBinding<Submit> {
			@Override
			protected HandlerRegistration bind1(Element element) {
				// required since form submit doesn't propagate
				LocalDom.flush();
				return element.addBitlessDomHandler(this::fireEvent,
						DomSubmitEvent.getType());
			}
		}

		public interface Handler extends NodeEvent.Handler {
			void onSubmit(Submit event);
		}
	}
}
