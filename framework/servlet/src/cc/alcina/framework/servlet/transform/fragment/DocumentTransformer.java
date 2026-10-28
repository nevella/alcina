package cc.alcina.framework.servlet.transform.fragment;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.HtmlParser;

import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.dom.DomDocument;
import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.process.ContextObservable;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.traversal.layer.MeasureSelection;
import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.common.client.util.IntPair;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.Inline;
import cc.alcina.framework.servlet.job.JobContext;
import cc.alcina.framework.servlet.transform.fragment.output.MeasureNode;

/**
 * <p>
 * Transforms a document to a FragmentDocumentNode subtype via
 * selectiontraversal.
 *
 * <p>
 * TODO - doc - the Peer pattern and implementations
 * 
 *
 */
/*
 * Implementation notes:
 * 
 * - Both input and output docs are server-side GWT/Alcina Documents, backed by
 * NodeLocal nodes (rather than Apache Xerxes) - this forces HTML validation
 * earlier and ensures stable rendering in the TraversalBrowser debug views
 */
public class DocumentTransformer {
	public static class TransformSourceObservable implements ContextObservable {
		public String source;

		public TransformSourceObservable(String source) {
			this.source = source;
		}
	}

	public static class Pdf2HtmlObservable implements ContextObservable {
	}

	public static class InvalidHtmlDomException
			extends IllegalArgumentException {
		public InvalidHtmlDomException() {
		}

		public InvalidHtmlDomException(String message) {
			super(message);
		}
	}

	public static Configuration.Key measureFilterKey = Configuration
			.key("measureFilter");

	public static Configuration.Key throwOnInvalidRemovalAffectingTextContent = Configuration
			.key("throwOnInvalidRemovalAffectingTextContent");

	public static boolean isPartialDocumentTransform() {
		return measureFilterKey.notBlank();
	}

	DocumentTransformerPeer peer;

	StyledDocument styledDocument;

	Pattern tableChildPattern = Pattern.compile("(?i)thead|tbody|tfoot");

	Logger logger = LoggerFactory.getLogger(DocumentTransformer.class);

	boolean pdf2Html;

	DomDocument sourceDocument;

	public DocumentTransformer(DocumentTransformerPeer peer, String markup) {
		this.peer = peer;
		this.peer.transformer = this;
		/*
		 * Emit a markup observable, this will be used for static section
		 * extracts
		 */
		new TransformSourceObservable(markup).publish();
		try {
			LooseContext.push();
			Document.CONTEXT_TAG_VALIDATION_DISABLED.setTrue();
			sourceDocument = DomDocument.from(markup, true);
		} catch (Exception e) {
			/*
			 * msword - WA - JAD-20154
			 */
			markup = markup.replaceAll("<p ([^<]+)</td>", "<p $1</p></td>");
			HtmlParser.CONTEXT_DO_NOT_CLOSE_TO_VALIDATE.setTrue();
			sourceDocument = DomDocument.from(markup, true);
		} finally {
			LooseContext.pop();
		}
		DocumentOffsetMapping.SnapshotBuilder snapshotBuilder = new DocumentOffsetMapping.SnapshotBuilder(
				sourceDocument);
		snapshotBuilder.snapshot();
		/*
		 * Invalid markup is allowed (unavoidably, MS Word produces a lot), but
		 * it's pre-filtered so does not complexify processing
		 */
		removeOrFixInvalidMarkup();
		peer.removeInvalidMarkup(sourceDocument);
		peer.removeExtraneousMarkup(sourceDocument);
		snapshotBuilder.snapshot();
		DocumentOffsetMapping preInputToInputMapping = snapshotBuilder
				.createMapping(DocumentOffsetMapping.Type.PRE_INPUT_TO_INPUT);
		preInputToInputMapping.publish();
		styledDocument = new StyledDocument(sourceDocument,
				peer.styledDocumentPeer);
		if (measureFilterKey.notBlank()) {
			IntPair filterPair = IntPair.parseIntPair(measureFilterKey.get());
			DomNode body = sourceDocument.html().body();
			Set<DomNode> toRemove = body.descendants().filter(n -> {
				IntPair testPair = n.asRange().toIntPair();
				return !filterPair.intersectsWithNonPoint(testPair);
			}).collect(AlcinaCollectors.toLinkedHashSet());
			List<DomNode> toRemoveTopLevel =
					// only remove the top-level to-removes
					toRemove.stream()
							.filter(tr -> !tr.tagIs("style")
									&& !toRemove.contains(tr.parent()))
							.toList();
			toRemoveTopLevel.forEach(DomNode::removeFromParent);
			DomDocument clonedDocument = sourceDocument.clone();
			styledDocument = new StyledDocument(clonedDocument,
					peer.styledDocumentPeer);
		}
	}

	public void transform() {
		peer.initialiseTraversal();
		peer.traversal.selections().topicAdded.add(this::onSelectionAdded);
		peer.traversal.selections().topicProcessed
				.add(v -> JobContext.checkCancelled());
		peer.traversal.selections().topicBeforeProcessed
				.add(this::beforeSelectionProcessed);
		peer.traversal.traverse();
		logTraversalStats();
		emitTransformObservable();
		new SourceFootnoteModel(this).publish();
		peer.afterTraversal();
	}

	/*
	 * This removes invalid -but- harmless markup (invalid elements, but
	 * empty/whitespace text content)
	 * 
	 * Invalid markup -with- non-trivial text will throw
	 */
	protected void removeOrFixInvalidMarkup() {
		{
			/*
			 * replace invalid P with DIV
			 */
			List<DomNode> invalidPs = sourceDocument.asDomNode().stream()
					.filter(n -> n.tagIs("p") && n.descendants().anyMatch(
							n1 -> n1.tagIsOneOf("TABLE", "P", "UL", "OL")))
					.toList();
			invalidPs.forEach(n -> n.builder().tag("div").replaceWith());
		}
		{
			/*
			 * com.google.gwt.dom.client.TableElement.validateInsert0(Node
			 * newChild)
			 * 
			 * verify table element children are html-valid
			 */
			List<DomNode> invalidTableChildren = sourceDocument.asDomNode()
					.stream().filter(n -> n.tagIs("table"))
					.flatMap(table -> table.children.nodes().stream()
							.filter(n2 -> !n2.isWhitespaceOrEmptyTextContent()
									&& !tableChildPattern.matcher(n2.name())
											.matches()))
					.collect(Collectors.toList());
			new InvalidTdRepair().repair(invalidTableChildren);
			removeCheckingWhitespaceOnly(invalidTableChildren);
		}
		{
			/*
			 * 
			 * 
			 * if there is a table-tbody-thead structure, split the table (move
			 * any table-tbody-tr prior to thead to a previous table), the
			 */
			List<DomNode> invalidTheads = sourceDocument.asDomNode().stream()
					.filter(n -> n.tagIs("thead") && n.parent().tagIs("tbody"))
					.toList();
			for (DomNode tHead : invalidTheads) {
				DomNode tBody = tHead.parent();
				DomNode table = tBody.parent();
				Preconditions.checkState(table.tagIs("table"));
				DomNode tableClone = table.cloneNode(false);
				table.relative().insertBeforeThis(tableClone);
				DomNode tBodyClone = tBody.cloneNode(false);
				tBodyClone.appendTo(tableClone);
				for (DomNode tBodyChild : tBody.children.nodes()) {
					if (tBodyChild == tHead) {
						break;
					}
					tBodyChild.appendTo(tBodyClone);
				}
				tBody.relative().insertBeforeThis(tHead);
			}
		}
		{
			/*
			 * com.google.gwt.dom.client.TableSectionElement.validateInsert0(
			 * Node newChild)
			 */
			List<DomNode> invalidTableSectionChildren = sourceDocument
					.asDomNode().stream()
					.filter(n -> tableChildPattern.matcher(n.name()).matches())
					.flatMap(table -> table.children.nodes().stream()
							.filter(n2 -> !n2.isWhitespaceOrEmptyTextContent()
									&& !n2.nameIs("tr")))
					.toList();
			removeCheckingWhitespaceOnly(invalidTableSectionChildren);
		}
		{
			/*
			 * com.google.gwt.dom.client.TableRowElement.validateInsert0(Node
			 * newChild)
			 */
			// specifically TR/SPAN (yep, ms.word)
			List<DomNode> invalidTrChildren = sourceDocument.stream()
					.filter(n -> n.parent() != null && n.parent().tagIs("tr")
							&& n.isElement() && !n.tagIsOneOf("td", "th"))
					.toList();
			removeCheckingWhitespaceOnly(invalidTrChildren);
		}
		/*
		 * table-out-of-p
		 */
		{
			List<DomNode> invalidTableAncestor = sourceDocument.asDomNode()
					.stream()
					.filter(n -> n.tagIs("table")
							&& n.ancestors().get("p") != null)
					.collect(Collectors.toList());
			Collections.reverse(invalidTableAncestor);
			invalidTableAncestor.forEach(
					n -> n.ancestors().get("p").relative().insertAfterThis(n));
		}
	}

	class InvalidTdRepair {
		void repair(List<DomNode> invalidTableChildren) {
			invalidTableChildren.removeIf(this::repair);
		}

		boolean repair(DomNode node) {
			if (node.tagIs("td") && node.parent().tagIs("table")
					&& node.parent().children
							.nonWhitespaceTextAndElementChildren()
							.allMatch(child -> child.tagIsOneOf("tr", "td"))) {
				node.builder().tag("tr").wrap();
				return true;
			} else {
				return false;
			}
		}
	}

	protected void logTraversalStats() {
		if (Configuration.is("logTraversalStats")) {
			peer.traversal.logTraversalStats();
		}
	}

	void removeCheckingWhitespaceOnly(List<DomNode> invalid) {
		List<DomNode> nonWhitespace = invalid.stream()
				.filter(n -> !n.isWhitespaceOrEmptyTextContent()).toList();
		if (nonWhitespace.size() > 0) {
			if (throwOnInvalidRemovalAffectingTextContent.is()) {
				throw new InvalidHtmlDomException(
						"DEVEX::1 - Transformation DOM validation repair - removing non-whitespace nodes");
			} else {
				logger.warn(
						"DEVEX::1 - Transformation DOM validation repair - removing non-whitespace nodes");
			}
		}
		List<DomNode> nonEmpty = invalid.stream()
				.filter(n -> n.textContent().length() > 0).toList();
		if (nonEmpty.size() > 0) {
			logger.warn(
					"Transformation DOM validation repair - removing non-empty nodes");
		}
		invalid.forEach(DomNode::removeFromParent);
	}

	/**
	 * <p>
	 * Emit an object which can be used to map back from the output document
	 * location to the input document location - very useful for
	 * debugging/trimming the source, and essential for reverse transformations
	 * 
	 * <p>
	 * If the source document has no exact mapping for a text node, -some- sort
	 * of corresponding range will be emitted - given output + input section
	 * headers may be ordered differently, all output section header nodes map
	 * to the same (container) input node
	 */
	void emitTransformObservable() {
		DocumentOffsetMapping inputToOutputMapping = new DocumentOffsetMapping()
				.withType(DocumentOffsetMapping.Type.INPUT_TO_OUTPUT);
		MeasureNode.ReverseLookup reverseLookup = new MeasureNode.ReverseLookup();
		peer.getOutputDocument().getRoot().domNode().document.setReadonly(true);
		peer.getOutputDocument().getRoot().byType(Inline.Text.class)
				.forEach(fragmentDocumentNode -> {
					MeasureNode inputNode = reverseLookup
							.getContainingMeasureNode(peer.traversal,
									fragmentDocumentNode);
					MeasureSelection sourceAncestor = inputNode
							.ancestor(n -> !(n instanceof Selection.Output));
					IntPair fromPair = sourceAncestor.get().toIntPair();
					DomNode fragmentDomNode = fragmentDocumentNode.provideNode()
							.getRendered().asDomNode();
					String mappingText = fragmentDocumentNode.asText();
					inputToOutputMapping.add(fromPair.i1,
							fragmentDomNode.asLocation().getIndex(),
							mappingText);
				});
		inputToOutputMapping.publish();
	}

	private void beforeSelectionProcessed() {
	}

	private void onSelectionAdded() {
	}
}
