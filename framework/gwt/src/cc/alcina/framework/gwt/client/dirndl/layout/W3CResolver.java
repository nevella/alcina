package cc.alcina.framework.gwt.client.dirndl.layout;

import org.w3c.dom.Element;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;

import cc.alcina.framework.common.client.dom.DomDocument;
import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.DomNodeType;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

public class W3CResolver extends ContextResolver {
	protected DomDocument document;

	protected boolean asGwtDoc;

	@Override
	public void renderElement(Node layoutNode, String tagName) {
		if (layoutNode.rendered != null) {
			return;
		}
		DomNode domNode = null;
		if (document == null) {
			document = DomDocument.from(Ax.format("<%s/>", tagName));
			if (asGwtDoc) {
				document = DomDocument.toGwtDocument(document);
			}
			domNode = document.getDocumentElementNode();
		} else {
			Element element = document.w3cDoc().createElement(tagName);
			domNode = document.nodeFor(element);
		}
		String cssClass = layoutNode.directed.className();
		if (cssClass.length() > 0) {
			domNode.style().addClassName(cssClass);
		}
		layoutNode.rendered = new RenderedW3cNode(domNode.w3cNode());
	}

	public void renderNode(DirectedLayout.Node layoutNode, DomNodeType nodeType,
			String tagName, String contents) {
		if (layoutNode.rendered != null) {
			return;
		}
		switch (nodeType) {
		case PROCESSING_INSTRUCTION:
			ProcessingInstruction processingInstruction = document.w3cDoc()
					.createProcessingInstruction(tagName, contents);
			layoutNode.rendered = new RenderedW3cNode(processingInstruction);
			break;
		default:
			throw new UnsupportedOperationException();
		}
	}

	@Override
	public void renderText(Node layoutNode, String contents) {
		if (layoutNode.rendered != null) {
			return;
		}
		Text text = document.w3cDoc().createTextNode(contents);
		layoutNode.rendered = new RenderedW3cNode(text);
	}

	public static class Linking extends W3CResolver {
		protected Model linkModel;

		protected DomNode linkNode;

		@Override
		public void renderElement(DirectedLayout.Node layoutNode,
				String tagName) {
			if (layoutNode.rendered != null) {
				return;
			}
			Object model = layoutNode.getModel();
			if (model != null && model == linkModel) {
				layoutNode.rendered = new RenderedW3cNode(linkNode.w3cNode());
			} else {
				super.renderElement(layoutNode, tagName);
			}
		}
	}
}