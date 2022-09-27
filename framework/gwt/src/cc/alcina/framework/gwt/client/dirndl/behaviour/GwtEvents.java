package cc.alcina.framework.gwt.client.dirndl.behaviour;

import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Widget;

public class GwtEvents {
	/**
	 * This event should not be listened for, since it occurs in a point in the
	 * model rendering sequence where changes to the model should not occur (so
	 * is incorrect as a 'populate me now' signal). Use
	 * LayoutEvents.BeforeRender instead, or LayoutEvents.Bind for symmetric
	 * change binding
	 *
	 * @author nick@alcina.cc
	 *
	 */
	@Deprecated
	public static class Attach extends NodeEvent<Attach.Handler> {
		@Override
		public void dispatch(Attach.Handler handler) {
			handler.onAttach(this);
		}

		@Override
		public Class<Attach.Handler> getHandlerClass() {
			return Attach.Handler.class;
		}

		public boolean isAttached() {
			return ((AttachEvent) getContext().gwtEvent).isAttached();
		}

		@Override
		protected HandlerRegistration bind0(Widget widget) {
			return widget.addAttachHandler(this::fireEvent);
		}

		public interface Handler extends NodeEvent.Handler {
			void onAttach(Attach event);
		}
	}

	public static class ValueChange extends NodeEvent<ValueChange.Handler> {
		@Override
		public void dispatch(ValueChange.Handler handler) {
			handler.onValueChange(this);
		}

		@Override
		public Class<ValueChange.Handler> getHandlerClass() {
			return ValueChange.Handler.class;
		}

		@Override
		protected HandlerRegistration bind0(Widget widget) {
			return widget.addHandler(this::fireEvent,
					ValueChangeEvent.getType());
		}

		public interface Handler extends NodeEvent.Handler {
			void onValueChange(ValueChange event);
		}
	}
}
