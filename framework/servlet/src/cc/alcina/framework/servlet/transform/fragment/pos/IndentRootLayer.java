package cc.alcina.framework.servlet.transform.fragment.pos;

import java.util.List;

import com.google.common.base.Preconditions;

import cc.alcina.framework.servlet.transform.fragment.StyledDocument.Styles;
import cc.alcina.framework.servlet.transform.fragment.StyledMeasureSelection;
import cc.alcina.framework.servlet.transform.fragment.StyledNode;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.tokens.LayoutTokens;
import cc.alcina.framework.common.client.dom.Measure;
import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.traversal.layer.MeasureSelection.IgnoreOverlaps;
import cc.alcina.framework.common.client.traversal.layer.MeasureSelection.Intermediate;
import cc.alcina.framework.common.client.util.Ax;

/**
 * Marks indent roots, which are used for relative indent calculations (such as
 * table cells). All indent/list computations for a given measure are made
 * relative to the nearest indent root container
 */
class IndentRootLayer extends Layer<StyledNode> {
	IndentRoot documentRoot;

	int counter = 0;

	@Override
	public void process(StyledNode node) throws Exception {
		Styles styles = node.styles();
		boolean indentRoot = styles.isIndentRoot();
		indentRoot |= styles.isFootnoteReferenceTarget();
		if (indentRoot) {
			Measure indentMeasure = Measure.fromNode(node.get(),
					LayoutTokens.IndentRoot.TYPE);
			IndentRoot root = new IndentRoot(node, indentMeasure, counter++);
			select(root);
			if (documentRoot == null) {
				documentRoot = root;
				root.documentRoot = true;
			}
		}
	}

	static class IndentRoot extends StyledMeasureSelection
			implements IgnoreOverlaps, Intermediate {
		int idx;

		IndentRoot(Selection parent, Measure measure, int idx) {
			super(parent, measure);
			this.idx = idx;
		}

		boolean documentRoot;

		List<Float> lefts;

		boolean addedSyntheticIndent;

		void addSyntheticIndent() {
			Preconditions.checkState(!addedSyntheticIndent);
			addedSyntheticIndent = true;
			Float last = Ax.last(lefts);
			lefts.add(last + 10.0F);
		}
	}
}