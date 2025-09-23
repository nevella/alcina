package cc.alcina.framework.common.client.traversal.layer.diff;

import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.common.client.dom.DomDocument;
import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.Measure;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.FilteringIterator;
import cc.alcina.framework.common.client.traversal.AbstractSelection;
import cc.alcina.framework.common.client.traversal.AbstractSelection.AllowsNullValue;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.traversal.layer.diff.MergeInputNode.DiffType;
import cc.alcina.framework.common.client.traversal.layer.diff.MergeInputNode.InputBranch;
import cc.alcina.framework.common.client.traversal.layer.diff.MergeInputNode.Left;
import cc.alcina.framework.common.client.traversal.layer.diff.MergeInputNode.Right;
import cc.alcina.framework.common.client.util.Ax;

/**
 * Models an output node of the diff process
 */
class MergeOutputNode extends AbstractSelection<Void>
		implements AbstractSelection.AllowsNullValue {
	MergeOutputNode(Selection parent, Void value) {
		super(parent, value);
	}

	MergeOutputNode parent;

	List<MergeOutputNode> children = new ArrayList<>();

	DomDocument doc;

	Left left;

	Right right;

	DomNode domNode;

	public GenerateOutputNodes layer;

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
			if (right == null) {
				return DiffType.LEFT_INSERT;
			} else {
				return DiffType.UNCHANGED;
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
			branch = toRoot.reversed();
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
			if (shallowClone.isText()) {
				shallowClone.setText("");
			}
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
		return domNode != null && domNode.tagIs("diff");
	}

	MergeOutputNode ensureDiffContainer() {
		switch (computeDiffType()) {
		case UNCHANGED:
			return this;
		case LEFT_INSERT:
		case RIGHT_INSERT:
			break;
		case NONE:
		default:
			throw new UnsupportedOperationException();
		}
		MergeOutputNode last = Ax.last(parent.children);
		switch (computeDiffType()) {
		case LEFT_INSERT:
			if (last != null && last.isDiff()
					&& last.computeDiffType() == DiffType.LEFT_INSERT) {
				return last;
			} else {
				break;
			}
		case RIGHT_INSERT:
			if (last != null && last.isDiff()
					&& last.computeDiffType() == DiffType.RIGHT_INSERT) {
				return last;
			} else {
				break;
			}
		}
		MergeOutputNode container = new MergeOutputNode(firstInput(), null);
		container.domNode = parent.domNode.builder().tag("diff")
				.attr("type", Ax.cssify(computeDiffType())).append();
		return parent.appendChild(firstInput(), container);
	}

	void appendContents() {
		Measure firstMeasure = firstMeasure();
		if (firstMeasure.containingNode().isText()) {
			String text = domNode.textContent();
			if (!firstMeasure.start.isAtNodeStart()) {
				text += " ";
			}
			text += firstMeasure.text();
			domNode.setText(text);
		}
	}
}
