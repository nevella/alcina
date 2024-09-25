package cc.alcina.framework.servlet.component.traversal;

import java.util.stream.Stream;

import cc.alcina.framework.common.client.process.TreeProcess;
import cc.alcina.framework.common.client.process.TreeProcess.Node;
import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.traversal.Selection.WithRange;
import cc.alcina.framework.common.client.traversal.SelectionTraversal;
import cc.alcina.framework.common.client.traversal.layer.Measure;
import cc.alcina.framework.common.client.traversal.layer.MeasureSelection;
import cc.alcina.framework.common.client.traversal.layer.SelectionMarkup.Query;
import cc.alcina.framework.common.client.util.traversal.DepthFirstTraversal;

class RangeSelectionSequence {
	Selection.WithRange<?> input;

	Selection.WithRange<?> output;

	Selection highestAncestor;

	SelectionTraversal traversal;

	Query query;

	Selection.WithRange<?> from;

	RangeSelectionSequence(SelectionTraversal traversal, Query query) {
		this.traversal = traversal;
		this.query = query;
		Selection fromCursor = query.selection;
		while (!(fromCursor instanceof Selection.WithRange<?>)) {
			fromCursor = fromCursor.parentSelection();
		}
		from = (WithRange<?>) fromCursor;
		Selection.WithRange<?> cursor = from;
		if (isOutput(cursor)) {
			output = cursor;
			while (isOutput(cursor)) {
				cursor = (WithRange<?>) cursor.parentSelection();
			}
			input = cursor;
		} else {
			input = cursor;
			TreeProcess.Node descentRoot = input.processNode();
			DepthFirstTraversal<TreeProcess.Node> descentTraversal = new DepthFirstTraversal<>(
					descentRoot, TreeProcess.Node::getChildren);
			Stream<Node> filtered = descentTraversal.stream().filter(n -> {
				Selection selection = (Selection) n.getValue();
				return selection instanceof Selection.Output;
			});
			output = filtered.findFirst()
					.map(n -> (Selection.WithRange) n.getValue()).orElse(null);
		}
		if (output != null) {
			output.provideRange();
		}
	}

	boolean isOutput(Selection cursor) {
		Layer layer = traversal.getLayer(cursor);
		return layer.layerContext(Layer.Output.class) != null;
	}

	Measure ioSelection() {
		Selection.WithRange<?> selection = query.input ? input : output;
		return selection != null && selection instanceof MeasureSelection
				? ((MeasureSelection) selection).get()
				: null;
	}

	public WithRange getRange(boolean input) {
		return input ? this.input : this.output;
	}
}