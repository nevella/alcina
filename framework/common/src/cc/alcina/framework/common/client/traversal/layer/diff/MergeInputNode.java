package cc.alcina.framework.common.client.traversal.layer.diff;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.Measure;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.FilteringIterator;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.traversal.layer.MeasureSelection;
import cc.alcina.framework.common.client.traversal.layer.diff.RootLayer.RootSelection;
import cc.alcina.framework.common.client.util.Ax;

abstract class MergeInputNode extends MeasureSelection {
	static class NodeToken implements Measure.Token {
		static final NodeToken TYPE = new NodeToken();

		private NodeToken() {
		}
	}

	static class Word implements Measure.Token {
		static final Word TYPE = new Word();

		private Word() {
		}
	}

	private RootSelection rootSelection;

	static MergeInputNode create(Selection parent, Measure measure,
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
		return rootSelection.get().peer.isLeaf(this);
	}

	MergeInputNode(Selection parentSelection, Measure measure) {
		super(parentSelection, measure);
		rootSelection = parentSelection().ancestor(RootSelection.class);
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
		FilteringIterator<MergeInputNode> itr;

		List<MergeInputNode> branch;

		InputBranch() {
			List<MergeInputNode> toRoot = new ArrayList<>();
			MergeInputNode cursor = MergeInputNode.this;
			for (;;) {
				toRoot.add(cursor);
				Selection<?> parentSelection = cursor.parentSelection();
				if (parentSelection instanceof MergeInputNode) {
					cursor = (MergeInputNode) parentSelection;
				} else {
					break;
				}
			}
			branch = toRoot.reversed();
			itr = FilteringIterator.wrap(branch);
		}

		@Override
		public String toString() {
			return Ax.newlineJoin(branch);
		}
	}

	enum DiffType {
		LEFT_INSERT, RIGHT_INSERT, UNCHANGED, NONE;

		String pretty() {
			switch (this) {
			case LEFT_INSERT:
				return "[L]";
			case RIGHT_INSERT:
				return "[L]";
			case UNCHANGED:
				return "[LR]";
			case NONE:
				return "[--]";
			default:
				throw new UnsupportedOperationException();
			}
		}
	}

	abstract DiffType getDiffType();
}