package cc.alcina.framework.common.client.traversal.layer.diff;

import java.util.stream.Stream;

import cc.alcina.framework.common.client.dom.DomDocument;
import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.Location.Range;
import cc.alcina.framework.common.client.dom.Measure;
import cc.alcina.framework.common.client.process.TreeProcess;
import cc.alcina.framework.common.client.traversal.SelectionTraversal;
import cc.alcina.framework.common.client.traversal.TraversalContext;
import cc.alcina.framework.common.client.traversal.layer.diff.MergeInputNode.DiffType;
import cc.alcina.framework.common.client.util.Ax;
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

		public void applyMarkerCss() {
			DomDocument htmlDoc = DomDocument.basicGwtHtmlDoc();
			htmlDoc.html().appendStyleNode(
					"diff[type=left-insert]{background-color: lightpink;}");
			htmlDoc.html().appendStyleNode(
					"diff[type=right-insert]{background-color: lightgreen;}");
			htmlDoc.html().body().children.importFrom(union);
			union = htmlDoc;
		}
	}

	public Result diff(TreeProcess.Node parentNode) {
		this.peer = new Peer();
		SelectionTraversal traversal = new SelectionTraversal(peer);
		traversal.select(new RootLayer.RootSelection(parentNode, this));
		traversal.layers().setRoot(new RootLayer());
		traversal.traverse();
		if (attributes.logStats) {
			traversal.logTraversalStats();
		}
		MergedOutput.SelectionImpl outputSelection = traversal.selections()
				.getSingleSelection(MergedOutput.SelectionImpl.class);
		Result result = new Result();
		result.union = outputSelection.get().doc.getDocumentElementNode();
		return result;
	}

	public static class Peer implements TraversalContext {
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
				return wordStream;
			}
		}

		/*
		 * TODO - also br, hr (html), #comment, #processing-instruction (xml)
		 */
		boolean isLeaf(MergeInputNode mergeInputNode) {
			Measure measure = mergeInputNode.get();
			DomNode containingNode = measure.containingNode();
			return measure.token == MergeInputNode.Word.TYPE
					|| containingNode.nameIs("img");
		}

		static final String TAG_DIFF = "diff";

		boolean isDiff(DomNode domNode) {
			return domNode != null && domNode.tagIs(TAG_DIFF);
		}

		DomNode createDiff(DomNode domNode, DiffType diffType) {
			return domNode.builder().tag(TAG_DIFF)
					.attr("type", Ax.cssify(diffType)).append();
		}

		public void stripRightInserts(DomNode node) {
			node.stream().filter(this::isDiff)
					.filter(n -> this.getDiffType(n) == DiffType.RIGHT_INSERT)
					.toList().forEach(DomNode::removeFromParent);
		}

		DiffType getDiffType(DomNode node) {
			String type = node.attr("type");
			return DiffType.ofCssified(type);
		}

		public boolean isDebug() {
			return false;
		}
	}

	public static class Attributes {
		Attributes() {
		}

		DomNode left;

		DomNode right;

		boolean logStats;

		public Attributes withLeft(DomNode left) {
			this.left = left;
			return this;
		}

		public Attributes withRight(DomNode right) {
			this.right = right;
			return this;
		}

		public Attributes withLogStats(boolean logStats) {
			this.logStats = logStats;
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
