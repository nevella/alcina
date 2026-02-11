package com.google.gwt.dom.client.behavior;

import java.util.List;
import java.util.Objects;

import com.google.common.base.Preconditions;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventBehavior;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.LocalDom;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Selection;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.Event.NativePreviewEvent;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.Location;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;

/**
 * Check if a dom node has a magic attribute set, and if so perform a specific
 * behavior
 */
/**
 * Perform a few actions client-side that require blocking (such as keyboard
 * navigation)
 * 
 */
@Registration.Self
@Reflected
@Bean(PropertySource.FIELDS)
public interface ElementBehavior extends EventBehavior {
	public abstract class NonParameterised implements ElementBehavior {
		@Override
		public boolean equals(Object obj) {
			return obj != null && obj.getClass() == getClass();
		}

		@Override
		public int hashCode() {
			return getClass().hashCode();
		}
	}

	public abstract class Parameterised implements ElementBehavior {
		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Parameterised) {
				Parameterised o = (Parameterised) obj;
				return o.getClass() == getClass() && Objects
						.equals(provideParameters(), o.provideParameters());
			} else {
				return false;
			}
		}

		public abstract List<?> provideParameters();

		@Override
		public int hashCode() {
			return getClass().hashCode() ^ provideParameters().hashCode();
		}
	}

	/**
	 * <p>
	 * This behavior prevents the default handling of [enter] on the element
	 * 
	 */
	public static class PreventDefaultEnterBehaviour
			extends ElementBehavior.NonParameterised {
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
			case KeyCodes.KEY_ENTER:
				nativeKeydownEvent.preventDefault();
				break;
			}
		}
	}

	/**
	 * Note that this stops propagation as well ( to stop bubbling of focusout)
	 */
	public static class PreventDefaultMousedownBehaviour
			extends ElementBehavior.NonParameterised {
		@Override
		public String getEventType() {
			return BrowserEvents.MOUSEDOWN;
		}

		@Override
		public void onNativeEvent(NativePreviewEvent event,
				Element registeredElement) {
			event.getNativeEvent().preventDefault();
			event.getNativeEvent().stopPropagation();
		}
	}

	/**
	 * Note that this *does not* prevent propagation as well. Note also it is
	 * ignored if meta/ctrl is pressed
	 */
	public static class PreventDefaultClickBehaviour
			extends ElementBehavior.NonParameterised {
		@Override
		public String getEventType() {
			return BrowserEvents.CLICK;
		}

		@Override
		public void onNativeEvent(NativePreviewEvent event,
				Element registeredElement) {
			NativeEvent nativeEvent = event.getNativeEvent();
			if (nativeEvent.getMetaKey() || nativeEvent.getCtrlKey()) {
				return;
			}
			nativeEvent.preventDefault();
		}
	}

	static class ContentEditable {
		static boolean is(DomNode node) {
			return node.attrIs("contenteditable", "true");
		}

		static boolean isNot(DomNode node) {
			return node.attrIs("contenteditable", "false");
		}

		static void ensureEditableBoundaryNodes(DomNode node) {
			{
				DomNode previousSibling = node.relative().previousSibling();
				if (previousSibling == null || isNot(previousSibling)) {
					node.builder().text("").insertBeforeThis();
				}
			}
			{
				DomNode nextSibling = node.relative().nextSibling();
				if (nextSibling == null || isNot(nextSibling)) {
					node.builder().text("").insertAfterThis();
				}
			}
		}

		public static DomNode getEditable(DomNode registeredElement) {
			DomNode cursor = registeredElement.asDomNode();
			if (is(cursor)) {
				return cursor;
			}
			DomNode firstElement = cursor.children.firstElement();
			Preconditions.checkState(is(firstElement));
			return firstElement;
		}
	}

	public static class EnsureEditableNodesAtUneditableBoundaries
			extends ElementBehavior.NonParameterised {
		@Override
		public String getEventType() {
			return BrowserEvents.INPUT;
		}

		@Override
		public void onNativeEvent(NativePreviewEvent event,
				Element registeredElement) {
			DomNode editable = ContentEditable
					.getEditable(registeredElement.asDomNode());
			List<DomNode> nonEditables = editable.children.nodes().stream()
					.filter(ContentEditable::isNot).toList();
			nonEditables.forEach(ContentEditable::ensureEditableBoundaryNodes);
			int debug = 3;
			// TODO Auto-generated method stub
		}
	}

	/**
	 * 
	 * <p>
	 * Move the selection (back) off non-editables onto (empty) text nodes, and
	 * from the end of the editable container to the final empty text node
	 */
	public static class EnsureCursorTargetIsTextNode
			extends ElementBehavior.NonParameterised {
		@Override
		public String getEventType() {
			return BrowserEvents.SELECTIONCHANGE;
		}

		@Override
		public void onNativeEvent(NativePreviewEvent event,
				Element registeredElement) {
			Selection selection = Document.get().getSelection();
			if (selection.isCollapsed()) {
				Location focusLocation = selection.getFocusLocation();
				DomNode containingNode = focusLocation.getContainingNode();
				if (containingNode.isElement()) {
					if (containingNode.attrIs("contenteditable", "false")) {
						DomNode previousSibling = containingNode.relative()
								.previousSibling();
						if (previousSibling != null
								&& previousSibling.isText()) {
							selection.collapse(previousSibling.gwtNode());
						}
					} else if (containingNode.attrIs("contenteditable",
							"true")) {
						Node focusNode = selection.getFocusNode();
						if (selection.getFocusOffset() == focusNode
								.asDomNode().children.nodes().size()) {
							DomNode lastChild = focusNode.asDomNode().children
									.lastNode();
							if (lastChild != null) {
								selection.collapse(lastChild.gwtNode());
							}
						}
					}
				}
			}
			/*
			 * wip - decorator
			 */
		}
	}

	/**
	 * Ensure the selection focus node is a (possibly newly created blank) text
	 * node
	 */
	public static class EnsureTopLevelCursorTargetIsWrappedBehaviour
			extends ElementBehavior.Parameterised {
		public String wrappedElementName;

		@Override
		public String getEventType() {
			return BrowserEvents.SELECTIONCHANGE;
		}

		@Override
		public List<?> provideParameters() {
			return List.of(wrappedElementName);
		}

		@Override
		public void onNativeEvent(NativePreviewEvent event,
				Element registeredElement) {
			Selection selection = Document.get().getSelection();
			/*
			 * wip - decorator
			 */
		}
	}

	public static class DisableContentEditableOnIsolateMousedown
			extends ElementBehavior.NonParameterised {
		@Override
		public String getEventType() {
			return BrowserEvents.MOUSEDOWN;
		}

		@Override
		public void onNativeEvent(NativePreviewEvent event,
				Element registeredElement) {
			EventTarget eventTarget = event.getNativeEvent().getEventTarget();
			if (FragmentIsolateBehavior.hasInterveningIsolate(registeredElement,
					eventTarget)) {
				registeredElement.setAttribute("contenteditable", "false");
				LocalDom.flush();
			}
		}
	}

	/*
	 * NOOP, marker for DisableContentEditableOnIsolateMousedown
	 */
	public static class FragmentIsolateBehavior
			extends ElementBehavior.NonParameterised {
		public static boolean hasInterveningIsolate(Element registeredElement,
				EventTarget eventTarget) {
			if (Element.is(eventTarget)) {
				Element target = Element.as(eventTarget);
				while (target != null && target != registeredElement) {
					if (target.hasBehavior(FragmentIsolateBehavior.class)) {
						return true;
					}
					target = target.getParentElement();
				}
			}
			return false;
		}

		@Override
		public String getEventType() {
			return null;
		}

		@Override
		public void onNativeEvent(NativePreviewEvent event,
				Element registeredElement) {
			throw new UnsupportedOperationException();
		}
	}

	public static class UndoEditableAutocreatedBr
			extends ElementBehavior.NonParameterised {
		@Override
		public String getEventType() {
			return BrowserEvents.INPUT;
		}

		@Override
		public void onNativeEvent(NativePreviewEvent event,
				Element registeredElement) {
			registeredElement.asDomNode().stream().filter(n -> n.tagIs("br"))
					.toList().forEach(DomNode::removeFromParent);
		}
	}

	default boolean isEventHandler() {
		return getEventType() != null;
	}

	/**
	 * This may be null, for more general housekeeping behaviors
	 * 
	 * @return
	 */
	String getEventType();

	default boolean matches(Element elem) {
		return elem.hasBehavior(getClass());
	}

	void onNativeEvent(NativePreviewEvent event, Element registeredElement);
}