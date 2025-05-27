package com.google.gwt.dom.client;

import java.util.Map;
import java.util.Objects;

import org.w3c.dom.DOMException;
import org.w3c.dom.Node;
import org.w3c.dom.ranges.RangeException;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.Location;
import cc.alcina.framework.common.client.dom.Location.RelativeDirection;
import cc.alcina.framework.common.client.util.AlcinaCollections;

public class Range implements org.w3c.dom.ranges.Range {
	Location.DomLocation start;

	Location.DomLocation end;

	Document document;

	Range(Document document) {
		this.document = document;
	}

	@Override
	public Node getStartContainer() throws DOMException {
		return start.getNode();
	}

	@Override
	public int getStartOffset() throws DOMException {
		return start.getOffset();
	}

	@Override
	public Node getEndContainer() throws DOMException {
		return end.getNode();
	}

	@Override
	public int getEndOffset() throws DOMException {
		return end.getOffset();
	}

	@Override
	public boolean getCollapsed() throws DOMException {
		return Objects.equals(start, end);
	}

	@Override
	public Node getCommonAncestorContainer() throws DOMException {
		return start.getLocation().getContainingNode()
				.getCommonAncestorContainer(
						end.getLocation().getContainingNode(), false)
				.w3cNode();
	}

	@Override
	public void setStart(Node refNode, int offset)
			throws RangeException, DOMException {
		start = document.domDocument.locations()
				.getLocation(refNode, offset, false).asDomLocation();
	}

	@Override
	public void setEnd(Node refNode, int offset)
			throws RangeException, DOMException {
		end = document.domDocument.locations()
				.getLocation(refNode, offset, false).asDomLocation();
	}

	@Override
	public void setStartBefore(Node refNode)
			throws RangeException, DOMException {
		start = document.domDocument.locations().getLocation(refNode, false)
				.asDomLocation();
	}

	@Override
	public void setStartAfter(Node refNode)
			throws RangeException, DOMException {
		start = document.domDocument.locations().getLocation(refNode, false)
				.getContainingNode().relative().treeSubsequentNodeNoDescent()
				.asLocation().asDomLocation();
	}

	@Override
	public void setEndBefore(Node refNode) throws RangeException, DOMException {
		end = document.domDocument.locations().getLocation(refNode, false)
				.getContainingNode().relative().treePreviousNode().asLocation()
				.relativeLocation(RelativeDirection.CURRENT_NODE_END)
				.asDomLocation();
	}

	@Override
	public void setEndAfter(Node refNode) throws RangeException, DOMException {
		end = document.domDocument.locations().getLocation(refNode, true)
				.asDomLocation();
	}

	@Override
	public void collapse(boolean toStart) throws DOMException {
		if (toStart) {
			end = start;
		} else {
			start = end;
		}
	}

	@Override
	public void selectNode(Node refNode) throws RangeException, DOMException {
		setStartBefore(refNode);
		setEndAfter(refNode);
	}

	@Override
	public void selectNodeContents(Node refNode)
			throws RangeException, DOMException {
		setStart(refNode, 0);
		setEndAfter(refNode.getLastChild());
	}

	@Override
	public short compareBoundaryPoints(short how,
			org.w3c.dom.ranges.Range sourceRange) throws DOMException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void deleteContents() throws DOMException {
		throw new UnsupportedOperationException();
	}

	@Override
	public DocumentFragment extractContents() throws DOMException {
		throw new UnsupportedOperationException();
	}

	@Override
	public DocumentFragment cloneContents() throws DOMException {
		Preconditions.checkState(start.getLocation().isAtNodeStart());
		Preconditions.checkState(end.getLocation().isAtNodeEnd());
		DocumentFragment documentFragment = document.createDocumentFragment();
		Map<DomNode, com.google.gwt.dom.client.Node> originalCloned = AlcinaCollections
				.newUnqiueMap();
		Location cursor = start.getLocation();
		/*
		 * commonContainerDomNode corresponds structurally to the fragment
		 */
		DomNode commonContainerDomNode = gwtNode(getCommonAncestorContainer())
				.asDomNode();
		originalCloned.put(commonContainerDomNode, documentFragment);
		while (cursor != null && cursor.isBefore(end.getLocation())) {
			DomNode ascentCursor = cursor.getContainingNode();
			DomNode appendTo = null;
			com.google.gwt.dom.client.Node lastCloned = null;
			for (;;) {
				com.google.gwt.dom.client.Node cloned = ascentCursor
						.cloneNode(false).gwtNode();
				if (lastCloned != null) {
					cloned.appendChild(lastCloned);
				}
				lastCloned = cloned;
				originalCloned.put(ascentCursor, cloned);
				DomNode next = ascentCursor.parent();
				com.google.gwt.dom.client.Node originalClonedNode = originalCloned
						.get(next);
				if (originalClonedNode != null) {
					originalClonedNode.appendChild(cloned);
					break;
				} else {
					ascentCursor = next;
				}
			}
			cursor = cursor
					.relativeLocation(RelativeDirection.NEXT_DOMNODE_START);
		}
		String markup = documentFragment.getMarkup();
		return documentFragment;
	}

	com.google.gwt.dom.client.Node gwtNode(Node node) {
		return (com.google.gwt.dom.client.Node) node;
	}

	@Override
	public void insertNode(Node newNode) throws DOMException, RangeException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void surroundContents(Node newParent)
			throws DOMException, RangeException {
		throw new UnsupportedOperationException();
	}

	@Override
	public org.w3c.dom.ranges.Range cloneRange() throws DOMException {
		Range result = new Range(document);
		result.start = start;
		result.end = end;
		return result;
	}

	@Override
	public void detach() throws DOMException {
		/*
		 * Noop, this introduces no cycles (LocationContext doesn't retain
		 * Locations)
		 */
	}
}
