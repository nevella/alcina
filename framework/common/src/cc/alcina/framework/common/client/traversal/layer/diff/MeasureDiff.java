package cc.alcina.framework.common.client.traversal.layer.diff;

import java.util.stream.Stream;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.Location.Range;
import cc.alcina.framework.common.client.dom.Measure;
import cc.alcina.framework.common.client.process.TreeProcess;
import cc.alcina.framework.common.client.traversal.SelectionTraversal;
import cc.alcina.framework.common.client.traversal.TraversalContext;
import cc.alcina.framework.common.client.util.TextUtils;

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

  - more generally, how do you diff a tree? this answer implementation is "diff the leaves, and build the 
    merged structure from the matching leaves". Intuitively, this seems better than top-down - but why? 
 
  - note that the left of the diff must be the only input (if any) to contain diff markers - this allows construction of 
    multi-diffs

 * 
 * </pre></code>
 */
public class MeasureDiff {
	Attributes attributes;

	Peer peer;

	public static class Result {
		public DomNode union;
	}

	public Result diff(TreeProcess.Node parentNode) {
		this.peer = new Peer();
		SelectionTraversal traversal = new SelectionTraversal(peer);
		traversal.select(new RootLayer.RootSelection(parentNode, this));
		traversal.layers().setRoot(new RootLayer());
		traversal.traverse();
		traversal.logTraversalStats();
		MergedOutput.SelectionImpl outputSelection = traversal.selections()
				.getSingleSelection(MergedOutput.SelectionImpl.class);
		Result result = new Result();
		result.union = outputSelection.get().doc.getDocumentElementNode();
		return result;
	}

	class Peer implements TraversalContext {
		Stream<Measure> createMergeMeasures(DomNode node) {
			Stream<Measure> nodeStream = Stream
					.of(Measure.fromNode(node, MergeInputNode.NodeToken.TYPE));
			if (!node.isText()) {
				return nodeStream;
			} else {
				Stream<Measure> wordStream = TextUtils
						.match(node.textContent(), "\\S+").stream()
						.map(wordOffsets -> {
							Range range = node.asRange();
							range = range.truncateRelative(wordOffsets.i1,
									wordOffsets.i2);
							return Measure.fromRange(range,
									MergeInputNode.Word.TYPE);
						});
				return Stream.concat(nodeStream, wordStream);
			}
		}

		boolean isLeaf(MergeInputNode mergeInputNode) {
			Measure measure = mergeInputNode.get();
			DomNode containingNode = measure.containingNode();
			return measure.token == MergeInputNode.Word.TYPE
					|| containingNode.nameIs("img");
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
