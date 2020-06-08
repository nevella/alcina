package cc.alcina.framework.gwt.client.util;

import java.util.List;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.dom.client.Text;

import cc.alcina.framework.common.client.dom.DomNode;

public class GwtDomUtils {
	public static List<Element> getChildElements(Element elt) {
		return (List<Element>) (List<?>) DomUtils.getChildElements(elt);
	}

	public static Element getContainingBlock(org.w3c.dom.Node node) {
		return (Element) DomNode.from(node).style().containingBlock().get()
				.domElement();
	}

	public static Element getMinimalParentWithOffsetHeight(Text text) {
		Element parent = text.getParentElement();
		while (parent != null) {
			if (parent.getOffsetHeight() != 0) {
				return parent;
			}
			parent = parent.getParentElement();
		}
		return null;
	}

	public static Element getParentElement(Element elt, String tagName) {
		// double-check it's really an element
		while (elt != null && Element.is(elt)) {
			String eltTagName = elt.getTagName();
			if (eltTagName.equalsIgnoreCase(tagName)) {
				return elt;
			}
			elt = elt.getParentElement();
		}
		return null;
	}

	public static Element getPrecedingElementBreadthFirst(Node n,
			Element lastChildElementOf) {
		Element parentOrLastChild = (Element) ((n == null) ? lastChildElementOf
				: n.getParentNode());
		NodeList<Node> nl = parentOrLastChild.getChildNodes();
		boolean foundNode = n == null;
		for (int i = nl.getLength() - 1; i >= 0; i--) {
			Node n2 = nl.getItem(i);
			if (n != null && n2 == n) {
				foundNode = true;
			}
			if (foundNode && n2.getNodeType() == Node.ELEMENT_NODE) {
				return getPrecedingElementBreadthFirst(null, (Element) n2);
			}
		}
		return parentOrLastChild;
	}

	public static Element getSelfOrAncestorWithTagName(Element node,
			String tagName) {
		return (Element) DomUtils.getSelfOrAncestorWithTagName(node, tagName);
	}

	public static Element getSelfOrAncestorWithTagName(Node node,
			String tagName, Node stop) {
		return (Element) DomUtils.getSelfOrAncestorWithTagName(node, tagName,
				stop);
	}

	public static boolean isVisibleAncestorChain(Element e) {
		while (e != null) {
			if (e.getStyle().getDisplay().equals(Display.NONE.getCssName())) {
				return false;
			}
			if (e.getStyle().getVisibility()
					.equals(Style.Visibility.HIDDEN.getCssName())) {
				return false;
			}
			e = e.getParentElement();
		}
		return true;
	}

	public static List<Node> nodeListToArrayList(NodeList nodeList) {
		return (List<Node>) (List<?>) DomUtils.nodeListToArrayList(nodeList);
	}

	public static List<Element> nodeListToElementList(NodeList nodeList) {
		return (List<Element>) (List<?>) DomUtils
				.nodeListToElementList(nodeList);
	}

	public static String stripStructuralTags(String html) {
		Element elt = Document.get().createDivElement();
		elt.setInnerHTML(html);
		boolean loopOk = true;
		while (loopOk) {
			loopOk = false;
			if (elt.getChildNodes().getLength() == 1) {
				Node child = elt.getChildNodes().getItem(0);
				if (child.getNodeType() == Node.ELEMENT_NODE) {
					Element childElt = (Element) child;
					String tag = childElt.getTagName().toLowerCase();
					if (tag.equals("div") || tag.equals("p")) {
						elt = childElt;
						loopOk = true;
					}
				}
			}
		}
		return elt.getInnerHTML();
	}

	public static String toText(String html) {
		Element elt = Document.get().createElement("DIV");
		elt.setInnerHTML(html);
		return elt.getInnerText();
	}
}
