package cc.alcina.framework.common.client.traversal.layer.diff;

import java.util.stream.Stream;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.Location.Range;
import cc.alcina.framework.common.client.dom.Measure;
import cc.alcina.framework.common.client.process.TreeProcess;
import cc.alcina.framework.common.client.traversal.SelectionTraversal;
import cc.alcina.framework.common.client.traversal.TraversalContext;
import cc.alcina.framework.common.client.util.TextUtils;
import cc.alcina.framework.servlet.job.JobContext;

/**
 * <code><pre>
 
 Algorithm sketch:

 - construct two tree representations (MergeNode trees) of *words* (words include [image] markers for html 
   - basically we're talking minimal inline spans) in the left + right docs
 - word parents will be text nodes (or the image parent), the tree representations include 
   MergeNode instances for all dom nodes in the left + right dom trees
 - diff the words
 - create match relations between the MergeNode instances where the words match
 - for all MergeNodes with matching words, match ancestors if the ancestor sequences match exactly
 - force a match relation between the roots
 - bi-traverse the mergenode trees, generating a result tree
   - basically, traverse left until match, generating [remove].
   - then traverse right until match, generating [add]
   - then traverse match(es), generating [unchanged]
  - wrinkles:
    - filters - omit (for chain computation + match) diff markers + diff-removed
 
 * 
 * </pre></code>
 */
public class MeasureDiff {
	Attributes attributes;

	Peer peer;

	public static class Result {
		public DomNode union;
	}

	public Result diff() {
		this.peer = new Peer();
		SelectionTraversal traversal = new SelectionTraversal(peer);
		TreeProcess.Node parentNode = JobContext.getSelectedProcessNode();
		traversal.select(new RootLayer.RootSelection(parentNode, this));
		traversal.layers().setRoot(new RootLayer());
		traversal.traverse();
		traversal.logTraversalStats();
		MergedOutput.SelectionImpl outputSelection = traversal.selections()
				.getSingleSelection(MergedOutput.SelectionImpl.class);
		Result result = new Result();
		result.union = outputSelection.get();
		return result;
	}

	class Peer implements TraversalContext {
		Stream<Measure> createMergeMeasures(DomNode node) {
			Measure.Token token = Measure.Token.Generic.TYPE;
			if (!node.isText()) {
				return Stream.of(Measure.fromNode(node, token));
			}
			return TextUtils.match(node.textContent(), "\\S+").stream()
					.map(wordOffsets -> {
						Range range = node.asRange();
						range = range.truncateRelative(wordOffsets.i1,
								wordOffsets.i2);
						return Measure.fromRange(range, token);
					});
		}

		boolean isLeaf(MergeInputNode mergeInputNode) {
			Measure measure = mergeInputNode.get();
			DomNode containingNode = measure.containingNode();
			return containingNode.isText() || containingNode.nameIs("img");
		}
	}

	public static class Attributes {
		Attributes() {
		}

		DomNode left;

		DomNode right;

		public Attributes withLeft(DomNode left) {
			this.left = left;
			return this;
		}

		public Attributes withRight(DomNode right) {
			this.right = right;
			return this;
		}

		public MeasureDiff construct() {
			return new MeasureDiff(this);
		}
	}

	MeasureDiff(Attributes attributes) {
		this.attributes = attributes;
	}

	public static Attributes attributes() {
		return new Attributes();
	}
}
