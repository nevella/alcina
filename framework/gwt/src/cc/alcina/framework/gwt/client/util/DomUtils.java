package cc.alcina.framework.gwt.client.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.ClientLayerLocator;
import cc.alcina.framework.gwt.client.ClientNofications;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.Text;
import com.google.gwt.user.client.ui.RootPanel;

public class DomUtils {
	private static final String TEXT_MARKER = "TEXT()";

	private boolean useXpathMap = true;

	private Node lastContainer = null;

	private Map<String, Node> xpathMap;

	private boolean debug = false;

	private static final String HTML_BLOCKS = ",ADDRESS,BLOCKQUOTE,DIV,DL,H1,H2,H3,H4,H5,"
			+ "H6,IFRAME,ILAYER,LAYER,OL,TABLE,TR,UL,TD,P,HR,BR,LI,";

	private static final String HTML_INVISIBLE_CONTENT_ELEMENTS = ",STYLE,TEXTAREA,SCRIPT,INPUT,SELECT,";

	public static boolean isBlockHTMLElement(Element e) {
		return HTML_BLOCKS.contains("," + e.getTagName().toUpperCase() + ",");
	}

	private static void addVisibleTextNodes(Element element, List<Text> texts) {
		NodeList<Node> nl = element.getChildNodes();
		int length = nl.getLength();
		for (int i = 0; i < length; i++) {
			Node node = nl.getItem(i);
			if (node.getNodeType() == Node.TEXT_NODE) {
				texts.add((Text) node);
			} else if (node.getNodeType() == Node.ELEMENT_NODE
					&& !isInvisibleContentElement((Element) node)) {
				addVisibleTextNodes((Element) node, texts);
			}
		}
	}

	public static List<Text> getVisibleTextNodes(Element root) {
		List<Text> texts = new ArrayList<Text>();
		addVisibleTextNodes(root, texts);
		return texts;
	}

	public static boolean isInvisibleContentElement(Element elt) {
		return HTML_INVISIBLE_CONTENT_ELEMENTS.contains(","
				+ elt.getTagName().toUpperCase() + ",");
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

	public Node findXpathWithIndexedText(String xpathStr, Node container) {
		if (xpathStr.length() == 0) {
			return container;
		}
		String ucXpath = xpathStr.toUpperCase();
		if (useXpathMap) {
			if (lastContainer != container) {
				lastContainer = container;
				xpathMap = new HashMap<String, Node>();
				ClientNofications notifications = ClientLayerLocator.get()
						.notifications();
				if (notifications != null) {
					notifications.metricLogStart("dom-xpath-map");
				}
				generateMap((Element) container, "", xpathMap);
				if (notifications != null) {
					notifications.metricLogEnd("dom-xpath-map");
				}
			}
			Node node = xpathMap.get(ucXpath);
			String singleTextPoss = "TEXT()[1]";
			if (node == null && ucXpath.endsWith(singleTextPoss)) {
				node = xpathMap.get(ucXpath.substring(0, ucXpath.length() - 3));
			}
			return node;
		}
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
			NodeList<Node> nodes = current.getChildNodes();
			current = null;
			int length = nodes.getLength();
			for (int i = 0; i < length; i++) {
				Node node = nodes.getItem(i);
				boolean foundIndexed = false;
				if (asText) {
					foundIndexed = node.getNodeType() == Node.TEXT_NODE;
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
			}
			if (current == null) {
				// if (ucXpath.contains("TABLE")&&!ucXpath.contains("TBODY")){
				// return findXpathWithIndexedText(ucXpath.replace("TABLE",
				// "TABLE/TBODY"), container);
				// }
				// System.out.println(nodes.getLength());
				// for (int i = 0; i < nodes.getLength(); i++) {
				// Node node = nodes.getItem(i);
				// if (node.getNodeType() == Node.ELEMENT_NODE) {
				// System.out.println(DOM.toString((com.google.gwt.user.client.Element)
				// node));
				// }
				// System.out.println(node);
				// }
				return null;
			}
		}
		return current;
	}

	public boolean isUseXpathMap() {
		return this.useXpathMap;
	}

	public void setUseXpathMap(boolean useXpathMap) {
		this.useXpathMap = useXpathMap;
	}

	public void generateMap(Element elt, String prefix,
			Map<String, Node> xpathMap) {
		Map<String, Integer> total = new HashMap<String, Integer>();
		Map<String, Integer> current = new HashMap<String, Integer>();
		NodeList<Node> nodes = elt.getChildNodes();
		if (prefix.length() <= 1) {
			xpathMap.put(prefix, elt);
		}
		int length = nodes.getLength();
		for (int i = 0; i < length; i++) {
			Node node = nodes.getItem(i);
			short nodeType = node.getNodeType();
			if (nodeType == Node.TEXT_NODE || nodeType == Node.ELEMENT_NODE) {
				String marker = nodeType == Node.TEXT_NODE ? DomUtils.TEXT_MARKER
						: node.getNodeName().toUpperCase();
				int c = total.containsKey(marker) ? total.get(marker) : 0;
				total.put(marker, c + 1);
			}
		}
		for (int i = 0; i < length; i++) {
			Node node = nodes.getItem(i);
			short nodeType = node.getNodeType();
			if (nodeType == Node.TEXT_NODE || nodeType == Node.ELEMENT_NODE) {
				String marker = nodeType == Node.TEXT_NODE ? DomUtils.TEXT_MARKER
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
					generateMap((Element) node, xp + "/", xpathMap);
					// this won't cause ambiguity
					if (post.equals("TBODY")) {
						generateMap((Element) node, prefix, xpathMap);
					}
				} else {
					if (debug && !xp.contains("TBODY")) {
						System.out.println("\t\t" + node.getNodeValue());
					}
				}
			}
		}
	}

	public void dumpNearestMatch(String xpathStr, Node container) {
		String ucXpath = xpathStr.toUpperCase();
		String[] sections = ucXpath.split("/");
		String matched = "";
		Node lastMatched = null;
		for (String section : sections) {
			if (matched.length() != 0) {
				matched += "/";
			}
			matched += section;
			Node match = findXpathWithIndexedText(matched, container);
			if (match == null) {
				Map<String, Node> xpathMap = new HashMap<String, Node>();
				debug = true;
				generateMap((Element) lastMatched, "", xpathMap);
				debug = false;
			} else {
				lastMatched = match;
			}
		}
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
					part = "text()";
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
							if (!((Element) n).getTagName().equals(
									((Element) item).getTagName())) {
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
	public void dumpMap() {
		xpathMap = new LinkedHashMap<String, Node>();
		generateMap((Element) lastContainer, "", xpathMap);
		System.out.println("---dump xpath map");
		for (String key : xpathMap.keySet()) {
			System.out.println(key);
		}
		System.out.println("\n---\n\n");
	}

	public static void wrap(Element wrapper, Node toWrap) {
		Element parent = (Element) toWrap.getParentNode();
		parent.insertBefore(wrapper, toWrap);
		parent.removeChild(toWrap);
		wrapper.appendChild(toWrap);
	}

	public static Element getParentElement(Element elt, String tagName) {
		while (elt != null) {
			if (elt.getTagName().equalsIgnoreCase(tagName)) {
				return elt;
			}
			elt = elt.getParentElement();
		}
		return null;
	}

	public static boolean isAncestorOf(Element ancestor, Node possibleChild) {
		Element stop = RootPanel.get().getElement();
		while (possibleChild != null && possibleChild != stop) {
			if (possibleChild == ancestor) {
				return true;
			}
			possibleChild = possibleChild.getParentNode();
		}
		return false;
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

	public static Element getAncestorWithTagName(Node node, String tagName) {
		while (node != null) {
			if (node.getNodeType() == Node.ELEMENT_NODE
					&& node.getNodeName().equalsIgnoreCase(tagName)) {
				return (Element) node;
			}
			node = node.getParentNode();
		}
		return null;
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

	public static class HighlightInfo {
		public String cssClassName;

		public Map<String, String> styleProperties;

		public Map<String, String> properties;

		public HighlightInfo(String cssClassName,
				Map<String, String> styleProperties,
				Map<String, String> properties) {
			this.cssClassName = cssClassName;
			this.styleProperties = styleProperties;
			this.properties = properties;
		}
	}
}
