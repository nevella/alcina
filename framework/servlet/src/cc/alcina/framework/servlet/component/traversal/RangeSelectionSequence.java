package cc.alcina.framework.servlet.component.traversal;

import java.util.stream.Stream;

import cc.alcina.framework.common.client.dom.Measure;
import cc.alcina.framework.common.client.process.TreeProcess;
import cc.alcina.framework.common.client.process.TreeProcess.Node;
import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.traversal.Selection.WithRange;
import cc.alcina.framework.common.client.traversal.SelectionTraversal;
import cc.alcina.framework.common.client.traversal.layer.MeasureSelection;
import cc.alcina.framework.common.client.traversal.layer.SelectionMarkup.Query;
import cc.alcina.framework.common.client.util.traversal.DepthFirstTraversal;

/*
 * computes the input + output selections from a given selection
 */
class RangeSelectionSequence {
	Selection.WithRange<?> input;

	Selection.WithRange<?> output;

	Selection highestAncestor;

	SelectionTraversal traversal;

	Query query;

	Selection.WithRange<?> from;

	boolean fullDocument;

	RangeSelectionSequence(SelectionTraversal traversal, Query query) {
		this.traversal = traversal;
		this.query = query;
		Selection fromCursor = query.selection;
		// query.selection will be the whole (input/output) doc if not defined
		if (fromCursor == null) {
			WithRange inputSelection = traversal
					.getSelections(Selection.WithRange.class, true).stream()
					.findFirst().orElse(null);
			WithRange outputSelection = traversal
					.getSelections(Selection.WithRange.class, true).stream()
					.filter(this::isOutput).findFirst().orElse(null);
			fromCursor = query.input ? inputSelection : outputSelection;
			fullDocument = true;
		}
		if (fromCursor == null) {
			return;
		}
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
			if (output == null) {
				output = traversal
						.getSelections(Selection.WithRange.class, true).stream()
						.filter(this::isOutput).findFirst().orElse(null);
				fullDocument = true;
			}
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