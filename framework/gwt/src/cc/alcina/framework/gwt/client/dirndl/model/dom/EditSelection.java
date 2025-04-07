package cc.alcina.framework.gwt.client.dirndl.model.dom;

import java.util.Optional;
import java.util.function.Predicate;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Selection;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.DomNode.DomNodeText.SplitResult;
import cc.alcina.framework.common.client.dom.DomNode.DomNodeTree;
import cc.alcina.framework.common.client.dom.Location;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.NestedName;

/**
 * A utility wrapper around a DOM Selection, for use by annotators and
 * decorators
 */
public class EditSelection {
	Selection selection;

	boolean triggerable;

	Location focusLocation;

	int focusOffset;

	int anchorOffset;

	Location anchorLocation;

	boolean collapsed;

	Location.Range range;

	/*
	 * the range from the start of the caret textnode to the caret, if any
	 */
	Location.Range priorRange;

	DomNode focusDomNode;

	DomNode anchorDomNode;

	boolean hasEditSelection;

	/*
	 * Note
	 *
	 * for a selection of #TEXT[@] - after the char '@' - the selection offset
	 * is 1, but our location offset is 0/end (since max location offset =
	 * length of content)
	 *
	 * And...this gives rise to 'do we need Location.after' - and answer is
	 * probably 'maybe',
	 */
	public EditSelection() {
		selection = Document.get().getSelection();
		collapsed = selection.isCollapsed();
		focusDomNode = selection.getFocusNode() == null ? null
				: selection.getFocusNode().asDomNode();
		anchorDomNode = selection.getAnchorNode() == null ? null
				: selection.getAnchorNode().asDomNode();
		if (!selection.hasSelection() || focusDomNode == null
				|| !focusDomNode.gwtNode().isAttached()) {
			return;
		}
		hasEditSelection = true;
		focusOffset = selection.getFocusOffset();
		focusLocation = focusDomNode.asLocation()
				.createTextRelativeLocation(focusOffset, false);
		anchorOffset = selection.getAnchorOffset();
		anchorLocation = anchorDomNode.asLocation()
				.createTextRelativeLocation(anchorOffset, false);
		range = new Location.Range(anchorLocation, focusLocation);
		if (anchorDomNode.isText() && collapsed) {
			// non-rtl
			Location anchorTextStartLocation = anchorDomNode.asLocation();
			priorRange = new Location.Range(anchorTextStartLocation,
					anchorLocation);
		}
		if (collapsed && focusLocation != null) {
			setTriggerable(true);
		}
	}

	/*
	 * Translates x.end to x.lastText.endOfTextRunOffset
	 *
	 * FIXME - Feature_Dirndl_ContentDecorator - also insert empty text if
	 * needed at end
	 */
	Location caretLocation(Location location) {
		if (!location.after) {
			return location;
		}
		DomNode containingNode = location.getContainingNode();
		DomNodeTree tree = containingNode.tree();
		DomNode cursor = containingNode.children.lastNode();
		tree.setCurrentNode(cursor);
		while ((cursor = tree.currentNode()) != containingNode) {
			if (cursor.isText() && !cursor.isEmptyTextContent()) {
				Location result = cursor.asLocation().clone();
				result.setIndex(result.getIndex() + cursor.textContent().length());
				return result;
			}
			tree.previousLogicalNode();
		}
		// contains no text,so for now return self (later, create an empty text
		// node after self)
		return location;
	}

	public void extendSelectionToIncludeAllOf(DomNode node) {
		String text = ((Element) node.gwtNode()).getInnerText();
		Location.Range tagRange = node.asRange();
		boolean anchorBeforeFocus = anchorLocation
				.compareTo(focusLocation) <= 0;
		Location modifiedFocusLocation = null;
		Location modifiedAnchorLocation = null;
		if (tagRange.start.isBefore(range.start)) {
			if (anchorBeforeFocus) {
				modifiedAnchorLocation = node.asLocation();
			} else {
				modifiedFocusLocation = node.asLocation();
			}
		}
		if (tagRange.end.isAfter(range.end)) {
			if (anchorBeforeFocus) {
				modifiedFocusLocation = node.asLocation().clone();
				modifiedFocusLocation.after = true;
				modifiedFocusLocation = caretLocation(modifiedFocusLocation);
			} else {
				modifiedAnchorLocation = node.asLocation().clone();
				modifiedAnchorLocation.after = true;
				modifiedAnchorLocation = caretLocation(modifiedAnchorLocation);
			}
		}
		if (modifiedAnchorLocation != null) {
			selection.collapse(modifiedAnchorLocation.getContainingNode().gwtNode(),
					modifiedAnchorLocation.indexInNode());
			Location extendTo = modifiedFocusLocation != null
					? modifiedFocusLocation
					: focusLocation;
			selection.extend(extendTo.getContainingNode().gwtNode(),
					extendTo.indexInNode());
		} else if (modifiedFocusLocation != null) {
			Location extendTo = modifiedFocusLocation;
			selection.extend(extendTo.getContainingNode().gwtNode(),
					extendTo.indexInNode());
		}
	}

	public DomNode focusNode() {
		return focusDomNode;
	}

	public Optional<DomNode> getFocusNodePartiallySelectedAncestor(
			Predicate<DomNode> predicate) {
		Optional<DomNode> ancestor = focusNode().ancestors().match(predicate);
		if (ancestor.isEmpty()) {
			return Optional.empty();
		}
		if (collapsed) {
			return ancestor;
		}
		Location.Range ancestorRange = ancestor.get().asRange();
		return !range.contains(ancestorRange) ? ancestor : Optional.empty();
	}

	public int getFocusOffset() {
		return this.focusOffset;
	}

	public boolean hasAncestorFocusTag(String tag) {
		return focusNode().ancestors().get(tag) != null;
	}

	public boolean isTriggerable() {
		return triggerable;
	}

	public String relativeString(int startOffset, int endOffset) {
		return focusLocation.content().relativeString(startOffset, endOffset);
	}

	public void setTriggerable(boolean triggerable) {
		this.triggerable = triggerable;
	}

	public SplitResult splitAt(int from, int to) {
		if (focusOffset > focusNode().textContent().length()) {
			// temporary hack - focusNode has been mutated without a selection
			// update
			focusOffset = focusNode().textContent().length();
		}
		return focusNode().text().split(from + focusOffset, to + focusOffset);
	}

	/*
	 * Strip any matching, preserving selection location
	 */
	public void strip(Node container, String tag) {
		container.asDomNode().children.byTag(tag).stream()
				.forEach(DomNode::strip);
	}

	@Override
	public String toString() {
		FormatBuilder format = new FormatBuilder().separator("  ");
		format.append(NestedName.get(this));
		format.appendKeyValues("range", range).toString();
		format.appendKeyValues("priorRange", priorRange).toString();
		return format.toString();
	}

	public boolean hasSelection() {
		return hasEditSelection;
	}

	public Location.Range getTriggerableRangePrecedingFocus() {
		return priorRange;
	}

	public SplitResult splitAtTriggerRange(String triggerSequence) {
		return splitAt(-triggerSequence.length(), 0);
	}
}