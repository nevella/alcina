package cc.alcina.framework.common.client.dom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.ranges.DocumentRange;

import com.google.common.base.Preconditions;

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

	default Location createTextRelativeLocation(Location location, int offset,
			boolean after) {
		int index = location.getIndex() + offset;
		/*
		 * Special case, preserve existing node if possible)
		 */
		DomNode containingNode = location.getContainingNode();
		int contentLength = getContentLength(containingNode);
		int relativeIndex = location.getIndex() + offset
				- containingNode.asLocation().getIndex();
		if (relativeIndex >= 0 && relativeIndex <= contentLength) {
			if (relativeIndex == contentLength && contentLength != 0) {
				after = true;
			}
			return new Location(location.getTreeIndex(), index, after,
					location.getContainingNode(), this);
		}
		Location test = new Location(-1, index, after, null, this, location);
		Location containingLocation = getContainingLocation(test, location);
		return new Location(containingLocation.getTreeIndex(), index,
				location.after, containingLocation.getContainingNode(), this);
	}

	/**
	 * Returns the deepest containing location (i.e. corresponding to a #TEXT
	 * node) at test.index
	 * 
	 * @param test
	 * @return
	 */
	Location getContainingLocation(Location test);

	/**
	 * Returns the deepest containing location (i.e. corresponding to a #TEXT
	 * node) at test.index, starting from searchFrom
	 * 
	 * @param test
	 * @return
	 */
	default Location getContainingLocation(Location test, Location searchFrom) {
		return getContainingLocation(test);
	}

	default DomNode getContainingNode(Location test, Location searchFrom) {
		return getContainingLocation(test, searchFrom).getContainingNode();
	}

	default List<DomNode> getContainingNodes(Location start, int index,
			boolean after) {
		List<DomNode> result = new ArrayList<>();
		DomNode root = getDocumentElementNode();
		Location test = start;
		/*
		 * traverse to the first text node lte test
		 */
		while (test != null
				&& !(test.isTextNode() && test.getIndex() > index)) {
			test = test.relativeLocation(RelativeDirection.PREVIOUS_LOCATION,
					TextTraversal.TO_START_OF_NODE);
		}
		/*
		 * ... or the first in the doc
		 */
		if (test == null) {
			test = getDocumentElementNode().asLocation();
			while (!test.isTextNode() && test != null) {
				test = test.relativeLocation(RelativeDirection.NEXT_LOCATION);
			}
		}
		test = test.textRelativeLocation(index - test.getIndex(), after);
		Location containingLocation = test;
		while (!containingLocation.isTextNode()) {
			containingLocation = containingLocation
					.relativeLocation(RelativeDirection.NEXT_LOCATION);
		}
		/*
		 * if this text starts at index, and we're ascending from an 'after', go
		 * to the previous text
		 */
		if (after && containingLocation.getContainingNode().asLocation()
				.getIndex() == index && index > 0) {
			containingLocation = containingLocation.relativeLocation(
					RelativeDirection.PREVIOUS_LOCATION,
					TextTraversal.EXIT_NODE);
			while (!containingLocation.isTextNode()) {
				containingLocation = containingLocation
						.relativeLocation(RelativeDirection.PREVIOUS_LOCATION);
			}
			containingLocation.relativeLocation(RelativeDirection.NEXT_LOCATION,
					TextTraversal.TO_END_OF_NODE);
		}
		Preconditions.checkState(containingLocation.getIndex() >= index
				&& containingLocation.getIndex() <= index + containingLocation
						.getContainingNode().textContent().length());
		DomNode cursor = containingLocation.getContainingNode();
		do {
			result.add(cursor);
			cursor = cursor.parent();
		} while (cursor.isElement());
		Collections.reverse(result);
		return result;
	}

	/**
	 * <p>
	 * Implementation for text is a little complicated, because "next" depends
	 * on the caller to a degree
	 * 
	 * <p>
	 * Note - don't use this for purely text traversal (e.g. mimicking actions
	 * of a keyevent), instead just increment/decrement location.index, and use
	 * {@link #createTextRelativeLocation(Location, int, boolean)}
	 */
	default Location getRelativeLocation(Location location,
			RelativeDirection direction, TextTraversal textTraversal) {
		/*
		 * See Location for a visual explanation of traversal
		 */
		DomNode node = location.getContainingNode();
		DomNode targetNode = node;
		int targetIndex = location.getIndex();
		boolean targetAfter = !location.after;
		Location baseLocation = node.asLocation();
		Location parentLocation = !node.parent().isElement() ? null
				: node.parent().asLocation();
		boolean nodeTraversalRequired = false;
		if (direction == RelativeDirection.CURRENT_NODE_END) {
			Preconditions.checkArgument(targetAfter);
			Integer length = getContentLength(node);
			targetIndex = baseLocation.getIndex() + length;
			return new Location(node.asLocation().getTreeIndex(), targetIndex,
					targetAfter, node, this);
		}
		if (node.isText()) {
			int relativeIndex = location.getIndex() - baseLocation.getIndex();
			switch (direction) {
			case NEXT_LOCATION: {
				if (relativeIndex == node.textContent().length()) {
					nodeTraversalRequired = true;
				} else {
					switch (textTraversal) {
					case NEXT_CHARACTER:
						targetIndex++;
						break;
					case NO_CHANGE:
						break;
					case EXIT_NODE:
						nodeTraversalRequired = true;
						break;
					case TO_END_OF_NODE:
						targetIndex = baseLocation.getIndex()
								+ node.textContent().length();
						break;
					default:
						throw new UnsupportedOperationException();
					}
				}
				break;
			}
			case PREVIOUS_LOCATION: {
				if (relativeIndex == 0) {
					nodeTraversalRequired = true;
				} else {
					switch (textTraversal) {
					case PREVIOUS_CHARACTER:
						targetIndex--;
						break;
					case NO_CHANGE:
						break;
					case EXIT_NODE:
						nodeTraversalRequired = true;
						break;
					case TO_START_OF_NODE:
						targetIndex = baseLocation.getIndex();
						break;
					default:
						throw new UnsupportedOperationException();
					}
				}
				break;
			}
			case PREVIOUS_DOMNODE_START: {
				nodeTraversalRequired = true;
				break;
			}
			case NEXT_DOMNODE_START: {
				nodeTraversalRequired = true;
				break;
			}
			default:
				throw new UnsupportedOperationException();
			}
		} else {
			nodeTraversalRequired = true;
		}
		if (nodeTraversalRequired) {
			targetIndex = -1;
			switch (direction) {
			case NEXT_LOCATION: {
				if (location.after) {
					DomNode nextSibling = node.relative().nextSibling();
					if (nextSibling == null) {
						// last, ascend
						targetNode = parentLocation != null
								? parentLocation.getContainingNode()
								: null;
						targetAfter = true;
					} else {
						targetNode = nextSibling;
					}
				} else {
					// descend or go to next sibling
					DomNode next = node.relative().treeSubsequentNode();
					if (next == null) {
						// top, ascend
						targetNode = parentLocation != null
								? parentLocation.getContainingNode()
								: getDocumentElementNode();
					} else {
						targetNode = next;
						targetAfter = false;
					}
				}
				break;
			}
			case PREVIOUS_LOCATION: {
				if (!location.after) {
					DomNode previousSibling = node.relative().previousSibling();
					if (previousSibling == null) {
						// last, ascend
						targetNode = parentLocation != null
								? parentLocation.getContainingNode()
								: null;
						targetAfter = false;
					} else {
						targetNode = previousSibling;
					}
				} else {
					DomNode lastChild = node.children.lastNode();
					if (lastChild == null) {
						// just the start of the current node
					} else {
						// end of the last child
						targetNode = lastChild;
						targetAfter = true;
					}
				}
				break;
			}
			case PREVIOUS_DOMNODE_START: {
				// if at start, go to previous logical node - if at end, go
				// to last descendant
				targetAfter = false;
				if (!location.after) {
					targetNode = node.relative().treePreviousNode();
				} else {
					DomNode lastDescendant = node.relative().lastDescendant();
					if (lastDescendant != null) {
						targetNode = lastDescendant;
					} else {
						targetNode = node.relative().treePreviousNode();
					}
				}
				break;
			}
			case NEXT_DOMNODE_START: {
				// if at start, go to next logical node from start - if at
				// end, go
				// next logical node from last descendant
				targetAfter = false;
				if (!location.after) {
					targetNode = node.relative().treeSubsequentNode();
				} else {
					DomNode lastDescendant = node.relative().lastDescendant();
					if (lastDescendant != null) {
						targetNode = lastDescendant.relative()
								.treeSubsequentNode();
					} else {
						targetNode = node.relative().treeSubsequentNode();
					}
				}
				break;
			}
			default:
				throw new UnsupportedOperationException();
			}
		}
		if (targetNode == null) {
			return null;
		}
		DomNode containingNode = targetNode;
		if (targetIndex == -1) {
			Location nodeLocation = containingNode.asLocation();
			targetIndex = nodeLocation.getIndex();
			if (targetAfter) {
				if (containingNode.isText()
						&& textTraversal == TextTraversal.TO_START_OF_NODE) {
				} else {
					targetIndex += getContentLength(containingNode);
				}
			}
		}
		return new Location(targetNode.asLocation().getTreeIndex(), targetIndex,
				targetAfter, containingNode, this);
	}

	DomNode getDocumentElementNode();

	String getSubsequentText(Location location, int chars);

	default Document w3cDoc() {
		return getDocumentElementNode().document.w3cDoc();
	}

	default String markupContent(Range range) {
		DomNode node = range.containingNode();
		if (node.isText()) {
			return range.text();
		}
		if (range.start.getContainingNode() == node
				&& range.end.getContainingNode() == node) {
			String markup = node.toMarkup();
			// if namespaced, return full
			// FIXME - selection - have a 'robust pretty' that uses a
			// variant on Element.getOuterHtml()
			if (markup
					.matches("(?s).*(</[a-zA-Z9-9]+:[a-zA-Z9-9]+>|&nbsp;).*")) {
				return markup;
			} else {
				return node.toPrettyMarkup();
			}
		} else {
			org.w3c.dom.ranges.Range w3cRange = ((DocumentRange) w3cDoc())
					.createRange();
			if (range.start.getContainingNode().isText()) {
				w3cRange.setStart(range.start.getContainingNode().node,
						range.start.indexInNode());
			} else {
				w3cRange.setStartBefore(range.start.getContainingNode().node);
			}
			if (range.end.getContainingNode().isText()) {
				w3cRange.setEnd(range.end.getContainingNode().node,
						range.end.indexInNode());
			} else {
				w3cRange.setEndAfter(range.end.getContainingNode().node);
			}
			DocumentFragment fragment = w3cRange.cloneContents();
			Element fragmentContainer = w3cDoc()
					.createElement("fragment-container");
			fragmentContainer.appendChild(fragment);
			return DomNode.from(fragmentContainer).toMarkup();
		}
	}

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

	int getContentLength(DomNode domNode);

	default public Location.Range asRange(DomNode domNode) {
		Location start = asLocation(domNode);
		Location end = null;
		if (domNode.isText()) {
			end = createTextRelativeLocation(start, getContentLength(domNode),
					true);
		} else {
			end = asLocation(domNode).clone();
			end.setIndex(end.getIndex() + getContentLength(domNode));
			// only for non-text (text locations do not use after)
			end.after = true;
		}
		return new Location.Range(start, end);
	}

	Range getDocumentRange();

	void ensureCurrent(Location location);

	default void validateLocations() {
	}

	default Location getLocation(Node refNode, int offset, boolean after) {
		DomNode domNode = getDocumentElementNode().document.nodeFor(refNode);
		if (domNode.isText()) {
			return domNode.asLocation().textRelativeLocation(offset, after);
		} else {
			Location location = domNode.children.nodes().get(offset)
					.asLocation();
			return after
					? location.relativeLocation(
							RelativeDirection.CURRENT_NODE_END)
					: location;
		}
	}

	default Location getLocation(Node refNode, boolean after) {
		Location location = getDocumentElementNode().document.nodeFor(refNode)
				.asLocation();
		return after
				? location.relativeLocation(RelativeDirection.CURRENT_NODE_END)
				: location;
	}
}
