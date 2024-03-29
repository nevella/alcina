package cc.alcina.framework.gwt.client.dirndl.event;

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.shared.HandlerRegistration;

import cc.alcina.framework.gwt.client.dirndl.layout.DomBinding;

public class GwtEvents {
	/**
	 * This event should not be listened for, since it occurs in a point in the
	 * model rendering sequence where changes to the model should not occur (so
	 * is incorrect as a 'populate me now' signal). Use
	 * LayoutEvents.BeforeRender instead, or LayoutEvents.Bind for symmetric
	 * change binding
	 *
	 * Note that there's no DomBindingImpl associated - so this event cannot be
	 * bound to the DOM
	 *
	 * 
	 *
	 */
	@Deprecated
	public static class Attach extends NodeEvent<Attach.Handler> {
		@Override
		public void dispatch(Attach.Handler handler) {
			handler.onAttach(this);
		}

		public boolean isAttached() {
			return ((AttachEvent) getContext().getGwtEvent()).isAttached();
		}

		public interface Handler extends NodeEvent.Handler {
			void onAttach(Attach event);
		}
	}

	/*
	 * Essentially deprecated (shouldn't be using widgets which fire non-dom
	 * events of interest)
	 */
	public static class ValueChange extends NodeEvent<ValueChange.Handler> {
		@Override
		public void dispatch(ValueChange.Handler handler) {
			handler.onValueChange(this);
		}

		public static class BindingImpl extends DomBinding<ValueChange> {
			@Override
			protected HandlerRegistration bind1(Element element) {
				return element.addHandler(this::fireEvent,
						ValueChangeEvent.getType());
			}
		}

		public interface Handler extends NodeEvent.Handler {
			void onValueChange(ValueChange event);
		}
	}
}
