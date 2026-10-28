package cc.alcina.framework.servlet.transform.fragment;

import java.util.Set;
import java.util.function.Predicate;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.util.AlcinaCollections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.HtmlConstants;
import cc.alcina.framework.common.client.util.IntPair;
import cc.alcina.framework.entity.Configuration;

/**
 * Traverses the initial input doc, outputs the tree as StyledNode and
 * IgnoredNodeSelection selections
 */
public abstract class StyledNodeLayer<S extends Selection & StyledDocument.AccessorSelection>
		extends Layer<S> {
	int counter;

	Predicate<DomNode> allowNode = n -> true;

	private StyledDocument styledDocument;

	static Configuration.Key measureFilterKey = Configuration
			.key(StyledNodeLayer.class, "measureFilter");

	IntPair measureFilter = null;

	protected StyledNodeLayer() {
		String measureFilterValue = measureFilterKey.get();
		if (Ax.notBlank(measureFilterValue)) {
			measureFilter = IntPair.parseIntPair(measureFilterValue);
		}
	}

	public static class Peer {
		public interface Has {
			Peer getStyledNodeLayerPeer();
		}

		public void markIgnoreTreeNodes(StyledDocument styledDocument,
				Set<DomNode> ignoreTreeNodes) {
		}
	}

	public static class DocumentTransformerLayer
			extends StyledNodeLayer<DocumentTransformerSelection> {
	}

	/*
	 * An information-only styled node layer, not intended for further
	 * transformation
	 */
	public static class DomDocumentStyledLayer
			extends StyledNodeLayer<DomDocumentLayer.Document> {
		protected boolean includeExcludedNode(DomNode node) {
			return !node.isDocumentNode();
		}
	}

	/*
	 * For processors/traversals which are info-only, rather than transforming,
	 * override this
	 */
	protected boolean includeExcludedNode(DomNode node) {
		return false;
	}

	/**
	 * output stylednodes wrapping domnodes, excluding areas of the input
	 * document that aren't of interest
	 *
	 * uses two 'not of interest' strategies - ignoreTree (which ignores node n
	 * + all descendants) and ignoreNonTree(ignore node n only)
	 *
	 */
	@Override
	public void process(S selection) throws Exception {
		StyledDocument.AccessorSelection accessorSelection = selection;
		styledDocument = StyledDocument.ancestorDocument(selection);
		Set<DomNode> ignoreTreeNodes = AlcinaCollections.newHashSet();
		Set<String> ignoreTreeTags = Set.of(HtmlConstants.SCRIPT,
				HtmlConstants.STYLE, HtmlConstants.HEAD, HtmlConstants.LINK);
		if (state.traversalContext(Peer.Has.class) != null) {
			Peer peer = state.traversalContext(Peer.Has.class)
					.getStyledNodeLayerPeer();
			peer.markIgnoreTreeNodes(styledDocument, ignoreTreeNodes);
		}
		styledDocument.sourceDocument.stream().forEach(node -> {
			boolean ignore = false;
			boolean ignoreTree = ignoreTreeTags
					.contains(node.name().toUpperCase())
					|| ignoreTreeNodes.contains(node.parent());
			if (ignoreTree) {
				ignoreTreeNodes.add(node);
				ignore = true;
			} else if (ignoreNonTree(node)) {
				ignore = true;
			}
			if (ignore && !includeExcludedNode(node)) {
				IgnoredNodeSelection nodeSelection = new IgnoredNodeSelection(
						selection, node);
				select(nodeSelection);
			} else {
				StyledNode nodeSelection = new StyledNode(selection, node);
				select(nodeSelection);
			}
		});
	}

	/*
	 * Deprecated - switch to specifying a custom StyledNodeLayer.Peer in the
	 * TransformerPeer
	 */
	public StyledNodeLayer withAllowNode(Predicate<DomNode> allowNode) {
		this.allowNode = allowNode;
		return this;
	}

	boolean ignoreNonTree(DomNode domNode) {
		if (domNode.isDocumentNode()) {
			return true;
		}
		if (domNode.isText() && domNode.parent().tagIsOneOf(HtmlConstants.BODY,
				HtmlConstants.HTML)) {
			return true;
		}
		if (measureFilter != null) {
			return !domNode.asRange().toIntPair()
					.intersectsWithNonPoint(measureFilter);
		}
		return !allowNode.test(domNode);
	}
}