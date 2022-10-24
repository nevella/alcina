package cc.alcina.framework.gwt.client.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Stack;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import com.google.common.base.Preconditions;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.RootPanel;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.logic.domaintransform.SequentialIdGenerator;
import cc.alcina.framework.common.client.logic.reflection.ClearStaticFieldsOnAppShutdown;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.HtmlConstants;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.common.client.util.TextUtils;
import cc.alcina.framework.gwt.client.ClientNotifications;

/**
 * Why does [1] sometimes need to be suffixed?
 *
 * <h2>Wrapping and unwrapping - 1</h2>
 * <p>
 * DomUtils uses expandos heavily, mostly to allow a consistent node addressing
 * scheme even if nodes have been decorated (this mostly happens in
 * OverlaidHtmlWriter - there may be some legacy client usage but I think that's
 * mostly gone).
 * </p>
 * <p>
 * When a (text) node is decorated (wrapped) by an element, that element has its
 * __j_wrap_id set to '1'. If there's an 'A' element in the nodes ancestor
 * chain, things get trickier because we can't have nested 'A' elements - so the
 * existing 'A' is split.
 * </p>
 * <p>
 * Splitting adds an '__j_unwrap_id' expando to split nodes - indicating they
 * should be combined during traversal. Currently...dodgy
 * </p>
 *
 * @author nick@alcina.cc
 */
@Registration(ClearStaticFieldsOnAppShutdown.class)
public class DomUtils implements NodeFromXpathProvider {
	public static String containerTagRegex = "(?i)/?(judgment|doc)";

	private static final String DOM_XPATH_MAP = "dom-xpath-map";

	public static final String TEXT_MARKER = "TEXT()";

	public static final String COMMENT_MARKER = "#COMMENT";

	private static final String HTML_INVISIBLE_CONTENT_ELEMENTS = ",STYLE,TEXTAREA,SCRIPT,INPUT,SELECT,";

	public static String ignoreableElementIdPrefix = "IGNORE__";

	public static final String ATTR_UNWRAP_EXPANDO_ID = "__j_unwrap_id";

	public static final String ATTR_WRAP_EXPANDO_ID = "__j_wrap_id";

	public static SequentialIdGenerator expandoIdProvider = new SequentialIdGenerator();

	public static Supplier<Map> mapSupplier = () -> new LinkedHashMap<>();

	private static DomUtilsBlockResolver blockResolver;

	public static Node debugNode;

	public static Stream<Element> ancestorStream(Element element) {
		// FIXME - dirndl 1x3 - (not optimal)
		List<Element> elements = new ArrayList<>();
		Node node = element;
		while (node != null && node.getNodeType() == Node.ELEMENT_NODE) {
			elements.add((Element) node);
			node = node.getParentNode();
		}
		return elements.stream();
	}

	public static boolean containsBlocks(Element elt) {
		NodeList childNodes = elt.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node child = childNodes.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE
					&& isBlockHTMLElement((Element) child)) {
				return true;
			}
		}
		return false;
	}

	public static String expandEmptyElements(String s) {
		// optimised
		// String regex = "<([a-zA-Z0-9_\\-]+)([^<]*?)/>";
		// Pattern p = Pattern.compile(regex, Pattern.MULTILINE | Pattern.DOTALL
		// | Pattern.CASE_INSENSITIVE);
		// Matcher m = p.matcher(s);
		// s = m.replaceAll("<$1$2></$1>");
		// return s;
		StringBuilder s2 = new StringBuilder((int) (s.length() * 1.1));
		int l = s.length() - 1;
		int tagStart = -1;
		int tagEnd = -1;
		int idx = 0;
		for (; idx < l; idx++) {
			boolean app = true;
			char c = s.charAt(idx);
			char c2 = s.charAt(idx + 1);
			if (c == '<') {
				tagEnd = -1;
				if (c2 != '/') {
					tagStart = idx + 1;
				} else {
					tagStart = -1;
				}
			} else if (tagStart != -1) {
				if (c == '/' && c2 == '>') {
					s2.append("></");
					tagEnd = tagEnd == -1 ? idx : tagEnd;
					for (int j = tagStart; j < tagEnd; j++) {
						s2.append(s.charAt(j));
					}
					s2.append(">");
					idx++;
					app = false;
				} else if (tagEnd == -1) {
					if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
							|| (c >= '0' && c <= '9') || c == '_' || c == '-') {
					} else {
						tagEnd = idx;
					}
				}
			}
			if (app) {
				s2.append(c);
			}
		}
		if (idx == l) {
			s2.append(s.charAt(idx));
		}
		return s2.toString();
	}

	public static List<Element> getChildElements(Element elt) {
		return nodeListToElementList(elt.getChildNodes());
	}

	public static Element getContainingBlock(Node n) {
		while (n != null) {
			if (n.getNodeType() == Node.ELEMENT_NODE
					&& isBlockHTMLElement((Element) n)) {
				return (Element) n;
			}
			n = n.getParentNode();
		}
		return null;
	}

	public static Text getFirstNonWhitespaceTextChild(Node node) {
		NodeList nl = node.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			node = nl.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Text t = getFirstNonWhitespaceTextChild(node);
				if (t != null) {
					return t;
				}
			}
			if (node.getNodeType() == Node.TEXT_NODE) {
				if (!TextUtils.isWhitespaceOrEmpty(node.getNodeValue())) {
					return (Text) node;
				}
			}
		}
		return null;
	}

	public static Text getLastTextChild(Node node) {
		Node n = lastChildOf(node);
		ClientNodeIterator itr = new ClientNodeIterator(n,
				ClientNodeIterator.SHOW_ALL);
		while (n != null) {
			if (n.getNodeType() == Node.TEXT_NODE) {
				return (Text) n;
			}
			n = itr.previousNode();
		}
		return null;
	}

	public static Element getNeighbouringBlock(Node n, int dir) {
		Element block = getContainingBlock(n);
		if (block == null) {
			return null;
		}
		ClientNodeIterator itr = new ClientNodeIterator(block,
				ClientNodeIterator.SHOW_ELEMENT);
		while (true) {
			Element e = (Element) (dir == 1 ? itr.nextNode()
					: itr.previousNode());
			if (e == null) {
				return null;
			}
			Element cb = getContainingBlock(e);
			if (cb != block && !isAncestorOf(block, cb)
					&& !isAncestorOf(cb, block)) {
				return cb;
			}
		}
	}

	public static Element getSelfOrAncestorWithTagName(Node node,
			String tagName) {
		return getSelfOrAncestorWithTagName(node, tagName, null);
	}

	public static Element getSelfOrAncestorWithTagName(Node node,
			String tagName, Node stop) {
		while (node != null && node != stop) {
			if (node.getNodeType() == Node.ELEMENT_NODE
					&& node.getNodeName().equalsIgnoreCase(tagName)) {
				return (Element) node;
			}
			node = node.getParentNode();
		}
		return null;
	}

	public static List<Text> getVisibleTextNodes(Element root) {
		List<Text> texts = new ArrayList<Text>();
		addVisibleTextNodes(root, texts);
		return texts;
	}

	public static boolean isAncestorOf(Element ancestor, Node possibleChild) {
		Element stop = GWT.isClient() ? RootPanel.get().getElement() : null;
		while (possibleChild != null && possibleChild != stop) {
			if (possibleChild == ancestor) {
				return true;
			}
			possibleChild = possibleChild.getParentNode();
		}
		return false;
	}

	public static boolean isAttachedToBody(Node node) {
		if (node == null) {
			return false;
		}
		return getSelfOrAncestorWithTagName(node, "BODY") != null;
	}

	public static boolean isBlockHTMLElement(Element e) {
		if (blockResolver == null) {
			blockResolver = Registry.impl(DomUtilsBlockResolver.class);
		}
		return blockResolver.isBlockHTMLElement(e);
	}

	public static boolean isInvisibleContentElement(Element elt) {
		return isInvisibleContentElement(elt.getTagName());
	}

	public static boolean isInvisibleContentElement(String tagName) {
		return HTML_INVISIBLE_CONTENT_ELEMENTS
				.contains("," + tagName.toUpperCase() + ",");
	}

	public static Node lastChildOf(Node node) {
		if (!node.hasChildNodes()) {
			return node;
		}
		return lastChildOf(node.getLastChild());
	}

	public static List<Node> nodeListToArrayList(NodeList list) {
		List<Node> result = new ArrayList<Node>();
		int length = list.getLength();
		for (int i = 0; i < length; i++) {
			Node node = list.item(i);
			result.add(node);
		}
		return result;
	}

	public static List<Element> nodeListToElementList(NodeList list) {
		List<Element> result = new ArrayList<Element>();
		int length = list.getLength();
		for (int i = 0; i < length; i++) {
			Node node = list.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				result.add((Element) node);
			}
		}
		return result;
	}

	public static void stripNode(Node oldNode) {
		Node parent = oldNode.getParentNode();
		NodeList nl = oldNode.getChildNodes();
		Node refChild = oldNode;
		for (int i = nl.getLength() - 1; i >= 0; i--) {
			Node child = nl.item(i);
			parent.insertBefore(child, refChild);
			refChild = child;
		}
		oldNode.getParentNode().removeChild(oldNode);
	}

	public static String toSimpleXPointer(Node n) {
		List<String> parts = new ArrayList<String>();
		while (n != null) {
			short nodeType = n.getNodeType();
			switch (nodeType) {
			case Node.DOCUMENT_NODE:
				parts.add("");
				break;
			default:
				String part = n.getNodeName();
				switch (nodeType) {
				case Node.TEXT_NODE:
					part = "TEXT()";
					break;
				}
				NodeList childNodes = n.getParentNode().getChildNodes();
				int pos = -1;
				int count = 0;
				int length = childNodes.getLength();
				for (int i = 0; i < length; i++) {
					Node item = childNodes.item(i);
					if (item == n) {
						pos = count + 1;
					}
					if (item.getNodeType() == nodeType) {
						if (nodeType == Node.ELEMENT_NODE) {
							if (!((Element) n).getTagName()
									.equals(((Element) item).getTagName())) {
								continue;
							}
						}
						count++;
					}
				}
				parts.add(count != 1 ? part + "[" + pos + "]" : part);
				break;
			}
			n = n.getParentNode();
		}
		Collections.reverse(parts);
		return CommonUtils.join(parts, "/");
	}

	private static void addVisibleTextNodes(Element elt, List<Text> texts) {
		Element displayNone = null;
		ClientNodeIterator itr = new ClientNodeIterator(elt,
				ClientNodeIterator.SHOW_ALL);
		itr.setRoot(elt);
		Node n;
		TextVisibilityObserver textVisibilityObserver = new TextVisibilityObserver();
		// duplicates ArticleTextModel (Jade)
		while ((n = itr.getCurrentNode()) != null) {
			if (n.getNodeType() == Node.TEXT_NODE
					&& !textVisibilityObserver.isIgnoreText()) {
				boolean currentDisplayNoneIsAncestor = displayNone != null
						&& DomUtils.isAncestorOf(displayNone, n);
				if (!currentDisplayNoneIsAncestor) {
					texts.add((Text) n);
				}
			} else if (n.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) n;
				String styleAttribute = element.getAttribute("style");
				boolean thisDisplayNone = styleAttribute != null
						&& (styleAttribute.contains("display: none")
								|| styleAttribute.contains("display:none"));
				boolean currentDisplayNoneIsAncestor = displayNone != null
						&& DomUtils.isAncestorOf(displayNone, element);
				if (thisDisplayNone) {
					if (!currentDisplayNoneIsAncestor) {
						displayNone = element;
					}
				} else {
					if (!currentDisplayNoneIsAncestor) {
						displayNone = null;
					}
				}
				textVisibilityObserver.update(element);
			}
			itr.nextNode();
		}
	}

	private boolean useXpathMap = true;

	private Node lastContainer = null;

	private Map<String, Node> xpathMap;

	private Map<Node, String> reverseXpathMap;

	boolean debug = false;

	private NodeFromXpathProvider nodeProvider = null;

	Map<Element, DomRequiredSplitInfo> domRequiredSplitInfo = mapSupplier.get();

	Stack<XpathMapPoint> itrStack = null;

	private Map<Node, StringBuilder> exactTextMap;

	private Map<Node, Node> precededByNonHtmlDomNodes = mapSupplier.get();

	Map<Element, Node> unwrappedFirstChildren = new LinkedHashMap<>();

	private BackupNodeResolver backupNodeResolver;

	public DomUtils() {
		invalidateUnwrapOrIgnoreCache();
	}

	public void collapseToBoundaries(Element link) {
		DomRequiredSplitInfo info = new DomRequiredSplitInfo(link, null);
		info.split();
		// do not add to unwrap - since this is not tracked.
		// * should't* be an issue since this is only used for overlay ext.
		// hyperlink removal
	}

	public void dumpContainerNoMap() {
		Node lc = lastContainer;
		System.out.println("---dump xpath map (no map)");
		useXpathMap = false;
		for (String key : xpathMap.keySet()) {
			Node node = findXpathWithIndexedText(key, lastContainer);
			if (node == null) {
				System.out.println("***MISSING***" + key);
			} else {
				String tc = node.getNodeType() == Node.TEXT_NODE
						? " - " + node.getNodeValue()
						: "";
				System.out.println(key + tc);
			}
		}
		System.out.println("\n---\n\n");
		useXpathMap = true;
	}

	public String dumpMap(boolean regenerate) {
		return dumpMap0(regenerate);
	}

	public Node dumpNearestMatch(String xpathStr, Node container) {
		String ucXpath = xpathStr.toUpperCase();
		String[] sections = ucXpath.split("/");
		String matched = "";
		String lastMatchedPath = "";
		Node lastMatched = null;
		int count = 0;
		for (String section : sections) {
			if (matched.length() != 0) {
				matched += "/";
			}
			matched += section;
			Node match = findXpathWithIndexedText(matched, container);
			if (match == null) {
				System.out.println(
						"Prefix matched:" + lastMatchedPath + "\n----------\n");
				Map<String, Node> xpathMap = mapSupplier.get();
				generateMap((Element) lastMatched, "");
				dumpMap0(false);
				if (count > 3) {
					System.out.println("Parent map:");
					xpathMap = mapSupplier.get();
					generateMap((Element) lastMatched.getParentNode(), "");
					dumpMap0(false);
				}
				return lastMatched;
			} else {
				lastMatched = match;
				lastMatchedPath = matched;
				count++;
			}
		}
		return null;
	}

	@Override
	public Node findXpathWithIndexedText(String xpathStr, Node container) {
		return findXpathWithIndexedText(xpathStr, container, -1);
	}

	public Node findXpathWithIndexedText(String xpathStr, Node container,
			Integer backupAbsTextOffset) {
		container = resolveContainer(container);
		if (nodeProvider != null) {
			return nodeProvider.findXpathWithIndexedText(xpathStr, container);
		}
		if (xpathStr.length() == 0) {
			return container;
		}
		String ucXpath = xpathStr.toUpperCase();
		if (useXpathMap) {
			ensureMaps(container);
			Node node = null;
			if (backupNodeResolver != null && backupAbsTextOffset != null) {
				node = backupNodeResolver.resolve(xpathStr,
						backupAbsTextOffset);
			} else {
				node = xpathMap.get(ucXpath);
			}
			String singleTextPoss = "TEXT()[1]";
			String possiblyWrappedTextPost = "TEXT()";
			if (node == null && ucXpath.endsWith(singleTextPoss)) {
				node = xpathMap.get(ucXpath.substring(0, ucXpath.length() - 3));
			}
			if (node == null && ucXpath.endsWith(possiblyWrappedTextPost)) {
				node = xpathMap.get(ucXpath + "[1]");
			}
			if (node == null
					&& container.getNodeName().matches(containerTagRegex)
					&& ucXpath.contains("/")) {
				node = xpathMap
						.get(ucXpath.substring(ucXpath.indexOf("/") + 1));
				if (node == null && ucXpath.length() > 1
						&& ucXpath.indexOf("/") == 0) {
					String adjusted = ucXpath
							.substring(ucXpath.indexOf("/", 1) + 1);
					node = xpathMap.get(adjusted);
					if (node == null) {
						if (adjusted.isEmpty()
								|| adjusted.matches(containerTagRegex)) {
							node = container;
						}
					}
				}
				if (node == null) {
				}
			}
			return node;
		} else {
			if (lastContainer != container) {
				lastContainer = container;
			}
		}
		// keep in sync with sedomutils
		Node current = container;
		String[] sections = ucXpath.split("/");
		for (String section : sections) {
			int index = 1;
			String tagName = section;
			if (section.contains("[")) {
				index = Integer.valueOf(section.substring(
						section.indexOf("[") + 1, section.length() - 1));
				tagName = section.substring(0, section.indexOf("["))
						.toUpperCase();
			}
			boolean asText = tagName.equals(DomUtils.TEXT_MARKER);
			WrappingAwareNodeIterator awareNodeIterator = new WrappingAwareNodeIterator(
					current);
			Node node = null;
			Node last = null;
			while ((node = awareNodeIterator.next()) != null) {
				boolean foundIndexed = false;
				if (asText) {
					foundIndexed = node.getNodeType() == Node.TEXT_NODE;
					if (foundIndexed && last != null
							&& last.getNodeType() == Node.TEXT_NODE) {
						throw new RuntimeException(
								"Split text node from aware iterator");
						// continue;// ignore - this was orginally a single text
						// node, now split (and "siblings" because
						// of wrapping)
					}
				} else {
					foundIndexed = node.getNodeType() == Node.ELEMENT_NODE
							&& node.getNodeName().equals(tagName);
				}
				if (foundIndexed) {
					index--;
					if (index == 0) {
						current = node;
						break;
					}
				}
				last = node;
			}
			if (current == null) {
				return null;
			}
		}
		return current;
	}

	public void generateMap(Node container, String prefix) {
		generateMap0(container, prefix);
	}

	public BackupNodeResolver getBackupNodeResolver() {
		return this.backupNodeResolver;
	}

	public NodeFromXpathProvider getNodeProvider() {
		return nodeProvider;
	}

	public Map<Node, Node> getPrecededByNonHtmlDomNodes() {
		return this.precededByNonHtmlDomNodes;
	}

	public Node getPrecededByNonHtmlDomNodes(Text text) {
		return precededByNonHtmlDomNodes.get(text);
	}

	public Map<Node, String> getReverseXpathMap(Element container) {
		if (reverseXpathMap == null) {
			ensureMaps(container);
		}
		return this.reverseXpathMap;
	}

	public Map<String, Node> getXpathMap() {
		return this.xpathMap;
	}

	public void invalidateUnwrapOrIgnoreCache() {
		// unwrapOrIgnoreCache = new IdentityHashMap<Node, Node>(10000);
	}

	public boolean isUseXpathMap() {
		return this.useXpathMap;
	}

	public void setBackupNodeResolver(BackupNodeResolver backupNodeResolver) {
		this.backupNodeResolver = backupNodeResolver;
	}

	public void setNodeProvider(NodeFromXpathProvider nodeProvider) {
		this.nodeProvider = nodeProvider;
	}

	public void setPrecededByNonHtmlDomNodes(
			Map<Node, Node> precededByNonHtmlDomNodes) {
		this.precededByNonHtmlDomNodes = precededByNonHtmlDomNodes;
	}

	public void setUseXpathMap(boolean useXpathMap) {
		this.useXpathMap = useXpathMap;
	}

	public void setXpathMap(Map<String, Node> xpathMap) {
		this.xpathMap = xpathMap;
	}

	public void unwrap(Element el) {
		// NO nodelist.stream - because our old faux-element doesn't support
		Element parent = (Element) el.getParentNode();
		NodeList nl = el.getChildNodes();
		Node[] tmp = new Node[nl.getLength()];
		for (int i = 0; i < nl.getLength(); i++) {
			tmp[i] = nl.item(i);
		}
		for (int i = 0; i < tmp.length; i++) {
			Node n = tmp[i];
			parent.insertBefore(n, el);
		}
		parent.removeChild(el);
		if (domRequiredSplitInfo.containsKey(el)) {
			domRequiredSplitInfo.get(el).unsplit();
			domRequiredSplitInfo.remove(el);
		}
	}

	public void wrap(Element wrapper, Text toWrap) {
		wrapper.setAttribute(ATTR_WRAP_EXPANDO_ID, "1");
		Node t = toWrap;
		Node lastT = null;
		Node splitAround = toWrap;
		while (t != null) {
			if (t.getNodeType() == Node.ELEMENT_NODE) {
				Element ancestor = (Element) t;
				if (isBlockHTMLElement(ancestor)) {
					break;
				}
				if (requiresSplit(ancestor, wrapper)) {
					DomRequiredSplitInfo info = new DomRequiredSplitInfo(
							ancestor, lastT);
					info.split();
					domRequiredSplitInfo.put(wrapper, info);
					break;
				}
			}
			lastT = t;
			t = t.getParentNode();
		}
		try {
			Element parent = (Element) toWrap.getParentNode();
			parent.insertBefore(wrapper, toWrap);
			parent.removeChild(toWrap);
			wrapper.appendChild(toWrap);
		} catch (RuntimeException e) {
			throw e;
		}
	}

	private String dumpMap0(boolean regenerate) {
		StringBuilder builder = new StringBuilder();
		if (regenerate) {
			exactTextMap = mapSupplier.get();
			xpathMap = mapSupplier.get();
			generateMap(lastContainer, "");
		} else {
			exactTextMap = null;
		}
		builder.append("---dump xpath map\n");
		for (String key : xpathMap.keySet()) {
			Node node = xpathMap.get(key);
			String tc = node.getNodeType() == Node.TEXT_NODE
					? " - " + (exactTextMap != null ? exactTextMap.get(node)
							: node.getNodeValue())
					: "";
			builder.append(key + tc + "\n");
		}
		builder.append("\n---\n\n");
		return builder.toString();
	}

	private void ensureMaps(Node container) {
		if (lastContainer != container) {
			lastContainer = container;
			xpathMap = mapSupplier.get();
			reverseXpathMap = mapSupplier.get();
			ClientNotifications notifications = Registry
					.optional(ClientNotifications.class).orElse(null);
			if (notifications != null) {
				notifications.metricLogStart(DOM_XPATH_MAP);
			}
			generateMap(container, GWT.isClient() ? "" : "/");
			if (notifications != null) {
				notifications.metricLogEnd(DOM_XPATH_MAP);
			}
		}
	}

	private void generateMap0(Node container, String prefix) {
		if (container == null) {
			return;
		}
		Map<String, Integer> total = mapSupplier.get();
		Map<String, Integer> current = mapSupplier.get();
		NodeList nodes = container.getChildNodes();
		if (prefix.length() <= 1) {
			xpathMap.put(prefix, container);
			reverseXpathMap.put(container, prefix);
		}
		short lastNodeType = Node.DOCUMENT_NODE;
		// ignore sequential texts (with wrapping), as per non-map version
		WrappingAwareNodeIterator awareNodeIterator = new WrappingAwareNodeIterator(
				container);
		Node node = null;
		while ((node = awareNodeIterator.next()) != null) {
			short nodeType = node.getNodeType();
			if (include(lastNodeType, nodeType, node)) {
				String marker = nodeType == Node.TEXT_NODE
						? DomUtils.TEXT_MARKER
						: node.getNodeName().toUpperCase();
				int c = total.containsKey(marker) ? total.get(marker) : 0;
				total.put(marker, c + 1);
			}
			lastNodeType = nodeType;
		}
		lastNodeType = Node.DOCUMENT_NODE;
		awareNodeIterator = new WrappingAwareNodeIterator(container);
		node = null;
		// double count totals due to strange issues with overlays (jcv)
		total = mapSupplier.get();
		while ((node = awareNodeIterator.next()) != null) {
			short nodeType = node.getNodeType();
			if (include(lastNodeType, nodeType, node)) {
				String marker = nodeType == Node.TEXT_NODE
						? DomUtils.TEXT_MARKER
						: node.getNodeName().toUpperCase();
				int c = total.containsKey(marker) ? total.get(marker) : 0;
				total.put(marker, c + 1);
			}
			lastNodeType = nodeType;
		}
		lastNodeType = Node.DOCUMENT_NODE;
		awareNodeIterator = new WrappingAwareNodeIterator(container);
		StringBuilder cumulativeText = null;
		while ((node = awareNodeIterator.next()) != null) {
			short nodeType = node.getNodeType();
			if (include(lastNodeType, nodeType, node)) {
				// if (debug && !xp.contains("TBODY")) {
				// System.out.println(xp);
				// }
				String marker = nodeType == Node.TEXT_NODE
						? DomUtils.TEXT_MARKER
						: node.getNodeName().toUpperCase();
				String post = marker;
				if (total.get(marker) != 1) {
					int c = current.containsKey(marker) ? current.get(marker)
							: 0;
					current.put(marker, ++c);
					post += "[" + c + "]";
				}
				String xp = prefix + post;
				xpathMap.put(xp, node);
				reverseXpathMap.put(node, xp);
				if (nodeType == Node.ELEMENT_NODE) {
					generateMap0((Element) node, xp + "/");
					// this won't cause ambiguity
					if (post.equals("TBODY")) {
						generateMap0((Element) node, prefix);
					}
				} else {
					if (exactTextMap != null) {
						cumulativeText = new StringBuilder();
						exactTextMap.put(node, cumulativeText);
					}
				}
			}
			if (exactTextMap != null && nodeType == Node.TEXT_NODE) {
				cumulativeText.append(node.getNodeValue());
			}
			lastNodeType = nodeType;
		}
	}

	private boolean include(short lastNodeType, short nodeType, Node node) {
		if (nodeType == Node.ELEMENT_NODE) {
			return true;
		}
		if (nodeType == Node.TEXT_NODE) {
			if (lastNodeType != Node.TEXT_NODE) {
				return true;
			}
			return precededByNonHtmlDomNodes.containsKey(node);
		}
		// comments and processing instructions are non-addressable
		return false;
		// throw new RuntimeException();
	}

	private Node resolveContainer(Node container) {
		return // all server-side addressing should be relative to the document
		GWT.isClient() ? // all server-side addressing should be relative to the
							// document
				container
				: container.getNodeType() == Node.DOCUMENT_NODE ? container
						: container.getOwnerDocument();
	}

	boolean requiresSplit(Element ancestor, Element wrapper) {
		if (ancestor.getTagName().toLowerCase().equals("a")
				&& wrapper.getTagName().toLowerCase().equals("a")) {
			return true;
		}
		return false;
	}

	public interface BackupNodeResolver {
		Node resolve(String xpathStr, int backupAbsTextOffset);
	}

	@Reflected
	@Registration.Singleton
	public static class DomUtilsBlockResolver {
		public boolean isBlockHTMLElement(Element e) {
			return HtmlConstants.isHtmlBlock(e.getTagName());
		}
	}

	public static class HighlightInfo {
		public String cssClassName;

		public StringMap styleProperties = new StringMap();

		public StringMap properties = new StringMap();

		public String tag = "a";

		public HighlightInfo() {
		}

		public HighlightInfo(String cssClassName, StringMap styleProperties,
				StringMap properties) {
			this.cssClassName = cssClassName;
			this.styleProperties = styleProperties;
			this.properties = properties;
		}

		public void applyTo(Element elem) {
			elem.setAttribute("class", cssClassName);
			for (Entry<String, String> entry : styleProperties.entrySet()) {
				DomContext.setStyleProperty(elem, entry.getKey(),
						entry.getValue());
			}
			for (Entry<String, String> entry : properties.entrySet()) {
				DomContext.setProperty(elem, entry.getKey(), entry.getValue());
			}
		}

		public HighlightInfo copy() {
			return new HighlightInfo(cssClassName,
					new StringMap(styleProperties), new StringMap(properties))
							.tag(tag);
		}

		public HighlightInfo span() {
			this.tag = "span";
			return this;
		}

		public HighlightInfo tag(String tag) {
			this.tag = tag;
			return this;
		}
	}

	public static class IsBlockFilter implements Predicate<Node> {
		@Override
		public boolean test(Node o) {
			return o.getNodeType() == Node.ELEMENT_NODE
					&& isBlockHTMLElement((Element) o);
		}
	}

	/**
	 * @param maxNodes
	 * @return true if finished
	 */
	public static class TextVisibilityObserver {
		private boolean ignoreText = false;

		// displaynone content (legislation TOC) has to stay in the DOM, for
		// printing...but we need to know for search filtering
		private Object displayNone;

		public boolean isDisplayNoneText() {
			return this.displayNone != null;
		}

		public boolean isIgnoreText() {
			return this.ignoreText;
		}

		public void update(Element element) {
			ignoreText = isInvisibleContentElement(element);
		}

		public void update(String tagName) {
			ignoreText = isInvisibleContentElement(tagName);
		}

		public void updateDisplayNone(Object displayNone) {
			this.displayNone = displayNone;
		}
	}

	static class DomRequiredSplitInfo {
		public static int expandoId;

		List<Node> contents = new ArrayList<Node>();

		public Node splitAround;

		public Element splitFrom;

		public Element splitEnd;

		/**
		 * @param splitAround
		 *            - null means "collapse to boundaries"
		 */
		public DomRequiredSplitInfo(Element splitFrom, Node splitAround) {
			this.splitFrom = splitFrom;
			this.splitAround = splitAround;
		}

		public void split() {
			if (!splitFrom.hasAttribute(ATTR_UNWRAP_EXPANDO_ID)) {
				splitFrom.setAttribute(ATTR_UNWRAP_EXPANDO_ID,
						String.valueOf(expandoIdProvider.incrementAndGet()));
			}
			String expandoId = splitFrom.getAttribute(ATTR_UNWRAP_EXPANDO_ID);
			Element grand = (Element) splitFrom.getParentNode();
			NodeList nl = splitFrom.getChildNodes();
			splitEnd = (Element) splitFrom.cloneNode(false);
			if (splitAround == null) {
				List<Node> children = nodeListToArrayList(
						splitFrom.getChildNodes());
				Node insertionPoint = splitFrom;
				for (Node node : children) {
					DomNode.from(insertionPoint).relative()
							.insertAfterThis(DomNode.from(node));
					insertionPoint = node;
				}
				DomNode.from(insertionPoint).relative()
						.insertAfterThis(DomNode.from(splitEnd));
			} else {
				boolean found = false;
				for (int i = 0; i < nl.getLength(); i++) {
					Node n = nl.item(i);
					if (splitAround == null || n == splitAround) {
						found = true;
					}
					if (found) {
						contents.add(n);
					}
				}
				for (int i = 1; i < contents.size(); i++) {
					splitEnd.appendChild(contents.get(i));
				}
				DomNode.from(splitFrom).relative()
						.insertAfterThis(DomNode.from(splitAround));
				DomNode.from(splitAround).relative()
						.insertAfterThis(DomNode.from(splitEnd));
				if (splitAround.getNodeType() == Node.TEXT_NODE) {
					Element splitAroundWrap = splitFrom.getOwnerDocument()
							.createElement("span");
					splitAroundWrap.setAttribute(ATTR_UNWRAP_EXPANDO_ID,
							expandoId);
					DomNode.from(splitAround).relative()
							.insertAfterThis(DomNode.from(splitAroundWrap));
					splitAroundWrap.appendChild(splitAround);
				} else {
					Element splitAroundElt = (Element) splitAround;
					if (!splitAroundElt.hasAttribute(ATTR_UNWRAP_EXPANDO_ID)) {
						splitAroundElt.setAttribute(ATTR_UNWRAP_EXPANDO_ID,
								expandoId);
					}
				}
			}
			List<Element> maybeRedundantSplits = new ArrayList<Element>();
			List<Element> childElements = DomUtils
					.nodeListToElementList(grand.getChildNodes());
			for (Element element : childElements) {
				if (element.getAttribute(ATTR_UNWRAP_EXPANDO_ID)
						.equals(expandoId)) {
					maybeRedundantSplits.add(element);
				}
			}
			for (int i = 1; i < maybeRedundantSplits.size() - 1; i++) {
				Element split = maybeRedundantSplits.get(i);
				if (!split.hasChildNodes()) {
					split.getParentNode().removeChild(split);
				}
			}
		}

		public void unsplit() {
			splitFrom.appendChild(splitAround);
			for (Node n : contents) {
				splitFrom.appendChild(n);
			}
			splitFrom.removeAttribute(ATTR_UNWRAP_EXPANDO_ID);
			splitEnd.getParentNode().removeChild(splitEnd);
		}
	}

	/*
	 * this makes a linear list of unwrapped elts - and unwraps wrapped - e.g.
	 * <a-uw><span>trux</span></a-uw><a-w>blah</a-w><a-uw><i>trix</i></a-uw>
	 * becomes <span>trux</span>blah<i>trix</i>
	 */
	class NodeWrapList {
		List<Node> kids = new ArrayList<Node>();

		List<Element> currentToUnwrap = new ArrayList<>();

		public NodeWrapList(Node parent) {
			List<Node> list = nodeListToArrayList(parent.getChildNodes());
			int length = list.size();
			boolean inWrap = false;
			String currentUnwrapId = null;
			List<Node> unwrapped = new ArrayList<>();
			for (int i = 0; i < length; i++) {
				Node n = list.get(i);
				if (n.getNodeType() == Node.ELEMENT_NODE) {
					Element e = (Element) n;
					String currentEltUnwrapId = e
							.getAttribute(ATTR_UNWRAP_EXPANDO_ID);
					String currentEltWrapId = e
							.getAttribute(ATTR_WRAP_EXPANDO_ID);
					if (e.getAttribute("id").startsWith("bnj_")) {
						continue;
					}
					if (Ax.notBlank(currentEltWrapId)) {
						flushToUnwrap();
						if (e.getChildNodes().getLength() == 0
								&& e.getTagName().equalsIgnoreCase("A")) {
							int debug = 3;
						} else {
							DomNode domNode = DomNode.from(e);
							List<DomNode> nonProcesingInstructionChildren = domNode.children
									.stream()
									.filter(n2 -> !n2.isProcessingInstruction())
									.collect(Collectors.toList());
							Preconditions
									.checkState(nonProcesingInstructionChildren
											.size() == 1);
							Preconditions
									.checkState(nonProcesingInstructionChildren
											.get(0).isText());
							kids.add(nonProcesingInstructionChildren.get(0)
									.domNode());
						}
					} else {
						if (Ax.notBlank(currentEltUnwrapId)) {
							if (!Objects.equals(currentEltUnwrapId,
									currentUnwrapId)) {
								flushToUnwrap();
								currentUnwrapId = currentEltUnwrapId;
							}
							currentToUnwrap.add(e);
						} else {
							flushToUnwrap();
							kids.add(n);
						}
					}
					if (unwrappedFirstChildren.containsKey(e)) {
						kids.add(unwrappedFirstChildren.get(e));
					}
				} else {
					flushToUnwrap();
					// text
					kids.add(n);
				}
			}
			flushToUnwrap();
		}

		public final Node getItem(int index) {
			return kids.get(index);
		}

		public final int getLength() {
			return kids.size();
		}

		private void flushToUnwrap() {
			// the interesting bit - here we merge...
			if (currentToUnwrap.size() > 0) {
				Preconditions.checkState(currentToUnwrap.get(0)
						.getNodeType() == Node.ELEMENT_NODE);
				Element firstUnwrap = currentToUnwrap.get(0);
				kids.add(firstUnwrap);
				if (firstUnwrap.getChildNodes().getLength() == 0) {
					if (!unwrappedFirstChildren.containsKey(firstUnwrap)) {
						Element unwrappedFirstChild = currentToUnwrap.get(1);
						unwrappedFirstChildren.put(firstUnwrap,
								unwrappedFirstChild);
					}
				}
				currentToUnwrap.clear();
			}
		}
	}

	/**
	 * 2019.10 - retry. Because wrapped/unwrapped structures will never
	 * completely match (duplicate text nodes e.g.) - drop what won't
	 *
	 * Mostly the app is interested (post-wrap) in extracts - which just use
	 * locations to find ranges. So mildly truncated text nodes will be ok.
	 *
	 * @author nick@alcina.cc
	 */
	class WrappingAwareNodeIterator {
		private NodeWrapList nodes;

		private int length;

		private int idx = 0;

		WrappingAwareNodeIterator(Node parent) {
			nodes = new NodeWrapList(parent);
			length = nodes.getLength();
		}

		Node next() {
			if (idx < length) {
				return nodes.getItem(idx++);
			}
			return null;
		}
	}

	class XpathMapPoint {
		Element elt;

		String prefix;

		public XpathMapPoint(Element elt, String prefix) {
			this.elt = elt;
			this.prefix = prefix;
		}
	}
}
