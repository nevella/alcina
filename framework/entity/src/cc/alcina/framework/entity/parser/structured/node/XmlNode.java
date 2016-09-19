package cc.alcina.framework.entity.parser.structured.node;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.entity.OptimizingXpathEvaluator;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.XmlUtils;
import cc.alcina.framework.entity.XpathHelper;
import cc.alcina.framework.entity.parser.structured.XmlTokenNode;

public class XmlNode {
	protected Node node;

	public XmlDoc doc;

	public XmlNodeChildren children;

	public XmlTokenNode open;

	public XmlTokenNode close;

	private String normalisedTextContent;

	private StringMap attributes;

	public XmlNode(Node node, XmlDoc xmlDoc) {
		this.node = node;
		this.doc = xmlDoc;
		this.children = new XmlNodeChildren();
	}

	public XmlNode(XmlNode from) {
		this(from.node, from.doc);
	}

	public XmlNodeBuilder add() {
		return new XmlNodeBuilder(this);
	}

	public String attr(String name) {
		if (!isElement()) {
			return null;
		}
		return ((Element) node).getAttribute(name);
	}

	public StringMap attributes() {
		if (attributes == null) {
			attributes = new StringMap();
			NamedNodeMap nnm = node.getAttributes();
			for (int idx = 0; idx < nnm.getLength(); idx++) {
				Attr attr = (Attr) nnm.item(idx);
				attributes.put(attr.getName(), attr.getValue());
			}
		}
		return attributes;
	}

	public XmlNodeDebug debug() {
		return new XmlNodeDebug();
	}

	public int depth() {
		int depth = 0;
		XmlNode cursor = this;
		while (cursor.parent() != null) {
			cursor = cursor.parent();
			depth++;
		}
		return depth;
	}

	public boolean has(String name) {
		if (!isElement()) {
			return false;
		}
		return ((Element) node).hasAttribute(name);
	}

	public void invalidate() {
		children.nodes = null;
		normalisedTextContent = null;
	}

	public boolean isElement() {
		return node.getNodeType() == Node.ELEMENT_NODE;
	}

	public boolean isText() {
		return node.getNodeType() == Node.TEXT_NODE;
	}

	public String name() {
		return node.getNodeName();
	}

	public String ntc() {
		if (normalisedTextContent == null) {
			normalisedTextContent = SEUtilities
					.normalizeWhitespaceAndTrim(node.getTextContent());
		}
		return normalisedTextContent;
	}

	public XmlNode parent() {
		return doc.nodeFor(node.getParentNode());
	}

	public boolean tagIs(String tagName) {
		return isElement() && getElement().getTagName().equals(tagName);
	}

	public boolean tagIsOneOf(Collection<String> tags) {
		return isElement() && tags.contains(name());
	}

	public String textContent() {
		return node.getTextContent();
	}

	public boolean textIs(String string) {
		return ntc().equals(string);
	}

	@Override
	public String toString() {
		return CommonUtils.trimToWsChars(
				XmlUtils.streamXML(node).replace(CommonUtils.XML_PI, ""), 100);
	}

	private Element getElement() {
		return (Element) node;
	}

	protected Document domDoc() {
		return node.getNodeType() == Node.DOCUMENT_NODE ? (Document) node
				: node.getOwnerDocument();
	}

	public class XmlNodeChildren {
		private List<XmlNode> nodes;

		public void invalidate() {
			XmlNode.this.invalidate();
		}

		public boolean noElements() {
			return elements().size() == 0;
		}

		public boolean soleElement(String tag) {
			List<XmlNode> elts = elements();
			return elts.size() == 1 && elts.get(0).tagIs(tag);
		}

		public List<XmlNode> elements() {
			return nodes().stream().filter(XmlNode::isElement)
					.collect(Collectors.toList());
		}

		public List<XmlNode> nodes() {
			if (nodes == null) {
				nodes = XmlUtils.nodeListToList(node.getChildNodes()).stream()
						.map(doc::nodeFor).collect(Collectors.toList());
			}
			return nodes;
		}

		public XmlNode firstElement() {
			return elements().get(0);
		}

		public boolean contains(String tag) {
			return elements().stream().anyMatch(xn -> xn.tagIs(tag));
		}
	}

	public class XmlNodeDebug {
		public String shortRepresentation() {
			String out = "";
			if (isElement()) {
				if (close != null && open != null) {
					out = String.format("<%s />", name());
				} else if (close != null) {
					out = String.format("</%s>", name());
				} else {
					out = String.format("<%s>", name());
				}
			}
			String ntc = ntc();
			if (ntc.length() > 0) {
				ntc = CommonUtils.trimToWsChars(ntc, 15);
				if (out.length() > 0) {
					out += " : ";
				}
				out += ntc;
			}
			return out;
		}
	}

	public boolean isAncestorOf(XmlNode xmlNode) {
		return XmlUtils.isAncestorOf(node, xmlNode.node);
	}

	private XmlNodeXpath xpath;

	public XmlNodeXpath xpath() {
		if (xpath == null) {
			xpath = new XmlNodeXpath();
		}
		return xpath;
	}

	public class XmlNodeXpath {
		private OptimizingXpathEvaluator eval;

		public XmlNodeXpath() {
			XpathHelper xh = new XpathHelper(node);
			eval = xh.createOptimisedEvaluator(node);
		}

		public List<XmlNode> nodes(String xpath) {
			List<Element> elements = eval.getElementsByXpath(xpath, node);
			return elements.stream().map(doc::nodeFor)
					.collect(Collectors.toList());
		}

		public XmlNode node(String xpath) {
			Element element = eval.getElementByXpath(xpath, node);
			return doc.nodeFor(element);
		}

		public String textOrEmpty(String xpath) {
			return Optional.ofNullable(node(xpath)).map(XmlNode::textContent)
					.orElse("");
		}
	}

	public class XmlNodeAncestor {
		private boolean orSelf = false;

		public XmlNodeAncestor orSelf() {
			orSelf = true;
			return this;
		}

		public boolean has(String tag) {
			return get(tag) != null;
		}

		public XmlNode get(String tag) {
			if (orSelf && tagIs(tag)) {
				return XmlNode.this;
			}
			Element ancestor = XmlUtils.getAncestorWithTagName(node, tag);
			return ancestor == null ? null : doc.nodeFor(ancestor);
		}
	}

	public String dumpXml() {
		return XmlUtils.streamXML(node);
	}

	private XmlNodeAncestor ancestor;

	public XmlNodeAncestor ancestor() {
		if (ancestor == null) {
			ancestor = new XmlNodeAncestor();
		}
		return ancestor;
	}
}
