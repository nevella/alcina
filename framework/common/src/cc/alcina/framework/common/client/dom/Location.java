package cc.alcina.framework.common.client.dom;

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

	/**
	 * Identical to a depth-first traversal position comparison
	 */
	@Override
	public int compareTo(Location o) {
		Location l1 = this;
		Location l2 = o;
		return locationContext.compare(l1, l2);
	}

	public DomNode containingNode() {
		if (containingNode == null) {
			containingNode = locationContext.getContainingNode(this);
		}
		return containingNode;
	}

	public Location createRelativeLocation(int offset) {
		return locationContext.createRelativeLocation(this, offset);
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

	public void setLocationContext(LocationContext locationSupplier) {
		this.locationContext = locationSupplier;
	}

	@Override
	public String toString() {
		return Ax.format("[%s/%s:%s]", treeIndex, index, after ? ">" : "<");
	}

	public static class Range {
		public final Location start;

		public final Location end;

		private transient String textContent;

		public Range(Location start, Location end) {
			this.start = start;
			this.end = end;
		}

		public int length() {
			return textContent().length();
		}

		public boolean provideIsPoint() {
			return start.index == end.index;
		}

		public String textContent() {
			if (textContent == null) {
				textContent = start.locationContext.textContent(this);
			}
			return textContent;
		}
	}
}
