package cc.alcina.framework.gwt.client.dirndl.event;

import com.google.gwt.dom.client.DomRect;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.user.client.DOM;

import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.util.DoublePair;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.MouseDown;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.MouseMove;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.MouseUp;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

/**
 * <p>
 * An event handler interface for an absolutely positioned model that can be
 * dragged, with constraints
 */
public interface Draggable extends DomEvents.MouseDown.Handler,
		DomEvents.MouseUp.Handler, DomEvents.MouseMove.Handler {
	/**
	 * <p>
	 * Model the delta from the most recent change (not the total delta)
	 */
	public static class Dragged
			extends ModelEvent<Dragged.Data, Dragged.Handler> {
		@Override
		public void dispatch(Dragged.Handler handler) {
			handler.onDragged(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onDragged(Dragged event);
		}

		public static class Data {
			public Data(DoublePair deltaFromLastDragged,
					DoublePair cumulativeDelta) {
				this.deltaFromLastDragged = deltaFromLastDragged;
				this.cumulativeDelta = cumulativeDelta;
			}

			public DoublePair deltaFromLastDragged;

			public DoublePair cumulativeDelta;
		}
	}

	/*
	 * FIXME - topic - composition vs inheritance
	 * 
	 * This is an example of 'composition _before_ inheritance' - i.e. have the
	 * best of both (reusable and clear separation-of-concerns, but minimal
	 * boilerplate in the standard implementation)
	 * 
	 * See com.google.gwt.user.client.ui.SplitPanel
	 * 
	 * Unlike GWT SplitPanel, this doesn't maintain a glass panel/model (so
	 * doesn't handle iframes - which is a pretty recondite edge case)
	 * 
	 * DOM capture is required because the dragged element may not actually be
	 * under the cursor during move
	 */
	public static class Support implements Draggable {
		Model model;

		public Support(Model model) {
			this.model = model;
		}

		DoublePair startPosition;

		DoublePair lastPosition;

		@Override
		public void onMouseDown(MouseDown event) {
			NativeEvent nativeEvent = event.getContext()
					.getOriginatingNativeEvent();
			Element elem = model.provideElement();
			DomRect elemRect = elem.getBoundingClientRect();
			startPosition = new DoublePair(
					nativeEvent.getClientX() - elemRect.left,
					nativeEvent.getClientY() - elemRect.top);
			lastPosition = startPosition;
			DOM.setCapture(elem);
			nativeEvent.preventDefault();
		}

		@Override
		public void onMouseUp(MouseUp event) {
			if (isResizing()) {
				stopResizing();
				DOM.releaseCapture(model.provideElement());
			}
		}

		void stopResizing() {
			startPosition = null;
		}

		boolean isResizing() {
			return startPosition != null;
		}

		@Override
		public void onMouseMove(MouseMove event) {
			if (isResizing()) {
				NativeEvent nativeEvent = event.getContext()
						.getOriginatingNativeEvent();
				assert DOM.getCaptureElement() != null;
				Element elem = model.provideElement();
				DomRect elemRect = elem.getBoundingClientRect();
				DoublePair dragPosition = new DoublePair(
						nativeEvent.getClientX() - elemRect.left,
						nativeEvent.getClientY() - elemRect.top);
				nativeEvent.preventDefault();
				DoublePair totalDelta = dragPosition.minus(startPosition);
				DoublePair moveDelta = dragPosition.minus(lastPosition);
				event.reemitAs(model, Dragged.class,
						new Dragged.Data(moveDelta, totalDelta));
			}
		}

		@Override
		public Support getDraggableSupport() {
			// never called
			throw new UnsupportedOperationException();
		}
	}

	@Property.Not
	Support getDraggableSupport();

	@Override
	default void onMouseDown(MouseDown event) {
		getDraggableSupport().onMouseDown(event);
	}

	@Override
	default void onMouseUp(MouseUp event) {
		getDraggableSupport().onMouseUp(event);
	}

	@Override
	default void onMouseMove(MouseMove event) {
		getDraggableSupport().onMouseMove(event);
	}

	/**
	 * A dragging sizer, for resizing layouts
	 */
	public static class Sizer extends Model implements Draggable {
		Draggable.Support support;

		@Override
		public Support getDraggableSupport() {
			return support;
		}

		public Sizer() {
			support = new Support(this);
		}
	}
}
