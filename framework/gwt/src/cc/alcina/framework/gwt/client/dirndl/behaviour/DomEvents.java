package cc.alcina.framework.gwt.client.dirndl.behaviour;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.dom.client.LocalDom;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.InputEvent;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.ScrollEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Widget;

public class DomEvents {
	public static class Change extends NodeEvent<Change.Handler> {
		@Override
		public void dispatch(Change.Handler handler) {
			handler.onChange(this);
		}

		@Override
		public Class<Change.Handler> getHandlerClass() {
			return Change.Handler.class;
		}

		@Override
		protected HandlerRegistration bind0(Widget widget) {
			return widget.addDomHandler(this::fireEvent, ChangeEvent.getType());
		}

		public interface Handler extends NodeEvent.Handler {
			void onChange(Change event);
		}
	}

	public static class Click extends NodeEvent<Click.Handler> {
		@Override
		public void dispatch(Click.Handler handler) {
			handler.onClick(this);
		}

		@Override
		public Class<Click.Handler> getHandlerClass() {
			return Click.Handler.class;
		}

		@Override
		protected HandlerRegistration bind0(Widget widget) {
			return widget.addDomHandler(this::fireEvent, ClickEvent.getType());
		}

		public interface Handler extends NodeEvent.Handler {
			void onClick(Click event);
		}
	}

	public static class Input extends NodeEvent<Input.Handler> {
		private boolean populated = false;
		
		private String value;

		public String getValue() {
			return this.value;
		}

		@Override
		public void dispatch(Input.Handler handler) {
			if (!populated) {
				populated = true;
				EventTarget eventTarget = ((InputEvent) getContext().gwtEvent)
						.getNativeEvent().getEventTarget();
				value=((InputElement) Element.as(eventTarget)).getValue();
			}
			handler.onInput(this);
		}

		@Override
		public Class<Input.Handler> getHandlerClass() {
			return Input.Handler.class;
		}

		@Override
		protected HandlerRegistration bind0(Widget widget) {
			return widget.addDomHandler(this::fireEvent, InputEvent.getType());
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

		@Override
		public Class<KeyDown.Handler> getHandlerClass() {
			return KeyDown.Handler.class;
		}

		@Override
		protected HandlerRegistration bind0(Widget widget) {
			return widget.addDomHandler(this::fireEvent,
					KeyDownEvent.getType());
		}

		public interface Handler extends NodeEvent.Handler {
			void onKeyDown(KeyDown event);
		}
	}

	public static class KeyUp extends NodeEvent<KeyUp.Handler> {
		@Override
		public void dispatch(KeyUp.Handler handler) {
			handler.onKeyUp(this);
		}

		@Override
		public Class<KeyUp.Handler> getHandlerClass() {
			return KeyUp.Handler.class;
		}

		@Override
		protected HandlerRegistration bind0(Widget widget) {
			return widget.addDomHandler(this::fireEvent, KeyUpEvent.getType());
		}

		public interface Handler extends NodeEvent.Handler {
			void onKeyUp(KeyUp event);
		}
	}

	public static class Scroll extends NodeEvent<Scroll.Handler> {
		@Override
		public void dispatch(Scroll.Handler handler) {
			handler.onScroll(this);
		}

		@Override
		public Class<Scroll.Handler> getHandlerClass() {
			return Scroll.Handler.class;
		}

		@Override
		protected HandlerRegistration bind0(Widget widget) {
			return widget.addDomHandler(this::fireEvent, ScrollEvent.getType());
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

		@Override
		public Class<Submit.Handler> getHandlerClass() {
			return Submit.Handler.class;
		}

		@Override
		protected HandlerRegistration bind0(Widget widget) {
			// required since form submit doesn't propagate
			LocalDom.flush();
			return widget.addBitlessDomHandler(this::fireEvent,
					DomSubmitEvent.getType());
		}

		public interface Handler extends NodeEvent.Handler {
			void onSubmit(Submit event);
		}
	}
}
