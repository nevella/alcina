package cc.alcina.framework.common.client.dom;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.IntPair;
import cc.alcina.framework.common.client.util.TextUtils;

/**
 * Models a position in an alcina dom document via [T,I,A]
 *
 * FIXME - Feature_Dirndl_ContentDecorator - make immutable, and encapsulate
 * fields (actually - no - locations are inherently mutable)
 *
 * Add plan re tracking of dom mutations
 *
 *
 */
/*
 * @formatter:off

The plan for general mutation handling
----------------------------------------

The serialized representation of a location must provide info as to whether a location is at the start or end of a range,
either by serialized field or by (say) having a 'start' and 'end' location property of some parent object.

The current (treeindex/runindex) fields should be augmented to 'treeindex/runindex/offset' - all computed via an ensure() check
in the getter, which checks the computedOffsetEvent value against the container (document) mutationEvent counter

Re-computation should just involve offset addition and ascent to root (O(N) where N is the tree depth), and update should just involve
mutation of parent.children.offset (only required if the child is not at the end of parent.children, the most common case durin)

'After' is computable via location.parent.children lookup - if 'containingNode' == location.parent.children[offset].node, then
 the location is at the start of the node, otherwise at the end (FIXME - doc - show a worked example)

 Note that because indicies in a text node are controlled by the index (not treeindex) field, 'before' and 'after' is not a 
 useful distinction, so Locations in a text node created with after == true and normalised to after == false, and 
 the serialized (tostring) form of a text location has no before/after info


(bf1) El1                                                                                   (af1)
	(bf2)  E2                                           (af2)  (bf4) E4 (af4)  (bf5) E5 (af5)
			(bf3.0) T3:'a'                      (af3)
					  (3.1)'b' (3.2)

Traversal is the 'tree' of bf/afs (before/after) shown above. before/after is ire


 *FIXME - ser - make most fields final
 * @formatter:on
 */
public class Location implements Comparable<Location> {
	public enum TextTraversal {
		NO_CHANGE, NEXT_CHARACTER, EXIT_NODE, TO_START_OF_NODE, TO_END_OF_NODE,
		// will throw if traversing a text node
		UNDEFINED
	}

	public class Adjust {
		public Location trimToFirstNonWhitespaceCharacer() {
			String text = containingNode.textContent();
			int idx = 0;
			for (; idx < text.length() - 1; idx++) {
				if (TextUtils
						.isWhitespaceOrEmpty(text.substring(idx, idx + 1))) {
					// continue
				} else {
					break;
				}
			}
			if (idx == 0) {
				return Location.this;
			} else {
				return createRelativeLocation(idx, after);
			}
		}
	}

	// Feature group class for content access
	public class Content {
		/**
		 * returns a relative substring of the containing Text node's text, or
		 * null if the relative offsets are out of bounds (of the text node)
		 */
		public String relativeString(int startOffset, int endOffset) {
			DomNode node = Location.this.containingNode;
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
		class Itr implements Iterator<Location> {
			Location cursor;

			Itr() {
				this.cursor = start;
			}

			@Override
			public boolean hasNext() {
				return cursor != null && cursor.compareTo(end) <= 0;
			}

			@Override
			public Location next() {
				Location next = cursor;
				cursor = cursor.relativeLocation(
						RelativeDirection.NEXT_LOCATION,
						TextTraversal.EXIT_NODE);
				return next;
			}
		}

		/**
		 * Create a range from start-end, or end-start if end is before start
		 */
		public static Range fromPossiblyReversedEndpoints(Range range1,
				Range range2) {
			if (range1.compareToEarlierEndEarlier(range2) <= 0) {
				return new Range(range1.start, range2.end);
			} else {
				return new Range(range2.start, range1.end);
			}
		}

		public final Location start;

		public final Location end;

		private transient String textContent;

		public Range(Location start, Location end) {
			if (start.isAfter(end)) {
				Location tmp = start;
				start = end;
				end = tmp;
			}
			this.start = start;
			this.end = end;
		}

		/*
		 * Returns a stream of locations in the range
		 */
		public Stream<Location> stream() {
			Iterable<Location> iterable = () -> new Itr();
			return StreamSupport.stream(iterable.spliterator(), false);
		}

		public Range truncateAbsolute(int startIndex, int endIndex) {
			Location modifiedEnd = end
					.createRelativeLocation(endIndex - end.index, true);
			Location modifiedStart = start
					.createRelativeLocation(startIndex - start.index, false);
			return new Range(modifiedStart, modifiedEnd);
		}

		public Range truncateToIndexEnd(int endIndex) {
			Location modifiedEnd = end.createRelativeLocation(
					endIndex - (end.index - start.index), true);
			return new Range(start, modifiedEnd);
		}

		public Range truncateToIndexStart(int startIndex) {
			Location modifiedStart = start.createRelativeLocation(startIndex,
					false);
			return new Range(modifiedStart, end);
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

		public int compareToEarlierEndEarlier(Range o) {
			{
				int cmp = start.compareTo(o.start, false);
				if (cmp != 0) {
					return cmp;
				}
			}
			{
				int cmp = end.compareTo(o.end, false);
				if (cmp != 0) {
					return cmp;
				}
			}
			return 0;
		}

		// FIXME - selection - throw if start.node != end.node?
		public DomNode containingNode() {
			return start.containingNode;
		}

		public boolean contains(Range o) {
			return start.compareTo(o.start) <= 0 && end.compareTo(o.end) >= 0;
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
			return toIntPair().isPoint();
		}

		public String text() {
			if (textContent == null && start.locationContext != null) {
				textContent = start.locationContext.textContent(this);
			}
			return textContent;
		}

		public String markup() {
			return start.locationContext.markupContent(this);
		}

		public IntPair toIntPair() {
			return new IntPair(start.index, end.index);
		}

		@Override
		public String toString() {
			return FormatBuilder.keyValues("start", start, "end", end, "text",
					Ax.trimForLogging(text()));
		}

		public Range truncateToEndNode() {
			Location modifiedStart = end
					.createRelativeLocation(start.index - end.index, false);
			return new Range(modifiedStart, end);
		}

		/*
		 * Preserves start.index and end.index, but changes treeindicies to that
		 * of the lowest/highest node with those indicies
		 */
		public Range toDeepestNodes() {
			List<DomNode> startContainers = start.locationContext
					.getContainingNodes(start.index, start.after);
			List<DomNode> endContainers = start.locationContext
					.getContainingNodes(end.index, end.after);
			DomNode minimal = startContainers.stream()
					.filter(endContainers::contains).reduce(Ax.last()).get();
			Location minimalLocation = minimal.asLocation();
			return new Range(
					minimalLocation.createRelativeLocation(
							start.index - minimalLocation.index, start.after),
					minimalLocation.createRelativeLocation(
							end.index - minimalLocation.index, end.after));
		}

		@Property.Not
		public boolean isWholeNode() {
			return start.containingNode.asRange().equals(this);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Range) {
				Range o = (Range) obj;
				return o.start.equals(start) && o.end.equals(end);
			} else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			return start.hashCode() ^ end.hashCode();
		}
	}

	public enum RelativeDirection {
		NEXT_LOCATION, NEXT_DOMNODE_START, PREVIOUS_LOCATION,
		PREVIOUS_DOMNODE_START, CURRENT_NODE_END, NEXT_CONTAINED_LOCATION,
		PREVIOUS_CONTAINED_LOCATION
	}

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
	 * The location is *after* (immediately after the end of) the containingNode
	 */
	public boolean after;

	public transient DomNode containingNode;

	transient LocationContext locationContext;

	public Location(int treeIndex, int index, boolean after) {
		this(treeIndex, index, after, null, null);
	}

	public Location(int treeIndex, int index, boolean after,
			DomNode containingNode, LocationContext locationContext) {
		this.treeIndex = treeIndex;
		this.index = index;
		this.locationContext = locationContext;
		if (containingNode == null) {
			containingNode = locationContext.getContainingNode(this);
		}
		if (containingNode.isText()) {
			after = false;
		}
		this.after = after;
		this.containingNode = containingNode;
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
		return treeIndex ^ index ^ (after ? 1 : 0);
	}

	public String getSubsequentText() {
		return getSubsequentText(100);
	}

	public String getSubsequentText(int chars) {
		return locationContext.getSubsequentText(this, chars);
	}

	public int indexInNode() {
		return index - containingNode.asLocation().index;
	}

	public boolean isAfter(Location other) {
		return compareTo(other) > 0;
	}

	public boolean isBefore(Location other) {
		return compareTo(other) < 0;
	}

	public Location relativeLocation(RelativeDirection direction) {
		return relativeLocation(direction, TextTraversal.UNDEFINED);
	}

	public Location relativeLocation(RelativeDirection direction,
			TextTraversal textTraversal) {
		return locationContext.getRelativeLocation(this, direction,
				textTraversal);
	}

	public void setLocationContext(LocationContext locationSupplier) {
		this.locationContext = locationSupplier;
	}

	public Adjust adjust() {
		return new Adjust();
	}

	@Override
	public String toString() {
		String nodeData = null;
		if (containingNode.isText()) {
			nodeData = Ax.format("'%s'", getSubsequentText(50));
		} else {
			String classData = Ax.notBlank(containingNode.getClassName())
					? "." + containingNode.getClassName()
					: "";
			nodeData = Ax.format("<%s%s> :: '%s'", containingNode.name(),
					classData, getSubsequentText(50));
		}
		String dir = containingNode.isText() ? "" : after ? ",>" : ",<";
		return Ax.format("%s,%s%s %s", treeIndex, index, dir, nodeData);
	}

	public String toLocationString() {
		String dir = containingNode.isText() ? "" : after ? ",>" : ",<";
		return Ax.format("%s,%s%s", treeIndex, index, dir);
	}

	public Location toTextLocation(boolean toTextLocations) {
		if (!toTextLocations) {
			return this;
		}
		if (containingNode.isText()) {
			return this;
		}
		DomNode text = Ax
				.last(locationContext.getContainingNodes(index, after));
		Preconditions.checkState(text.isText());
		return new Location(text.asLocation().treeIndex, index, after, text,
				locationContext);
	}

	public boolean isAtNodeStart() {
		return getTextOffsetInNode() == 0;
	}

	@Property.Not
	public int getTextOffsetInNode() {
		return index - containingNode.asLocation().index;
	}

	public Location toEndOfTextLocation() {
		Preconditions.checkState(containingNode.isText());
		return new Location(treeIndex,
				index + containingNode.textContent().length(), false,
				containingNode, locationContext);
	}

	public boolean isAtNodeEnd() {
		return getTextOffsetInNode() == containingNode.textContent().length();
	}

	public boolean isTextNode() {
		return containingNode.isText();
	}

	public Location toStartTextLocationIfAtEnd() {
		if (isAtNodeEnd() && isTextNode()) {
			return new Location(treeIndex + 1, index, false, null,
					locationContext);
		} else {
			return this;
		}
	}

	public Location toEndTextLocationIfAtStart() {
		if (isAtNodeStart()) {
			Location cursor = this;
			while (true) {
				// the logic of text runs guarantees there will be a text
				// nodlocatione ending at index prior to this location
				cursor = new Location(cursor.treeIndex - 1, index, false, null,
						locationContext);
				if (cursor.isTextNode()) {
					return cursor;
				}
			}
		} else {
			return this;
		}
	}
}
