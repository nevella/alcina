package cc.alcina.framework.gwt.client.dirndl.model.edit;

import com.google.gwt.dom.client.AttributeBehaviorHandler;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Selection;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.Event.NativePreviewEvent;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.Location;
import cc.alcina.framework.common.client.dom.Location.Range;
import cc.alcina.framework.common.client.dom.Location.RelativeDirection;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent;

/**
 * Complex/gritty mini-processes used to handle the extra appendages required by
 * the Decorator system, particularly interpolated empty text Nodes
 * 
 */
public interface DecoratorBehavior {
	static void squelch(NodeEvent event) {
		DomEvent domEvent = (DomEvent) event.getContext()
				.getOriginatingGwtEvent();
		domEvent.getNativeEvent().squelch();
	}

	/**
	 * Handle repeatable choices (during ChoiceEditor ask)
	 */
	interface RepeatableChoiceHandling extends DecoratorBehavior {
	}

	/**
	 * <p>
	 * This is a client behavior, since it needs to occur synchronously
	 * <p>
	 * Extends the affected range of keyboard navigation events (and
	 * backspace/delete) if the traversed range is a zero-width-space.
	 * 
	 * <p>
	 * This effectively makes the ZWS nodes invisible to keyboard navigation -
	 * they're there to allow (and control the appearance of) cursor targets,
	 * but they should be otherwise invisible to the user.
	 */
	public static class ModifyNonEditableSelectionBehaviour
			implements DecoratorBehavior, AttributeBehaviorHandler {
		@Override
		public String getEventType() {
			return BrowserEvents.SELECTIONCHANGE;
		}

		@Override
		public void onNativeEvent(NativePreviewEvent event,
				Element registeredElement) {
			onSelectionChange(event.getNativeEvent(), registeredElement);
		}

		public void onSelectionChange(NativeEvent nativeSelectionChangeEvent,
				Element registeredElement) {
			Selection selection = Document.get().getSelection();
			if (!selection.isCollapsed()) {
				return;
			}
			Location anchorLocation = selection.getAnchorLocation();
			DomNode containingNode = anchorLocation.getContainingNode();
			if (containingNode.attrIsIgnoreCase("contenteditable", "false")) {
				/*
				 * focus on a non-editable (choice) - this indicates the user
				 * clicked between two non-eds. Try and reposition
				 */
				if (anchorLocation.isAtNodeStart()) {
					DomNode previousSibling = containingNode.relative()
							.previousSibling();
					if (previousSibling != null) {
						if (previousSibling.isText()) {
							selection.select(previousSibling.gwtNode());
						}
					}
				} else if (anchorLocation.isAtNodeEnd()) {
					DomNode nextSibling = containingNode.relative()
							.nextSibling();
					if (nextSibling != null) {
						if (nextSibling.isText()) {
							selection.select(nextSibling.gwtNode());
						}
					}
				}
			} else {
				if (containingNode.isElement() && containingNode
						.parent() == registeredElement.asDomNode()) {
					if (anchorLocation.isAtNodeEnd()) {
						DomNode lastChild = containingNode.children.lastNode();
						if (lastChild != null && lastChild.isText()) {
							selection.select(lastChild.gwtNode());
						}
					}
				}
			}
		}

		@Override
		public String getMagicAttributeName() {
			return ATTR_NAME;
		}

		public static final String ATTR_NAME = "__bhvr_mnesb";
	}

	/**
	 * <p>
	 * This is a client behavior, since it needs to occur synchronously
	 * <p>
	 * If in a content editable, *and* effectively with an overlay showing,
	 * prevent default up/down
	 * 
	 */
	public static class InterceptUpDownBehaviour
			implements DecoratorBehavior, AttributeBehaviorHandler {
		@Override
		public String getEventType() {
			return BrowserEvents.KEYDOWN;
		}

		@Override
		public void onNativeEvent(NativePreviewEvent event,
				Element registeredElement) {
			onKeyDown(event.getNativeEvent(), registeredElement);
		}

		public void onKeyDown(NativeEvent nativeKeydownEvent,
				Element registeredElement) {
			switch (nativeKeydownEvent.getKeyCode()) {
			case KeyCodes.KEY_DOWN:
			case KeyCodes.KEY_UP:
				nativeKeydownEvent.preventDefault();
				break;
			}
		}

		@Override
		public String getMagicAttributeName() {
			return ATTR_NAME;
		}

		public static final String ATTR_NAME = "__bhvr_iudbb";
	}

	/**
	 * <p>
	 * This is a client behavior, since it needs to occur synchronously
	 * <p>
	 * 
	 * Extends the affected range of keyboard navigation events (and
	 * backspace/delete) if the traversed range is a non-editable
	 * 
	 * <p>
	 * Normally browser keyboard navigation skips contenteditable=false (CEF)
	 * 'islands' in a sea of contenteditable=true (CET) sea. This behavior
	 * treats the non-editables equivalently to a single-cursor position
	 * (between two editable text cursor positions)
	 */
	public static class ExtendKeyboardNavigationAction
			implements DecoratorBehavior, AttributeBehaviorHandler {
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
		public String getEventType() {
			return BrowserEvents.KEYDOWN;
		}

		public void onKeyDown(NativeEvent nativeKeydownEvent,
				Element registeredElement) {
			Selection selection = Document.get().getSelection();
			Direction direction = null;
			boolean delete = false;
			boolean extend = nativeKeydownEvent.getShiftKey();
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
			int numericDelta = direction.numericDelta();
			Location.Range contextBoundary = registeredElement.asDomNode()
					.asRange();
			Location focusLocation = selection.getFocusLocation();
			DomNode nonEditableFocusAncestor = focusLocation.getContainingNode()
					.html().contentEditable().getNonEditableAncestor();
			if (nonEditableFocusAncestor != null) {
				/*
				 * note that delete must be false, since selection will not be
				 * collapsed
				 * 
				 * if focus is at the other end of the non-editable, switch it
				 * prior to move
				 */
				Direction focusDirection = focusLocation.after ? Direction.right
						: Direction.left;
				if (focusDirection != direction) {
					focusLocation = focusLocation.toOppositeEnd();
				}
			}
			Location shiftedFocusLocation = focusLocation.clone();
			/*
			 * Normal keyboard behavior doesn't move if deleting non-collapsed
			 */
			if (delete && !selection.isCollapsed()) {
				// preserve selection
			} else {
				if (focusLocation.getContainingNode().html().contentEditable()
						.isDefined(false)) {
					shiftedFocusLocation = focusLocation.relativeLocation(
							RelativeDirection.ofNumericDelta(numericDelta));
				} else {
					shiftedFocusLocation = focusLocation
							.textRelativeLocation(numericDelta, false);
				}
			}
			if (contextBoundary.contains(shiftedFocusLocation)) {
				DomNode nonEditableAncestor = shiftedFocusLocation
						.getContainingNode().html().contentEditable()
						.getNonEditableAncestor();
				if (nonEditableAncestor != null && contextBoundary
						.contains(nonEditableAncestor.asLocation())) {
					/*
					 * this will be at the extreme end of the non-editable, so
					 * select the entirety
					 */
					/*
					 * Note that the adjust-selection behavior would do this
					 * anyway (but with more loopin')
					 */
					shiftedFocusLocation = nonEditableAncestor.asLocation();
					if (direction == Direction.right) {
						shiftedFocusLocation = shiftedFocusLocation
								.toOppositeEnd();
					}
				}
			}
			if (delete) {
				/*
				 * for deletion, use browser mod (preserves cursor) if possible
				 */
				Range preDeleteRange = new Location.Range(
						selection.getAnchorLocation(), shiftedFocusLocation);
				boolean noNonEditables = preDeleteRange.stream()
						.noneMatch(loc -> loc.getContainingNode().html()
								.contentEditable().hasNonEditableAncestor());
				if (noNonEditables) {
					return;
				}
				if (selection.isCollapsed()) {
					/*
					 * backspace/fwd onto a non-editable, delete it but don't
					 * modify selection
					 */
					shiftedFocusLocation.getContainingNode().removeFromParent();
				} else {
					/*
					 * complex (shift nav across possibly multiple decorator
					 * nodes). This may lose the cursor (Chrome), so it may be
					 * better to only allow one selected non-editable
					 */
					selection.extend(shiftedFocusLocation);
					selection.deleteFromDocument();
				}
			} else {
				if (extend) {
					selection.extend(shiftedFocusLocation);
				} else {
					if (shiftedFocusLocation.getContainingNode().html()
							.contentEditable().isDefined(false)) {
						Location shiftedAnchorLocation = shiftedFocusLocation
								.toOppositeEnd();
						selection.collapse(shiftedAnchorLocation);
						selection.extend(shiftedFocusLocation);
					} else {
						selection.collapse(shiftedFocusLocation);
					}
				}
			}
			nativeKeydownEvent.squelch();
		}

		@Override
		public void onNativeEvent(NativePreviewEvent event,
				Element registeredElement) {
			onKeyDown(event.getNativeEvent(), registeredElement);
		}

		@Override
		public String getMagicAttributeName() {
			return ATTR_NAME;
		}

		public static final String ATTR_NAME = "__bhvr_ekbna";
	}
}
