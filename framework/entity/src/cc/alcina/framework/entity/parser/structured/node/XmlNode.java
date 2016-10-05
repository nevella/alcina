package cc.alcina.framework.entity.parser.structured.node;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;

import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.entity.OptimizingXpathEvaluator;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.XmlUtils;
import cc.alcina.framework.entity.XpathHelper;
import cc.alcina.framework.entity.parser.structured.XmlStructuralJoin;

public class XmlNode {
	protected Node node;

	public XmlDoc doc;

	public XmlNodeChildren children;

	public XmlStructuralJoin open;

	public XmlStructuralJoin close;

	private String normalisedTextContent;

	private StringMap attributes;

	private XmlNodeXpath xpath;

	private XmlNodeAncestor ancestor;

	public XmlNode(Node node, XmlDoc xmlDoc) {
		this.node = node;
		this.doc = xmlDoc;
		this.children = new XmlNodeChildren();
	}

	public XmlNode(XmlNode from) {
		this(from.node, from.doc);
	}

	public XmlNodeAncestor ancestors() {
		if (ancestor == null) {
			ancestor = new XmlNodeAncestor();
		}
		return ancestor;
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
			if (isElement()) {
				NamedNodeMap nnm = node.getAttributes();
				for (int idx = 0; idx < nnm.getLength(); idx++) {
					Attr attr = (Attr) nnm.item(idx);
					attributes.put(attr.getName(), attr.getValue());
				}
			}
		}
		return attributes;
	}

	public boolean attrIs(String key, String value) {
		return attributes().getOrDefault(key, "").equals(value);
	}

	public XmlNodeBuilder builder() {
		return new XmlNodeBuilder(this);
	}

	public XmlNode cloneNode(boolean deep) {
		return doc.nodeFor(node.cloneNode(deep));
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

	public String dumpXml() {
		return XmlUtils.streamXML(node);
	}

	public String fullToString() {
		return XmlUtils.streamXML(node).replace(CommonUtils.XML_PI, "");
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

	public boolean isAncestorOf(XmlNode xmlNode) {
		return XmlUtils.isAncestorOf(node, xmlNode.node);
	}

	public boolean isElement() {
		return node.getNodeType() == Node.ELEMENT_NODE;
	}

	public boolean isEmptyTextContent() {
		return textContent().isEmpty();
	}

	public boolean isNonWhitespaceTextContent() {
		return ntc().length() > 0;
	}

	public boolean isProcessingInstruction() {
		return node.getNodeType() == Node.PROCESSING_INSTRUCTION_NODE;
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
					.normalizeWhitespaceAndTrim(textContent());
		}
		return normalisedTextContent;
	}

	public XmlNode parent() {
		return doc.nodeFor(node.getParentNode());
	}

	public XmlNodeRelative relative() {
		return new XmlNodeRelative();
	}

	public void removeFromParent() {
		parent().invalidate();
		node.getParentNode().removeChild(node);
	}

	public void replaceWith(XmlNode other) {
		relative().insertBefore(other);
		removeFromParent();
	}

	public XmlNode setAttr(String key, String value) {
		((Element) node).setAttribute(key, value);
		return this;
	}

	public void setText(String text) {
		((Text) node).setData(text);
		invalidate();
	}

	public boolean tagIs(String tagName) {
		return isElement() && getElement().getTagName().equals(tagName)
				|| isProcessingInstruction() && getProcessingInstruction()
						.getNodeName().equals(tagName);
	}

	public boolean tagIsOneOf(Collection<String> tags) {
		return isElement() && tags.contains(name());
	}

	public boolean tagIsOneOf(String... tags) {
		if (isElement()) {
			for (int idx = 0; idx < tags.length; idx++) {
				if (name().equals(tags[idx])) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean textContains(String string) {
		return textContent().toLowerCase().contains(string.toLowerCase());
	}

	public String textContent() {
		return isProcessingInstruction() ? getProcessingInstruction().getData()
				: node.getTextContent();
	}

	public boolean textIs(String string) {
		return ntc().equals(string);
	}

	public boolean textMatches(String regex) {
		return textContent().matches(regex);
	}

	@Override
	public String toString() {
		return CommonUtils.trimToWsChars(
				XmlUtils.streamXML(node).replace(CommonUtils.XML_PI, ""), 255,
				true);
	}

	public XmlNodeXpath xpath() {
		if (xpath == null) {
			xpath = new XmlNodeXpath();
		}
		return xpath;
	}

	private Element getElement() {
		return (Element) node;
	}

	private ProcessingInstruction getProcessingInstruction() {
		return (ProcessingInstruction) node;
	}

	protected Document domDoc() {
		return node.getNodeType() == Node.DOCUMENT_NODE ? (Document) node
				: node.getOwnerDocument();
	}

	public class XmlNodeAncestor {
		private boolean orSelf = false;

		public XmlNode get(String... tags) {
			List<String> tagList = Arrays.asList(tags);
			if (orSelf && tagIsOneOf(tagList)) {
				return XmlNode.this;
			}
			XmlNode cursor = XmlNode.this;
			while (cursor != null) {
				if (cursor.tagIsOneOf(tagList)) {
					return cursor;
				}
				cursor = cursor.parent();
			}
			return null;
		}

		public boolean has(String... tags) {
			return get(tags) != null;
		}

		public boolean has(XmlNode test) {
			test=test.unwrap();
			XmlNode node = XmlNode.this.unwrap();
			while (node != null) {
				if (node == test) {
					return true;
				}
				node = node.parent();
			}
			return false;
		}

		public boolean isFirstChild() {
			return parent().children.isFirstChild(XmlNode.this);
		}

		public List<XmlNode> list() {
			List<XmlNode> result = new ArrayList<>();
			XmlNode cursor = XmlNode.this;
			while (cursor != null) {
				result.add(cursor);
				cursor = cursor.parent();
			}
			return result;
		}

		public XmlNodeAncestor orSelf() {
			orSelf = true;
			return this;
		}
	}

	public class XmlNodeChildren {
		private List<XmlNode> nodes;

		public void append(XmlNode xmlNode) {
			XmlNode.this.node.appendChild(xmlNode.node);
			invalidate();
		}

		public boolean contains(String tag) {
			return elements().stream().anyMatch(xn -> xn.tagIs(tag));
		}

		public List<XmlNode> elements() {
			return nodes().stream().filter(XmlNode::isElement)
					.collect(Collectors.toList());
		}

		public XmlNode firstElement() {
			return CommonUtils.first(elements());
		}

		public XmlNode firstNode() {
			return CommonUtils.first(nodes());
		}

		public XmlNode firstNonElementChild() {
			return flatten().filter(n -> !n.isElement()).findFirst()
					.orElse(null);
		}

		public Stream<XmlNode> flat() {
			return flatten();
		}

		public Stream<XmlNode> flatten(String... tags) {
			List<String> tagArray = Arrays.asList(tags);
			Iterable<XmlNode> iterable = () -> new XmlTokenStream(XmlNode.this);
			Stream<XmlNode> targetStream = StreamSupport
					.stream(iterable.spliterator(), false);
			return targetStream.filter(t -> t.isText() || tagArray.isEmpty()
					|| t.tagIsOneOf(tagArray));
		}

		public void insertAsFirstChild(XmlNode newChild) {
			invalidate();
			node.insertBefore(newChild.node, node.getFirstChild());
		}

		public void invalidate() {
			XmlNode.this.invalidate();
		}

		public boolean isFirstChild(XmlNode xmlNode) {
			return xmlNode!=null&&firstNode() == xmlNode.unwrap();
		}

		public XmlNode lastElementNode() {
			List<XmlNode> nodes = nodes();
			for (int idx = nodes.size() - 1; idx >= 0; idx--) {
				XmlNode kid = nodes.get(idx);
				if (kid.isElement()) {
					return kid;
				}
			}
			return null;
		}

		public XmlNode lastNode() {
			return CommonUtils.last(nodes());
		}

		public XmlNode lastNonEmptyTextNode() {
			List<XmlNode> nodes = nodes();
			for (int idx = nodes.size() - 1; idx >= 0; idx--) {
				XmlNode kid = nodes.get(idx);
				if (kid.isElement()
						|| kid.isText() && !kid.isEmptyTextContent()) {
					return kid;
				}
			}
			return null;
		}

		public List<XmlNode> nodes() {
			if (nodes == null) {
				nodes = XmlUtils.nodeListToList(node.getChildNodes()).stream()
						.map(doc::nodeFor).collect(Collectors.toList());
			}
			return nodes;
		}

		public boolean noElements() {
			return elements().size() == 0;
		}

		public XmlNode soleElement() {
			List<XmlNode> elts = elements();
			return elts.size() == 1 && nodes().size() == 1 ? elts.get(0) : null;
		}

		public boolean soleElement(String tag) {
			List<XmlNode> elts = elements();
			return elts.size() == 1 && elts.get(0).tagIs(tag);
		}

		public boolean isLastChild(XmlNode node) {
			return node!=null&&lastNode() == node.unwrap();
		}

		public boolean isLastElementNode(XmlNode node) {
			return node!=null&&lastElementNode() == node.unwrap();
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

	public class XmlNodeRelative {
		public boolean hasPreviousSibling() {
			return node.getPreviousSibling() != null;
		}

		public void insertAfter(XmlNode node) {
			parent().invalidate();
			parent().node.insertBefore(node.node,
					XmlNode.this.node.getNextSibling());
		}

		public void insertBefore(XmlNode node) {
			parent().invalidate();
			parent().node.insertBefore(node.node, XmlNode.this.node);
		}
	}

	public class XmlNodeXpath {
		private OptimizingXpathEvaluator eval;

		private XpathHelper xh;

		public XmlNodeXpath() {
			xh = new XpathHelper(node);
			eval = xh.createOptimisedEvaluator(node);
		}

		public boolean contains(String xpath) {
			return node(xpath) != null;
		}

		public XmlNode node(String xpath) {
			Node domNode = eval.getNodeByXpath(xpath, node);
			return doc.nodeFor(domNode);
		}

		public List<XmlNode> nodes(String xpath) {
			List<Node> domNodes = eval.getNodesByXpath(xpath, node);
			return domNodes.stream().map(doc::nodeFor)
					.collect(Collectors.toList());
		}

		public boolean selfIs(String xpath) {
			return XmlNode.this.parent().xpath().nodes(xpath)
					.contains(doc.nodeFor(node));
		}

		public String textOrEmpty(String xpath) {
			return Optional.ofNullable(node(xpath)).map(XmlNode::textContent)
					.orElse("");
		}
	}

	public XmlNode unwrap() {
		return this.getClass()==XmlNode.class?this:doc.nodeFor(node);
	}
}
