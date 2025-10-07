package cc.alcina.framework.common.client.traversal.layer.diff;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.dom.DomDocument;
import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.DomNodeBuilder;
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
	- merged text nodes must preserve not just ancestor structure but anccestor structure relative to each other - so:
	  p.a1 .. #text[..].a2 p.a3 .. #text[..].a3 must preserve the p/#text/p/#text stucture
	- also, content in structured cells (grid, flex, table, list) can't match content in different columns (TODO)
	- ditto structured cells can't be inserted/deleted - if they would be, invalidate all matches for the row 
	  [note that rows definitely *can* be inserted/deleted  (TODO)
	- sequential counters (footnotes, list items) must be noramlised for left + right docs (1, 2, 3) to #footnote say - and then 
	  denormalised post diff (TODO)
	- where possible, swap matches so they're contiguous within grid levels (TODO)

  - more generally, how do you diff a tree? this answer implementation is "diff the leaves, and build the 
    merged structure from the matching leaves". Intuitively, this seems better than top-down - but why? 
 
  - note that the left of the diff must be the only input (if any) to contain diff markers - this allows construction of 
    multi-diffs

 * 
 * </pre></code>
 */
public class MeasureDiff {
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

	public static class Peer
			implements TraversalContext, TraversalContext.ShortTraversal {
		static final String TAG_DIFF = "diff";

		MeasureDiff measureDiff;

		public void stripRightInserts(DomNode node) {
			node.stream().filter(this::isDiff)
					.filter(n -> this.getDiffType(n) == DiffType.RIGHT_INSERT)
					.toList().forEach(DomNode::removeFromParent);
		}

		public boolean isDebug() {
			return false;
		}

		Stream<Measure> createMergeMeasures(DomNode node) {
			Stream<Measure> nodeStream = Stream
					.of(Measure.fromNode(node, MergeInputNode.NodeToken.TYPE));
			if (!node.isText()) {
				return nodeStream;
			} else {
				String textContent = node.textContent();
				/*
				 * Preserve standalone whiespace nodes
				 */
				if (textContent.matches("\\s+")) {
					return Stream.of(
							Measure.fromNode(node, MergeInputNode.Word.TYPE));
				}
				Stream<Measure> wordStream = TextUtils
						.match(textContent, "\\S+").stream()
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
		protected boolean isLeaf(MergeInputNode mergeInputNode) {
			Measure measure = mergeInputNode.get();
			DomNode containingNode = measure.containingNode();
			return measure.token == MergeInputNode.Word.TYPE
					|| containingNode.nameIs("img");
		}

		boolean isDiff(DomNode domNode) {
			return domNode != null && domNode.tagIs(TAG_DIFF);
		}

		DomNode createDiff(DomNode domNode, DiffType diffType) {
			String marker = diffType == DiffType.LEFT_INSERT
					? measureDiff.attributes.leftChangeMarker
					: measureDiff.attributes.rightChangeMarker;
			DomNodeBuilder builder = createMarkerBuilder(domNode, diffType,
					marker);
			return builder.append();
		}

		DomNodeBuilder createMarkerBuilder(DomNode domNode, DiffType diffType,
				String marker) {
			DomNodeBuilder builder = domNode.builder().tag(TAG_DIFF)
					.attr("type", Ax.cssify(diffType));
			builder.attr("marker", marker);
			return builder;
		}

		DiffType getDiffType(DomNode node) {
			String type = node.attr("type");
			return DiffType.ofCssified(type);
		}

		public DiffType getDiffTypeTree(DomNode node) {
			List<DiffType> types = node.stream().filter(this::isDiff)
					.map(this::getDiffType).distinct().toList();
			if (types.size() == 1) {
				return types.getFirst();
			} else {
				return DiffType.MIXED;
			}
		}

		public String getFirstChangeMarker(DomNode domNode) {
			return domNode.stream().filter(this::isDiff)
					.map(n -> n.attr("marker")).findFirst().get();
		}

		/*
		 * 
		 */
		public void setDiffTypeTree(DomNode node, DiffType diffType,
				String changeMarker) {
			Preconditions.checkArgument(node.stream().noneMatch(this::isDiff));
			node.stream().filter(DomNode::isText).toList().forEach(text -> {
				DomNodeBuilder builder = createMarkerBuilder(text, diffType,
						changeMarker);
				builder.wrap();
			});
		}

		public boolean isStructuralMatch(DomNode leftDomNode,
				DomNode rightDomNode) {
			if (leftDomNode.tagIsOneOf("td", "tr", "table", "tbody")) {
				/*
				 * ignore style changes etc - structure wins
				 */
				return Objects.equals(leftDomNode.name(), rightDomNode.name());
			} else {
				return leftDomNode.shallowEquals(rightDomNode);
			}
		}
	}

	public static class Attributes {
		DomNode left;

		String leftChangeMarker;

		DomNode right;

		String rightChangeMarker;

		boolean logStats;

		Peer peer = new Peer();

		Attributes() {
		}

		public Attributes withLeftChangeMarker(String leftChangeMarker) {
			this.leftChangeMarker = leftChangeMarker;
			return this;
		}

		public Attributes withRightChangeMarker(String rightChangeMarker) {
			this.rightChangeMarker = rightChangeMarker;
			return this;
		}

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

		public Attributes withPeer(Peer peer) {
			this.peer = peer;
			return this;
		}

		public MeasureDiff construct() {
			return new MeasureDiff(this);
		}
	}

	public static Attributes attributes() {
		return new Attributes();
	}

	Attributes attributes;

	Peer peer;

	MeasureDiff(Attributes attributes) {
		this.attributes = attributes;
	}

	public Result diff(TreeProcess.Node parentNode) {
		this.peer = attributes.peer;
		peer.measureDiff = this;
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
}
