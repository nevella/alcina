package cc.alcina.framework.gwt.client.dirndl.behaviour;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
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
}
