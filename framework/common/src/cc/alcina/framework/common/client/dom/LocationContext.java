package cc.alcina.framework.common.client.dom;

import java.util.List;

import cc.alcina.framework.common.client.dom.Location.Range;
import cc.alcina.framework.common.client.dom.Location.RelativeDirection;
import cc.alcina.framework.common.client.dom.Location.TextTraversal;

public interface LocationContext {
	default int compare(Location l1, Location l2) {
		if (l1.equals(l2)) {
			return 0;
		}
		{
			int cmp = l1.getIndex() - l2.getIndex();
			if (cmp != 0) {
				return cmp;
			}
		}
		// if containingNode is not search, this is a search (rather than a
		// comparison of fully-populated Location instances)
		if (l1.getContainingNode() == null || l2.getContainingNode() == null) {
			{
				// for a given character, end is always after start (at any
				// depth)
				int end1 = l1.after ? 1 : 0;
				int end2 = l2.after ? 1 : 0;
				int cmp = end1 - end2;
				if (cmp != 0) {
					return cmp;
				}
			}
			return 0;
		}
		DomNode n1 = l1.getContainingNode();
		DomNode n2 = l2.getContainingNode();
		if (n1.isAncestorOf(n2)) {
			{
				// for a given character, end is always after start (at any
				// depth)
				int end1 = l1.after ? 1 : 0;
				int end2 = l2.after ? 1 : 0;
				int cmp = end1 - end2;
				if (cmp != 0) {
					return cmp;
				}
			}
			{
				// for a given character, continer end is always after contained
				// end
				if (l1.after) {
					return 1;
				} else {
					return -1;
				}
			}
		} else if (n2.isAncestorOf(n1)) {
			return -compare(l2, l1);
		} else {
			// no ancestry
			return l1.getTreeIndex() - l2.getTreeIndex();
		}
	}

	default int compareIndexOnly(Location l1, Location l2) {
		return l1.getIndex() - l2.getIndex();
	}

	Location createTextRelativeLocation(Location location, int offset,
			boolean end);

	DomNode getContainingNode(Location location);

	List<DomNode> getContainingNodes(int index, boolean after);

	Location getRelativeLocation(Location location, RelativeDirection direction,
			TextTraversal textTraversal);

	String getSubsequentText(Location location, int chars);

	String markupContent(Range range);

	default String textContent(int from, int to) {
		Location start = new Location(-1, toValidIndex(from), false, null,
				this);
		Location end = new Location(-1, toValidIndex(to), true, null, this);
		return new Range(start, end).text();
	}

	String textContent(Range range);

	int toValidIndex(int idx);

	// FIXME - FN - remove once framework complete
	void invalidate();

	int getDocumentMutationPosition();

	Location asLocation(DomNode domNode);

	Range asRange(DomNode domNode);

	Range getDocumentRange();

	void ensureCurrent(Location location);
}
