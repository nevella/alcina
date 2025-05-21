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
import cc.alcina.framework.common.client.dom.Location.RelativeDirection;
import cc.alcina.framework.common.client.dom.Location.TextTraversal;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent;

/**
 * Complex/gritty mini-processes used to handle the extra appendages required by
 * the Decorator system, particularly
 * {@link cc.alcina.framework.gwt.client.dirndl.model.edit.ZeroWidthCursorTarget}
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

		/*
		 * TODO
		 * 
		 * @formatter:off
		 * 
		 * - this is really "treat contenteditable=false as wholes". Normally contenteditables are not in the keyboard selection flow
		 * - if kb b/f nav (test focus) would cause overlap with a CE, hijack the nav and move the focus
		 * - delete/bs is really "clear-sel, shift-move, delete/bs". we'll want an abstractish keyboard-move-result with a futureselection
		 * - if focus is *on* the CE, again hijck
		 * 
		 * - PendingSelection object (backed by a SelectionLocal)
		 * - it receives kb events, which are themselves "KeyboardSelectionMutation" events - selecting: boolean; direction: boolean
		 * - it's the one that behaves differently when the Fn moves to/from a CEF
		 * 
		 * - move focus node *first* - then ensure selection boundaries contain (including the element) 
		 * - actually no - *this* bit should just 'move one more if zws' - but honestly with invis text nodes let's try that first
		 * - most of what we want is actually onSelectionChanged handling - to expand to cover the selection
		 * - but do need 
		 * 
		 * 
		 * * @formatter:on
		 */
		/*
		 * - DOC: Normally browser keyboard navigation skips
		 * contenteditable=false (CEF) 'islands' in a sea of
		 * contenteditable=true (CET) sea.
		 * 
		 * In this case, we want to treat the CEF decorator nodes as part of the
		 * KB navigation sequence, but as effectively 1-character atoms. So we
		 * emulate kb navigation via the KeyboardSelectionMutation and
		 * PendingSelection objects
		 * 
		 * Note that this is sort of orthogonal to ZWS (which will be removed?)
		 * 
		 */
		public void onKeyDown(NativeEvent nativeKeydownEvent,
				Element registeredElement) {
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
			Location.Range range = selection.getFocusLocation().asRange();
			Location.Range contextBoundary = registeredElement.asDomNode()
					.asRange();
			range = range.extendText(direction.numericDelta());
			/*
			 * range now covers what would be selected with shift-[cursor move]
			 */
			String text = range.text();
			/*
			 * the text is a ZWS, so check extending one more FIXME change
			 * post-zws
			 */
			Location.Range testExtended = range
					.extendText(direction.numericDelta());
			/*
			 * FIXME - romcom - this *should* use location containment, but
			 * selection -> location/range transformation is not quite right
			 */
			if (!contextBoundary.toIntPair()
					.contains(testExtended.toIntPair())) {
				return;
			}
			range = testExtended;
			/*
			 * More outré combinations, like zws/zws, should be handled in
			 * non-behavir code
			 * 
			 */
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
			DomNode containingNode = decoratorLocation.getContainingNode();
			/*
			 * don't delete the zws (we could, and rely on ensure-zws behavior
			 * to re-insert, but that's async in the case of romcom), just the
			 * non-editable. confirm it's non-editable
			 */
			/*
			 * attrIsIgnoreCase is ok -just- for this
			 */
			if (containingNode.attrIsIgnoreCase("contenteditable", "false")) {
				if (delete) {
					containingNode.removeFromParent();
				} else {
					/*
					 * expand the selection to the whole non-editable
					 */
					selection.select(containingNode.gwtNode());
					containingNode.gwtElement().focus();
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
