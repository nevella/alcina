package cc.alcina.framework.gwt.client.dirndl.model.dom;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.SelectionRemote;

import cc.alcina.framework.common.client.dom.DomNode.DomNodeText.SplitResult;
import cc.alcina.framework.common.client.dom.Location;

public class RelativeInputModel {
	SelectionRemote selectionRemote;

	private boolean triggerable;

	Location location;

	private int focusOffset;

	public RelativeInputModel() {
		selectionRemote = Document.get().typedRemote().getSelection();
		boolean collapsed = selectionRemote.isCollapsed();
		Node focusDomNode = selectionRemote.getFocusNode().node();
		if (focusDomNode != null) {
			focusOffset = selectionRemote.getFocusOffset();
			location = focusDomNode.asDomNode().asLocation()
					.createRelativeLocation(focusOffset, true);
		}
		if (collapsed && location != null) {
			setTriggerable(true);
		}
	}

	public boolean hasAncestorTag(String tag) {
		return location.containingNode().ancestors().get(tag) != null;
	}

	public boolean isTriggerable() {
		return triggerable;
	}

	public String relativeString(int startOffset, int endOffset) {
		return location.content().relativeString(startOffset, endOffset);
	}

	public void setTriggerable(boolean triggerable) {
		this.triggerable = triggerable;
	}

	public SplitResult splitAt(int from, int to) {
		return location.containingNode().text().split(from + focusOffset,
				to + focusOffset);
	}
}