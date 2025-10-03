package cc.alcina.framework.common.client.traversal.layer.diff;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.Measure;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.FilteringIterator;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.traversal.layer.MeasureSelection;
import cc.alcina.framework.common.client.traversal.layer.diff.MeasureDiff.Peer;
import cc.alcina.framework.common.client.traversal.layer.diff.RootLayer.RootSelection;
import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.HasEquivalence;
import cc.alcina.framework.common.client.util.HasEquivalence.HasEquivalenceAdapter;

public abstract class MergeInputNode extends MeasureSelection {
	interface RelationType extends Selection.Relation.Type {
		interface WordEquivalent extends RelationType {
		}

		interface StructureInequivalent extends RelationType {
		}
	}

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

	MergeInputNode priorLeaf;

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

	void markWordEquivalentTo(MergeInputNode rightNode) {
		getRelations().addRelation(RelationType.WordEquivalent.class,
				rightNode);
	}

	void markStructureInequivalentTo(MergeInputNode rightNode) {
		getRelations().addRelation(RelationType.StructureInequivalent.class,
				rightNode);
	}

	boolean hasEquivalent() {
		return getRelations().has(RelationType.WordEquivalent.class)
				&& !getRelations()
						.has(RelationType.StructureInequivalent.class);
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
			Collections.reverse(toRoot);
			branch = toRoot;
			itr = FilteringIterator.wrap(branch);
		}

		@Override
		public String toString() {
			return Ax.newlineJoin(branch);
		}

		boolean structureEquivalentTo(InputBranch other) {
			List<MergeInputNode> filtered = computeStructuralComparisonNodes();
			List<MergeInputNode> otherFiltered = other
					.computeStructuralComparisonNodes();
			return HasEquivalence.areEquivalent(InputShallowEquivalence.class,
					filtered, otherFiltered);
		}

		private List<MergeInputNode> computeStructuralComparisonNodes() {
			Peer peer = peer();
			return branch.stream()
					.filter(mergeNode -> !mergeNode.domNode().isText()
							&& !peer.isDiff(mergeNode.domNode()))
					.toList();
		}
	}

	@Reflected
	static class InputShallowEquivalence extends
			HasEquivalenceAdapter<MergeInputNode, InputShallowEquivalence> {
		@Override
		public int equivalenceHash() {
			return getReferent().domNode().name().hashCode();
		}

		@Override
		public boolean equivalentTo(InputShallowEquivalence other) {
			Peer peer = getReferent().rootSelection.get().peer;
			return peer.isStructuralMatch(getReferent().containingNode(),
					other.getReferent().containingNode());
		}
	}

	MeasureDiff.Peer peer() {
		return ancestor(RootSelection.class).get().peer;
	}

	@Override
	public String toString() {
		return Ax.format("%s - %s", getDiffType().pretty(), super.toString());
	}

	enum DiffType {
		LEFT_INSERT, RIGHT_INSERT, UNCHANGED, NONE;

		String pretty() {
			switch (this) {
			case LEFT_INSERT:
				return "[L]";
			case RIGHT_INSERT:
				return "[R]";
			case UNCHANGED:
				return "[LR]";
			case NONE:
				return "[--]";
			default:
				throw new UnsupportedOperationException();
			}
		}

		String cssified() {
			ensureLookups();
			return typeCssifiedName.get(this);
		}

		static Map<DiffType, String> typeCssifiedName;

		static Map<String, DiffType> cssifiedNameType;

		/*
		 * written as it is - particularly final assignment order - to avoid
		 * locking
		 */
		static void ensureLookups() {
			if (typeCssifiedName == null) {
				Map<String, DiffType> cssifiedNameType = new LinkedHashMap<>();
				Map<DiffType, String> typeCssifiedName = new LinkedHashMap<>();
				cssifiedNameType = Arrays.stream(DiffType.values())
						.collect(AlcinaCollectors.toKeyMap(Ax::cssify));
				typeCssifiedName = Arrays.stream(DiffType.values())
						.collect(AlcinaCollectors.toValueMap(Ax::cssify));
				DiffType.cssifiedNameType = cssifiedNameType;
				DiffType.typeCssifiedName = typeCssifiedName;
			}
		}

		static DiffType ofCssified(String type) {
			ensureLookups();
			return cssifiedNameType.get(type);
		}
	}

	abstract DiffType getDiffType();
}