package cc.alcina.framework.servlet.transform.fragment;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.dom.DomDocument;
import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.process.ContextObservable;
import cc.alcina.framework.common.client.process.ProcessObserver;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.entity.xml.EntityCleaner;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.FragmentDocument;
import cc.alcina.framework.servlet.transform.fragment.html.ImageLayer;
import cc.alcina.framework.servlet.transform.fragment.pos.MergedIndentLayer.IndentStylesObservable;

/*
 */
public class DocumentTransformerHost {
	/*
	 * Log the transform source to /tmp/log/log.xml
	 */
	static Configuration.Key logSource = Configuration
			.key(DocumentTransformerHost.class, "logSource");

	String indentCss;

	class IndentStylesObserver
			implements ProcessObserver<IndentStylesObservable> {
		@Override
		public void topicPublished(IndentStylesObservable message) {
			indentCss = message.css;
		}
	}

	public String filterHtml(String inputMarkup, DocumentTransformerPeer peer)
			throws Exception {
		new IndentStylesObserver().bind();
		/*
		 * Convert to fragment document
		 */
		if (peer == null) {
			peer = new DocumentTransformerPeer();
		}
		DocumentTransformer transformer = new DocumentTransformer(peer,
				inputMarkup);
		if (logSource.is()) {
			DomDocument sourceDocument = transformer.styledDocument
					.getSourceDocument();
			sourceDocument.logPretty();
		}
		try {
			transformer.transform();
		} catch (Exception e) {
			throw e;
		}
		DomDocument node = null;
		node = peer.outputDocument.getDomDocument();
		String outputMarkup = node.toMarkup();
		outputMarkup = EntityCleaner.cleanIllegalXmlCharacters(outputMarkup);
		if (Ax.isTest()) {
			DomDocument cleanup = DomDocument.from(outputMarkup);
			cleanup.xpath("//style").nodes().stream().skip(1).toList()
					.forEach(DomNode::removeFromParent);
			cleanup.document.getDocumentElementNode().builder().tag("style")
					.text(Ax.blankToEmpty(indentCss)).insertAsFirstChild();
			String debugMarkup = cleanup.toMarkup();
			new TransformedMarkupGenerated(peer.outputDocument, debugMarkup)
					.publish();
		}
		return outputMarkup;
	}

	public static class TransformedMarkupGenerated
			implements ContextObservable, DocumentGenerated {
		public String markup;

		public FragmentDocument document;

		public TransformedMarkupGenerated(FragmentDocument document,
				String markup) {
			this.document = document;
			this.markup = markup;
		}

		@Override
		public FragmentDocument document() {
			return document;
		}

		@Override
		public String markup() {
			return markup;
		}
	}

	protected boolean isDelegatedChild() {
		return false;
	}

	protected boolean isUnconvertibleHtml(String bodyHtml) {
		return false;
	}

	public static class Util {
		public static String
				moveNonTextFromHeadingsToPrecedingDevs(String markup) {
			DomDocument from = Io.read().string(markup).asDomDocument();
			List<DomNode> headings = from.stream()
					.filter(n -> n.isElement() && n.name().matches("h\\d"))
					.toList();
			headings.forEach(heading -> {
				List<DomNode> moveContent = new ArrayList<>();
				heading.children.nodes().forEach(node -> {
					if (node.isComment()
							&& ImageLayer.containsImageData(node)) {
						moveContent.add(node);
					} else if (node.descendants()
							.anyMatch(ImageLayer::isImageLike)) {
						moveContent.add(node);
					}
				});
				if (moveContent.size() > 0) {
					DomNode movedContainer = heading.builder().tag("div")
							.attr("fdoc-moved-image-data", "true")
							.insertBeforeThis();
					moveContent.forEach(movedContainer.children::append);
					LoggerFactory.getLogger(Util.class).info(
							"Moved {} image-likes in headers",
							moveContent.size());
				}
			});
			return from.toMarkup();
		}
	}
}
