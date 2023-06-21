package cc.alcina.framework.gwt.client.dirndl.model.dom;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.SelectionJso;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.DomNode.DomNodeText.SplitResult;
import cc.alcina.framework.common.client.dom.DomNode.DomNodeTree;
import cc.alcina.framework.common.client.dom.Location;
import cc.alcina.framework.common.client.util.FormatBuilder;

public class RelativeInputModel {
	SelectionJso selection;

	boolean triggerable;

	Location focusLocation;

	int focusOffset;

	int anchorOffset;

	Location anchorLocation;

	boolean collapsed;

	Location.Range range;

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
	public RelativeInputModel() {
		selection = Document.get().jsoRemote().getSelection();
		collapsed = selection.isCollapsed();
		{
			Node focusDomNode = selection.getFocusNode().node();
			if (focusDomNode != null) {
				focusOffset = selection.getFocusOffset();
				focusLocation = focusDomNode.asDomNode().asLocation()
						.createRelativeLocation(focusOffset, false);
			}
		}
		{
			Node anchorDomNode = selection.getAnchorNode().node();
			if (anchorDomNode != null) {
				anchorOffset = selection.getAnchorOffset();
				anchorLocation = anchorDomNode.asDomNode().asLocation()
						.createRelativeLocation(anchorOffset, false);
				range = new Location.Range(anchorLocation, focusLocation);
			}
		}
		if (collapsed && focusLocation != null) {
			setTriggerable(true);
		}
	}

	public boolean containsPartialAncestorFocusTag(String tag) {
		DomNode focussedTag = focusLocation.containingNode().ancestors()
				.get(tag);
		if (focussedTag == null) {
			return false;
		}
		if (collapsed) {
			return true;
		}
		Location.Range tagRange = focussedTag.asRange();
		return !range.contains(tagRange);
	}

	public boolean hasAncestorFocusTag(String tag) {
		return focusLocation.containingNode().ancestors().get(tag) != null;
	}

	public boolean isTriggerable() {
		return triggerable;
	}

	public String relativeString(int startOffset, int endOffset) {
		return focusLocation.content().relativeString(startOffset, endOffset);
	}

	// FIXME - Feature_Dirndl_ContentDecorator - if selecting start of
	// decorator (e.g. char 0, decorator starts contenteditable), insert a blank
	// text node + move caret
	public void selectWholeAncestorFocusTag(String tag) {
		DomNode focussedTag = focusLocation.containingNode().ancestors()
				.get(tag);
		String text = ((Element) focussedTag.gwtNode()).getInnerText();
		Location.Range tagRange = focussedTag.asRange();
		boolean anchorBeforeFocus = anchorLocation
				.compareTo(focusLocation) <= 0;
		Location modifiedFocusLocation = null;
		Location modifiedAnchorLocation = null;
		if (tagRange.start.isBefore(range.start)) {
			if (anchorBeforeFocus) {
				modifiedAnchorLocation = focussedTag.asLocation();
			} else {
				modifiedFocusLocation = focussedTag.asLocation();
			}
		}
		if (tagRange.end.isAfter(range.end)) {
			if (anchorBeforeFocus) {
				modifiedFocusLocation = focussedTag.asLocation().clone();
				modifiedFocusLocation.after = true;
				modifiedFocusLocation = caretLocation(modifiedFocusLocation);
			} else {
				modifiedAnchorLocation = focussedTag.asLocation().clone();
				modifiedAnchorLocation.after = true;
				modifiedAnchorLocation = caretLocation(modifiedAnchorLocation);
			}
		}
		if (modifiedAnchorLocation != null) {
			selection.collapse(
					modifiedAnchorLocation.containingNode().gwtNode()
							.implAccess().jsoRemote(),
					modifiedAnchorLocation.indexInNode());
			Location extendTo = modifiedFocusLocation != null
					? modifiedFocusLocation
					: focusLocation;
			selection.extend(extendTo.containingNode().gwtNode().implAccess()
					.jsoRemote(), extendTo.indexInNode());
		} else if (modifiedFocusLocation != null) {
			Location extendTo = modifiedFocusLocation;
			selection.extend(extendTo.containingNode().gwtNode().implAccess()
					.jsoRemote(), extendTo.indexInNode());
		}
	}

	public void setTriggerable(boolean triggerable) {
		this.triggerable = triggerable;
	}

	public SplitResult splitAt(int from, int to) {
		return focusLocation.containingNode().text().split(from + focusOffset,
				to + focusOffset);
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
		return FormatBuilder.keyValues("range", range);
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
		DomNode containingNode = location.containingNode();
		DomNodeTree tree = containingNode.tree();
		DomNode cursor = containingNode.children.lastNode();
		tree.setCurrentNode(cursor);
		while ((cursor = tree.currentNode()) != containingNode) {
			if (cursor.isText() && !cursor.isEmptyTextContent()) {
				Location result = cursor.asLocation().clone();
				result.index += cursor.textContent().length();
				return result;
			}
			tree.previousLogicalNode();
		}
		// contains no text,so for now return self (later, create an empty text
		// node after self)
		return location;
	}
}