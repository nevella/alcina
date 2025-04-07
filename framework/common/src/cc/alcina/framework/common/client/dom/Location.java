package cc.alcina.framework.common.client.dom;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.w3c.dom.Text;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.traversal.layer.MeasureSelection;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.IntPair;

/**
 * <p>
 * Models a position in an alcina dom document via [T,I,A]
 *
 * <p>
 * FIXME - Feature_Dirndl_ContentDecorator - make immutable, and encapsulate
 * fields (actually - no - locations are inherently mutable)
 *
 * <h2>The case for 'after'</h2>
 *
 * <p>
 * See the (text) diagram below. Note the sequence af3 - af2 - bf4. This is
 * impossible to express uniquely with just "befores" - it could be expressed
 * with a node/offset (as per XPointer) - as say E1[0] == bf2, E2[1]= af2 - but
 * that's using a model of two different levels of the node hierarchy to express
 * facts that are essentially about one level. In any case, that won't work for
 * our model of the doc (where the second ordinal is absolute, not relative)
 *
 * <p>
 * <code><pre>

(bf1) E1                                                                                   (af1)
	(bf2)  E2                                           (af2)  (bf4) E4 (af4)  (bf5) E5 (af5)
			(bf3.0) T3:'a'                      (af3)
					  (3.1)'b' (3.2)
 * </pre></code>
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

Traversal is the 'tree' of bf/afs (before/after) shown above. before/after is ignored when

# Mutation continued

All class usages should validate document locations (treeIndex, index, containingNode) on non-private entry

 *FIXME - ser - make most fields final
 * @formatter:on
 */
/*
 * @formatter:off
 * 
 
 # Location mutation

 ## Overview

 - Each dom mutation is recorded as a location mutation - i.e. a delta of [index.delta,treeindex.delta] at [index,treeindex]
 - Each mutation immediately updates directly affected nodes. Added/removed nodes are added/removed from the locations 
   lookups, in the case of cdata mutations [an exception is thrown in non-split/join, pending diff computation]
 - On access, the location recomputes its indicies from the locationmutations since the last mutation
 - After a recomputation threshold is reached, all locations are recomputed
 - TODO -
   - How does this interact with ordering of locations? 
   - Are locations unique for a given node/index? [should be] 
   - If so, should they be modelled on the node [probably]

## Locations2 implementation

- Locations are stored on the domnode
- There's no explicit 'contents' text storage - just the dom text nodes. It's the indicies we store separately
- 


 * @formatter:on
 */
public class Location implements Comparable<Location> {
	/**
	 * Node index in depth-first traversal.
	 */
	private int treeIndex;

	/**
	 * Absolute character index (in the document tex run, aka 'innerText". As
	 * per Swing, it's notionally 'before' the character. Can be
	 * innerText.length()+1 (in which case it's after the last character in the
	 * document)
	 */
	private int index;

	/**
	 *
	 */
	/***
	 * The location is *after* (immediately after the end of) the
	 * containingNode. Immutable under document modification (wheras index and
	 * treeIndex are not)
	 *
	 *
	 */
	public boolean after;

	/*
	 * the domnode at treeindex
	 */
	private transient DomNode containingNode;

	private transient LocationContext locationContext;

	transient int documentMutationPosition;

	public Location(int treeIndex, int index, boolean after) {
		this(treeIndex, index, after, null, null);
	}

	/**
	 * Constructs a representation location in a document
	 *
	 *
	 * @param treeIndex
	 *            the depth-first traversal encounter index of the dom node. Can
	 *            by -1, in which case it will be the encounter index of the
	 *            text node containing the text at param index
	 * @param index
	 *            the index in the document text of the location
	 * @param after
	 *            at the end of the dom node at treeIndex
	 * @param containingNode
	 *            the containing node
	 * @param locationContext
	 *            used to resolve domnodes from treeIndex/index
	 */
	public Location(int treeIndex, int index, boolean after,
			DomNode containingNode, LocationContext locationContext) {
		this.treeIndex = treeIndex;
		this.index = index;
		this.locationContext = locationContext;
		this.documentMutationPosition = locationContext
				.getDocumentMutationPosition();
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

	public Range asRange() {
		return new Range(this, this);
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

	/**
	 *
	 * This creates a *text* relative location, rather than traversing the
	 * location tree (which unifies the node and text sequences).
	 *
	 */
	public Location createTextRelativeLocation(int offset, boolean after) {
		return locationContext.createTextRelativeLocation(this, offset, after);
	}

	public void detach() {
		containingNode = null;
		locationContext = null;
	}

	/**
	 * This does not invalidate the LocationContext locations, do that manually
	 * (until the tree/log location invalidation handling is implemented)
	 */
	public void ensureAtBoundary() {
		int textOffsetInNode = getTextOffsetInNode();
		int length = containingNode.textContent().length();
		if (textOffsetInNode != 0 && textOffsetInNode != length) {
			((Text) containingNode.w3cNode()).splitText(textOffsetInNode);
		}
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

	public DomNode getContainingNode() {
		return containingNode;
	}

	public int getIndex() {
		locationContext.ensureCurrent(this);
		return index;
	}

	public LocationContext getLocationContext() {
		return locationContext;
	}

	public String getSubsequentText() {
		return getSubsequentText(100);
	}

	public String getSubsequentText(int chars) {
		return locationContext.getSubsequentText(this, chars);
	}

	@Property.Not
	public int getTextOffsetInNode() {
		return index - containingNode.asLocation().index;
	}

	public int getTreeIndex() {
		locationContext.ensureCurrent(this);
		return treeIndex;
	}

	@Override
	public int hashCode() {
		return treeIndex + index + treeIndex ^ index ^ (after ? 1 : 0);
	}

	public int indexInNode() {
		return index - containingNode.asLocation().index;
	}

	public boolean isAfter(Location other) {
		return compareTo(other) > 0;
	}

	public boolean isAtDocumentEnd() {
		return treeIndex == 0 && isAtNodeEnd();
	}

	public boolean isAtDocumentStart() {
		return treeIndex == 0 && isAtNodeStart();
	}

	public boolean isAtNodeEnd() {
		return getTextOffsetInNode() == containingNode.textContent().length();
	}

	public boolean isAtNodeStart() {
		return getTextOffsetInNode() == 0;
	}

	public boolean isBefore(Location other) {
		return compareTo(other) < 0;
	}

	public boolean isTextNode() {
		return containingNode.isText();
	}

	public Location relativeLocation(RelativeDirection direction) {
		return relativeLocation(direction, TextTraversal.UNDEFINED);
	}

	public Location relativeLocation(RelativeDirection direction,
			TextTraversal textTraversal) {
		return locationContext.getRelativeLocation(this, direction,
				textTraversal);
	}

	public void setContainingNode(DomNode containingNode) {
		this.containingNode = containingNode;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public void setTreeIndex(int treeIndex) {
		this.treeIndex = treeIndex;
	}

	public Location toEndOfTextLocation() {
		Preconditions.checkState(containingNode.isText());
		return new Location(treeIndex,
				index + containingNode.textContent().length(), false,
				containingNode, locationContext);
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

	public String toLocationString() {
		String dir = containingNode == null ? "[detached location]"
				: containingNode.isText() ? "" : after ? ",>" : ",<";
		return Ax.format("%s,%s%s", treeIndex, index, dir);
	}

	public Location toStartTextLocationIfAtEnd() {
		if (isAtNodeEnd() && isTextNode()) {
			return new Location(treeIndex + 1, index, false, null,
					locationContext);
		} else {
			return this;
		}
	}

	public Location toStartLocation() {
		if (isAtNodeEnd() && isTextNode()) {
			return new Location(treeIndex + 1, index, false, null,
					locationContext);
		} else if (after) {
			return new Location(treeIndex + 1, index, false, null,
					locationContext);
		} else {
			return this;
		}
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

	void validateIndicies() {
	}

	// Feature group class for content access
	public class Content {
		public String absoluteString(int from, int to) {
			return getLocationContext().textContent(from, to);
		}

		/**
		 * returns a relative substring of the containing Text node's text, or
		 * null if the relative offsets are out of bounds (of the text node)
		 */
		public String relativeString(int startOffset, int endOffset) {
			DomNode node = Location.this.getContainingNode();
			Preconditions.checkState(node.isText());
			String textContent = node.textContent();
			int end = textContent.length();
			Location base = node.asLocation();
			int relativeOffset = getIndex() - base.getIndex();
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

		private transient String normalisedTextContent;

		public Range(Location start, Location end) {
			if (start.isAfter(end)) {
				Location tmp = start;
				start = end;
				end = tmp;
			}
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
			return start.getContainingNode();
		}

		public boolean contains(Location l) {
			return start.compareTo(l) <= 0 && end.compareTo(l) >= 0;
		}

		/**
		 * This method is end-inclusive (this.contains(this) == true)
		 *
		 * @param o
		 * @return
		 */
		public boolean contains(Range o) {
			return start.compareTo(o.start) <= 0 && end.compareTo(o.end) >= 0;
		}

		public void delete() {
			// TODO Auto-generated method stub
			throw new UnsupportedOperationException(
					"Unimplemented method 'delete'");
		}

		public void detach() {
			start.detach();
			end.detach();
			textContent = null;
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

		/**
		 * Extend the range via text movement
		 */
		public Location.Range extendText(int delta) {
			if (delta < 0) {
				return new Range(start.createTextRelativeLocation(-1, false),
						end);
			} else {
				return new Range(start,
						end.createTextRelativeLocation(1, false));
			}
		}

		@Override
		public int hashCode() {
			return start.hashCode() + end.hashCode() + start.hashCode()
					^ end.hashCode();
		}

		@Property.Not
		public boolean isWholeNode() {
			return start.getContainingNode().asRange().equals(this);
		}

		public int length() {
			return text().length();
		}

		public String markup() {
			return start.getLocationContext().markupContent(this);
		}

		public Range merge(Range other) {
			Location mergedStart = start.isBefore(other.start) ? start
					: other.start;
			Location mergedEnd = end.isAfter(other.end) ? end : other.end;
			return new Range(mergedStart, mergedEnd);
		}

		public String ntc() {
			if (normalisedTextContent == null
					&& start.getLocationContext() != null) {
				normalisedTextContent = Ax.ntrim(text());
			}
			return normalisedTextContent;
		}

		public Location provideEndpoint(int numericDirection) {
			if (numericDirection < 0) {
				return start;
			} else if (numericDirection > 0) {
				return end;
			} else {
				throw new UnsupportedOperationException();
			}
		}

		public boolean provideIsPoint() {
			return toIntPair().isPoint();
		}

		/*
		 * Returns a stream of locations in the range
		 */
		public Stream<Location> stream() {
			Iterable<Location> iterable = () -> new Itr();
			return StreamSupport.stream(iterable.spliterator(), false);
		}

		public String text() {
			if (textContent == null && start.getLocationContext() != null) {
				textContent = start.getLocationContext().textContent(this);
			}
			return textContent;
		}

		/*
		 * Preserves start.index and end.index, but changes treeindicies to that
		 * of the lowest/highest node with those indicies
		 */
		public Range toDeepestNodes() {
			List<DomNode> startContainers = start.getLocationContext()
					.getContainingNodes(start.getIndex(), start.after);
			List<DomNode> endContainers = start.getLocationContext()
					.getContainingNodes(end.getIndex(), end.after);
			DomNode minimal = startContainers.stream()
					.filter(endContainers::contains).reduce(Ax.last()).get();
			Location minimalLocation = minimal.asLocation();
			return new Range(minimalLocation.createTextRelativeLocation(
					start.getIndex() - minimalLocation.getIndex(), start.after),
					minimalLocation.createTextRelativeLocation(
							end.getIndex() - minimalLocation.getIndex(),
							end.after));
		}

		public IntPair toIntPair() {
			return new IntPair(start.getIndex(), end.getIndex());
		}

		@Override
		public String toString() {
			return FormatBuilder.keyValues("text", toIntPair(), "start", start,
					"end", end, "text", Ax.trimForLogging(text()));
		}

		public Range truncateAbsolute(int startIndex, int endIndex) {
			Location modifiedEnd = end.createTextRelativeLocation(
					endIndex - end.getIndex(), true);
			Location modifiedStart = start.createTextRelativeLocation(
					startIndex - start.getIndex(), false);
			return new Range(modifiedStart, modifiedEnd);
		}

		/**
		 *
		 * Creates a relative range. So [0,range.length()] parameters returns a
		 * copy of the range, [0,range.length()-1] returns the range less the
		 * last character, etc
		 */
		public Range truncateRelative(int startIndex, int endIndex) {
			Location modifiedEnd = end.createTextRelativeLocation(
					start.getIndex() + endIndex - end.getIndex(), true);
			Location modifiedStart = start
					.createTextRelativeLocation(startIndex, false);
			return new Range(modifiedStart, modifiedEnd);
		}

		public Range truncateToEndNode() {
			Location modifiedStart = end.createTextRelativeLocation(
					start.getIndex() - end.getIndex(), false);
			return new Range(modifiedStart, end);
		}

		public Range truncateToIndexEnd(int endIndex) {
			Location modifiedEnd = end.createTextRelativeLocation(
					endIndex - (end.getIndex() - start.getIndex()), true);
			return new Range(start, modifiedEnd);
		}

		public Range truncateToIndexStart(int startIndex) {
			Location modifiedStart = start
					.createTextRelativeLocation(startIndex, false);
			return new Range(modifiedStart, end);
		}

		public interface Has {
			Range provideRange();
		}

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
	}

	public enum RelativeDirection {
		NEXT_LOCATION, NEXT_DOMNODE_START, PREVIOUS_LOCATION,
		PREVIOUS_DOMNODE_START, CURRENT_NODE_END, NEXT_CONTAINED_LOCATION,
		PREVIOUS_CONTAINED_LOCATION
	}

	public enum TextTraversal {
		NO_CHANGE, NEXT_CHARACTER, EXIT_NODE, TO_START_OF_NODE, TO_END_OF_NODE,
		// will throw if traversing a text node
		UNDEFINED
	}

	/**
	 * Returns a location with the index/after of this node, but the treeIndex
	 * of the containing location. Throws an IllegalArgumentException if
	 * containingLocation.node does not contain this location
	 * 
	 * @param containingLocation
	 *            the containing location.
	 * @return the computed index
	 */
	public Location toContainingTreeIndex(Location containingLocation) {
		Preconditions.checkArgument(
				containingLocation.containingNode.asRange().contains(this));
		return new Location(containingLocation.treeIndex, index, after,
				containingLocation.containingNode, locationContext);
	}
}
