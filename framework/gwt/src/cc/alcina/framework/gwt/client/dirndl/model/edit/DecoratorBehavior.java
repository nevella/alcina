package cc.alcina.framework.gwt.client.dirndl.model.edit;

import java.util.Objects;

import com.google.gwt.dom.client.AttributeBehaviorHandler;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Selection;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.user.client.Event.NativePreviewEvent;

import cc.alcina.framework.common.client.dom.Location;
import cc.alcina.framework.common.client.dom.Location.RelativeDirection;
import cc.alcina.framework.common.client.dom.Location.TextTraversal;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.KeyDown;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent.Context;
import cc.alcina.framework.gwt.client.dirndl.model.edit.DecoratorNode.ZeroWidthCursorTarget;

/**
 * Complex/gritty mini-processes used to handle the extra appendages required by
 * the Decorator system, particularly
 * {@link cc.alcina.framework.gwt.client.dirndl.model.edit.DecoratorNode.ZeroWidthCursorTarget}
 */
public interface DecoratorBehavior {
	default void squelch(NodeEvent event) {
		DomEvent domEvent = (DomEvent) event.getContext()
				.getOriginatingGwtEvent();
		domEvent.getNativeEvent().squelch();
	}

	/**
	 * Extends the affected range of keyboard navigation events (and
	 * backspace/delete) if the traversed range is a zero-width-space
	 */
	public static class ExtendKeyboardNavigationAction
			implements DecoratorBehavior, DomEvents.KeyDown.Handler,
			AttributeBehaviorHandler {
		enum Direction {
			left, right, none;

			int numericDelta() {
				switch (this) {
				case left:
					return -1;
				case right:
					return 1;
				default:
					throw new UnsupportedOperationException();
				}
			}
		}

		@Override
		public void onKeyDown(KeyDown event) {
			Context context = event.getContext();
			if (context.util().hasKeyboardModifier()) {
				return;
			}
			KeyDownEvent domEvent = (KeyDownEvent) context.getGwtEvent();
			onKeyDown(domEvent.getNativeEvent());
		}

		@Override
		public String getEventType() {
			return BrowserEvents.KEYDOWN;
		}

		public void onKeyDown(NativeEvent nativeKeydownEvent) {
			Selection selection = Document.get().getSelection();
			if (!selection.isCollapsed()) {
				return;
			}
			Direction direction = null;
			boolean delete = false;
			switch (nativeKeydownEvent.getKeyCode()) {
			case KeyCodes.KEY_LEFT:
				direction = Direction.left;
				break;
			case KeyCodes.KEY_BACKSPACE:
				direction = Direction.left;
				delete = true;
				break;
			case KeyCodes.KEY_RIGHT:
				direction = Direction.right;
				break;
			case KeyCodes.KEY_DELETE:
				direction = Direction.right;
				delete = true;
				break;
			default:
				break;
			}
			if (direction == null) {
				return;
			}
			Location.Range range = selection.getAnchorLocation().asRange();
			range = range.extendText(direction.numericDelta());
			String text = range.text();
			if (!ZeroWidthCursorTarget.is(text)) {
				return;
			}
			range = range.extendText(direction.numericDelta());
			/*
			 * Allow for editabledecorator/zws - boundary/zws -
			 * decorator/non-zws
			 */
			if (ZeroWidthCursorTarget.isOneOrMore(text)) {
				text = range.text();
				range = range.extendText(direction.numericDelta());
			}
			Location boundary = range.provideEndpoint(direction.numericDelta());
			/*
			 * ZWS logic ensures the boundary is within a DecoratorNode - so
			 * extend to cover that node (it will be the parent)
			 */
			switch (direction) {
			case left:
				boundary = boundary.relativeLocation(
						RelativeDirection.PREVIOUS_LOCATION,
						TextTraversal.EXIT_NODE);
				break;
			case right:
				boundary = boundary.relativeLocation(
						RelativeDirection.NEXT_LOCATION,
						TextTraversal.EXIT_NODE);
				break;
			}
			Location decoratorLocation = boundary;
			/*
			 * position before/after the decorator
			 */
			switch (direction) {
			case left:
				boundary = boundary.relativeLocation(
						RelativeDirection.PREVIOUS_LOCATION,
						TextTraversal.TO_END_OF_NODE);
				break;
			case right:
				boundary = boundary.relativeLocation(
						RelativeDirection.NEXT_LOCATION,
						TextTraversal.TO_START_OF_NODE);
				break;
			}
			selection.collapse(boundary.getContainingNode().gwtNode(),
					boundary.getTextOffsetInNode());
			/*
			 * don't delete the zws, just the decorator
			 */
			if (delete) {
				decoratorLocation.getContainingNode().removeFromParent();
			}
			nativeKeydownEvent.squelch();
		}

		@Override
		public void onPreviewNativeEvent(NativePreviewEvent event) {
			onKeyDown(event.getNativeEvent());
		}

		@Override
		public String getMagicAttributeName() {
			return ATTR_NAME;
		}

		public static final String ATTR_NAME = "__bhvr_ekbna";
	}
}
