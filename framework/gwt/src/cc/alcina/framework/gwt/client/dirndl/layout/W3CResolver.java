package cc.alcina.framework.gwt.client.dirndl.layout;

import org.w3c.dom.Element;
import org.w3c.dom.Text;

import cc.alcina.framework.common.client.dom.DomDocument;
import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;

public class W3CResolver extends ContextResolver {
	DomDocument doc;

	@Override
	public void renderElement(Node layoutNode, String tagName) {
		DomNode domNode = null;
		if (doc == null) {
			doc = DomDocument.from(Ax.format("<%s/>", tagName));
			domNode = doc.getDocumentElementNode();
		} else {
			Element element = doc.domDoc().createElement(tagName);
			domNode = doc.nodeFor(element);
		}
		String cssClass = layoutNode.directed.cssClass();
		if (cssClass.length() > 0) {
			domNode.style().addClassName(cssClass);
		}
		layoutNode.rendered = new RenderedW3cNode(domNode.w3cNode());
	}

	@Override
	public void renderText(Node layoutNode, String contents) {
		Text text = doc.domDoc().createTextNode(contents);
		layoutNode.rendered = new RenderedW3cNode(text);
	}
}