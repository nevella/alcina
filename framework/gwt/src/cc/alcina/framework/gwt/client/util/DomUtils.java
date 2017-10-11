package cc.alcina.framework.gwt.client.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.dom.client.Text;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.RootPanel;

import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.logic.domaintransform.SequentialIdGenerator;
import cc.alcina.framework.common.client.logic.reflection.ClearOnAppRestartLoc;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.CommonConstants;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.gwt.client.ClientNotifications;

@RegistryLocation(registryPoint = ClearOnAppRestartLoc.class)
public class DomUtils implements NodeFromXpathProvider {
	private static final String DOM_XPATH_MAP = "dom-xpath-map";

	public static final String TEXT_MARKER = "TEXT()";

	public static final String COMMENT_MARKER = "#COMMENT";

	private static final String HTML_INVISIBLE_CONTENT_ELEMENTS = ",STYLE,TEXTAREA,SCRIPT,INPUT,SELECT,";

	public static String ignoreableElementIdPrefix = "IGNORE__";

	public static final String ATTR_UNWRAP_EXPANDO_ID = "__j_unwrap_id";

	public static final String ATTR_WRAP_EXPANDO_ID = "__j_wrap_id";

	public static SequentialIdGenerator expandoIdProvider = new SequentialIdGenerator();

	public static Supplier<Map> mapSupplier = () -> new LinkedHashMap<>();

	public static Stream<Element> ancestorStream(Element element) {
		// FIXME-jadex (not optimal)
		List<Element> elements = new ArrayList<>();
		Node node = element;
		while (node != null && node.getNodeType() == Node.ELEMENT_NODE) {
			elements.add((Element) node);
			node = node.getParentNode();
		}
		return elements.stream();
	}

	public static boolean containsBlocks(Element elt) {
		elt.getChildNodes();
		for (int i = 0; i < elt.getChildCount(); i++) {
			Node child = elt.getChild(i);
			if (child.getNodeType() == Node.ELEMENT_NODE
					&& isBlockHTMLElement((Element) child)) {
				return true;
			}
		}
		return false;
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
			node = nl.getItem(i);
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
		return CommonConstants.HTML_BLOCKS
				.contains("," + e.getTagName().toUpperCase() + ",");
	}

	public static boolean isInvisibleContentElement(Element elt) {
		return isInvisibleContentElement(elt.getTagName());
	}

	public static boolean isInvisibleContentElement(String tagName) {
		return HTML_INVISIBLE_CONTENT_ELEMENTS
				.contains("," + tagName.toUpperCase() + ",");
	}

	public static boolean isVisibleAncestorChain(Element e) {
		while (e != null) {
			if (e.getStyle().getDisplay().equals(Display.NONE.getCssName())) {
				return false;
			}
			e = e.getParentElement();
		}
		return true;
	}

	public static Node lastChildOf(Node node) {
		if (node.getChildCount() == 0) {
			return node;
		}
		return lastChildOf(node.getLastChild());
	}

	public static List<Node> nodeListToArrayList(NodeList list) {
		List<Node> result = new ArrayList<Node>();
		int length = list.getLength();
		for (int i = 0; i < length; i++) {
			Node node = list.getItem(i);
			result.add(node);
		}
		return result;
	}

	public static List<Element> nodeListToElementList(NodeList list) {
		List<Element> result = new ArrayList<Element>();
		int length = list.getLength();
		for (int i = 0; i < length; i++) {
			Node node = list.getItem(i);
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
			Node child = nl.getItem(i);
			parent.insertBefore(child, refChild);
			refChild = child;
		}
		oldNode.getParentNode().removeChild(oldNode);
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
					Node item = childNodes.getItem(i);
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

	public static String toText(String html) {
		Element elt = Document.get().createElement("DIV");
		elt.setInnerHTML(html);
		return DOM.getInnerText((com.google.gwt.user.client.Element) elt);
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

	private boolean debug = false;

	private NodeFromXpathProvider nodeProvider = null;

	private ClientNodeIterator walker;

	Map<Element, DomRequiredSplitInfo> domRequiredSplitInfo = mapSupplier.get();

	Stack<XpathMapPoint> itrStack = null;

	private Map<Node, StringBuilder> exactTextMap;

	private Map<Node, Node> precededByNonHtmlDomNodes = mapSupplier.get();

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
						? " - " + node.getNodeValue() : "";
				System.out.println(key + tc);
			}
		}
		System.out.println("\n---\n\n");
		useXpathMap = true;
	}

	public String dumpMap(boolean regenerate) {
		return dumpMap0(regenerate, xpathMap);
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
				generateMap((Element) lastMatched, "", xpathMap);
				dumpMap0(false, xpathMap);
				if (count > 3) {
					System.out.println("Parent map:");
					xpathMap = mapSupplier.get();
					generateMap((Element) lastMatched.getParentElement(), "",
							xpathMap);
					dumpMap0(false, xpathMap);
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

	public Node findXpathWithIndexedText(String xpathStr, Node container) {
		if (nodeProvider != null) {
			return nodeProvider.findXpathWithIndexedText(xpathStr, container);
		}
		if (xpathStr.length() == 0) {
			return container;
		}
		String ucXpath = xpathStr.toUpperCase();
		if (useXpathMap) {
			if (lastContainer != container) {
				lastContainer = container;
				xpathMap = mapSupplier.get();
				ClientNotifications notifications = Registry
						.implOrNull(ClientNotifications.class);
				if (notifications != null) {
					notifications.metricLogStart(DOM_XPATH_MAP);
				}
				generateMap((Element) container, "", xpathMap);
				if (notifications != null) {
					notifications.metricLogEnd(DOM_XPATH_MAP);
				}
			}
			Node node = xpathMap.get(ucXpath);
			String singleTextPoss = "TEXT()[1]";
			String possiblyWrappedTextPost = "TEXT()";
			if (node == null && ucXpath.endsWith(singleTextPoss)) {
				node = xpathMap.get(ucXpath.substring(0, ucXpath.length() - 3));
			}
			if (node == null && ucXpath.endsWith(possiblyWrappedTextPost)) {
				node = xpathMap.get(ucXpath + "[1]");
			}
			if (node == null && container.getNodeName().equals("judgment")
					&& ucXpath.contains("/")) {
				if (ucXpath.startsWith("/")) {
					ucXpath = ucXpath.substring(1);
				}
				node = xpathMap
						.get(ucXpath.substring(ucXpath.indexOf("/") + 1));
			}
			return node;
		} else {
			if (lastContainer != container) {
				lastContainer = container;
				walker = new ClientNodeIterator(container,
						ClientNodeIterator.SHOW_ELEMENT
								| ClientNodeIterator.SHOW_TEXT);
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
						continue;// ignore - this was orginally a single text
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

	public void generateMap(Element elt, String prefix,
			Map<String, Node> xpathMap) {
		walker = new ClientNodeIterator(elt,
				ClientNodeIterator.SHOW_ELEMENT | ClientNodeIterator.SHOW_TEXT);
		generateMap0(elt, prefix, xpathMap);
	}

	public void generateMapItr(XpathMapPoint point) {
		Map<String, Integer> total = mapSupplier.get();
		Map<String, Integer> current = mapSupplier.get();
		Element elt = point.elt;
		String prefix = point.prefix;
		NodeList<Node> nodes = elt.getChildNodes();
		if (prefix.length() <= 1) {
			xpathMap.put(prefix, elt);
		}
		int length = nodes.getLength();
		for (int i = 0; i < length; i++) {
			Node node = nodes.getItem(i);
			short nodeType = node.getNodeType();
			if (nodeType == Node.TEXT_NODE || nodeType == Node.ELEMENT_NODE) {
				String marker = nodeType == Node.TEXT_NODE
						? DomUtils.TEXT_MARKER
						: node.getNodeName().toUpperCase();
				int c = total.containsKey(marker) ? total.get(marker) : 0;
				total.put(marker, c + 1);
			}
		}
		for (int i = 0; i < length; i++) {
			Node node = nodes.getItem(i);
			short nodeType = node.getNodeType();
			if (nodeType == Node.TEXT_NODE || nodeType == Node.ELEMENT_NODE) {
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
				if (debug && !xp.contains("TBODY")) {
					System.out.println(xp);
				}
				xpathMap.put(xp, node);
				if (nodeType == Node.ELEMENT_NODE) {
					itrStack.push(new XpathMapPoint((Element) node, xp + "/"));
					// this won't cause ambiguity
					if (post.equals("TBODY")) {
						itrStack.push(
								new XpathMapPoint((Element) node, prefix));
					}
				} else {
					if (debug && !xp.contains("TBODY")) {
						System.out.println("\t\t" + node.getNodeValue());
					}
				}
			}
		}
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

	public void invalidateUnwrapOrIgnoreCache() {
		// unwrapOrIgnoreCache = new IdentityHashMap<Node, Node>(10000);
	}

	public boolean isUseXpathMap() {
		return this.useXpathMap;
	}

	public boolean iterateCache(int numberIterations) {
		for (int i = 0; i < numberIterations && !itrStack.isEmpty(); i++) {
			generateMapItr(itrStack.pop());
		}
		if (itrStack.isEmpty()) {
			ClientNotifications notifications = Registry
					.implOrNull(ClientNotifications.class);
			if (notifications != null) {
				notifications.metricLogEnd(DOM_XPATH_MAP);
			}
		}
		return !itrStack.isEmpty();
	}

	public void resetCache(Node container) {
		if (useXpathMap) {
			if (lastContainer != container) {
				lastContainer = container;
				xpathMap = mapSupplier.get();
				ClientNotifications notifications = Registry
						.implOrNull(ClientNotifications.class);
				if (notifications != null) {
					notifications.metricLogStart(DOM_XPATH_MAP);
				}
				itrStack = new Stack<DomUtils.XpathMapPoint>();
				itrStack.add(new XpathMapPoint((Element) container, ""));
			}
		}
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

	public void unwrap(Element el) {
		Element parent = el.getParentElement();
		NodeList<Node> nl = el.getChildNodes();
		Node[] tmp = new Node[nl.getLength()];
		for (int i = 0; i < nl.getLength(); i++) {
			tmp[i] = nl.getItem(i);
		}
		for (int i = 0; i < tmp.length; i++) {
			Node n = tmp[i];
			el.removeChild(n);
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
		Element parent = (Element) toWrap.getParentNode();
		parent.insertBefore(wrapper, toWrap);
		parent.removeChild(toWrap);
		wrapper.appendChild(toWrap);
	}

	private String dumpMap0(boolean regenerate, Map<String, Node> xpathMap) {
		StringBuilder builder = new StringBuilder();
		if (regenerate) {
			exactTextMap = mapSupplier.get();
			xpathMap = mapSupplier.get();
			generateMap((Element) lastContainer, "", xpathMap);
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

	private void generateMap0(Element container, String prefix,
			Map<String, Node> xpathMap) {
		if (container == null) {
			return;
		}
		Map<String, Integer> total = mapSupplier.get();
		Map<String, Integer> current = mapSupplier.get();
		NodeList<Node> nodes = container.getChildNodes();
		if (prefix.length() <= 1) {
			xpathMap.put(prefix, container);
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
				if (nodeType == Node.ELEMENT_NODE) {
					generateMap0((Element) node, xp + "/", xpathMap);
					// this won't cause ambiguity
					if (post.equals("TBODY")) {
						generateMap0((Element) node, prefix, xpathMap);
					}
				} else {
					if (exactTextMap != null) {
						cumulativeText = new StringBuilder();
						exactTextMap.put(node, cumulativeText);
					}
				}
			}
			// if (debug && !xp.contains("TBODY") && nodeType == Node.TEXT_NODE)
			// {
			//
			// }
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
		throw new RuntimeException();
	}

	boolean requiresSplit(Element ancestor, Element wrapper) {
		if (ancestor.getTagName().toLowerCase().equals("a")
				&& wrapper.getTagName().toLowerCase().equals("a")) {
			return true;
		}
		return false;
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

		public void applyTo(Element wrapper) {
			wrapper.setClassName(cssClassName);
			for (Entry<String, String> entry : styleProperties.entrySet()) {
				wrapper.getStyle().setProperty(entry.getKey(),
						entry.getValue());
			}
			for (Entry<String, String> entry : properties.entrySet()) {
				wrapper.setPropertyString(entry.getKey(), entry.getValue());
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

	public static class IsBlockFilter implements CollectionFilter<Node> {
		@Override
		public boolean allow(Node o) {
			return o.getNodeType() == Node.ELEMENT_NODE
					&& isBlockHTMLElement((Element) o);
		}
	}

	/**
	 * TODO - av2 - optimise the display-none check on the client
	 *
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
			Element grand = splitFrom.getParentElement();
			NodeList<Node> nl = splitFrom.getChildNodes();
			splitEnd = (Element) splitFrom.cloneNode(false);
			if (splitAround == null) {
				List<Node> children = nodeListToArrayList(
						splitFrom.getChildNodes());
				Node insertionPoint = splitFrom;
				for (Node node : children) {
					grand.insertAfter(node, insertionPoint);
					insertionPoint = node;
				}
				grand.insertAfter(splitEnd, insertionPoint);
			} else {
				boolean found = false;
				for (int i = 0; i < nl.getLength(); i++) {
					Node n = nl.getItem(i);
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
				grand.insertAfter(splitAround, splitFrom);
				grand.insertAfter(splitEnd, splitAround);
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
				if (maybeRedundantSplits.get(i).getChildCount() == 0) {
					maybeRedundantSplits.get(i).removeFromParent();
				}
			}
		}

		public void unsplit() {
			splitFrom.appendChild(splitAround);
			for (Node n : contents) {
				splitFrom.appendChild(n);
			}
			splitFrom.removeAttribute(ATTR_UNWRAP_EXPANDO_ID);
			splitEnd.removeFromParent();
		}
	}

	interface MaybeWrappedNodeCollection {
		public abstract Node getItem(int index);

		public abstract int getLength();
	}

	static class MaybeWrappedNodeCollectionList
			implements MaybeWrappedNodeCollection {
		List<Node> aList = new ArrayList<Node>();

		public MaybeWrappedNodeCollectionList(NodeList list) {
			int length = list.getLength();
			boolean inWrap = false;
			for (int i = 0; i < length; i++) {
				Node n = list.getItem(i);
				boolean nHasUnwrap = false;
				if (!inWrap) {
					aList.add(n);
				}
				if (n.getNodeType() == Node.ELEMENT_NODE) {
					Element e = (Element) n;
					nHasUnwrap = e.getAttribute(ATTR_UNWRAP_EXPANDO_ID)
							.length() > 0;
					if (nHasUnwrap) {
						inWrap = !inWrap;
					}
				}
			}
		}

		@Override
		public final Node getItem(int index) {
			return aList.get(index);
		}

		@Override
		public final int getLength() {
			return aList.size();
		}
	}

	/*
	 * this makes a linear list of unwrapped elts - including wrapped (they're
	 * filtered elsewhere) e.g.
	 * <a-uw><span></a-uw><a-w>blah</a-w><a-uw><i></a-uw> becomes
	 * <span><a-w>blah</a-w><i>
	 */
	static class NodeWrapList implements MaybeWrappedNodeCollection {
		List<Node> kids = new ArrayList<Node>();

		public NodeWrapList(Element hasUnwrap) {
			List<Node> sibs = nodeListToArrayList(
					hasUnwrap.getParentNode().getChildNodes());
			int length = sibs.size();
			boolean inWrap = false;
			String expandoId = hasUnwrap.getAttribute(ATTR_UNWRAP_EXPANDO_ID);
			for (int i = 0; i < length; i++) {
				Node n = sibs.get(i);
				boolean nHasUnwrap = false;
				if (!inWrap) {
					if (n == hasUnwrap) {
						inWrap = true;
					} else {
						continue;
					}
				}
				if (n.getNodeType() == Node.ELEMENT_NODE) {
					Element e = (Element) n;
					String currentEltExpandoId = e
							.getAttribute(ATTR_UNWRAP_EXPANDO_ID);
					nHasUnwrap = currentEltExpandoId.length() > 0;
					if (nHasUnwrap) {
						kids.addAll(nodeListToArrayList(e.getChildNodes()));
						if (n != hasUnwrap
								&& expandoId.equals(currentEltExpandoId)) {
							break;// finished expando
						} else {
							continue;// ignore this (different) expando id
						}
					} // normal elementnode
					else {
						kids.add(n);
					}
				} else {
					kids.add(n);// text
				}
			}
		}

		@Override
		public final Node getItem(int index) {
			return kids.get(index);
		}

		@Override
		public final int getLength() {
			return kids.size();
		}
	}

	class WrappingAwareNodeIterator {
		private MaybeWrappedNodeCollection nodes;

		private int length;

		private int idx = 0;

		private boolean parentHasUnwrap = false;

		WrappingAwareNodeIterator(Node parent) {
			nodes = new MaybeWrappedNodeCollectionList(parent.getChildNodes());
			if (parent.getNodeType() == Node.ELEMENT_NODE) {
				Element parentElt = (Element) parent;
				parentHasUnwrap = parentElt.getAttribute(ATTR_UNWRAP_EXPANDO_ID)
						.length() > 0;
				if (parentHasUnwrap) {
					nodes = new NodeWrapList(parentElt);
				}
			}
			length = nodes.getLength();
		}

		// covers both old and new variants of wrap() {span id=bnj_ or a
		// __unwrap =}
		Node next() {
			while (idx < length) {
				Node node = nodes.getItem(idx++);
				short nodeType = node.getNodeType();
				if (nodeType != Node.ELEMENT_NODE) {
					return node;
				}
				Element e = (Element) node;
				if (e.getAttribute("id").startsWith(ignoreableElementIdPrefix)
						|| e.getAttribute(ATTR_WRAP_EXPANDO_ID).length() > 0) {
					if (node.getNodeName().equalsIgnoreCase("span") || e
							.getAttribute(ATTR_WRAP_EXPANDO_ID).length() > 0) {
						walker.setCurrentNode(node);
						while (walker.nextNode() != null) {
							node = walker.getCurrentNode();
							if (node.getNodeType() == Node.ELEMENT_NODE
									&& ((Element) node).getAttribute("id")
											.startsWith(
													ignoreableElementIdPrefix)) {
							} else {
								return node;
							}
						}
					} else {
						// fallthrough, continue while
					}
				} else if (e.getAttribute(ATTR_UNWRAP_EXPANDO_ID).length() > 0
						&& e.getChildCount() == 0) {
					return e;
					// empty unwrap node, ignore
				} else {
					return e;
				}
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
