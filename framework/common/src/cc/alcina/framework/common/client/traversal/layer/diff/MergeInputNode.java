package cc.alcina.framework.common.client.traversal.layer.diff;

import cc.alcina.framework.common.client.dom.Measure;
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
	}

	static class Right extends MergeInputNode {
		Right(Selection parentSelection, Measure measure) {
			super(parentSelection, measure);
		}
	}
}