package cc.alcina.framework.common.client.traversal.layer.diff;

import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.common.client.dom.DomDocument;
import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.FilteringIterator;
import cc.alcina.framework.common.client.traversal.layer.diff.MergeInputNode.InputBranch;
import cc.alcina.framework.common.client.traversal.layer.diff.MergeInputNode.Left;
import cc.alcina.framework.common.client.traversal.layer.diff.MergeInputNode.Right;

/**
 * Models an output node of the diff process
 */
class MergeOutputNode {
	MergeOutputNode parent;

	List<MergeOutputNode> children = new ArrayList<>();

	DomDocument doc;

	Left left;

	Right right;

	DomNode domNode;

	OutputBranch getBranch() {
		return new OutputBranch();
	}

	class OutputBranch {
		FilteringIterator<MergeOutputNode> nodes;
	}

	MergeOutputNode ensureOutputParent(MergeInputNode inputNode) {
		InputBranch inputBranch = inputNode.getBranch();
		OutputBranch outputBranch = getBranch();
		boolean matched = true;
		MergeOutputNode outputCursor = outputBranch.nodes.next();
		for (;;) {
			MergeInputNode inputCursor = inputBranch.nodes.next();
			if (inputCursor.isLeaf()) {
				return outputCursor;
			}
			if (matched) {
				// TODO.- skip wrappers if input is a diff wrapper
				if (outputBranch.nodes.hasNext()) {
					MergeOutputNode outputNode = outputBranch.nodes.next();
					if (inputCursor.containingNode()
							.shallowEquals(outputNode.domNode)) {
						outputCursor = outputNode;
						continue;
					}
				}
				matched = false;
			}
			outputCursor = outputCursor.appendShallow(inputNode);
		}
	}

	MergeOutputNode appendShallow(MergeInputNode inputNode) {
		MergeOutputNode child = null;
		if (domNode == null) {
			String rootMarkup = inputNode.domNode().cloneNode(false)
					.fullToString();
			this.doc = DomDocument.from(rootMarkup, true);
			domNode = doc;
			child = new MergeOutputNode();
			child.domNode = doc.getDocumentElementNode();
		} else {
			DomNode shallowClone = domNode.children
					.importFrom(inputNode.domNode(), false);
			child = new MergeOutputNode();
			child.domNode = shallowClone;
		}
		child.parent = this;
		parent.children.add(child);
		return child;
	}

	void ensureDiffContainer(MergeInputNode inputNode) {
		/*
		 * TODO - peer/didrndl the heck outta this
		 */
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException(
				"Unimplemented method 'ensureDiffContainer'");
	}

	void appendContents(MergeInputNode inputNode) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException(
				"Unimplemented method 'appendContents'");
	}
}
