package cc.alcina.framework.common.client.dom;

import cc.alcina.framework.common.client.dom.Location.Range;

public interface LocationContext {
	default int compare(Location l1, Location l2) {
		if (l1.equals(l2)) {
			return 0;
		}
		{
			int cmp = l1.index - l2.index;
			if (cmp != 0) {
				return cmp;
			}
		}
		// if containingNode is not search, this is a search (rather than a
		// comparison of fully-populated Location instances)
		if (l1.containingNode == null || l2.containingNode == null) {
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
		DomNode n1 = l1.containingNode();
		DomNode n2 = l2.containingNode();
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
			return l1.treeIndex - l2.treeIndex;
		}
	}

	Location createRelativeLocation(Location location, int offset);

	DomNode getContainingNode(Location location);

	String textContent(Range range);
}
