package cc.alcina.framework.common.client.dom;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Preconditions;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.mutations.MutationRecord;

import cc.alcina.framework.common.client.dom.DomNode.DomNodeTree;
import cc.alcina.framework.common.client.dom.Location.Range;
import cc.alcina.framework.common.client.dom.Location.RelativeDirection;
import cc.alcina.framework.common.client.dom.Location.TextTraversal;

class LocationContext2 implements LocationContext {
	class Mutation {
		int index;

		MutationRecord record;

		Mutation(MutationRecord record) {
			this.record = record;
			index = mutations.size();
		}
	}

	DomDocument document;

	Document gwtDocument;

	List<Mutation> mutations = new ArrayList<>();

	DomNodeTree tree;

	LocationContext2(DomDocument document) {
		Preconditions.checkState(document.domDoc() instanceof Document);
		this.document = document;
		this.gwtDocument = (Document) document.domDoc();
		tree = this.document.getDocumentElementNode().tree();
		document.descendants().forEach(DomNode::asLocation);
	}

	@Override
	public Location createTextRelativeLocation(Location location, int offset,
			boolean end) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException(
				"Unimplemented method 'createTextRelativeLocation'");
	}

	@Override
	public DomNode getContainingNode(Location location) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException(
				"Unimplemented method 'getContainingNode'");
	}

	@Override
	public List<DomNode> getContainingNodes(int index, boolean after) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException(
				"Unimplemented method 'getContainingNodes'");
	}

	@Override
	public Location getRelativeLocation(Location location,
			RelativeDirection direction, TextTraversal textTraversal) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException(
				"Unimplemented method 'getRelativeLocation'");
	}

	@Override
	public String getSubsequentText(Location location, int chars) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException(
				"Unimplemented method 'getSubsequentText'");
	}

	@Override
	public String markupContent(Range range) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException(
				"Unimplemented method 'markupContent'");
	}

	@Override
	public String textContent(Range range) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException(
				"Unimplemented method 'textContent'");
	}

	@Override
	public int toValidIndex(int idx) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException(
				"Unimplemented method 'toValidIndex'");
	}

	@Override
	public void invalidate() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException(
				"Unimplemented method 'invalidate'");
	}

	@Override
	public int getDocumentMutationPosition() {
		return mutations.size();
	}

	@Override
	public Location asLocation(DomNode domNode) {
		tree.setCurrentNode(domNode);
		DomNode previous = tree.previousLogicalNode();
		int index = 0;
		int treeIndex = 0;
		if (previous != null) {
			Location previousLocation = previous.asLocation();
			treeIndex = previousLocation.getTreeIndex() + 1;
			index = previousLocation.getIndex()
					+ (previous.isText() ? previous.textContent().length() : 0);
		}
		return new Location(treeIndex, index, false, domNode, this);
	}

	@Override
	public Range asRange(DomNode domNode) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException(
				"Unimplemented method 'asRange'");
	}

	@Override
	public Range getDocumentRange() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException(
				"Unimplemented method 'getDocumentRange'");
	}
}
