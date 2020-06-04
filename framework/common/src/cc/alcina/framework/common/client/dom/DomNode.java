package cc.alcina.framework.common.client.dom;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
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

import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.dom.DomEnvironment.StyleResolver;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.gwt.client.util.TextUtils;

public class DomNode {
	public static final transient String CONTEXT_DEBUG_SUPPORT = DomNode.class
			.getName() + ".CONTEXT_DEBUG_SUPPORT";

	/**
	 * Basically, don't use in a loop - more a debugging aid
	 */
	public static DomNode from(Node n) {
		DomDoc doc = null;
		if (n.getNodeType() == Node.DOCUMENT_NODE) {
			doc = new DomDoc((Document) n);
		} else {
			doc = new DomDoc(n.getOwnerDocument());
		}
		return doc.nodeFor(n);
	}

	protected Node node;

	public DomDoc doc;

	public DomNodeChildren children;

	private String normalisedTextContent;

	private StringMap attributes;

	private DomNodeXpath xpath;

	private DomNodeAncestors ancestors;

	private transient DomNodeReadonlyLookup lookup;

	public DomNode(DomNode from) {
		this(from.node, from.doc);
	}

	public DomNode(Node node, DomDoc xmlDoc) {
		this.node = node;
		this.doc = xmlDoc;
		this.children = new DomNodeChildren();
	}

	public DomNode addAttr(String name, String value, String separator) {
		String currentValue = attr(name);
		if (currentValue.length() > 0) {
			currentValue += separator;
		}
		currentValue += value;
		setAttr(name, value);
		return this;
	}

	public DomNodeAncestors ancestors() {
		if (ancestors == null) {
			ancestors = new DomNodeAncestors();
		}
		return ancestors;
	}

	public DomNode asDomNode() {
		return this.getClass() == DomNode.class ? this : doc.nodeFor(node);
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

	public boolean attrMatches(String attrName, String regex) {
		return attr(attrName).matches(regex);
	}

	public DomNodeBuilder builder() {
		return new DomNodeBuilder(this);
	}

	public boolean classIs(String value) {
		return attrIs("class", value);
	}

	public boolean classIsOneOf(String... names) {
		String className = getClassName();
		for (int idx = 0; idx < names.length; idx++) {
			if (className.equals(names[idx])) {
				return true;
			}
		}
		return false;
	}

	public DomNode clearAttributes() {
		attributes().keySet()
				.forEach(k -> node.getAttributes().removeNamedItem(k));
		return this;
	}

	public DomNode cloneNode(boolean deep) {
		return doc.nodeFor(node.cloneNode(deep));
	}

	public void copyAttributesFrom(DomNode xmlNode) {
		xmlNode.attributes().forEach((k, v) -> setAttr(k, v));
	}

	public DomNodeCss css() {
		return new DomNodeCss();
	}

	public DomNodeDebug debug() {
		return new DomNodeDebug();
	}

	public void deleteAttribute(String key) {
		node.getAttributes().removeNamedItem(key);
	}

	public int depth() {
		int depth = 0;
		DomNode cursor = this;
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

	public DomNode ensurePath(String path) {
		if (path.contains("/")) {
			DomNode cursor = this;
			for (String pathPart : path.split("/")) {
				cursor = cursor.ensurePath(pathPart);
			}
			return cursor;
		}
		List<DomNode> kids = children.byTag(path);
		if (kids.size() > 1) {
			throw new RuntimeException("Ambiguous path");
		}
		if (kids.size() == 1) {
			return kids.get(0);
		}
		return builder().tag(path).append();
	}

	public String fullToString() {
		return DomEnvironment.get().toXml(node).replace(CommonUtils.XML_PI, "");
	}

	public String getClassName() {
		return attr("class");
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

	public String href() {
		return attr("href");
	}

	public DomNodeHtml html() {
		return new DomNodeHtml();
	}

	public int indexInParentChildElements() {
		return parent() == null ? -1
				: parent().children.elements().indexOf(this);
	}

	public void invalidate() {
		children.nodes = null;
		normalisedTextContent = null;
	}

	public boolean isAncestorOf(DomNode cursor) {
		while (cursor != null) {
			if (cursor == this) {
				return true;
			}
			cursor = cursor.parent();
		}
		return false;
	}

	public boolean isAttachedToDocument() {
		return doc.getDocumentElementNode().isAncestorOf(this);
	}

	public boolean isComment() {
		return node.getNodeType() == Node.COMMENT_NODE;
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

	public boolean isWhitespaceOrEmptyTextContent() {
		return !isNonWhitespaceTextContent();
	}

	public boolean isWhitespaceTextContent() {
		return ntc().length() == 0;
	}

	public String logPretty() {
		return DomEnvironment.get().log(this, true);
	}

	public void logToFile() {
		try {
			DomEnvironment.get().log(this, false);
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

	public boolean normalisedTextMatches(String regex) {
		return ntc().matches(regex);
	}

	public String ntc() {
		if (normalisedTextContent == null) {
			normalisedTextContent = TextUtils
					.normalizeWhitespaceAndTrim(textContent());
		}
		return normalisedTextContent;
	}

	public DomNode parent() {
		return doc.nodeFor(node.getParentNode());
	}

	public String prettyToString() {
		return DomEnvironment.get().prettyToString(this);
	}

	public DomRange range() {
		return new DomRange();
	}

	public DomNodeRelative relative() {
		return new DomNodeRelative();
	}

	public void removeAttribute(String key) {
		node.getAttributes().removeNamedItem(key);
	}

	public void removeFromParent() {
		parent().invalidate();
		node.getParentNode().removeChild(node);
	}

	public void removeWhitespaceNodes() {
		children.flat().filter(n -> n.isText() && n.isWhitespaceTextContent())
				.forEach(DomNode::removeFromParent);
	}

	public void replaceWith(DomNode other) {
		relative().insertBeforeThis(other);
		removeFromParent();
	}

	public DomNode setAttr(String key, String value) {
		((Element) node).setAttribute(key, value);
		return this;
	}

	public DomNode setClassName(String className) {
		return setAttr("class", className);
	}

	public DomNode setId(String id) {
		return setAttr("id", id);
	}

	public void setInnerXml(String xml) {
		DomDoc importDoc = new DomDoc(xml);
		children.importFrom(importDoc.getDocumentElementNode());
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
		List<DomNode> nodes = children.nodes();
		if (nodes.stream().allMatch(DomNode::isElement)) {
			nodes.forEach(DomNode::removeFromParent);
			nodes = nodes.stream().sorted(Comparator.comparing(DomNode::name))
					.collect(Collectors.toList());
			children.append(nodes);
			nodes.forEach(DomNode::sort);
		}
	}

	public String streamNCleanForBrowserHtmlFragment() {
		return DomEnvironment.get().streamNCleanForBrowserHtmlFragment(node);
	}

	public void strip() {
		DocumentFragment frag = domDoc().createDocumentFragment();
		DomNode fragNode = new DomNode(frag, doc);
		fragNode.children.adoptFrom(this);
		relative().insertBeforeThis(fragNode);
		removeFromParent();
	}

	public DomNodeStyle style() {
		return new DomNodeStyle();
	}

	public boolean tagAndClassIs(String tagName, String className) {
		return tagIs(tagName) && classIs(className);
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

	public DocumentFragment toFragment() {
		DocumentFragment fragment = domDoc().createDocumentFragment();
		fragment.appendChild(domNode());
		return fragment;
	}

	@Override
	public String toString() {
		return CommonUtils.trimToWsChars(DomEnvironment.get().toXml(node)
				.replace(CommonUtils.XML_PI, ""), 255, true);
	}

	public String toXml() {
		return DomEnvironment.get().toXml(node);
	}

	public DomNodeTree tree() {
		return new DomNodeTree();
	}

	public DomNodeXpath xpath(String query) {
		return xpath(query, new Object[] {});
	}

	public DomNodeXpath xpath(String query, Object... args) {
		if (xpath == null) {
			xpath = new DomNodeXpath();
		}
		xpath.query = Ax.format(query, args);
		return xpath;
	}

	private ProcessingInstruction getProcessingInstruction() {
		return (ProcessingInstruction) node;
	}

	private DomNodeReadonlyLookup lookup() {
		if (lookup == null) {
			lookup = new DomNodeReadonlyLookup();
		}
		return lookup;
	}

	protected Document domDoc() {
		return node.getNodeType() == Node.DOCUMENT_NODE ? (Document) node
				: node.getOwnerDocument();
	}

	public class DomNodeAncestors {
		private boolean orSelf = false;

		public DomNode ancestorBefore(DomNode node) {
			DomNode cursor = DomNode.this;
			while (cursor != null) {
				DomNode parent = cursor.parent();
				if (parent == node) {
					return cursor;
				}
				cursor = parent;
			}
			return null;
		}

		public DomNode get(String... tags) {
			List<String> tagList = Arrays.asList(tags);
			DomNode cursor = getStartingCursor();
			while (cursor != null) {
				if (cursor.tagIsOneOf(tagList)) {
					return cursor;
				}
				cursor = cursor.parent();
			}
			return null;
		}

		public boolean has(DomNode test) {
			test = test.asDomNode();
			DomNode node = getStartingCursor();
			while (node != null) {
				if (node == test) {
					return true;
				}
				node = node.parent();
			}
			return false;
		}

		public boolean has(Predicate<DomNode> test) {
			DomNode node = getStartingCursor();
			while (node != null) {
				if (test.test(node)) {
					return true;
				}
				node = node.parent();
			}
			return false;
		}

		public boolean has(String... tags) {
			return get(tags) != null;
		}

		public boolean isFirstChild() {
			return parent().children.isFirstChild(DomNode.this);
		}

		public List<DomNode> list() {
			List<DomNode> result = new ArrayList<>();
			DomNode cursor = getStartingCursor();
			while (cursor != null) {
				result.add(cursor);
				cursor = cursor.parent();
			}
			return result;
		}

		public Optional<DomNode> match(Predicate<DomNode> predicate) {
			DomNode cursor = getStartingCursor();
			while (cursor != null) {
				if (predicate.test(cursor)) {
					return Optional.of(cursor);
				}
				cursor = cursor.parent();
			}
			return Optional.empty();
		}

		public DomNodeAncestors orSelf() {
			DomNodeAncestors ancestor = new DomNodeAncestors();
			ancestor.orSelf = true;
			return ancestor;
		}

		public boolean parentHasNoTextOrElementsBeforeThisChild() {
			for (DomNode node : parent().children.nodes()) {
				if (node == DomNode.this) {
					return true;
				}
				if (node.isText() || node.isElement()) {
					return false;
				}
			}
			return false;
		}

		public DomNode selfOrContainingElement() {
			return isElement() ? DomNode.this : parent();
		}

		private DomNode getStartingCursor() {
			return orSelf ? DomNode.this : DomNode.this.parent();
		}
	}

	public class DomNodeChildren {
		private List<DomNode> nodes;

		public void adoptFrom(DomNode n) {
			n.children.nodes().forEach(this::append);
		}

		public void append(Collection<DomNode> childNodes) {
			childNodes.stream().forEach(n -> append(n));
		}

		public void append(DomNode xmlNode) {
			DomNode.this.node.appendChild(xmlNode.node);
			invalidate();
		}

		public List<DomNode> byTag(String tag) {
			List<DomNode> elements = elements();
			elements.removeIf(n -> !n.tagIs(tag));
			return elements;
		}

		public List<DomNode> byTags(String... tags) {
			List<DomNode> elements = elements();
			elements.removeIf(n -> !n.tagIsOneOf(tags));
			return elements;
		}

		public void clear() {
			nodes().stream().forEach(DomNode::removeFromParent);
		}

		public boolean contains(DomNode n) {
			return nodes().contains(n);
		}

		public boolean contains(String tag) {
			return elements().stream().anyMatch(xn -> xn.tagIs(tag));
		}

		public List<DomNode> elements() {
			return nodes().stream().filter(DomNode::isElement)
					.collect(Collectors.toList());
		}

		public DomNode firstElement() {
			return CommonUtils.first(elements());
		}

		public DomNode firstNode() {
			return CommonUtils.first(nodes());
		}

		public DomNode firstNonElementChild() {
			return flatten().filter(n -> !n.isElement()).findFirst()
					.orElse(null);
		}

		public DomNode firstNonWhitespaceNode() {
			return nodes().stream()
					.filter(n -> !(n.isText() && n.isWhitespaceTextContent()))
					.findFirst().orElse(null);
		}

		public DomNode firstNonWhitespaceTextDescendant() {
			return flat()
					.filter(n -> n.isText() && !n.isWhitespaceTextContent())
					.findFirst().orElse(null);
		}

		public Stream<DomNode> flat() {
			return flatten();
		}

		public Stream<DomNode> flatten(String... tags) {
			List<String> tagArray = Arrays.asList(tags);
			Iterable<DomNode> iterable = () -> new DomTokenStream(DomNode.this);
			Stream<DomNode> targetStream = StreamSupport
					.stream(iterable.spliterator(), false);
			return targetStream.filter(t -> t.isText() || tagArray.isEmpty()
					|| t.tagIsOneOf(tagArray));
		}

		public DomNode importAsFirstChild(DomNode n) {
			Node importNode = doc.domDoc().importNode(n.node, true);
			DomNode imported = doc.nodeFor(importNode);
			insertAsFirstChild(imported);
			return imported;
		}

		public DomNode importFrom(DomNode n) {
			Node importNode = doc.domDoc().importNode(n.node, true);
			DomNode imported = doc.nodeFor(importNode);
			append(imported);
			return imported;
		}

		public void insertAsFirstChild(DomNode newChild) {
			invalidate();
			node.insertBefore(newChild.node, node.getFirstChild());
		}

		public void invalidate() {
			DomNode.this.invalidate();
		}

		public boolean isFirstChild(DomNode xmlNode) {
			return xmlNode != null && firstNode() == xmlNode.asDomNode();
		}

		public boolean isFirstNonWhitespaceChild(DomNode xmlNode) {
			return xmlNode != null && firstNonWhitespaceNode() != null
					&& firstNonWhitespaceNode().domNode() == xmlNode.domNode();
		}

		public boolean isFirstNonWhitespaceTextDescendant(DomNode xmlNode) {
			return xmlNode != null && firstNonWhitespaceTextDescendant() != null
					&& firstNonWhitespaceTextDescendant().domNode() == xmlNode
							.domNode();
		}

		public boolean isLastChild(DomNode node) {
			return node != null && lastNode() == node.asDomNode();
		}

		public boolean isLastElementNode(DomNode node) {
			return node != null && lastElementNode() == node.asDomNode();
		}

		public boolean isLastNonWhitespaceChild(DomNode node) {
			return node != null
					&& lastNonWhitespaceNode().domNode() == node.domNode();
		}

		public DomNode lastElementNode() {
			List<DomNode> nodes = nodes();
			for (int idx = nodes.size() - 1; idx >= 0; idx--) {
				DomNode kid = nodes.get(idx);
				if (kid.isElement()) {
					return kid;
				}
			}
			return null;
		}

		public DomNode lastNode() {
			return CommonUtils.last(nodes());
		}

		public DomNode lastNonEmptyTextNode() {
			List<DomNode> nodes = nodes();
			for (int idx = nodes.size() - 1; idx >= 0; idx--) {
				DomNode kid = nodes.get(idx);
				if (kid.isElement()
						|| kid.isText() && !kid.isEmptyTextContent()) {
					return kid;
				}
			}
			return null;
		}

		public DomNode lastNonWhitespaceNode() {
			return nodes().stream()
					.filter(n -> !(n.isText() && n.isWhitespaceTextContent()))
					.reduce((n1, n2) -> n2).orElse(null);
		}

		public DomNode lastNonWhitespaceTextNode() {
			return nodes().stream()
					.filter(n -> n.isText() && !n.isWhitespaceTextContent())
					.reduce((n1, n2) -> n2).orElse(null);
		}

		public List<DomNode> nodes() {
			if (nodes == null) {
				nodes = DomEnvironment.nodeListToList(node.getChildNodes())
						.stream().map(doc::nodeFor)
						.collect(Collectors.toList());
			}
			return nodes;
		}

		public boolean noElements() {
			return elements().size() == 0;
		}

		public DomNode soleElement() {
			List<DomNode> elts = elements();
			return elts.size() == 1 && nodes().size() == 1 ? elts.get(0) : null;
		}

		public boolean soleElement(String tag) {
			List<DomNode> elts = elements();
			return elts.size() == 1 && elts.get(0).tagIs(tag);
		}

		public Optional<DomNode>
				soleElementExcludingProcessingInstructionsAndWhitespace() {
			List<DomNode> nodes = nodes().stream().filter(
					n -> !n.isProcessingInstruction() && !n.ntc().isEmpty())
					.collect(Collectors.toList());
			return nodes.size() == 1 && nodes.get(0).isElement()
					? Optional.of(nodes.get(0))
					: Optional.empty();
		}

		public String textContent() {
			return TextUtils.normalizeWhitespaceAndTrim(nodes().stream()
					.filter(DomNode::isText).map(DomNode::textContent)
					.collect(Collectors.joining()));
		}
	}

	public class DomNodeCss {
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

	public class DomNodeDebug {
		public String shortRepresentation() {
			String out = "";
			DomNodeDebugSupport debugSupport = LooseContext
					.get(CONTEXT_DEBUG_SUPPORT);
			if (debugSupport != null) {
				out = debugSupport.shortRepresentation(DomNode.this);
			}
			String xml = DomNode.this.toString().replace("\n", "\\n");
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

	public interface DomNodeDebugSupport {
		public String shortRepresentation(DomNode node);
	}

	public class DomNodeHtml {
		public DomNode addLink(String text, String href, String target) {
			return builder().tag("a").attr("href", href).attr("target", target)
					.text(text).append();
		}

		public void appendStyleNode(String css) {
			head().builder().tag("style").text(css).append();
		}

		public DomNode body() {
			return xpath("//body").optionalNode()
					.orElse(xpath("//BODY").node());
		}

		public DomNode head() {
			return xpath("//head").node();
		}

		public DomNodeHtmlTableBuilder tableBuilder() {
			return new DomNodeHtmlTableBuilder(DomNode.this);
		}

		public List<DomNode> trs() {
			List<DomNode> trs = children.byTag("TR");
			if (trs.isEmpty()) {
				trs = xpath("./TBODY/TR").nodes();
			}
			return trs;
		}
	}

	public class DomNodeRelative {
		public boolean hasNextSibling() {
			return node.getNextSibling() != null;
		}

		public boolean hasPreviousSibling() {
			return node.getPreviousSibling() != null;
		}

		public void insertAfterThis(DomNode node) {
			parent().invalidate();
			parent().node.insertBefore(node.node,
					DomNode.this.node.getNextSibling());
		}

		public void insertAsFirstChildOf(DomNode other) {
			other.children.insertAsFirstChild(DomNode.this);
		}

		public void insertBeforeThis(DomNode node) {
			parent().invalidate();
			parent().node.insertBefore(node.node, DomNode.this.node);
		}

		public DomNode nextLogicalNode() {
			if (hasNextSibling()) {
				return nextSibling();
			}
			DomNode parent = parent();
			if (parent != null) {
				return parent.relative().nextLogicalNode();
			}
			return null;
		}

		public DomNode nextSibling() {
			return doc.nodeFor(node.getNextSibling());
		}

		public DomNode nextSiblingElement() {
			Node cursor = node.getNextSibling();
			while (cursor != null) {
				DomNode xnCursor = doc.nodeFor(cursor);
				if (xnCursor.isElement()) {
					return xnCursor;
				}
				cursor = cursor.getNextSibling();
			}
			return null;
		}

		public DomNode previousLogicalNode() {
			if (hasNextSibling()) {
				return nextSibling();
			}
			DomNode parent = parent();
			if (parent != null) {
				return parent.relative().nextLogicalNode();
			}
			return null;
		}

		public DomNode previousSibling() {
			return doc.nodeFor(node.getPreviousSibling());
		}

		public DomNode previousSiblingExcludingWhitespace() {
			DomNode cursor = DomNode.this;
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

		public DomNode previousSibOrParentSibNode() {
			if (hasPreviousSibling()) {
				return previousSibling();
			} else {
				return parent();
			}
		}

		public DomNode replaceWithTag(String tag) {
			DomNode wrapper = doc.nodeFor(doc.domDoc().createElement(tag));
			replaceWith(wrapper);
			wrapper.copyAttributesFrom(DomNode.this);
			wrapper.children.adoptFrom(DomNode.this);
			return wrapper;
		}

		public void swapWith(DomNode other) {
			DomNode parent = parent();
			DomNode insertBefore = nextSibling();
			other.relative().insertBeforeThis(DomNode.this);
			if (insertBefore != null) {
				insertBefore.relative().insertBeforeThis(other);
			} else {
				parent.children.append(other);
			}
		}

		public DomNode wrap(String tag) {
			DomNode wrapper = doc.nodeFor(doc.domDoc().createElement(tag));
			replaceWith(wrapper);
			wrapper.children.append(DomNode.this);
			wrapper.copyAttributesFrom(DomNode.this);
			return wrapper;
		}
	}

	public class DomNodeStyle {
		public DomNode addClassName(String string) {
			Set<String> classes = new LinkedHashSet<>();
			Arrays.stream(attr("class").split(" ")).filter(Ax::notBlank)
					.forEach(classes::add);
			classes.add(string);
			setAttr("class", classes.stream().collect(Collectors.joining(" ")));
			return DomNode.this;
		}

		public Optional<DomNode> containingBlock() {
			return ancestors().orSelf().list().stream()
					.filter(n -> n.style().isBlock()).findFirst();
		}

		public boolean hasClassName(String className) {
			return isElement() && Arrays.stream(attr("class").split(" "))
					.anyMatch(cn -> cn.equals(className));
		}

		public boolean isBlock() {
			return isElement() && DomEnvironment.contextBlockResolver()
					.isBlock(DomNode.this);
		}

		public boolean isBold() {
			return DomEnvironment.contextBlockResolver()
					.isBold(ancestors().selfOrContainingElement());
		}

		public boolean isItalic() {
			return DomEnvironment.contextBlockResolver()
					.isItalic(ancestors().selfOrContainingElement());
		}

		public boolean isOrContainsBlock(StyleResolver blockResolver) {
			if (blockResolver.isBlock(DomNode.this)) {
				return true;
			}
			return children.flat().anyMatch(blockResolver::isBlock);
		}

		public DomNode setClassName(String string) {
			setAttr("class", string);
			return DomNode.this;
		}

		public void setProperty(String key, String value) {
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
					styles.entrySet().stream().map(
							e -> Ax.format("%s:%s", e.getKey(), e.getValue()))
							.collect(Collectors.joining("; ")));
		}
	}

	public class DomNodeTree {
		private TreeWalker tw;

		public DomNodeTree() {
			tw = ((DocumentTraversal) doc.domDoc()).createTreeWalker(
					doc.domDoc(), NodeFilter.SHOW_ALL, null, true);
			tw.setCurrentNode(node);
		}

		public DomNode currentNode() {
			return doc.nodeFor(tw.getCurrentNode());
		}

		public List<DomNode> listUntil(DomNode end, boolean endInclusive) {
			List<DomNode> result = new ArrayList<>();
			while (currentNode() != end) {
				result.add(currentNode());
				nextLogicalNode();
			}
			if (endInclusive) {
				result.add(end);
			}
			return result;
		}

		public DomNode nextLogicalNode() {
			Node next = tw.nextNode();
			return doc.nodeFor(next);
		}

		public String nextNonWhitespaceText() {
			return nextNonWhitespaceTextNode().map(DomNode::ntc).orElse(null);
		}

		public Optional<DomNode> nextNonWhitespaceTextNode() {
			while (true) {
				Node next = tw.nextNode();
				if (next == null) {
					return Optional.empty();
				}
				DomNode xNext = doc.nodeFor(next);
				if (xNext.isText() && xNext.isNonWhitespaceTextContent()) {
					return Optional.of(xNext);
				}
			}
		}

		public String previousNonWhitespaceText() {
			return previousNonWhitespaceTextNode().map(DomNode::ntc)
					.orElse(null);
		}

		public Optional<DomNode> previousNonWhitespaceTextNode() {
			while (true) {
				Node previous = tw.previousNode();
				if (previous == null) {
					return Optional.empty();
				}
				DomNode xPrevious = doc.nodeFor(previous);
				if (xPrevious.isText()
						&& xPrevious.isNonWhitespaceTextContent()) {
					return Optional.of(xPrevious);
				}
			}
		}
	}

	public class DomNodeXpath {
		public String query;

		private XpathEvaluator evaluator;

		public DomNodeXpath() {
			if (doc == DomNode.this) {
				evaluator = DomEnvironment.get()
						.createXpathEvaluator(DomNode.this, null);
			} else {
				evaluator = DomEnvironment.get().createXpathEvaluator(
						DomNode.this, doc.xpath("").getEvaluator());
			}
		}

		public boolean booleanValue() {
			return Boolean.valueOf(textOrEmpty());
		}

		public void forEach(Consumer<DomNode> consumer) {
			stream().forEach(consumer);
		}

		public XpathEvaluator getEvaluator() {
			return this.evaluator;
		}

		public boolean matchExists() {
			return node() != null;
		}

		/**
		 * Warning - uses 'find', not 'matches'
		 */
		public Stream<DomNode> matching(String pattern) {
			RegExp regex = RegExp.compile(pattern);
			return stream().filter(n -> regex.exec(n.ntc()) != null);
		}

		public Stream<DomNode> matchingAttr(String attrName, String pattern) {
			RegExp regex = RegExp.compile(pattern);
			return stream().filter(n -> {
				String attr = n.attr(attrName);
				MatchResult matchResult = regex.exec(attr);
				return matchResult != null
						&& matchResult.getGroup(0).equals(attr);
			});
		}

		public DomNode node() {
			if (doc.isReadonly() && lookup().handlesXpath(query)) {
				return stream().findFirst().orElse(null);
			} else {
				Node domNode = evaluator.getNodeByXpath(query, node);
				return doc.nodeFor(domNode);
			}
		}

		public List<DomNode> nodes() {
			return stream().collect(Collectors.toList());
		}

		public Optional<DomNode> optionalNode() {
			return Optional.ofNullable(node());
		}

		public boolean selfIs() {
			return DomNode.this.parent().xpath(query).nodes()
					.contains(doc.nodeFor(node));
		}

		public Stream<DomNode> stream() {
			if (doc.isReadonly() && lookup().handlesXpath(query)) {
				return lookup.stream(query);
			} else {
				List<Node> domNodes = evaluator.getNodesByXpath(query, node);
				return domNodes.stream().map(doc::nodeFor);
			}
		}

		public String textOrEmpty() {
			return Optional.ofNullable(node()).map(DomNode::textContent)
					.orElse("");
		}
	}

	public class DomRange {
		private DomNode end;

		private boolean startAfterThis;

		private boolean endBefore;

		public DocumentFragment asFragment() {
			return (DocumentFragment) toNode().node;
		}

		public void clearContents() {
			List<DomNode> kids = doc.getDocumentElementNode().children.flat()
					.collect(Collectors.toList());
			boolean inRange = false;
			List<DomNode> toRemoveNodes = new ArrayList<>();
			Objects.requireNonNull(end);
			DomNode keepAncestorsOf = end;
			if (!endBefore) {
				TreeWalker tw = ((DocumentTraversal) doc.domDoc())
						.createTreeWalker(doc.domDoc(), NodeFilter.SHOW_ALL,
								null, true);
				tw.setCurrentNode(end.node);
				Node keep = tw.nextNode();
				keepAncestorsOf = keep == null ? null : doc.nodeFor(keep);
			}
			for (DomNode cursor : kids) {
				if (cursor == DomNode.this) {
					inRange = true;
					if (startAfterThis) {
						continue;
					}
				}
				if (cursor == end && endBefore) {
					break;
				}
				if (inRange) {
					if (keepAncestorsOf == null
							|| !cursor.isAncestorOf(keepAncestorsOf)) {
						cursor.removeFromParent();
					}
				}
				if (cursor == end) {
					break;
				}
			}
		}

		public DomRange end(DomNode end) {
			this.end = end;
			return this;
		}

		public DomRange endBefore(DomNode endBefore) {
			this.end = endBefore;
			this.endBefore = true;
			return this;
		}

		public boolean isBefore(DomNode other) {
			Range r1 = createRange();
			Range r2 = other.range().createRange();
			boolean result = r1.compareBoundaryPoints(Range.START_TO_START,
					r2) < 0;
			r1.detach();
			r2.detach();
			return result;
		}

		public boolean isEndAfter(DomNode other) {
			Range r1 = createRange();
			Range r2 = other.range().createRange();
			boolean result = r1.compareBoundaryPoints(Range.END_TO_END, r2) > 0;
			r1.detach();
			r2.detach();
			return result;
		}

		public DomRange startAfterThis() {
			startAfterThis = true;
			return this;
		}

		public DomNode toNode() {
			Range range = createRange();
			DocumentFragment frag = range.cloneContents();
			range.detach();
			return doc.nodeFor(frag);
		}

		public DomNode toWrappedNode(String tag, boolean clone) {
			Element wrapper = doc.domDoc().createElement(tag);
			Range range = createRange();
			DocumentFragment frag = range.cloneContents();
			range.detach();
			wrapper.appendChild(frag);
			if (!clone) {
				clearContents();
			}
			return doc.nodeFor(wrapper);
		}

		private Range createRange() {
			Range range = ((DocumentRange) doc.domDoc()).createRange();
			if (startAfterThis) {
				range.setStartAfter(node);
			} else {
				range.setStartBefore(node);
			}
			if (endBefore) {
				range.setEndBefore(end.node);
			} else {
				range.setEndAfter(end == null ? node : end.node);
			}
			return range;
		}
	}

	public interface XpathEvaluator {
		Node getNodeByXpath(String query, Node node);

		List<Node> getNodesByXpath(String query, Node node);
	}

	class DomNodeReadonlyLookup {
		public DomNodeReadonlyLookup() {
		}

		public boolean handlesXpath(String xpath) {
			DomNodeReadonlyLookupQuery query = parse(xpath);
			return query.valid && (query.immediateChild || DomNode.this == doc);
		}

		DomNodeReadonlyLookupQuery parse(String xpath) {
			DomNodeReadonlyLookupQuery query = new DomNodeReadonlyLookupQuery();
			String xmlIdentifierChars = "[a-zA-Z\\-_0-9\\.]+";
			String tagOnlyRegex = Ax.format("//(%s)", xmlIdentifierChars);
			String tagAttrNodeRegex = Ax.format("//(%s)/@(%s)",
					xmlIdentifierChars, xmlIdentifierChars);
			String tagAttrValueRegex = Ax.format("//(%s)/\\[@(%s)='(%s)'\\]",
					xmlIdentifierChars, xmlIdentifierChars, xmlIdentifierChars);
			String immediateChildRegex = xmlIdentifierChars;
			if (xpath.matches(immediateChildRegex)) {
				query.tag = xpath;
				query.valid = true;
				query.immediateChild = true;
			} else if (xpath.matches(tagOnlyRegex)) {
				query.tag = xpath.replaceFirst(tagOnlyRegex, "$1");
				query.valid = true;
			} else if (xpath.matches(tagAttrNodeRegex)) {
				query.tag = xpath.replaceFirst(tagAttrNodeRegex, "$1");
				String attrName = xpath.replaceFirst(tagAttrNodeRegex, "$2");
				query.predicate = node -> node.has(attrName);
				query.map = node -> node.doc.nodeFor(
						((Element) node.domNode()).getAttributeNode(attrName));
				query.valid = true;
			} else if (xpath.matches(tagAttrValueRegex)) {
				query.tag = xpath.replaceFirst(tagAttrValueRegex, "$1");
				String attrName = xpath.replaceFirst(tagAttrValueRegex, "$2");
				String attrValue = xpath.replaceFirst(tagAttrValueRegex, "$3");
				query.predicate = node -> node.attrIs(attrName, attrValue);
				query.valid = true;
			}
			return query;
		}

		Stream<DomNode> stream(String xpath) {
			DomNodeReadonlyLookupQuery query = parse(xpath);
			if (query.immediateChild) {
				return children.byTag(query.tag).stream()
						.filter(query.predicate).map(query.map);
			} else {
				return doc.byTag().getAndEnsure(query.tag).stream()
						.filter(query.predicate).map(query.map);
			}
		}

		class DomNodeReadonlyLookupQuery {
			boolean immediateChild;

			String tag;

			Predicate<DomNode> predicate = node -> true;

			Function<DomNode, DomNode> map = node -> node;

			boolean valid = false;
		}
	}
}
