package cc.alcina.framework.common.client.traversal.layer.diff;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.dom.DomDocument;
import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.DomNodeType;
import cc.alcina.framework.common.client.dom.Measure;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.FilteringIterator;
import cc.alcina.framework.common.client.traversal.AbstractSelection;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.traversal.layer.diff.MeasureDiff.Peer;
import cc.alcina.framework.common.client.traversal.layer.diff.MergeInputNode.DiffType;
import cc.alcina.framework.common.client.traversal.layer.diff.MergeInputNode.InputBranch;
import cc.alcina.framework.common.client.traversal.layer.diff.MergeInputNode.Left;
import cc.alcina.framework.common.client.traversal.layer.diff.MergeInputNode.Right;
import cc.alcina.framework.common.client.util.Ax;

/**
 * Models an output node of the diff process
 * 
 * TODO - should be type DomNode (in the output doc)
 */
class MergeOutputNode extends AbstractSelection<Void>
		implements AbstractSelection.AllowsNullValue {
	static final String TAG_DIFF = "diff";

	MergeOutputNode(Selection parent, Void value) {
		super(parent, value);
	}

	MergeOutputNode parent;

	List<MergeOutputNode> children = new ArrayList<>();

	DomDocument doc;

	Left left;

	Right right;

	DomNode domNode;

	GenerateOutputNodes layer;

	OutputBranch getBranch() {
		return new OutputBranch();
	}

	@Override
	public String toString() {
		if (domNode == null) {
			return "[domNode not set]";
		} else {
			String text = firstMeasure() != null
					? Ax.trim(firstMeasure().text(), 20)
					: "[no text]";
			String range = firstMeasure() != null
					? firstMeasure().toIntPair().toDashString()
					: "[]";
			return Ax.format("%s :: %s :: [%s] :: %s", range, domNode.name(),
					computeDiffType().pretty(), text);
		}
	}

	DiffType computeDiffType() {
		if (left == null) {
			if (right == null) {
				return DiffType.NONE;
			} else {
				return DiffType.RIGHT_INSERT;
			}
		} else {
			if (left.hasEquivalent() || right != null) {
				return DiffType.UNCHANGED;
			} else {
				return DiffType.LEFT_INSERT;
			}
		}
	}

	Measure firstMeasure() {
		return firstInput() == null ? null : firstInput().get();
	}

	MergeInputNode firstInput() {
		return left != null ? left : right != null ? right : null;
	}

	class OutputBranch {
		FilteringIterator<MergeOutputNode> itr;

		List<MergeOutputNode> branch;

		OutputBranch() {
			List<MergeOutputNode> toRoot = new ArrayList<>();
			MergeOutputNode cursor = MergeOutputNode.this;
			while (cursor != null) {
				toRoot.add(cursor);
				cursor = cursor.parent;
			}
			Collections.reverse(toRoot);
			branch = toRoot;
			itr = FilteringIterator.wrap(branch);
		}

		@Override
		public String toString() {
			return Ax.newlineJoin(branch);
		}
	}

	MergeOutputNode ensureOutputParent(MergeInputNode inputNode) {
		InputBranch inputBranch = inputNode.getBranch();
		OutputBranch outputBranch = getBranch();
		boolean matched = true;
		MergeOutputNode outputCursor = outputBranch.itr.next();
		for (;;) {
			MergeInputNode inputCursor = inputBranch.itr.next();
			if (inputCursor.isLeaf()) {
				return outputCursor;
			}
			if (matched) {
				// TODO.- skip wrappers if input is a diff wrapper
				if (outputBranch.itr.hasNext()) {
					MergeOutputNode outputNode = outputBranch.itr.next();
					if (inputCursor.containingNode()
							.shallowEquals(outputNode.domNode)) {
						outputNode.associateInput(inputNode);
						outputCursor = outputNode;
						continue;
					}
				}
				matched = false;
			}
			outputCursor = outputCursor.appendShallow(inputCursor);
		}
	}

	void associateInput(MergeInputNode inputNode) {
		if (inputNode instanceof Left) {
			left = (Left) inputNode;
		} else {
			right = (Right) inputNode;
		}
	}

	MergeOutputNode rootOutputNode() {
		MergeOutputNode cursor = this;
		while (cursor.parent != null) {
			cursor = cursor.parent;
		}
		return cursor;
	}

	MergeOutputNode appendShallow(MergeInputNode inputNode) {
		MergeOutputNode child = null;
		if (domNode == null) {
			String rootMarkup = inputNode.domNode().cloneNode(false)
					.fullToString();
			this.doc = DomDocument.from(rootMarkup, true);
			domNode = doc;
			child = new MergeOutputNode(inputNode, null);
			child.domNode = doc.getDocumentElementNode();
		} else {
			DomNode shallowClone = domNode.children
					.importFrom(inputNode.domNode(), false);
			child = new MergeOutputNode(inputNode, null);
			child.domNode = shallowClone;
		}
		return appendChild(inputNode, child);
	}

	MergeOutputNode appendChild(MergeInputNode inputNode,
			MergeOutputNode child) {
		child.associateInput(inputNode);
		child.parent = this;
		children.add(child);
		rootOutputNode().layer.select(child);
		return child;
	}

	boolean isDiff() {
		return peer().isDiff(domNode);
	}

	private Peer peer() {
		return rootOutputNode().layer.peer;
	}

	MergeOutputNode ensureDiffContainer(MergeInputNode input) {
		DiffType diffType = input.getDiffType();
		switch (diffType) {
		case UNCHANGED:
		case LEFT_INSERT:
		case RIGHT_INSERT:
			break;
		case NONE:
		default:
			throw new UnsupportedOperationException();
		}
		if (diffType == DiffType.UNCHANGED
				&& input.containingNode().isElement()) {
			return this;
		}
		/*
		 * if the current append container can accept the input, use it,
		 * otherwise ensure a new one
		 */
		MergeOutputNode last = Ax.last(children);
		DomNodeType inputType = input.containingNode().getDomNodeType();
		switch (diffType) {
		case LEFT_INSERT:
			if (last != null && last.isDiff()
					&& last.computeDiffType() == DiffType.LEFT_INSERT
					&& last.domNode.children.firstNode()
							.getDomNodeType() == inputType) {
				return last;
			} else {
				break;
			}
		case RIGHT_INSERT:
			if (last != null && last.isDiff()
					&& last.computeDiffType() == DiffType.RIGHT_INSERT
					&& last.domNode.children.firstNode()
							.getDomNodeType() == inputType) {
				return last;
			} else {
				break;
			}
		case UNCHANGED:
			if (last != null && last.domNode.getDomNodeType() == inputType) {
				return last;
			} else {
				break;
			}
		}
		MergeOutputNode container = new MergeOutputNode(input, null);
		switch (diffType) {
		case UNCHANGED:
			container.domNode = domNode.builder().text("").append();
			break;
		case LEFT_INSERT:
		case RIGHT_INSERT:
			container.domNode = peer().createDiff(domNode, diffType);
			break;
		}
		return appendChild(input, container);
	}

	void appendContents(MergeInputNode inputNode) {
		MergeOutputNode container = null;
		if (inputNode.containingNode().isText()) {
			String text = domNode.textContent();
			if (!inputNode.get().start.isAtNodeStart()) {
				text += " ";
			}
			text += inputNode.text();
			domNode.setText(text);
			container = new MergeOutputNode(this, null);
			container.domNode = domNode;
		} else {
			Preconditions.checkArgument(domNode.children.nodes().stream()
					.noneMatch(DomNode::isText));
			DomNode imported = domNode.children.importFrom(inputNode.domNode(),
					false);
			container = new MergeOutputNode(this, null);
			container.domNode = imported;
		}
		appendChild(inputNode, container);
	}
}
