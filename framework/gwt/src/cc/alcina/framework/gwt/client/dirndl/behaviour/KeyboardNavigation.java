package cc.alcina.framework.gwt.client.dirndl.behaviour;

import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;

import cc.alcina.framework.gwt.client.dirndl.behaviour.KeyboardNavigation.Navigation.Type;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.KeyDown;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent.Context;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

/*
 * This was not implemented as an interface (although it would have been
 * slightly prettier, if it had been) because the interactions are generally
 * complex - composition (and delegation) gives the end-user class more
 * flexibility
 */
public class KeyboardNavigation implements DomEvents.KeyDown.Handler {
	boolean emitLeftRightEvents;

	private Model model;

	public KeyboardNavigation(Model model) {
		this.model = model;
	}

	public boolean isEmitLeftRightEvents() {
		return this.emitLeftRightEvents;
	}

	@Override
	public void onKeyDown(KeyDown event) {
		Context context = event.getContext();
		KeyDownEvent domEvent = (KeyDownEvent) context.getGwtEvent();
		Navigation.Type emitType = null;
		switch (domEvent.getNativeKeyCode()) {
		case KeyCodes.KEY_ENTER:
			emitType = Type.COMMIT;
			break;
		case KeyCodes.KEY_ESCAPE:
			emitType = Type.CANCEL;
			break;
		case KeyCodes.KEY_LEFT:
			emitType = emitLeftRightEvents ? Type.LEFT : null;
			break;
		case KeyCodes.KEY_RIGHT:
			emitType = emitLeftRightEvents ? Type.RIGHT : null;
			break;
		case KeyCodes.KEY_UP:
			emitType = Type.UP;
			break;
		case KeyCodes.KEY_DOWN:
			emitType = Type.DOWN;
			break;
		}
		if (emitType != null) {
			event.reemitAs(model, Navigation.class, emitType);
			domEvent.preventDefault();
			domEvent.stopPropagation();
		}
	}

	public KeyboardNavigation
			withEmitLeftRightEvents(boolean emitLeftRightEvents) {
		this.emitLeftRightEvents = emitLeftRightEvents;
		return this;
	}

	public static class Navigation
			extends ModelEvent<Navigation.Type, Navigation.Handler> {
		/**
		 * When a keyevent on a contenteditable DOM node is intercepted by a
		 * navigation handler, it must not cause changes in the editable DOM
		 */
		public void consume() {
			KeyDownEvent domEvent = (KeyDownEvent) getContext()
					.getOriginatingGwtEvent();
			domEvent.getNativeEvent().preventDefault();
			domEvent.getNativeEvent().stopPropagation();
		}

		@Override
		public void dispatch(Navigation.Handler handler) {
			handler.onNavigation(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onNavigation(Navigation event);
		}

		public enum Type {
			UP, DOWN, COMMIT, CANCEL, LEFT, RIGHT, FIRST
		}
	}
}
