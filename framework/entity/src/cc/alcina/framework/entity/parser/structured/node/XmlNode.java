package cc.alcina.framework.entity.parser.structured.node;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;
import org.w3c.dom.ranges.DocumentRange;
import org.w3c.dom.ranges.Range;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.TreeWalker;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.entity.J8Utils;
import cc.alcina.framework.entity.OptimizingXpathEvaluator;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.XmlUtils;
import cc.alcina.framework.entity.XmlUtils.BlockResolver;
import cc.alcina.framework.entity.XpathHelper;
import cc.alcina.framework.entity.parser.structured.XmlStructuralJoin;

public class XmlNode {
	/**
	 * Basically, don't use in a loop - more a debugging aid
	 */
	public static XmlNode from(Node n) {
		XmlDoc doc = new XmlDoc(n.getOwnerDocument());
		return doc.nodeFor(n);
	}

	protected Node node;

	public XmlDoc doc;

	public XmlNodeChildren children;

	public XmlStructuralJoin open;

	public XmlStructuralJoin close;

	private String normalisedTextContent;

	private StringMap attributes;

	private XmlNodeXpath xpath;

	private XmlNodeAncestor ancestor;

	private XmlNodeHtml xmlNodeHtml;

	public XmlNode(Node node, XmlDoc xmlDoc) {
		this.node = node;
		this.doc = xmlDoc;
		this.children = new XmlNodeChildren();
	}

	public XmlNode(XmlNode from) {
		this(from.node, from.doc);
	}

	public XmlNode addAttr(String name, String value, String separator) {
		String currentValue = attr(name);
		if (currentValue.length() > 0) {
			currentValue += separator;
		}
		currentValue += value;
		setAttr(name, value);
		return this;
	}

	public XmlNodeAncestor ancestors() {
		if (ancestor == null) {
			ancestor = new XmlNodeAncestor();
		}
		return ancestor;
	}

	public XmlNode asXmlNode() {
		return this.getClass() == XmlNode.class ? this : doc.nodeFor(node);
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

	public XmlNode clearAttributes() {
		attributes().keySet()
				.forEach(k -> node.getAttributes().removeNamedItem(k));
		return this;
	}

	public XmlNode cloneNode(boolean deep) {
		return doc.nodeFor(node.cloneNode(deep));
	}

	public void copyAttributesFrom(XmlNode xmlNode) {
		xmlNode.attributes().forEach((k, v) -> setAttr(k, v));
	}

	public XmlNodeCss css() {
		return new XmlNodeCss();
	}

	public XmlNodeDebug debug() {
		return new XmlNodeDebug();
	}

	public void deleteAttribute(String key) {
		node.getAttributes().removeNamedItem(key);
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

	public Element domElement() {
		return (Element) node;
	}

	public Node domNode() {
		return node;
	}

	public String dumpXml() {
		return XmlUtils.streamXML(node);
	}

	public XmlNode ensurePath(String path) {
		if (path.contains("/")) {
			XmlNode cursor = this;
			for (String pathPart : path.split("/")) {
				cursor = cursor.ensurePath(pathPart);
			}
			return cursor;
		}
		List<XmlNode> kids = children.byTag(path);
		if (kids.size() > 1) {
			throw new RuntimeException("Ambiguous path");
		}
		if (kids.size() == 1) {
			return kids.get(0);
		}
		return builder().tag(path).append();
	}

	public String fullToString() {
		return XmlUtils.streamXML(node).replace(CommonUtils.XML_PI, "");
	}

	public Element getElement() {
		return (Element) node;
	}

	public boolean has(String name) {
		if (!isElement()) {
			return false;
		}
		return ((Element) node).hasAttribute(name);
	}

	public XmlNodeHtml html() {
		if (xmlNodeHtml == null) {
			xmlNodeHtml = new XmlNodeHtml();
		}
		return xmlNodeHtml;
	}

	public void invalidate() {
		children.nodes = null;
		normalisedTextContent = null;
	}

	public boolean isAncestorOf(XmlNode xmlNode) {
		return XmlUtils.isAncestorOf(node, xmlNode.node);
	}

	public boolean isDocumentNode() {
		return node.getNodeType() == Node.DOCUMENT_NODE;
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

	public boolean isWhitespaceTextContent() {
		return ntc().length() == 0;
	}

	public String logPretty() {
		try {
			XmlUtils.logToFilePretty(node);
			return "ok";
		} catch (Exception e) {
			try {
				XmlUtils.logToFile(node);
				return "could not log pretty - logged raw instead";
			} catch (Exception e1) {
				throw new WrappedRuntimeException(e);
			}
		}
	}

	public void logToFile() {
		try {
			XmlUtils.logToFile(node);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public String name() {
		return node.getNodeName();
	}

	public boolean nameIs(String name) {
		return name().equals(name);
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

	public String prettyToString() {
		try {
			if (node.getNodeType() == Node.DOCUMENT_FRAGMENT_NODE) {
				return XmlUtils
						.prettyPrintWithDOM3LSNode((DocumentFragment) node);
			} else {
				return XmlUtils.prettyPrintWithDOM3LSNode(getElement());
			}
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public XmlRange range() {
		return new XmlRange();
	}

	public XmlNodeRelative relative() {
		return new XmlNodeRelative();
	}

	public void removeFromParent() {
		parent().invalidate();
		node.getParentNode().removeChild(node);
	}

	public void removeWhitespaceNodes() {
		children.flat().filter(n -> n.isText() && n.isWhitespaceTextContent())
				.forEach(XmlNode::removeFromParent);
	}

	public void replaceWith(XmlNode other) {
		relative().insertBeforeThis(other);
		removeFromParent();
	}

	public XmlNode setAttr(String key, String value) {
		((Element) node).setAttribute(key, value);
		return this;
	}

	public void setText(String text) {
		if (isText()) {
			((Text) node).setData(text);
		} else {
			if (children.noElements()) {
				node.setTextContent(text);
			} else {
				throw new RuntimeException("node has child elements");
			}
		}
		invalidate();
	}

	/*
	 * only sort if element-only children
	 * 
	 */
	public void sort() {
		List<XmlNode> nodes = children.nodes();
		if (nodes.stream().allMatch(XmlNode::isElement)) {
			nodes.forEach(XmlNode::removeFromParent);
			nodes = nodes.stream().sorted(Comparator.comparing(XmlNode::name))
					.collect(Collectors.toList());
			children.append(nodes);
			nodes.forEach(XmlNode::sort);
		}
	}

	public void strip() {
		DocumentFragment frag = domDoc().createDocumentFragment();
		XmlNode fragNode = new XmlNode(frag, doc);
		fragNode.children.adoptFrom(this);
		relative().insertBeforeThis(fragNode);
		removeFromParent();
	}

	public boolean tagIs(String tagName) {
		return isElement()
				&& getElement().getTagName().equalsIgnoreCase(tagName)
				|| isProcessingInstruction() && getProcessingInstruction()
						.getNodeName().equalsIgnoreCase(tagName);
	}

	public boolean tagIsOneOf(Collection<String> tags) {
		return isElement()
				&& tags.stream().anyMatch(t -> t.equalsIgnoreCase(name()));
	}

	public boolean tagIsOneOf(String... tags) {
		if (isElement()) {
			for (int idx = 0; idx < tags.length; idx++) {
				if (name().equalsIgnoreCase(tags[idx])) {
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

	public XmlNodeTree tree() {
		return new XmlNodeTree();
	}

	public XmlNodeXpath xpath(String query) {
		return xpath(query, new Object[] {});
	}

	public XmlNodeXpath xpath(String query, Object... args) {
		if (xpath == null) {
			xpath = new XmlNodeXpath();
		}
		xpath.query = Ax.format(query, args);
		return xpath;
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

		public XmlNode ancestorBefore(XmlNode node) {
			XmlNode cursor = XmlNode.this;
			while (cursor != null) {
				XmlNode parent = cursor.parent();
				if (parent == node) {
					return cursor;
				}
				cursor = parent;
			}
			return null;
		}

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
			test = test.asXmlNode();
			XmlNode node = XmlNode.this.asXmlNode();
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

		public Optional<XmlNode> match(Predicate<XmlNode> predicate) {
			XmlNode cursor = XmlNode.this;
			while (cursor != null) {
				if (predicate.test(cursor)) {
					return Optional.of(cursor);
				}
				cursor = cursor.parent();
			}
			return Optional.empty();
		}

		public XmlNodeAncestor orSelf() {
			XmlNodeAncestor ancestor = new XmlNodeAncestor();
			ancestor.orSelf = true;
			return ancestor;
		}
	}

	public class XmlNodeChildren {
		private List<XmlNode> nodes;

		public void adoptFrom(XmlNode n) {
			n.children.nodes().forEach(this::append);
		}

		public void append(Collection<XmlNode> childNodes) {
			childNodes.stream().forEach(n -> append(n));
		}

		public void append(XmlNode xmlNode) {
			XmlNode.this.node.appendChild(xmlNode.node);
			invalidate();
		}

		public List<XmlNode> byTag(String tag) {
			List<XmlNode> elements = elements();
			elements.removeIf(n -> !n.tagIs(tag));
			return elements;
		}

		public List<XmlNode> byTags(String... tags) {
			List<XmlNode> elements = elements();
			elements.removeIf(n -> !n.tagIsOneOf(tags));
			return elements;
		}

		public void clear() {
			nodes().stream().forEach(XmlNode::removeFromParent);
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

		public XmlNode importFrom(XmlNode n) {
			Node importNode = doc.domDoc().importNode(n.node, true);
			XmlNode imported = doc.nodeFor(importNode);
			append(imported);
			return imported;
		}

		public XmlNode importAsFirstChild(XmlNode n) {
			Node importNode = doc.domDoc().importNode(n.node, true);
			XmlNode imported = doc.nodeFor(importNode);
			insertAsFirstChild(imported);
			return imported;
		}

		public void insertAsFirstChild(XmlNode newChild) {
			invalidate();
			node.insertBefore(newChild.node, node.getFirstChild());
		}

		public void invalidate() {
			XmlNode.this.invalidate();
		}

		public boolean isFirstChild(XmlNode xmlNode) {
			return xmlNode != null && firstNode() == xmlNode.asXmlNode();
		}

		public boolean isLastChild(XmlNode node) {
			return node != null && lastNode() == node.asXmlNode();
		}

		public boolean isLastElementNode(XmlNode node) {
			return node != null && lastElementNode() == node.asXmlNode();
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

		public XmlNode
				soleElementExcludingProcessingInstructionsAndWhitespace() {
			List<XmlNode> nodes = nodes().stream().filter(
					n -> !n.isProcessingInstruction() && !n.ntc().isEmpty())
					.collect(Collectors.toList());
			return nodes.size() == 1 && nodes.get(0).isElement() ? nodes.get(0)
					: null;
		}

		public String textContent() {
			return SEUtilities.normalizeWhitespaceAndTrim(nodes().stream()
					.filter(XmlNode::isText).map(XmlNode::textContent)
					.collect(Collectors.joining()));
		}
	}

	public class XmlNodeCss {
		public void addBold() {
			addStyle("font-weight: bold");
		}

		public void addClass(String className) {
			addAttr("class", className, " ");
		}

		public void addStyle(String style) {
			addAttr("style", style, "; ");
		}

		public void displayNone() {
			addStyle("display:none");
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
			String xml = XmlNode.this.toString();
			if (xml.length() > 0) {
				xml = CommonUtils.trimToWsChars(xml, 50);
				if (out.length() > 0) {
					out += " : ";
				}
				out += xml;
			}
			return out;
		}
	}

	public class XmlNodeHtml {
		public void addClassName(String string) {
			Set<String> classes = Arrays.stream(attr("class").split(" ")).filter(Ax::notBlank)
					.collect(J8Utils.toLinkedHashSet());
			classes.add(string);
			setAttr("class", classes.stream().collect(Collectors.joining(" ")));
		}

		public Optional<XmlNode> ancestorBlock() {
			return ancestors().list().stream().filter(n -> n.html().isBlock())
					.findFirst();
		}

		public boolean hasClassName(String className) {
			return isElement() && Arrays.stream(attr("class").split(" "))
					.anyMatch(cn -> cn.equals(className));
		}

		public boolean isBlock() {
			return isElement() && XmlUtils.isBlockTag(name());
		}

		public boolean isOrContainsBlock(BlockResolver blockResolver) {
			if (blockResolver.isBlock(XmlNode.this)) {
				return true;
			}
			return children.flat().anyMatch(blockResolver::isBlock);
		}

		public void setStyleProperty(String key, String value) {
			StringMap styles = new StringMap();
			// t0tes naive
			if (has("style")) {
				String existing = attr("style");
				Arrays.stream(existing.split(";")).forEach(s -> {
					String[] parts = s.split(":");
					styles.put(parts[0], parts[1]);
				});
			}
			styles.put(key, value);
			setAttr("style",
					styles.entrySet().stream()
							.map(e -> Ax.format("%s:%s", e.getKey(),
									e.getValue()))
							.collect(Collectors.joining("; ")));
		}

		public XmlNodeHtmlTableBuilder tableBuilder() {
			return new XmlNodeHtmlTableBuilder(XmlNode.this);
		}

		public List<XmlNode> trs() {
			List<XmlNode> trs = children.byTag("TR");
			if (trs.isEmpty()) {
				trs = xpath("./TBODY/TR").nodes();
			}
			return trs;
		}

		public XmlNode addLink(String text, String href, String target) {
			return builder().tag("a").attr("href", href).attr("target", target)
					.text(text).append();
		}
	}

	public class XmlNodeRelative {
		public boolean hasNextSibling() {
			return node.getNextSibling() != null;
		}

		public boolean hasPreviousSibling() {
			return node.getPreviousSibling() != null;
		}

		public void insertAfterThis(XmlNode node) {
			parent().invalidate();
			parent().node.insertBefore(node.node,
					XmlNode.this.node.getNextSibling());
		}

		public void insertAsFirstChild(XmlNode other) {
			other.children.insertAsFirstChild(XmlNode.this);
		}

		public void insertBeforeThis(XmlNode node) {
			parent().invalidate();
			parent().node.insertBefore(node.node, XmlNode.this.node);
		}

		public XmlNode nextSibling() {
			return doc.nodeFor(node.getNextSibling());
		}

		public XmlNode nextSiblingElement() {
			Node cursor = node.getNextSibling();
			while (cursor != null) {
				XmlNode xnCursor = doc.nodeFor(cursor);
				if (xnCursor.isElement()) {
					return xnCursor;
				}
				cursor = cursor.getNextSibling();
			}
			return null;
		}

		public XmlNode nextSibOrParentSibNode() {
			if (hasNextSibling()) {
				return nextSibling();
			}
			XmlNode parent = parent();
			if (parent != null) {
				return parent.relative().nextSibOrParentSibNode();
			}
			return null;
		}

		public XmlNode previousSibling() {
			return doc.nodeFor(node.getPreviousSibling());
		}

		public XmlNode previousSiblingExcludingWhitespace() {
			XmlNode cursor = XmlNode.this;
			while (true) {
				cursor = cursor.relative().previousSibling();
				if (cursor == null) {
					return null;
				}
				if (cursor.isText() && cursor.isWhitespaceTextContent()) {
				} else {
					return cursor;
				}
			}
		}

		public XmlNode previousSibOrParentSibNode() {
			if (hasPreviousSibling()) {
				return previousSibling();
			} else {
				return parent();
			}
		}

		public XmlNode replaceWithTag(String tag) {
			XmlNode wrapper = doc.nodeFor(doc.domDoc().createElement(tag));
			replaceWith(wrapper);
			wrapper.copyAttributesFrom(XmlNode.this);
			wrapper.children.adoptFrom(XmlNode.this);
			return wrapper;
		}

		public XmlNode wrap(String tag) {
			XmlNode wrapper = doc.nodeFor(doc.domDoc().createElement(tag));
			replaceWith(wrapper);
			wrapper.children.append(XmlNode.this);
			wrapper.copyAttributesFrom(XmlNode.this);
			return wrapper;
		}
	}

	public class XmlNodeTree {
		private TreeWalker tw;

		public XmlNodeTree() {
			tw = ((DocumentTraversal) doc.domDoc()).createTreeWalker(
					doc.domDoc(), NodeFilter.SHOW_ALL, null, true);
			tw.setCurrentNode(node);
		}

		public XmlNode nextLogicalNode() {
			Node next = tw.nextNode();
			return doc.nodeFor(next);
		}
	}

	public class XmlNodeXpath {
		public String query;

		private OptimizingXpathEvaluator eval;

		private XpathHelper xh;

		public XmlNodeXpath() {
			if (doc == XmlNode.this) {
				xh = new XpathHelper(node);
			} else {
				xh = doc.xpath(query).xh;
			}
			eval = xh.createOptimisedEvaluator(node);
		}

		public boolean booleanValue() {
			return Boolean.valueOf(textOrEmpty());
		}

		public boolean contains() {
			return node() != null;
		}

		public XmlNode node() {
			Node domNode = eval.getNodeByXpath(query, node);
			return doc.nodeFor(domNode);
		}

		public List<XmlNode> nodes() {
			return stream().collect(Collectors.toList());
		}

		public Optional<XmlNode> optionalNode() {
			return Optional.ofNullable(node());
		}

		public boolean selfIs() {
			return XmlNode.this.parent().xpath(query).nodes()
					.contains(doc.nodeFor(node));
		}

		public Stream<XmlNode> stream() {
			List<Node> domNodes = eval.getNodesByXpath(query, node);
			return domNodes.stream().map(doc::nodeFor);
		}

		public String textOrEmpty() {
			return Optional.ofNullable(node()).map(XmlNode::textContent)
					.orElse("");
		}
	}

	public class XmlRange {
		private XmlNode end;

		public DocumentFragment asFragment() {
			return (DocumentFragment) toNode().node;
		}

		public void clearNodes() {
			List<XmlNode> kids = doc.getDocumentElementNode().children.flat()
					.collect(Collectors.toList());
			boolean inRange = false;
			for (XmlNode xmlNode : kids) {
				if (xmlNode == XmlNode.this) {
					inRange = true;
				}
				if (inRange) {
					xmlNode.removeFromParent();
				}
				if (xmlNode == end) {
					break;
				}
			}
		}

		public XmlRange end(XmlNode end) {
			this.end = end;
			return this;
		}

		public XmlRange endBefore(XmlNode endBefore) {
			TreeWalker tw = ((DocumentTraversal) doc.domDoc()).createTreeWalker(
					doc.domDoc(), NodeFilter.SHOW_ALL, null, true);
			tw.setCurrentNode(endBefore.node);
			tw.previousNode();
			end = doc.nodeFor(tw.getCurrentNode());
			return this;
		}

		public boolean isBefore(XmlNode other) {
			Range r1 = createRange();
			Range r2 = other.range().createRange();
			boolean result = r1.compareBoundaryPoints(Range.START_TO_START,
					r2) < 0;
			r1.detach();
			r2.detach();
			return result;
		}

		public XmlNode toNode() {
			Range range = createRange();
			DocumentFragment frag = range.cloneContents();
			range.detach();
			return doc.nodeFor(frag);
		}

		public XmlNode toWrappedNode(String tag) {
			Element wrapper = doc.domDoc().createElement(tag);
			Range range = createRange();
			DocumentFragment frag = range.cloneContents();
			range.detach();
			wrapper.appendChild(frag);
			return doc.nodeFor(wrapper);
		}

		private Range createRange() {
			Range range = ((DocumentRange) doc.domDoc()).createRange();
			range.setStartBefore(node);
			range.setEndAfter(end == null ? node : end.node);
			return range;
		}
	}

	public void setInnerXml(String xml) {
		XmlDoc importDoc = new XmlDoc(xml);
		children.importFrom(importDoc.getDocumentElementNode());
	}
}
