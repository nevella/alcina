package cc.alcina.framework.common.client.dom;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;
import org.w3c.dom.ranges.DocumentRange;
import org.w3c.dom.ranges.Range;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.TreeWalker;

import com.google.common.base.Preconditions;
import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.dom.DomEnvironment.StyleResolver;
import cc.alcina.framework.common.client.dom.DomNode.DomNodeReadonlyLookup.DomNodeReadonlyLookupQuery;
import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.common.client.util.TextUtils;
import cc.alcina.framework.common.client.util.traversal.DepthFirstTraversal;
import cc.alcina.framework.gwt.client.util.StyleUtil;

/**
 * <p>
 * This class and supporting classes in the package are a fluent wrapper around
 * the standard JDK org.w3c.dom XML model, with optimised xpath support.
 *
 * <p>
 * Many operations can be performed as streams, and the {@link DomNode} class
 * can be converted to or from the standard org.w3c.dom.Node
 *
 * <p>
 * As an example:
 *
 *
 * <code>
 * <pre>
DomDocument.from(
"&lt;html>&lt;head/><body>&lt;div class=
'class-1'>haggle&lt;/div>&lt;/body>&lt;/html>")
.xpath("//div[@class='class-1']").stream()
.filter(node -> node.textMatches("hag.*")).map(DomNode::ntc)
.forEach(System.out::println);

 (output: haggle)
 </pre>

</code>
 *
 * is a significant ease-of-use improvement on a similar operation with the w3c
 * DOM classes. There's also a fluent node builder - DomNode.builder() - and
 * other manipulation commands such as DomNode.strip()
 *
 *
 *
 */
public class DomNode {
	public static final transient String CONTEXT_DEBUG_SUPPORT = DomNode.class
			.getName() + ".CONTEXT_DEBUG_SUPPORT";

	private static Map<String, DomNodeReadonlyLookupQuery> queryLookup = Collections
			.synchronizedMap(new LinkedHashMap<>());

	public static DomNode from(Node node) {
		Document document = null;
		DomDocument doc = null;
		if (node.getNodeType() == Node.DOCUMENT_NODE) {
			document = (Document) node;
		} else {
			document = node.getOwnerDocument();
		}
		return DomDocument.from(document).nodeFor(node);
	}

	protected Node node;

	public DomDocument document;

	public DomNodeChildren children;

	private StringMap attributes;

	private DomNodeXpath xpath;

	private transient DomNodeReadonlyLookup lookup;

	private List<Location> locations = null;

	public DomNode(DomNode from) {
		this(from.node, from.document);
	}

	public DomNode(Node node, DomDocument xmlDoc) {
		this.node = node;
		this.document = xmlDoc;
		this.children = new DomNodeChildren();
	}

	/**
	 * Add or remove <code>value</code> to the attribute specified by
	 * <code>name</code>, with the parts of the attribute string separated by
	 * <code>separator</code>
	 */
	public DomNode putAttrPart(String name, String value, String separator,
			boolean add) {
		String currentValue = attr(name);
		Set<String> parts = Arrays.stream(currentValue.split(separator))
				.collect(AlcinaCollectors.toLinkedHashSet());
		if (add) {
			parts.add(value);
		} else {
			parts.remove(value);
		}
		if (parts.isEmpty()) {
			removeAttribute(name);
		} else {
			setAttr(name,
					parts.stream().collect(Collectors.joining(separator)));
		}
		return this;
	}

	public void addClassName(String className) {
		setAttr("class", attr("class") + " " + className);
	}

	public DomNodeAncestors ancestors() {
		return new DomNodeAncestors();
	}

	public void appendTo(DomNode newParent) {
		newParent.children.append(this);
	}

	public DomNode asDomNode() {
		return this.getClass() == DomNode.class ? this : document.nodeFor(node);
	}

	public Location asLocation() {
		if (locations != null) {
			return locations.get(0);
		}
		Location location = document.locations().asLocation(this);
		locations = new ArrayList<>();
		locations.add(location);
		return location;
	}

	public Location.Range asRange() {
		return isAttached() ? document.locations().asRange(this) : null;
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

	public DomNode attrNode(String name) {
		return document.nodeFor(node.getAttributes().getNamedItem(name));
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

	public boolean classMatches(String regex) {
		return attr("class").matches(regex);
	}

	public DomNode clearAttributes() {
		attributes().keySet()
				.forEach(k -> node.getAttributes().removeNamedItem(k));
		return this;
	}

	public DomNode cloneNode(boolean deep) {
		return document.nodeFor(node.cloneNode(deep));
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

	/**
	 * Returns a stream of nodes, in depth-first order, rooted at this node,
	 * excluding this node
	 */
	public Stream<DomNode> descendants() {
		return stream(false);
	}

	protected Document domDoc() {
		return node.getNodeType() == Node.DOCUMENT_NODE ? (Document) node
				: node.getOwnerDocument();
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

	public DomNodeType getDomNodeType() {
		return DomNodeType.fromW3cNode(node);
	}

	private ProcessingInstruction getProcessingInstruction() {
		return (ProcessingInstruction) node;
	}

	public com.google.gwt.dom.client.Node gwtNode() {
		return (com.google.gwt.dom.client.Node) node;
	}

	public com.google.gwt.dom.client.Element gwtElement() {
		return (com.google.gwt.dom.client.Element) node;
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

	public boolean isAncestorOf(DomNode cursor) {
		while (cursor != null) {
			if (cursor.w3cNode() == this.w3cNode()) {
				return true;
			}
			cursor = cursor.parent();
		}
		return false;
	}

	public boolean isAttached() {
		return isGwtNode() ? gwtNode().isAttached()
				: document.getDocumentElementNode().isAncestorOf(this);
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

	public boolean isGwtNode() {
		return node instanceof com.google.gwt.dom.client.Node;
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

	private DomNodeReadonlyLookup lookup() {
		if (lookup == null) {
			lookup = new DomNodeReadonlyLookup();
		}
		return lookup;
	}

	public String name() {
		return node.getNodeName();
	}

	/** Note that this is case-sensitive, tagIs() is case-insensitive */
	public boolean nameIs(String name) {
		return name().equals(name);
	}

	public boolean normalisedTextMatches(String regex) {
		return ntc().matches(regex);
	}

	public String ntc() {
		return TextUtils.normalizeWhitespaceAndTrim(textContent());
	}

	public DomNode parent() {
		return document.nodeFor(node.getParentNode());
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
		if (node.getAttributes().getNamedItem(key) != null) {
			node.getAttributes().removeNamedItem(key);
		}
	}

	public void removeFromParent() {
		node.getParentNode().removeChild(node);
	}

	public void removeWhitespaceNodes() {
		descendants().filter(n -> n.isText() && n.isWhitespaceTextContent())
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
		DomDocument importDoc = DomDocument.from(xml);
		children.importFrom(importDoc.getDocumentElementNode());
	}

	public void setText(String text) {
		if (isText()) {
			((Text) node).setData(text);
		} else {
			if (children.noElements() && descendants()
					.noneMatch(DomNode::isProcessingInstruction)) {
				node.setTextContent(text);
			} else {
				throw new RuntimeException("node has child elements");
			}
		}
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

	/**
	 * Returns a stream of nodes, in depth-first order, rooted at this node
	 */
	public Stream<DomNode> stream() {
		return stream(true);
	}

	/*
	 * Returns a stream of nodes, in depth-first order, rooted at this node.
	 *
	 * If includingSelf is false, returns only the descendants
	 */
	private Stream<DomNode> stream(boolean includingSelf) {
		DomTokenStream domTokenStream = new DomTokenStream(DomNode.this);
		if (!includingSelf) {
			domTokenStream.next();
		}
		Iterable<DomNode> iterable = () -> domTokenStream;
		return StreamSupport.stream(iterable.spliterator(), false);
	}

	public String streamNCleanForBrowserHtmlFragment() {
		return DomEnvironment.get().streamNCleanForBrowserHtmlFragment(node);
	}

	/**
	 * 
	 * @return the first child node if any, or null
	 */
	public DomNode strip() {
		List<DomNode> nodes = children.nodes();
		nodes.forEach(relative()::insertBeforeThis);
		removeFromParent();
		return Ax.first(nodes);
	}

	public DomNodeStyle style() {
		return new DomNodeStyle();
	}

	public boolean tagAndClassIs(String tagName, String className) {
		return tagIs(tagName) && classIs(className);
	}

	/** Note that this is case-insensitive, nameIs() is case-sensitive */
	public boolean tagIs(String tagName) {
		return isElement()
				&& w3cElement().getTagName().equalsIgnoreCase(tagName)
				|| isProcessingInstruction() && getProcessingInstruction()
						.getNodeName().equalsIgnoreCase(tagName);
	}

	public boolean tagIsOneOf(Collection<String> tags) {
		return isElement()
				&& tags.stream().anyMatch(t -> t.equalsIgnoreCase(name()));
	}

	public boolean tagIsOneOf(String... tags) {
		if (isElement() || isProcessingInstruction()) {
			for (int idx = 0; idx < tags.length; idx++) {
				if (name().equalsIgnoreCase(tags[idx])) {
					return true;
				}
			}
		}
		return false;
	}

	public DomNodeText text() {
		return new DomNodeText();
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
		fragment.appendChild(w3cNode());
		return fragment;
	}

	@Override
	public String toString() {
		return CommonUtils.trimToWsChars(DomEnvironment.get().toXml(node)
				.replace(CommonUtils.XML_PI, ""), 255, true);
	}

	public String toTagClassName() {
		return has("class") ? Ax.format("%s.%s", name(), attr("class"))
				: name();
	}

	public String toXml() {
		return DomEnvironment.get().toXml(node);
	}

	public DomNodeTree tree() {
		return new DomNodeTree();
	}

	public Element w3cElement() {
		return (Element) node;
	}

	public Node w3cNode() {
		return node;
	}

	public Text w3cText() {
		return (Text) node;
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

	public static class DocumentOrderComparator implements Comparator<DomNode> {
		DomEnvironment domEnvironment = DomEnvironment.get();

		@Override
		public int compare(DomNode o1, DomNode o2) {
			if (o1 == o2) {
				return 0;
			}
			return domEnvironment.isEarlierThan(o1.w3cNode(), o2.w3cNode()) ? -1
					: 1;
		}
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

		public DomNode tagAndClassIs(String tagName, String className) {
			DomNode cursor = getStartingCursor();
			while (cursor != null) {
				if (cursor.tagAndClassIs(tagName, className)) {
					return cursor;
				}
				cursor = cursor.parent();
			}
			return null;
		}

		private DomNode getStartingCursor() {
			return orSelf ? DomNode.this : DomNode.this.parent();
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

		public Stream<DomNode> stream() {
			return list().stream();
		}
	}

	public class DomNodeChildren {
		List<DomNode> nodes;

		public void adoptFrom(DomNode n) {
			n.children.nodes().forEach(this::append);
		}

		public void append(Collection<DomNode> childNodes) {
			childNodes.stream().forEach(n -> append(n));
		}

		public void append(DomNode xmlNode) {
			DomNode.this.node.appendChild(xmlNode.node);
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
			return descendants().filter(n -> !n.isElement()).findFirst()
					.orElse(null);
		}

		public DomNode firstNonWhitespaceNode() {
			return nodes().stream()
					.filter(n -> !(n.isText() && n.isWhitespaceTextContent()))
					.findFirst().orElse(null);
		}

		public DomNode firstNonWhitespaceTextDescendant() {
			return stream()
					.filter(n -> n.isText() && !n.isWhitespaceTextContent())
					.findFirst().orElse(null);
		}

		public DomNode importAsFirstChild(DomNode n) {
			return importAsFirstChild(n, true);
		}

		public DomNode importAsFirstChild(DomNode n, boolean deep) {
			Node importNode = document.domDoc().importNode(n.node, deep);
			DomNode imported = document.nodeFor(importNode);
			insertAsFirstChild(imported);
			return imported;
		}

		public DomNode importFrom(DomNode n) {
			return importFrom(n, true);
		}

		public DomNode importFrom(DomNode n, boolean deep) {
			Node importNode = document.domDoc().importNode(n.node, deep);
			DomNode imported = document.nodeFor(importNode);
			append(imported);
			return imported;
		}

		public void insertAsFirstChild(DomNode newChild) {
			node.insertBefore(newChild.node, node.getFirstChild());
		}

		public boolean isFirstChild(DomNode xmlNode) {
			return xmlNode != null && firstNode() == xmlNode.asDomNode();
		}

		public boolean isFirstNonWhitespaceChild(DomNode xmlNode) {
			return xmlNode != null && firstNonWhitespaceNode() != null
					&& firstNonWhitespaceNode().w3cNode() == xmlNode.w3cNode();
		}

		public boolean isFirstNonWhitespaceTextDescendant(DomNode xmlNode) {
			return xmlNode != null && firstNonWhitespaceTextDescendant() != null
					&& firstNonWhitespaceTextDescendant().w3cNode() == xmlNode
							.w3cNode();
		}

		public boolean isLastChild(DomNode node) {
			return node != null && lastNode() == node.asDomNode();
		}

		public boolean isLastElementNode(DomNode node) {
			return node != null && lastElementNode() == node.asDomNode();
		}

		public boolean isLastNonWhitespaceChild(DomNode node) {
			return node != null
					&& lastNonWhitespaceNode().w3cNode() == node.w3cNode();
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
			if (this.nodes != null) {
				return this.nodes;
			}
			List<DomNode> nodes = DomEnvironment
					.nodeListToList(node.getChildNodes()).stream()
					.map(document::nodeFor).collect(Collectors.toList());
			if (document.isReadonly()) {
				this.nodes = nodes;
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

		/**
		 * Add or remove <code>className</code> to/from the class attribute
		 */
		public void putClass(String className, boolean add) {
			putAttrPart("class", className, " ", add);
		}

		public void addStyle(String style) {
			putAttrPart("style", style, "; ", true);
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
			appendStyleNode(css, false);
		}

		public void appendStyleNode(String css, boolean wrapStyleInCdata) {
			DomNode node = head().builder().tag("style").append();
			if (wrapStyleInCdata) {
				node.builder().cdata().text(css).append();
			} else {
				node.setText(css);
			}
		}

		public void appendScriptNode(String js, boolean wrapInCdata) {
			DomNode node = head().builder().tag("script").append();
			if (wrapInCdata) {
				node.builder().cdata().text(js).append();
			} else {
				node.setText(js);
			}
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

		public String toHtml() {
			return toHtml(true);
		}

		public String toHtml(boolean pretty) {
			return DomEnvironment.get().toHtml(document, pretty);
		}

		public List<DomNode> trs() {
			List<DomNode> trs = children.byTag("TR");
			if (trs.isEmpty()) {
				trs = xpath("./TBODY/TR").nodes();
			}
			return trs;
		}

		public Optional<DomNode> getContainingBlock() {
			return style().containingBlock();
		}
	}

	class DomNodeReadonlyLookup {
		public DomNodeReadonlyLookup() {
		}

		public boolean handlesXpath(String xpath) {
			DomNodeReadonlyLookupQuery query = parse(xpath);
			return query.valid && (query.immediateChild || query.grandChild
					|| DomNode.this == document
					|| DomNode.this == document.getDocumentElementNode());
		}

		private String normaliseTag(String tag) {
			return tag.replace("xhtml:", "");
		}

		DomNodeReadonlyLookupQuery parse(String xpath) {
			return queryLookup.computeIfAbsent(xpath, this::parse0);
		}

		// yes, this could be optimised and made prettier. but works for 98% of
		// cases I care about, with no real regex compilation performance issues
		//
		// Anything to avoid going to xalan...
		private DomNodeReadonlyLookupQuery parse0(String xpath) {
			DomNodeReadonlyLookupQuery query = new DomNodeReadonlyLookupQuery();
			String xmlIdentifierChars = "[a-zA-Z\\-_0-9\\.:]+";
			String tagOnlyRegex = Ax.format("//(%s)", xmlIdentifierChars);
			String tagAttrNodeRegex = Ax.format("//(%s)/@(%s)",
					xmlIdentifierChars, xmlIdentifierChars);
			String tagIdNodeRegex = Ax.format("//(%s)\\[@id='(%s)'\\]",
					xmlIdentifierChars, xmlIdentifierChars);
			String tagIdsNodeRegex = Ax.format(
					"//(%s)\\[@id='(%s)' or @id='(%s)'\\]", xmlIdentifierChars,
					xmlIdentifierChars, xmlIdentifierChars);
			String tagIdChildRegex = Ax.format(
					"//(%s)\\[@id='(%s)'\\]/(%s)(?:\\[(%s)\\])?",
					xmlIdentifierChars, xmlIdentifierChars, xmlIdentifierChars,
					xmlIdentifierChars);
			String tagIdDescendantRegex = Ax.format(
					"//(%s)\\[@id='(%s)'\\]//(%s)(?:\\[(%s)\\])?",
					xmlIdentifierChars, xmlIdentifierChars, xmlIdentifierChars,
					xmlIdentifierChars);
			String tagIdDescendantAttrValueDescendantTagRegex = Ax.format(
					"//(%s)\\[@id='(%s)'\\]//(%s)/?\\[@(%s)='(%s)'\\]/(%s)",
					xmlIdentifierChars, xmlIdentifierChars, xmlIdentifierChars,
					xmlIdentifierChars, xmlIdentifierChars, xmlIdentifierChars);
			String tagAttrValueRegex = Ax.format("//(%s)/?\\[@(%s)='(%s)'\\]",
					xmlIdentifierChars, xmlIdentifierChars, xmlIdentifierChars);
			String immediateChildRegex = xmlIdentifierChars;
			String grandChildRegex = Ax.format("(%s)/(%s)", xmlIdentifierChars,
					xmlIdentifierChars);
			if (xpath.matches(immediateChildRegex)) {
				query.tag = xpath;
				query.valid = true;
				query.immediateChild = true;
			} else if (xpath.matches(grandChildRegex)) {
				query.tag = xpath.replaceFirst(grandChildRegex, "$1");
				query.grandChildTag = xpath.replaceFirst(grandChildRegex, "$2");
				query.valid = true;
				query.grandChild = true;
			} else if (xpath.matches(tagOnlyRegex)) {
				query.tag = xpath.replaceFirst(tagOnlyRegex, "$1");
				query.valid = true;
			} else if (xpath.matches(tagAttrNodeRegex)) {
				query.tag = xpath.replaceFirst(tagAttrNodeRegex, "$1");
				String attrName = xpath.replaceFirst(tagAttrNodeRegex, "$2");
				query.predicate = node -> node.has(attrName);
				query.map = node -> Stream.of(node.document.nodeFor(
						((Element) node.w3cNode()).getAttributeNode(attrName)));
				query.valid = true;
			} else if (xpath.matches(tagAttrValueRegex)) {
				query.tag = xpath.replaceFirst(tagAttrValueRegex, "$1");
				String attrName = xpath.replaceFirst(tagAttrValueRegex, "$2");
				String attrValue = xpath.replaceFirst(tagAttrValueRegex, "$3");
				query.predicate = node -> node.attrIs(attrName, attrValue);
				query.valid = true;
			} else if (xpath.matches(tagIdNodeRegex)) {
				query.tag = xpath.replaceFirst(tagIdNodeRegex, "$1");
				query.id = xpath.replaceFirst(tagIdNodeRegex, "$2");
				query.predicate = node -> node.tagIs(query.tag);
				query.valid = true;
			} else if (xpath.matches(tagIdsNodeRegex)) {
				query.tag = xpath.replaceFirst(tagIdsNodeRegex, "$1");
				query.id = xpath.replaceFirst(tagIdsNodeRegex, "$2");
				query.id2 = xpath.replaceFirst(tagIdsNodeRegex, "$3");
				query.predicate = node -> node.tagIs(query.tag);
				query.valid = true;
			} else if (xpath.matches(tagIdChildRegex)) {
				query.tag = xpath.replaceFirst(tagIdChildRegex, "$1");
				query.id = xpath.replaceFirst(tagIdChildRegex, "$2");
				query.predicate = node -> node.tagIs(query.tag);
				query.map = new DescendantMap(
						xpath.replaceFirst(tagIdChildRegex, "$3"),
						xpath.replaceFirst(tagIdChildRegex, "$4"), true);
				query.valid = true;
			} else if (xpath.matches(tagIdDescendantRegex)) {
				query.tag = xpath.replaceFirst(tagIdDescendantRegex, "$1");
				query.id = xpath.replaceFirst(tagIdDescendantRegex, "$2");
				query.predicate = node -> node.tagIs(query.tag);
				query.map = new DescendantMap(
						xpath.replaceFirst(tagIdDescendantRegex, "$3"),
						xpath.replaceFirst(tagIdDescendantRegex, "$4"), false);
				query.valid = true;
			} else if (xpath
					.matches(tagIdDescendantAttrValueDescendantTagRegex)) {
				query.tag = xpath.replaceFirst(
						tagIdDescendantAttrValueDescendantTagRegex, "$1");
				query.id = xpath.replaceFirst(
						tagIdDescendantAttrValueDescendantTagRegex, "$2");
				query.map = new DescendantTagAttrTagMap(xpath.replaceFirst(
						tagIdDescendantAttrValueDescendantTagRegex, "$3"),
						xpath.replaceFirst(
								tagIdDescendantAttrValueDescendantTagRegex,
								"$4"),
						xpath.replaceFirst(
								tagIdDescendantAttrValueDescendantTagRegex,
								"$5"),
						xpath.replaceFirst(
								tagIdDescendantAttrValueDescendantTagRegex,
								"$6"));
				query.valid = true;
			}
			if (query.valid) {
				query.tag = normaliseTag(query.tag);
				Preconditions.checkState(!query.tag.contains(":"));
			}
			return query;
		}

		Stream<DomNode> stream(String xpath) {
			DomNodeReadonlyLookupQuery query = parse(xpath);
			if (query.immediateChild) {
				return children.byTag(query.tag).stream()
						.filter(query.predicate).flatMap(query.map);
			} else if (query.grandChild) {
				return children.byTag(query.tag).stream()
						.filter(query.predicate)
						.map(n -> n.children.byTag(query.grandChildTag))
						.flatMap(Collection::stream).flatMap(query.map);
			} else {
				Stream<DomNode> stream = null;
				if (Ax.notBlank(query.id2)) {
					stream = Stream.concat(
							document.byId().getAndEnsure(query.id).stream(),
							document.byId().getAndEnsure(query.id2).stream());
				} else if (Ax.notBlank(query.id)) {
					stream = document.byId().getAndEnsure(query.id).stream();
				} else {
					stream = document.byTag().getAndEnsure(query.tag).stream();
				}
				return stream.filter(query.predicate).flatMap(query.map)
						.filter(Objects::nonNull);
			}
		}

		class DescendantMap implements Function<DomNode, Stream<DomNode>> {
			private String tag;

			private int index;

			private boolean immediateChildrenOnly;

			public DescendantMap(String tag, String indexStr,
					boolean immediateChildrenOnly) {
				this.immediateChildrenOnly = immediateChildrenOnly;
				this.tag = normaliseTag(tag);
				this.index = Ax.isBlank(indexStr) ? -1
						: Integer.parseInt(indexStr);
			}

			@Override
			public Stream<DomNode> apply(DomNode t) {
				Stream<DomNode> stream = immediateChildrenOnly
						? t.children.elements().stream()
						: t.descendants();
				stream = stream.filter(n -> n.tagIs(tag));
				if (index != -1) {
					stream = stream.skip(index - 1).limit(1);
				}
				return stream;
			}
		}

		class DescendantTagAttrTagMap
				implements Function<DomNode, Stream<DomNode>> {
			private String tag;

			private String attrName;

			private String attrValue;

			private String childTag;

			public DescendantTagAttrTagMap(String tag, String attrName,
					String attrValue, String childTag) {
				this.tag = normaliseTag(tag);
				this.attrName = attrName;
				this.attrValue = attrValue;
				this.childTag = normaliseTag(childTag);
			}

			@Override
			public Stream<DomNode> apply(DomNode t) {
				return t.descendants().filter(n -> n.tagIs(tag))
						.filter(n -> n.attrIs(attrName, attrValue))
						.flatMap(n -> n.children.byTag(childTag).stream());
			}
		}

		class DomNodeReadonlyLookupQuery {
			String grandChildTag;

			boolean grandChild;

			public String id2;

			public String id;

			boolean immediateChild;

			String tag;

			Predicate<DomNode> predicate = node -> true;

			Function<DomNode, Stream<DomNode>> map = node -> Stream.of(node);

			boolean valid = false;
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
			parent().node.insertBefore(node.node,
					DomNode.this.node.getNextSibling());
		}

		public void insertAfterThis(List<DomNode> list) {
			List<DomNode> copy = list.stream().collect(Collectors.toList());
			Collections.reverse(copy);
			copy.forEach(n -> insertAfterThis(n));
		}

		public void insertAsFirstChildOf(DomNode other) {
			other.children.insertAsFirstChild(DomNode.this);
		}

		public void insertBeforeThis(DomNode node) {
			parent().node.insertBefore(node.node, DomNode.this.node);
		}

		public DomNode lastDescendant() {
			DomNode cursor = DomNode.this;
			while (cursor != null) {
				DomNode last = cursor.children.lastNode();
				if (last == null) {
					return cursor;
				}
				cursor = last;
			}
			return null;
		}

		public DomNode lastDescendantElement() {
			DomNode cursor = DomNode.this;
			while (cursor != null) {
				DomNode last = cursor.children.lastElementNode();
				if (last == null) {
					return cursor;
				}
				cursor = last;
			}
			return null;
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
			return document.nodeFor(node.getNextSibling());
		}

		public DomNode nextSiblingElement() {
			Node cursor = node.getNextSibling();
			while (cursor != null) {
				DomNode xnCursor = document.nodeFor(cursor);
				if (xnCursor.isElement()) {
					return xnCursor;
				}
				cursor = cursor.getNextSibling();
			}
			return null;
		}

		public DomNode previousSiblingElement() {
			Node cursor = node.getPreviousSibling();
			while (cursor != null) {
				DomNode xnCursor = document.nodeFor(cursor);
				if (xnCursor.isElement()) {
					return xnCursor;
				}
				cursor = cursor.getPreviousSibling();
			}
			return null;
		}

		public DomNode previousSibling() {
			return document.nodeFor(node.getPreviousSibling());
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

		public DomNode replaceWithMoveContents(DomNode node) {
			replaceWith(node);
			node.copyAttributesFrom(DomNode.this);
			node.children.adoptFrom(DomNode.this);
			return node;
		}

		public DomNode replaceWithTag(String tag) {
			DomNode wrapper = document
					.nodeFor(document.domDoc().createElement(tag));
			return replaceWithMoveContents(wrapper);
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
			DomNode wrapper = document
					.nodeFor(document.domDoc().createElement(tag));
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
			return descendants().anyMatch(blockResolver::isBlock);
		}

		private String jsToDom(String key) {
			if (key.equals(key.toLowerCase())) {
				return key;
			}
			StringBuilder builder = new StringBuilder();
			/*
			 * Could use a regex...
			 */
			for (int idx = 0; idx < key.length(); idx++) {
				char c = key.charAt(idx);
				if (c >= 'A' && c <= 'Z') {
					builder.append('-');
					builder.append(String.valueOf(c).toLowerCase());
				} else {
					builder.append(c);
				}
			}
			return builder.toString();
		}

		public DomNode setClassName(String string) {
			setAttr("class", string);
			return DomNode.this;
		}

		public String getStyleProperty(String key) {
			return getStyleMap().get(key);
		}

		public boolean hasStyleProperty(String key) {
			return getStyleMap().containsKey(key);
		}

		/*
		 * returns an empty map if the styles attr is null/empty
		 */
		public StringMap getStyleMap() {
			return has("style") ? StyleUtil.styleAttributeToMap(attr("style"))
					: new StringMap();
		}

		public void setStyleProperty(String key, String value) {
			key = jsToDom(key);
			StringMap styles = getStyleMap();
			styles.put(key, value);
			setAttr("style", StyleUtil.styleMapToAttribute(styles));
		}

		public Set<String> getClassNames() {
			String className = getClassName();
			if (Ax.isBlank(className)) {
				return Set.of();
			} else {
				return Set.of(className.split(" "));
			}
		}
	}

	public class DomNodeText {
		public void mergeWithAdjacentTexts() {
			DomNode cursor = DomNode.this;
			for (;;) {
				DomNode previousSibling = cursor.relative().nextSibling();
				if (previousSibling != null && previousSibling.isText()) {
					cursor = previousSibling;
				} else {
					break;
				}
			}
			for (;;) {
				DomNode nextSibling = cursor.relative().nextSibling();
				if (nextSibling != null && nextSibling.isText()) {
					cursor.setText(
							cursor.textContent() + nextSibling.textContent());
					nextSibling.removeFromParent();
				} else {
					break;
				}
			}
		}

		public SplitResult split(int from, int to) {
			SplitResult result = new SplitResult();
			Preconditions.checkState(isText());
			DomNode cursor = DomNode.this;
			result.contents = cursor;
			if (from > 0) {
				result.before = cursor;
				result.contents = cursor.builder()
						.text(cursor.textContent().substring(from))
						.insertAfterThis();
				cursor.setText(cursor.textContent().substring(0, from));
				cursor = result.contents;
				to -= from;
				from = 0;
			}
			if (to < cursor.textContent().length()) {
				result.after = cursor.builder()
						.text(cursor.textContent().substring(to))
						.insertAfterThis();
				cursor.setText(cursor.textContent().substring(0, to));
			}
			return result;
		}

		public class SplitResult {
			public DomNode before;

			public DomNode contents;

			public DomNode after;
		}
	}

	public static class DomNodeTraversal extends DepthFirstTraversal<DomNode> {
		public DomNodeTraversal(DomNode root) {
			super(root, node -> node.children.nodes());
		}
	}

	public class DomNodeTree implements Iterator<DomNode> {
		private TreeWalker tw;

		public boolean forwards = true;

		private Node hasNextFor;

		private Node next;

		private boolean currentIterated;

		public DomNodeTree() {
			tw = ((DocumentTraversal) document.domDoc()).createTreeWalker(
					document.domDoc(), NodeFilter.SHOW_ALL, null, true);
			tw.setCurrentNode(node);
		}

		public DomNode currentNode() {
			return document.nodeFor(tw.getCurrentNode());
		}

		@Override
		public boolean hasNext() {
			Node currentNode = tw.getCurrentNode();
			if (currentNode != hasNextFor) {
				hasNextFor = currentNode;
				next = next0();
				if (next != null) {
					tw.setCurrentNode(currentNode);
				} else {
				}
			}
			return next != null;
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

		@Override
		public DomNode next() {
			if (!hasNext()) {
				throw new NoSuchElementException();
			} else {
				if (!currentIterated) {
					currentIterated = true;
					// clear, since this will equal root, and we need to recalc
					// next
					hasNextFor = null;
				}
				tw.setCurrentNode(next);
				return document.nodeFor(tw.getCurrentNode());
			}
		}

		Node next0() {
			if (!currentIterated) {
				return tw.getCurrentNode();
			}
			while (true) {
				// will be null at the end of the traversal
				Node next = nextNodeWithReversed();
				return next;
			}
		}

		public DomNode nextLogicalNode() {
			Node next = nextNodeWithReversed();
			return document.nodeFor(next);
		}

		Node nextNodeWithReversed() {
			return forwards ? tw.nextNode() : tw.previousNode();
		}

		public String nextNonWhitespaceText() {
			return nextNonWhitespaceTextNode().map(DomNode::ntc).orElse(null);
		}

		public Optional<DomNode> nextNonWhitespaceTextNode() {
			return nextTextNode(false);
		}

		public Optional<DomNode> nextTextNode(boolean nonWhitespace) {
			while (true) {
				Node next = nextNodeWithReversed();
				if (next == null) {
					return Optional.empty();
				}
				DomNode xNext = document.nodeFor(next);
				if (xNext.isText() && (nonWhitespace
						|| xNext.isNonWhitespaceTextContent())) {
					return Optional.of(xNext);
				}
			}
		}

		public DomNode previousLogicalNode() {
			return withReversed(this::nextLogicalNode);
		}

		public String previousNonWhitespaceText() {
			return withReversed(this::nextNonWhitespaceText);
		}

		public Optional<DomNode> previousNonWhitespaceTextNode() {
			return withReversed(this::nextNonWhitespaceTextNode);
		}

		public DomNodeTree reversed() {
			this.forwards = !forwards;
			return this;
		}

		public void setCurrentNode(DomNode cursor) {
			tw.setCurrentNode(cursor.node);
		}

		public Stream<DomNode> stream() {
			Iterable<DomNode> iterable = () -> this;
			return StreamSupport.stream(iterable.spliterator(), false);
		}

		<T> T withReversed(Supplier<T> supplier) {
			reversed();
			T result = supplier.get();
			reversed();
			return result;
		}
	}

	public class DomNodeXpath {
		public String query;

		private XpathEvaluator evaluator;

		public DomNodeXpath() {
			if (document == DomNode.this) {
				evaluator = DomEnvironment.get()
						.createXpathEvaluator(DomNode.this, null);
			} else {
				evaluator = DomEnvironment.get().createXpathEvaluator(
						DomNode.this, document.xpath("").getEvaluator());
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

		public Stream<DomNode> matching(String pattern) {
			return matching(pattern, false);
		}

		/**
		 * Warning - uses 'find', not 'matches'
		 */
		public Stream<DomNode> matching(String pattern, boolean ignoreCase) {
			RegExp regex = ignoreCase ? RegExp.compile(pattern, "i")
					: RegExp.compile(pattern);
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
			if (document.isReadonly() && lookup().handlesXpath(query)) {
				return stream().findFirst().orElse(null);
			} else {
				Node domNode = evaluator.getNodeByXpath(query, node);
				return document.nodeFor(domNode);
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
					.contains(document.nodeFor(node));
		}

		public Stream<DomNode> stream() {
			if (document.isReadonly() && lookup().handlesXpath(query)) {
				return lookup.stream(query);
			} else {
				List<Node> domNodes = evaluator.getNodesByXpath(query, node);
				return domNodes.stream().map(document::nodeFor);
			}
		}

		public String textNormalised() {
			return node().ntc();
		}

		public String textNormalised(boolean required) {
			DomNode node = node();
			Optional<DomNode> optional = required ? Optional.of(node)
					: Optional.ofNullable(node);
			return optional.map(DomNode::ntc).orElse("");
		}

		public String textNormalisedOrEmpty() {
			return textNormalised(false);
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
			boolean inRange = false;
			List<DomNode> toRemoveNodes = new ArrayList<>();
			Objects.requireNonNull(end);
			DomNode keepAncestorsOf = end;
			if (!endBefore) {
				DomNodeTree tree = end.tree();
				DomNode next = tree.nextLogicalNode();
				Node keep = next == null ? null : next.node;
				keepAncestorsOf = keep == null ? null : document.nodeFor(keep);
			}
			DomNodeTree tree = tree();
			List<DomNode> remove = new ArrayList<>();
			while (true) {
				DomNode cursor = tree.currentNode();
				if (cursor == DomNode.this) {
					inRange = true;
					if (startAfterThis) {
						tree.nextLogicalNode();
						continue;
					}
				}
				if (cursor == end && endBefore) {
					break;
				}
				if (inRange) {
					if (keepAncestorsOf == null
							|| !cursor.isAncestorOf(keepAncestorsOf)) {
						remove.add(cursor);
					}
				}
				if (cursor == end) {
					break;
				}
				tree.nextLogicalNode();
			}
			remove.stream().filter(n -> n.parent() != null)
					.forEach(DomNode::removeFromParent);
		}

		private Range createRange() {
			Range range = ((DocumentRange) document.domDoc()).createRange();
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
			return document.nodeFor(frag);
		}

		public DomNode toWrappedNode(String tag, boolean clone) {
			Element wrapper = document.domDoc().createElement(tag);
			Range range = createRange();
			DocumentFragment frag = range.cloneContents();
			range.detach();
			wrapper.appendChild(frag);
			if (!clone) {
				clearContents();
			}
			return document.nodeFor(wrapper);
		}
	}

	public static class W3cNodeTraversal
			extends DepthFirstTraversal<org.w3c.dom.Node> {
		static List<Node> children(Node node) {
			List<Node> result = new ArrayList<>();
			NodeList childNodes = node.getChildNodes();
			int length = childNodes.getLength();
			for (int idx = 0; idx < length; idx++) {
				result.add(childNodes.item(idx));
			}
			return result;
		}

		public W3cNodeTraversal(Node root) {
			super(root, W3cNodeTraversal::children);
		}
	}

	public interface XpathEvaluator {
		Node getNodeByXpath(String query, Node node);

		List<Node> getNodesByXpath(String query, Node node);
	}

	public String getId() {
		return attr("id");
	}

	public String getInnerMarkup() {
		StringBuilder builder = new StringBuilder();
		children.nodes().forEach(child -> builder.append(child.fullToString()));
		return builder.toString();
	}
}
