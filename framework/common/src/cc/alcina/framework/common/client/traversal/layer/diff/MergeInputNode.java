package cc.alcina.framework.common.client.traversal.layer.diff;

import java.util.Objects;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.Measure;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.FilteringIterator;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.traversal.layer.MeasureSelection;
import cc.alcina.framework.common.client.traversal.layer.diff.RootLayer.RootSelection;

abstract class MergeInputNode extends MeasureSelection {
	static MergeInputNode create(RootSelection parent, Measure measure,
			boolean left) {
		if (left) {
			return new Left(parent, measure);
		} else {
			return new Right(parent, measure);
		}
	}

	/**
	 * returns true if the corresponding two dom nodes are exactly shallow
	 * equals
	 */
	public boolean shallowEquals(MergeInputNode other) {
		return containingNode().shallowEquals(other.containingNode());
	}

	boolean isLeaf() {
		return ((RootSelection) parentSelection()).get().peer.isLeaf(this);
	}

	MergeInputNode(Selection parentSelection, Measure measure) {
		super(parentSelection, measure);
	}

	static class Left extends MergeInputNode {
		Left(Selection parentSelection, Measure measure) {
			super(parentSelection, measure);
		}

		@Override
		DiffType getDiffType() {
			return hasEquivalent() ? DiffType.UNCHANGED : DiffType.LEFT_INSERT;
		}
	}

	static class Right extends MergeInputNode {
		Right(Selection parentSelection, Measure measure) {
			super(parentSelection, measure);
		}

		@Override
		DiffType getDiffType() {
			return hasEquivalent() ? DiffType.UNCHANGED : DiffType.RIGHT_INSERT;
		}
	}

	boolean contentEquals(MergeInputNode other) {
		DomNode nThis = containingNode();
		DomNode nOther = other.containingNode();
		if (!Objects.equals(nThis.name(), nOther.name())) {
			return false;
		}
		return Objects.equals(contentString(), other.contentString());
	}

	String contentString;

	int contentHashCode() {
		return contentString().hashCode();
	}

	String contentString() {
		if (contentString == null) {
			contentString = get().containingNode().isText() ? get().ntc()
					: get().containingNode().name();
		}
		return contentString;
	}

	void markEquivalentTo(MergeInputNode rightNode) {
		getRelations().addRelation(Relations.Type.Equivalent.class, rightNode);
	}

	boolean hasEquivalent() {
		return getRelations().has(Relations.Type.Equivalent.class);
	}

	InputBranch getBranch() {
		return new InputBranch();
	}

	class InputBranch {
		FilteringIterator<MergeInputNode> nodes;
	}

	enum DiffType {
		LEFT_INSERT, RIGHT_INSERT, UNCHANGED
	}

	abstract DiffType getDiffType();
}