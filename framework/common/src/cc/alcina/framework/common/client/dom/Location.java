package cc.alcina.framework.common.client.dom;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.util.Ax;

/**
 * Models a position in an alcina dom document via [T,I,A]
 */
public class Location implements Comparable<Location> {
	/**
	 * Node index in depth-first traversal
	 */
	public int treeIndex;

	/**
	 * Absolute character index (in the document tex run, aka 'innerText". As
	 * per Swing, it's notionally 'before' the character. Can be
	 * innerText.length()+1 (in which case it's after the last character in the
	 * document)
	 */
	public int index;

	/**
	 *
	 */
	/***
	 * The location is *after* the resolved node
	 */
	public boolean after;

	transient DomNode containingNode;

	transient LocationContext locationContext;

	public Location(int treeIndex, int index, boolean after) {
		this(treeIndex, index, after, null, null);
	}

	public Location(int treeIndex, int index, boolean after,
			DomNode containingNode, LocationContext locationContext) {
		this.treeIndex = treeIndex;
		this.index = index;
		this.after = after;
		this.containingNode = containingNode;
		this.locationContext = locationContext;
	}

	/**
	 * For serialization
	 */
	Location() {
	}

	@Override
	public Location clone() {
		return new Location(treeIndex, index, after, containingNode,
				locationContext);
	}

	/**
	 * Identical to a depth-first traversal position comparison
	 */
	@Override
	public int compareTo(Location o) {
		return compareTo(o, false);
	}

	public int compareTo(Location o, boolean indexOnly) {
		if (indexOnly) {
			return index - o.index;
		} else {
			/**
			 * Identical to a depth-first traversal position comparison
			 */
			Location l1 = this;
			Location l2 = o;
			return locationContext.compare(l1, l2);
		}
	}

	public DomNode containingNode() {
		if (containingNode == null) {
			containingNode = locationContext.getContainingNode(this);
		}
		return containingNode;
	}

	public Content content() {
		return new Content();
	}

	public Location createRelativeLocation(int offset, boolean after) {
		return locationContext.createRelativeLocation(this, offset, after);
	}

	public void detach() {
		containingNode = null;
		locationContext = null;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Location) {
			Location o = (Location) obj;
			return treeIndex == o.treeIndex && index == o.index
					&& after == o.after;
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return treeIndex ^ index;
	}

	public boolean isAfter(Location other) {
		return compareTo(other) > 0;
	}

	public boolean isBefore(Location other) {
		return compareTo(other) < 0;
	}

	public Location relativeLocation(RelativeDirection direction) {
		return locationContext.getRelativeLocation(this, direction);
	}

	public void setLocationContext(LocationContext locationSupplier) {
		this.locationContext = locationSupplier;
	}

	@Override
	public String toString() {
		String nodeName = containingNode == null ? ""
				: " - " + containingNode.name();
		return Ax.format("[node:%s - txt:%s - %s%s]", treeIndex, index,
				after ? ">" : "<", nodeName);
	}

	// Feature group class for content access
	public class Content {
		/**
		 * returns a relative substring of the containing Text node's text, or
		 * null if the relative offsets are out of bounds (of the text node)
		 */
		public String relativeString(int startOffset, int endOffset) {
			DomNode node = Location.this.containingNode();
			Preconditions.checkState(node.isText());
			String textContent = node.textContent();
			int end = textContent.length();
			Location base = node.asLocation();
			int relativeOffset = index - base.index;
			// check bounds
			if (startOffset + relativeOffset < 0) {
				return null;
			}
			if (endOffset + relativeOffset > end) {
				return null;
			}
			return textContent.substring(startOffset + relativeOffset,
					endOffset + relativeOffset);
		}
	}

	public static class Range implements Comparable<Range> {
		public final Location start;

		public final Location end;

		private transient String textContent;

		public Range(Location start, Location end) {
			this.start = start;
			this.end = end;
		}

		@Override
		public int compareTo(Range o) {
			return compareTo(o, false);
		}

		public int compareTo(Range o, boolean indexOnly) {
			{
				int cmp = start.compareTo(o.start, indexOnly);
				if (cmp != 0) {
					return cmp;
				}
			}
			{
				int cmp = end.compareTo(o.end, indexOnly);
				if (cmp != 0) {
					// later end (and same start) implies this contains o - so
					// order before
					return -cmp;
				}
			}
			return 0;
		}

		// FIXME - selection - throw if start.node != end.node?
		public DomNode containingNode() {
			return start.containingNode();
		}

		public void detach() {
			start.detach();
			end.detach();
			textContent = null;
		}

		public int length() {
			return text().length();
		}

		public boolean provideIsPoint() {
			return start.index == end.index;
		}

		public String text() {
			if (textContent == null) {
				textContent = start.locationContext.textContent(this);
			}
			return textContent;
		}
	}

	public enum RelativeDirection {
		NEXT_LOCATION, NEXT_DOMNODE_START, PREVIOUS_LOCATION,
		PREVIOUS_DOMNODE_START
	}
}
