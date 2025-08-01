/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package cc.alcina.framework.entity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.xerces.parsers.DOMParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import org.w3c.dom.ranges.DocumentRange;
import org.w3c.dom.ranges.Range;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.TreeWalker;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.dom.DomDocument;
import cc.alcina.framework.common.client.dom.DomEnvironment.StyleResolver;
import cc.alcina.framework.common.client.dom.DomEnvironment.StyleResolverHtml;
import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.DomNode.DomNodeTree;
import cc.alcina.framework.common.client.dom.Location;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CachingMap;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.HtmlConstants;
import cc.alcina.framework.common.client.util.Ref;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.entity.util.CachingConcurrentMap;
import cc.alcina.framework.entity.xml.EntityCleaner;

/**
 * @author Nick Reddel
 */
public class XmlUtils {
	static Logger logger = LoggerFactory.getLogger(XmlUtils.class);

	private static boolean useJAXP;

	public static boolean noTransformerCaching;

	private static DocumentBuilder db;

	public static final int A4_MAX_PIXEL_WIDTH = 550;

	public static final int A4_MAX_PIXEL_HEIGHT = 950;

	public static final int A4_MAX_PIXEL_HEIGHT_PRINT = 850;

	public static final String CONTEXT_MUTE_XML_SAX_EXCEPTIONS = XmlUtils.class
			.getName() + ".CONTEXT_MUTE_XML_SAX_EXCEPTIONS";

	public static final String CONTEXT_XSL_STRIP_WHITESPACE = XmlUtils.class
			.getName() + ".CONTEXT_XSL_STRIP_WHITESPACE";

	private static XPointerConverter xPointerConverter;

	private static Set<String> selfClosingTags = Arrays.asList("area", "base",
			"br", "col", "command", "embed", "hr", "img", "input", "keygen",
			"link", "meta", "param", "source", "track", "wbr").stream()
			.collect(Collectors.toSet());

	public static List<String> ignoreBlockChildForHasNbText = Arrays
			.asList(new String[] { "HTML", "BODY", "HEAD" });

	public static final transient String CONTEXT_IGNORE_EMPTY_ANCHORS = XmlUtils.class
			.getName() + ".CONTEXT_IGNORE_EMPTY_ANCHORS";

	private static final String TRANSFORMER_CACHE_MARKER_NULL = "TRANSFORMER_CACHE_MARKER_NULL";

	private static final String TRANSFORMER_CACHE_MARKER_STREAM_XML = "TRANSFORMER_CACHE_MARKER_STREAM_XML";

	private static CachingMap<String, TransformerPool> transformersPool = new CachingConcurrentMap<>(
			key -> new TransformerPool(true), 10);

	private static void _streamXML(Node n, Writer w, OutputStream s)
			throws Exception {
		transformDoc(new DOMSource(n), null,
				w == null ? new StreamResult(s) : new StreamResult(w),
				TRANSFORMER_CACHE_MARKER_STREAM_XML, null);
	}

	public static String addHtmlSchema(String html) {
		return html.replace("<html>",
				"<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\">");
	}

	public static List<Node> allChildren(Node node) {
		Stack<Node> nodes = new Stack<Node>();
		nodes.push(node);
		List<Node> result = new ArrayList<>();
		while (!nodes.isEmpty()) {
			node = nodes.pop();
			result.add(node);
			NodeList nl = node.getChildNodes();
			Node lastChild = null;
			for (int i = 0; i < nl.getLength(); i++) {
				nodes.push(nl.item(i));
			}
		}
		return result;
	}

	public static boolean areSeparatedOnlyByWhitespace(Element e1, Element e2) {
		List<Node> list = nodeListToList(e1.getParentNode().getChildNodes());
		int idx1 = list.indexOf(e1);
		int idx2 = list.indexOf(e2);
		if (idx1 == -1 || idx2 == -1) {
			return false;
		}
		if (idx1 > idx2) {
			int tmp = idx2;
			idx2 = idx1;
			idx1 = tmp;
		}
		for (int idx = idx1 + 1; idx < idx2; idx++) {
			if (!isWhitespaceText(list.get(idx))) {
				return false;
			}
		}
		return true;
	}

	public static String balanceForXhtml(String htmlContent) {
		htmlContent = htmlContent.replaceAll("(?i)<(META|BR|IMG)([^>]*?)>",
				"<$1$2/>");
		return htmlContent;
	}

	public static List<Element> childElements(Node node) {
		return nodeListToElementList(node.getChildNodes());
	}

	public static void cleanNamespacedAttributes(Document doc) {
		DomDocument.from(doc).descendants().filter(DomNode::isElement)
				.forEach(n -> {
					if (n.w3cElement().hasAttributes()) {
						n.attributes().keySet().stream()
								.collect(Collectors.toList())
								.forEach(attrName -> {
									if (attrName.contains(":")) {
										n.removeAttribute(attrName);
									}
								});
					}
				});
	}

	public static String cleanXmlHeaders(String xml) {
		xml = xml.replaceAll("<\\?xml.+?\\?>", "");
		String regex = "<!DOCTYPE .+?>";
		Pattern p = Pattern.compile(regex,
				Pattern.MULTILINE | Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(xml);
		xml = m.replaceAll("");
		return xml;
	}

	public static void clearChildren(Node n) {
		NodeList nl = n.getChildNodes();
		for (int i = nl.getLength() - 1; i >= 0; i--) {
			n.removeChild(nl.item(i));
		}
	}

	public static Document createDocument() throws Exception {
		if (db == null) {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			db = dbf.newDocumentBuilder();
		}
		return db.newDocument();
	}

	public static Element createElementAttr(Document doc, String tagName,
			String attr, String value) {
		Element e = doc.createElement(tagName);
		e.setAttribute(attr, value);
		return e;
	}

	public static Element createSimpleTextElement(Document doc, String tag,
			String textContent) {
		Element element = doc.createElement(tag);
		element.setTextContent(textContent);
		return element;
	}

	public static String divWrapper(String content, boolean wrap) {
		if (wrap) {
			return String.format("<div>%s</div>", content);
		} else {
			return content.replaceFirst("(?is)^.*?<div>(.+)</div>$", "$1");
		}
	}

	public static Element earliest(Element... elements) {
		Element earliest = null;
		for (Element element : elements) {
			if (element != null) {
				if (earliest == null) {
					earliest = element;
				} else {
					if (isEarlierThan(element, earliest)) {
						earliest = element;
					}
				}
			}
		}
		return earliest;
	}

	public static String elementNamesToLowerCase(String s) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			sb.append(c);
			if (c == '<') {
				if (s.substring(i + 1, i + 2).matches("[/a-zA-Z_]")) {
					int x = s.indexOf(">", i);
					int y = s.indexOf(" ", i);
					x = y != -1 && y < x ? y : x;
					String tagName = s.substring(i + 1, x);
					sb.append(tagName.toLowerCase());
					i += tagName.length();
				}
			}
		}
		return sb.toString();
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

	public static Element firstElementChild(Node node) {
		return CommonUtils.first(nodeListToElementList(node.getChildNodes()));
	}

	public static String fixStyleNodeContents(String result) {
		return Pattern.compile("(?is)<style>.+?</style>").matcher(result)
				.replaceAll(mr -> mr.group().replace("&gt;", ">"));
	}

	public static Element getAncestorWithTagName(Node n, String tagName) {
		return getAncestorWithTagName(n, tagName, false);
	}

	public static Element getAncestorWithTagName(Node n, String tagName,
			boolean includeNode) {
		boolean wildcard = tagName.equals("*");
		while (n.getParentNode() != null) {
			Node test = includeNode ? n : n.getParentNode();
			if (test.getNodeType() == Node.ELEMENT_NODE) {
				Element el = (Element) test;
				if (wildcard || el.getTagName().equalsIgnoreCase(tagName)) {
					return el;
				}
			}
			n = includeNode ? n : n.getParentNode();
			includeNode = false;
		}
		return null;
	}

	public static StringMap getAttributeMap(Element node) {
		StringMap result = new StringMap();
		NamedNodeMap nnm = node.getAttributes();
		for (int idx = 0; idx < nnm.getLength(); idx++) {
			Attr attr = (Attr) nnm.item(idx);
			result.put(attr.getName(), attr.getValue());
		}
		return result;
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

	public static Element getContainingBlock(Node n,
			Predicate<Element> blockTest) {
		while (n != null) {
			if (n.getNodeType() == Node.ELEMENT_NODE
					&& (isBlockHTMLElement((Element) n)
							|| blockTest.test((Element) n))) {
				return (Element) n;
			}
			n = n.getParentNode();
		}
		return null;
	}

	public static Element getContainingElement(Node n) {
		while (n != null) {
			if (n.getNodeType() == Node.ELEMENT_NODE) {
				return (Element) n;
			}
			n = n.getParentNode();
		}
		return null;
	}

	public static Element getElementExt(Element root, String tagName,
			String attrName, String attrValue) {
		List<Element> elementList = nodeListToElementList(
				root.getElementsByTagName(tagName));
		for (Element element : elementList) {
			if (element.getAttribute(attrName).equals(attrValue)) {
				return element;
			}
		}
		return null;
	}

	public static Text getFirstNonWhitespaceTextChild(Node node) {
		NodeList nl = node.getChildNodes();
		int length = nl.getLength();
		for (int i = 0; i < length; i++) {
			node = nl.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Text t = getFirstNonWhitespaceTextChild(node);
				if (t != null) {
					return t;
				}
			}
			if (node.getNodeType() == Node.TEXT_NODE) {
				if (!SEUtilities.isWhitespace(node.getTextContent())) {
					return (Text) node;
				}
			}
		}
		return null;
	}

	public static Text getFirstTextChildOrSelf(Node node) {
		if (node.getNodeType() == Node.TEXT_NODE) {
			return (Text) node;
		}
		NodeList nl = node.getChildNodes();
		int length = nl.getLength();
		for (int i = 0; i < length; i++) {
			node = nl.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Text t = getFirstTextChildOrSelf(node);
				if (t != null) {
					return t;
				}
			}
			if (node.getNodeType() == Node.TEXT_NODE) {
				return (Text) node;
			}
		}
		return null;
	}

	public static Element getNextElement(Node node) {
		List<Node> kids = nodeListToList(node.getParentNode().getChildNodes());
		for (int idx = kids.indexOf(node) + 1; idx < kids.size(); idx++) {
			Node kid = kids.get(idx);
			if (kid.getNodeType() == Node.ELEMENT_NODE) {
				return (Element) kid;
			}
		}
		return null;
	}

	public static int getNodeIndexInParent(Node n) {
		NodeList siblings = n.getParentNode().getChildNodes();
		int index = 0;
		short nodeType = n.getNodeType();
		int length = siblings.getLength();
		for (int i = 0; i < length; i++) {
			Node test = siblings.item(i);
			if (test.getNodeType() == nodeType) {
				if (nodeType == Node.ELEMENT_NODE) {
					if (test.getNodeName().equalsIgnoreCase(n.getNodeName())) {
						index++;
					}
				} else {
					index++;
				}
			}
			if (test == n) {
				break;
			}
		}
		return index;
	}

	public static Element getParentElement(Element element) {
		return (Element) element.getParentNode();
	}

	public static Element getPreContainingBlock(Element elt) {
		Element preContainer = elt;
		while (elt != null) {
			if (isBlockHTMLElement(elt)) {
				return preContainer;
			}
			preContainer = elt;
			Node n = elt.getParentNode();
			if (n.getNodeType() != Node.ELEMENT_NODE) {
				return null;
			}
			elt = (Element) n;
		}
		return null;
	}

	public static Element getPreviousElement(Node node) {
		List<Node> kids = nodeListToList(node.getParentNode().getChildNodes());
		for (int idx = kids.indexOf(node) - 1; idx >= 0; idx--) {
			Node kid = kids.get(idx);
			if (kid.getNodeType() == Node.ELEMENT_NODE) {
				return (Element) kid;
			}
		}
		return null;
	}

	public static Node getRootContainer(Node node) {
		if (node == null)
			return null;
		while (node.getParentNode() != null)
			node = node.getParentNode();
		return node;
	}

	public static Element getSoleElementExceptingWhitespace(Element parent) {
		NodeList nl = parent.getChildNodes();
		Element found = null;
		int length = nl.getLength();
		for (int i = 0; i < length; i++) {
			Node item = nl.item(i);
			if (item.getNodeType() == Node.ELEMENT_NODE) {
				if (LooseContext.is(XmlUtils.CONTEXT_IGNORE_EMPTY_ANCHORS)
						&& item.getNodeName().equals("A")
						&& item.getTextContent().isEmpty()) {
					continue;
				}
				if (found == null) {
					found = (Element) item;
				} else {
					return null;
				}
			} else {
				if (!SEUtilities.isWhitespaceOrEmpty(item.getTextContent())) {
					return null;
				}
			}
		}
		return found;
	}

	public static Node getSucceedingBlock(Node node) {
		SurroundingBlockTuple tuple = getSurroundingBlockTuple(node);
		tuple.detach();
		return tuple.nextBlock;
	}

	public static SurroundingBlockTuple getSurroundingBlockTuple(Node node) {
		return getSurroundingBlockTuple(node, new StyleResolverHtml());
	}

	public static SurroundingBlockTuple getSurroundingBlockTuple(Node node,
			StyleResolver blockResolver) {
		DomDocument xmlDoc = DomDocument.from(node.getOwnerDocument());
		DomNode prev = xmlDoc.nodeFor(node);
		DomNode next = prev;
		SurroundingBlockTuple tuple = new SurroundingBlockTuple(prev.w3cNode());
		DomNode cursor = prev;
		if (!hasLegalRootContainer(node)) {
			throw new RuntimeException("Node has no legal root container");
		}
		DomNode currentBlockAncestor = blockResolver.getContainingBlock(cursor)
				.orElse(null);
		while (true) {
			cursor = cursor.relative().previousSibOrParentSibNode();
			if (cursor.isDocumentNode()) {
				tuple.prevBlock = null;
				break;
			}
			if (cursor.style().isOrContainsBlock(blockResolver)
					|| blockResolver.getContainingBlock(cursor)
							.orElse(null) != currentBlockAncestor) {
				tuple.prevBlock = cursor.w3cNode();
				break;
			} else {
				if (hasLegalRootContainer(cursor.w3cNode())) {
					prev = cursor;
				}
			}
		}
		cursor = next;
		currentBlockAncestor = blockResolver.getContainingBlock(cursor)
				.orElse(null);
		while (true) {
			cursor = cursor.relative().treeSubsequentNodeNoDescent();
			if (cursor == null || cursor.isDocumentNode()) {
				tuple.nextBlock = null;
				break;
			}
			if (cursor.style().isOrContainsBlock(blockResolver)
					|| blockResolver.getContainingBlock(cursor)
							.orElse(null) != currentBlockAncestor) {
				tuple.nextBlock = cursor.w3cNode();
				break;
			} else {
				if (hasLegalRootContainer(cursor.w3cNode())) {
					next = cursor;
				}
			}
		}
		tuple.firstNode = prev.w3cNode();
		tuple.lastNode = next.w3cNode();
		return tuple;
	}

	public static boolean hasAncestorWithTagName(Node n,
			Collection<String> blks, Node stop) {
		while (n != stop && n != null) {
			if (blks.contains(n.getNodeName())) {
				return true;
			}
			n = n.getParentNode();
		}
		return false;
	}

	public static boolean hasAncestorWithTagName(Node n, String tagName) {
		return hasAncestorWithTagName(n,
				Arrays.asList(new String[] { tagName }), null);
	}

	public static boolean hasLegalRootContainer(Node node) {
		if (node == null)
			return false;
		Node rootContainer = getRootContainer(node);
		switch (rootContainer.getNodeType()) {
		case Node.ATTRIBUTE_NODE:
		case Node.DOCUMENT_NODE:
		case Node.DOCUMENT_FRAGMENT_NODE:
			return true;
		}
		return false;
	}

	public static boolean hasOnlyTextChildren(Node node) {
		return !nodeListToList(node.getChildNodes()).stream()
				.anyMatch(n -> n.getNodeType() != Node.TEXT_NODE);
	}

	public static boolean hasOnlyWhitespaceChildren(Element elt) {
		return !nodeListToList(elt.getChildNodes()).stream()
				.anyMatch(n -> n.getNodeType() != Node.TEXT_NODE || SEUtilities
						.normalizeWhitespaceAndTrim(n.getTextContent())
						.length() > 0);
	}

	public static void insertAfter(Node newNode, Node insertAfter) {
		insertAfter.getParentNode().insertBefore(newNode,
				insertAfter.getNextSibling());
	}

	public static Source interpolateStreamSource(InputStream trans) {
		try {
			String xsl = Io.read().fromStream(trans).asString();
			Pattern p = Pattern.compile("<\\?import \"res:/(.+?)\"\\?>");
			Matcher m = p.matcher(xsl);
			StringBuffer out = new StringBuffer();
			while (m.find()) {
				String toImport = Io.read().resource(m.group(1)).asString();
				m.appendReplacement(out, Matcher.quoteReplacement(toImport));
			}
			m.appendTail(out);
			if (LooseContext.is(CONTEXT_XSL_STRIP_WHITESPACE)) {
				DomDocument doc = DomDocument.from(out.toString());
				doc.descendants().filter(DomNode::isText).forEach(
						n -> n.setText(trimAndNormaliseWrappingNewlines(
								n.parent().nameIs("xsl:text"),
								n.textContent())));
				out = new StringBuffer(streamXML(doc.w3cDoc()));
			}
			return new StreamSource(
					Io.read().string(out.toString()).asInputStream());
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public static boolean isAncestorOf(Node possParent, Node node) {
		while (node != null) {
			if (node == possParent) {
				return true;
			}
			node = node.getParentNode();
		}
		return false;
	}

	public static boolean isBlockHTMLElement(Element e) {
		String tagName = e.getTagName().toUpperCase();
		return isBlockTag(tagName);
	}

	public static boolean isBlockTag(String tagName) {
		return HtmlConstants.isHtmlBlock(tagName);
	}

	public static boolean isCompleteBlock(Element elt) {
		Element block = getContainingBlock(elt);
		if (block != null && elt.getTextContent().trim()
				.equals(block.getTextContent().trim())) {
			return true;
		}
		Element preContainer = getPreContainingBlock(elt);
		if (preContainer.getTextContent().trim()
				.equals(elt.getTextContent().trim())) {
			Element prev = previousSibOrParentSib(preContainer);
			Element next = nextSibOrParentSib(preContainer);
			if (isBlockHTMLElement(prev) && isBlockHTMLElement(next)) {
				return true;
			}
		}
		return false;
	}

	public static boolean isEarlierThan(Node n1, Node n2) {
		Range r1 = ((DocumentRange) n1.getOwnerDocument()).createRange();
		Range r2 = ((DocumentRange) n2.getOwnerDocument()).createRange();
		r1.setStartBefore(n1);
		r2.setStartBefore(n2);
		short result = r1.compareBoundaryPoints(Range.START_TO_START, r2);
		r1.detach();
		r2.detach();
		return result < 0;
	}

	public static boolean isFirstNonWhitespaceChild(Node node) {
		List<Node> kids = nodeListToList(node.getParentNode().getChildNodes());
		for (Node kid : kids) {
			if (node == kid) {
				return true;
			}
			if (kid.getNodeType() != Node.TEXT_NODE) {
				return false;
			}
			if (!SEUtilities.isWhitespace(kid.getTextContent())) {
				return false;
			}
		}
		return false;
	}

	public static boolean isInvisibleContentElement(Element elt) {
		return HtmlConstants.HTML_INVISIBLE_CONTENT_ELEMENTS
				.contains("," + elt.getTagName().toUpperCase() + ",");
	}

	public static boolean isNoBlockChildrenHasText(Node item) {
		NodeList nl;
		if (item.getNodeType() == Node.ELEMENT_NODE) {
			Element el1 = (Element) item;
			String tagName = item.getNodeName().toUpperCase();
			if (XmlUtils.ignoreBlockChildForHasNbText.contains(tagName)
					|| isInvisibleContentElement(el1)) {
				return false;
			}
			nl = item.getChildNodes();
			for (int i = 0; i < nl.getLength(); i++) {
				Node child = nl.item(i);
				if (child.getNodeType() == Node.ELEMENT_NODE) {
					Element elt = (Element) child;
					if (isBlockHTMLElement(elt) && XmlUtils
							.getFirstNonWhitespaceTextChild(child) != null) {
						return false;
					}
				}
			}
			return XmlUtils.getFirstNonWhitespaceTextChild(item) != null;
		}
		return false;
	}

	public static boolean isOrContainsBlock(Node sib) {
		if (sib == null || sib.getNodeType() != Node.ELEMENT_NODE) {
			return false;
		}
		Element elt = (Element) sib;
		if (isBlockHTMLElement(elt)) {
			return true;
		}
		NodeList list = elt.getElementsByTagName("*");
		int length = list.getLength();
		for (int i = 0; i < length; i++) {
			if (isBlockHTMLElement((Element) list.item(i))) {
				return true;
			}
		}
		return false;
	}

	public static boolean isSoleNonWhitespaceChild(Element node) {
		List<Node> kids = nodeListToList(node.getParentNode().getChildNodes());
		for (Node kid : kids) {
			if (node == kid) {
				continue;
			}
			if (kid.getNodeType() != Node.TEXT_NODE) {
				return false;
			}
			if (!SEUtilities.isWhitespace(kid.getTextContent())) {
				return false;
			}
		}
		return true;
	}

	public static boolean isUseJAXP() {
		return useJAXP;
	}

	public static boolean isWhitespaceText(Node node) {
		return node.getNodeType() == Node.TEXT_NODE
				&& SEUtilities.isWhitespaceOrEmpty(node.getTextContent());
	}

	public static Node lastDirectChild(Element element) {
		NodeList childNodes = element.getChildNodes();
		Node node = childNodes.item(childNodes.getLength() - 1);
		return node;
	}

	public static Element lastDirectElementChild(Element element) {
		NodeList childNodes = element.getChildNodes();
		Node node = childNodes.item(childNodes.getLength() - 1);
		if (node.getNodeType() == Node.ELEMENT_NODE) {
			return (Element) node;
		}
		return getPreviousElement(node);
	}

	public static Element lastElementChild(Element element) {
		NodeList childNodes = element.getChildNodes();
		int length = childNodes.getLength();
		for (int i = length - 1; i >= 0; i--) {
			Node node = childNodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				return lastElementChild((Element) node);
			}
		}
		return element;
	}

	public static Text lastNonWsTextChild(Element element) {
		NodeList childNodes = element.getChildNodes();
		int length = childNodes.getLength();
		for (int i = length - 1; i >= 0; i--) {
			Node node = childNodes.item(i);
			if (node.getNodeType() == Node.TEXT_NODE && !SEUtilities
					.isWhitespaceOrEmpty(node.getTextContent())) {
				return (Text) node;
			}
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Text last = lastNonWsTextChild((Element) node);
				if (last != null) {
					return last;
				}
			}
		}
		return null;
	}

	public static Document loadDocument(File f) throws Exception {
		return loadDocument(new FileInputStream(f));
	}

	public static Document loadDocument(InputStream stream) throws Exception {
		if (useJAXP) {
			DocumentBuilderFactory factory = DocumentBuilderFactory
					.newInstance();
			// never forget this!
			factory.setNamespaceAware(true);
			DocumentBuilder builder = factory.newDocumentBuilder();
			return builder.parse(stream);
		}
		DOMParser parser = new DOMParser();
		parser.setErrorHandler(new XmlErrHandler());
		parser.setFeature(
				"http://apache.org/xml/features/nonvalidating/load-dtd-grammar",
				false);
		parser.setFeature(
				"http://apache.org/xml/features/nonvalidating/load-external-dtd",
				false);
		parser.parse(new InputSource(stream));
		return parser.getDocument();
	}

	public static Document loadDocument(String xml) throws Exception {
		return loadDocument(xml, false);
	}

	public static Document loadDocument(String xml, boolean knownUtf8)
			throws Exception {
		ByteArrayInputStream bais = null;
		if (knownUtf8 || xml.contains("encoding=\"UTF-8\"")
				|| xml.contains("encoding=\"utf-8\"")) {
			ByteArrayOutputStream bOut = new ByteArrayOutputStream();
			OutputStreamWriter out = new OutputStreamWriter(bOut, "UTF-8");
			out.write(xml);
			out.close();
			bais = new ByteArrayInputStream(bOut.toByteArray());
		} else {
			bais = new ByteArrayInputStream(xml.getBytes());
		}
		return loadDocument(bais);
	}

	public static Document loadDocument(URL url) throws Exception {
		return loadDocument(url.openStream());
	}

	public static DOMLocation locationOfTextIndex(List<Text> texts, int index) {
		MutableDomLocation result = new MutableDomLocation();
		Text save = null;
		for (Text t : texts) {
			String s = t.getTextContent();
			if (s.length() >= index) {
				result.characterOffset = index;
				result.node = t;
			}
			if (s.length() > index) {
				return result.toDomLocation();
			}
			index -= s.length();
			result.nodeIndex++;
		}
		return result.node == null ? null : result.toDomLocation();
	}

	public static DOMLocation locationOfTextIndex(Node container, int index) {
		MutableDomLocation result = new MutableDomLocation();
		TreeWalker walker = ((DocumentTraversal) container.getOwnerDocument())
				.createTreeWalker(container, NodeFilter.SHOW_TEXT, null, true);
		Text t = (Text) (container.getNodeType() == Node.TEXT_NODE ? container
				: null);
		Text save = null;
		while (t == container || (t = (Text) walker.nextNode()) != null) {
			Node n2 = t;
			Node parentNode = t.getParentNode();
			String s = t.getTextContent();
			result.node = t;
			result.nodeIndex = getNodeIndexInParent(t);
			if (s.length() >= index) {
				result.characterOffset = index;
			}
			if (s.length() > index) {
				return result.toDomLocation();
			}
			index -= s.length();
			if (t == container) {
				t = null;
			}
		}
		if (index > 0) {
			return locationOfTextIndex(nextSibOrParentSibNode(container),
					index);
		}
		return result.node == null ? null : result.toDomLocation();
	}

	public static void logToFile(Node n) throws Exception {
		new File("/tmp/log").mkdirs();
		Io.write().string(streamXML(n)).toPath("/tmp/log/log.xml");
		Io.write().string(streamXML(n)).toPath("/tmp/log/log.html");
	}

	public static void logToFilePretty(Node n) throws Exception {
		if (n instanceof DocumentFragment) {
			new File("/tmp/log").mkdirs();
			Io.write().string(prettyPrintWithDOM3LS((DocumentFragment) n))
					.toPath("/tmp/log/log.xml");
		} else {
			new File("/tmp/log").mkdirs();
			Io.write().string(prettyPrintWithDOM3LSNode(n))
					.toPath("/tmp/log/log.xml");
		}
	}

	public static void merge(Element to, Element from) {
		List<Node> nodes = nodeListToList(from.getChildNodes());
		for (Node node : nodes) {
			to.appendChild(node);
		}
		from.getParentNode().removeChild(from);
	}

	public static void moveKids(Node old, Node newNode) {
		NodeList nl = old.getChildNodes();
		Node lastChild = null;
		int length = nl.getLength();
		for (int i = length - 1; i >= 0; i--) {
			Node child = nl.item(i);
			newNode.insertBefore(child, lastChild);
			lastChild = child;
		}
	}

	public static Element nextSibOrParentSib(Node fromNode) {
		Node nextSibling = fromNode.getNextSibling();
		if (nextSibling != null) {
			if (nextSibling.getNodeType() == Node.ELEMENT_NODE) {
				return (Element) nextSibling;
			}
			return nextSibOrParentSib(nextSibling);
		}
		Node parentNode = fromNode.getParentNode();
		if (parentNode != null) {
			return nextSibOrParentSib(parentNode);
		}
		return null;
	}

	public static Node nextSibOrParentSibNode(Node fromNode) {
		Node nextSibling = fromNode.getNextSibling();
		if (nextSibling != null) {
			return nextSibling;
		}
		Node parentNode = fromNode.getParentNode();
		if (parentNode != null) {
			return nextSibOrParentSibNode(parentNode);
		}
		return null;
	}

	public static Node nextSibOrParentSibWithNonEmptyText(Node fromNode) {
		Node nextSibling = fromNode.getNextSibling();
		if (nextSibling != null) {
			short nodeType = nextSibling.getNodeType();
			if ((nodeType == Node.ELEMENT_NODE || nodeType == Node.TEXT_NODE)
					&& !nextSibling.getTextContent().isEmpty()) {
				return nextSibling;
			}
			return nextSibOrParentSibWithNonEmptyText(nextSibling);
		}
		Node parentNode = fromNode.getParentNode();
		if (parentNode != null) {
			return nextSibOrParentSibWithNonEmptyText(parentNode);
		}
		return null;
	}

	public static Node nextSibOrParentSibWithText(Node fromNode) {
		Node nextSibling = fromNode.getNextSibling();
		if (nextSibling != null) {
			short nodeType = nextSibling.getNodeType();
			if (nodeType == Node.ELEMENT_NODE || nodeType == Node.TEXT_NODE) {
				return nextSibling;
			}
			return nextSibOrParentSibWithText(nextSibling);
		}
		Node parentNode = fromNode.getParentNode();
		if (parentNode != null) {
			return nextSibOrParentSibWithText(parentNode);
		}
		return null;
	}

	public static List<Element> nodeListToElementList(NodeList nl) {
		List<Element> rVal = new ArrayList<Element>();
		int length = nl.getLength();
		for (int i = 0; i < length; i++) {
			Node item = nl.item(i);
			if (item.getNodeType() == Node.ELEMENT_NODE) {
				rVal.add((Element) item);
			}
		}
		return rVal;
	}

	public static List<Element> nodeListToElementList(NodeList nl,
			String tagname) {
		List<Element> rVal = new ArrayList<Element>();
		int length = nl.getLength();
		for (int i = 0; i < length; i++) {
			Node item = nl.item(i);
			if (item.getNodeType() == Node.ELEMENT_NODE) {
				Element elt = (Element) item;
				if (elt.getTagName().equalsIgnoreCase(tagname)) {
					rVal.add(elt);
				}
			}
		}
		return rVal;
	}

	public static List<Node> nodeListToList(NodeList nl) {
		List<Node> rVal = new ArrayList<Node>();
		int length = nl.getLength();
		for (int i = 0; i < length; i++) {
			rVal.add(nl.item(i));
		}
		return rVal;
	}

	public static Document ownerDocumentOrSelf(Node item) {
		return (Document) (item.getNodeType() == Node.DOCUMENT_NODE ? item
				: item.getOwnerDocument());
	}

	public static String prettyPrintWithDOM3LS(Document document) {
		// Pretty-prints a DOM document to XML using DOM Load and Save's
		// LSSerializer.
		// Note that the "format-pretty-print" DOM configuration parameter can
		// only be set in JDK 1.6+.
		DOMImplementation domImplementation = document.getImplementation();
		if (domImplementation.hasFeature("LS", "3.0")
				&& domImplementation.hasFeature("Core", "2.0")) {
			DOMImplementationLS domImplementationLS = (DOMImplementationLS) domImplementation
					.getFeature("LS", "3.0");
			LSSerializer lsSerializer = domImplementationLS
					.createLSSerializer();
			DOMConfiguration domConfiguration = lsSerializer.getDomConfig();
			if (domConfiguration.canSetParameter("format-pretty-print",
					Boolean.TRUE)) {
				lsSerializer.getDomConfig().setParameter("format-pretty-print",
						Boolean.TRUE);
				LSOutput lsOutput = domImplementationLS.createLSOutput();
				lsOutput.setEncoding("UTF-8");
				StringWriter stringWriter = new StringWriter();
				lsOutput.setCharacterStream(stringWriter);
				lsSerializer.write(document, lsOutput);
				return stringWriter.toString();
			} else {
				throw new RuntimeException(
						"DOMConfiguration 'format-pretty-print' parameter isn't settable.");
			}
		} else {
			throw new RuntimeException(
					"DOM 3.0 LS and/or DOM 2.0 Core not supported.");
		}
	}

	public static String
			prettyPrintWithDOM3LS(DocumentFragment documentFragment) {
		try {
			Document doc = loadDocument("<doc-wrapper-ppls/>");
			Node adopted = doc.adoptNode(documentFragment.cloneNode(true));
			doc.getDocumentElement().appendChild(adopted);
			String str = prettyPrintWithDOM3LS(doc);
			Pattern p = Pattern.compile(
					".*<doc-wrapper-ppls>[ \n\t]*(.+?)[ \n\t]*</doc-wrapper-ppls>.*",
					Pattern.DOTALL);
			Matcher m = p.matcher(str);
			m.matches();
			String group = m.group(1);
			group = group.replace("\n   ", "\n");
			return group;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public static String prettyPrintWithDOM3LSNode(Node n) throws Exception {
		String xml = streamXML(n);
		return prettyPrintWithDOM3LS(loadDocument(xml));
	}

	public static Element previousSibOrParentSib(Node fromNode) {
		Node previousSibling = fromNode.getPreviousSibling();
		if (previousSibling != null) {
			if (previousSibling.getNodeType() == Node.ELEMENT_NODE) {
				return (Element) previousSibling;
			}
			return previousSibOrParentSib(previousSibling);
		}
		return (Element) fromNode.getParentNode();
	}

	public static Node previousSibOrParentSibNode(Node fromNode) {
		Node previousSibling = fromNode.getPreviousSibling();
		if (previousSibling != null) {
			return previousSibling;
		}
		return fromNode.getParentNode();
	}

	public static void rebaseImages(Document doc, String baseHref) {
		if (baseHref.endsWith("/")) {
			baseHref = baseHref.substring(0, baseHref.length() - 1);
		}
		String[] tags = { "IMG", "img" };
		for (String tag : tags) {
			NodeList imgs = doc.getElementsByTagName(tag);
			int length = imgs.getLength();
			for (int i = 0; i < length; i++) {
				Element img = (Element) imgs.item(i);
				String src = img.getAttribute("src");
				if (src.startsWith("/")) {
					img.setAttribute("src", baseHref + src);
				}
			}
		}
	}

	public static String removeNamespaceInfo(String s) {
		StringBuffer sb = new StringBuffer();
		boolean inTag = false;
		boolean inAttr = false;
		int length = s.length();
		char closeAttr = ' ';
		for (int i = 0; i < length; i++) {
			char c = s.charAt(i);
			if (c == '<' && i < length) {
				char next = s.charAt(i + 1);
				if (next == '/' || ('A' <= next && 'Z' >= next)
						|| ('a' <= next && 'z' >= next)) {
					inTag = true;
					inAttr = false;
				}
			}
			if (c == '>') {
				inTag = false;
			}
			if (inTag && (c == '\'' || c == '"')) {
				if (!inAttr) {
					closeAttr = c;
					inAttr = true;
				} else {
					if (c == closeAttr) {
						closeAttr = ' ';
						inAttr = false;
					}
				}
			}
			if (inTag && !inAttr && c == ':') {
				c = '-';
			}
			sb.append(c);
		}
		return sb.toString();
	}

	public static void removeNode(Node node) {
		if (node != null) {
			node.getParentNode().removeChild(node);
		}
	}

	public static String removeSelfClosingHtmlTags(String content) {
		String regex = Ax.format("</(%s)>",
				selfClosingTags.stream().collect(Collectors.joining("|")));
		return content.replaceAll(regex, "");
	}

	public static String removeXmlDeclaration(String xml) {
		if (xml.startsWith("<?xml")) {
			return xml.replaceFirst("^<\\?xml.+?>", "");
		} else {
			return xml;
		}
	}

	public static void replaceNode(Element from, Element to) {
		insertAfter(to, from);
		moveKids(from, to);
		removeNode(from);
	}

	public static void setUseJAXP(boolean useJAXP) {
		XmlUtils.useJAXP = useJAXP;
	}

	public static Element splitNode(Element toSplit, Node splitAt) {
		Element split = toSplit.getOwnerDocument()
				.createElement(toSplit.getTagName());
		insertAfter(split, toSplit);
		List<Element> kids = nodeListToElementList(toSplit.getChildNodes());
		for (int idx = kids.indexOf(splitAt); idx < kids.size(); idx++) {
			split.appendChild(kids.get(idx));
		}
		return split;
	}

	public static String streamNCleanForBrowserHtmlFragment(Node n) {
		return streamNCleanForBrowserHtmlFragment(streamXML(n));
	}

	public static String streamNCleanForBrowserHtmlFragment(String markup) {
		markup = expandEmptyElements(markup);
		markup = cleanXmlHeaders(markup);
		markup = EntityCleaner.get().htmlToUnicodeEntities(markup);
		return markup;
	}

	public static String streamXML(Node n) {
		DOMSource d = new DOMSource(n);
		StringWriter w = new StringWriter();
		try {
			_streamXML(n, w, null);
			return w.toString();
		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}

	public static void streamXML(Node n, OutputStream os) throws Exception {
		_streamXML(n, null, os);
	}

	public static void streamXML(Node n, Writer w) throws Exception {
		_streamXML(n, w, null);
	}

	public static void stripFixedWidthInfo(Document doc, int maxWidth,
			int maxHeight) {
		String[] tags = { "IMG", "img" };
		for (String tag : tags) {
			NodeList imgs = doc.getElementsByTagName(tag);
			int length = imgs.getLength();
			for (int i = 0; i < length; i++) {
				Element img = (Element) imgs.item(i);
				try {
					int width = Integer.parseInt(img.getAttribute("width"));
					int height = Integer.parseInt(img.getAttribute("height"));
					if (width > maxWidth) {
						height = height * maxWidth / width;
						width = maxWidth;
					}
					if (height > maxHeight) {
						width = width * maxHeight / height;
						height = maxHeight;
					}
					img.setAttribute("height", String.valueOf(height));
					img.setAttribute("width", String.valueOf(width));
				} catch (NumberFormatException nfe) {
				}
			}
		}
	}

	public static String stripMsCommentedStyles(String htmlContent) {
		return htmlContent.replaceAll(
				"(?s)<!--\\[if gte mso 12]>.+?<!\\[endif\\]-->", "");
	}

	public static void stripNode(Node oldNode) {
		Document doc = oldNode.getOwnerDocument();
		DocumentFragment newNode = doc.createDocumentFragment();
		NodeList nl = oldNode.getChildNodes();
		Node refChild = null;
		int length = nl.getLength();
		for (int i = length - 1; i >= 0; i--) {
			Node child = nl.item(i);
			oldNode.removeChild(child);
			newNode.insertBefore(child, refChild);
			refChild = child;
		}
		oldNode.getParentNode().insertBefore(newNode, oldNode);
		oldNode.getParentNode().removeChild(oldNode);
	}

	// http://cse-mjmcl.cse.bris.ac.uk/blog/2007/02/14/1171465494443.html
	public static String stripNonValidXMLCharacters(String in) {
		// Used to hold the output.
		StringBuffer out = new StringBuffer();
		// Used to reference the current character.
		char current;
		if (in == null || ("".equals(in)))
			// vacancy test.
			return "";
		for (int i = 0; i < in.length(); i++) {
			// NOTE: No IndexOutOfBoundsException caught
			current = in.charAt(i);
			// here; it should not happen.
			if ((current == 0x9) || (current == 0xA) || (current == 0xD)
					|| ((current >= 0x20) && (current <= 0xD7FF))
					|| ((current >= 0xE000) && (current <= 0xFFFD))
					|| ((current >= 0x10000) && (current <= 0x10FFFF)))
				out.append(current);
		}
		return out.toString();
	}

	public static void sysXml(Node node) {
		System.out.println(streamXML(node));
	}

	public static String toSimpleXPointer(Node n) {
		if (xPointerConverter == null) {
			xPointerConverter = Registry.impl(XPointerConverter.class);
		}
		return n == null ? "" : xPointerConverter.toSimpleXPointer(n);
	}

	public static void transformDoc(Source xmlSource, Source xsltSource,
			StreamResult sr) throws Exception {
		transformDoc(xmlSource, xsltSource, sr, null, null);
	}

	private static void transformDoc(Source xmlSource, Source xsltSource,
			StreamResult sr, String cacheMarker,
			TransformerFactoryConfigurator configurator) throws Exception {
		TransformerPool pool = null;
		if (cacheMarker == null) {
			cacheMarker = TRANSFORMER_CACHE_MARKER_NULL;
		}
		if ((noTransformerCaching
				&& cacheMarker != TRANSFORMER_CACHE_MARKER_STREAM_XML)
				|| cacheMarker == TRANSFORMER_CACHE_MARKER_NULL) {
			pool = new TransformerPool(false);
		} else {
			pool = transformersPool.get(cacheMarker);
		}
		Transformer transformer = null;
		try {
			transformer = pool.borrow(xsltSource, configurator);
			transformer.transform(xmlSource, sr);
		} finally {
			if (transformer != null) {
				pool.returnObject(transformer);
			}
		}
	}

	public static String transformDocToString(Source dataSource,
			Source trSource) throws Exception {
		return transformDocToString(dataSource, trSource, null, null);
	}

	public static String transformDocToString(Source dataSource,
			Source trSource, String marker) throws Exception {
		return transformDocToString(dataSource, trSource, marker, null);
	}

	public static String transformDocToString(Source dataSource,
			Source trSource, String marker,
			TransformerFactoryConfigurator configurator) throws Exception {
		StringWriter wr = new StringWriter();
		StreamResult streamResult = new StreamResult(wr);
		transformDoc(dataSource, trSource, streamResult, marker, configurator);
		return wr.toString();
	}

	private static String trimAndNormaliseWrappingNewlines(
			boolean parentIsTextNode, String text) {
		if (parentIsTextNode) {
			return text.replaceFirst("(?s)^[\n]*(.+?)[\n]*$", "$1");
		} else {
			return text.replaceFirst("(?s)^[ \t\n]*(.+?)[ \t\n]*$", "$1");
		}
	}

	public static void wrapContentIn(Element elt, Element newElt) {
		moveKids(elt, newElt);
		elt.appendChild(newElt);
	}

	public static class DOMLocation {
		public static Range createRange(DOMLocation start, DOMLocation end) {
			Range r = ((DocumentRange) start.node.getOwnerDocument())
					.createRange();
			r.setStart(start.node, start.characterOffset);
			if (end.node.getNodeType() == Node.TEXT_NODE) {
				r.setEnd(end.node, end.characterOffset);
			} else {
				r.setEndAfter(end.node);
			}
			return r;
		}

		public final Node node;

		public final int characterOffset;

		public final int nodeIndex;

		private String toString;

		public DOMLocation(Node node, int characterOffset, int nodeIndex) {
			this.node = node;
			Preconditions.checkArgument(characterOffset >= 0);
			this.characterOffset = characterOffset;
			if (characterOffset > 0) {
				Preconditions.checkArgument(
						characterOffset <= node.getNodeValue().length());
			}
			this.nodeIndex = nodeIndex;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof DOMLocation) {
				return toString().equals(obj.toString());
			}
			return super.equals(obj);
		}

		@Override
		public int hashCode() {
			return toString().hashCode();
		}

		@Override
		public String toString() {
			if (toString == null) {
				toString = toSimpleXPointer(node) + "[" + characterOffset + "]";
			}
			return toString;
		}
	}

	public interface IsIgnorePredicate {
		boolean isIgnore(Node n);
	}

	public interface IsInlinePredicate {
		boolean isInline(Element e);
	}

	private static class MutableDomLocation {
		public Node node;

		public int characterOffset;

		public int nodeIndex;

		public DOMLocation toDomLocation() {
			return new DOMLocation(node, characterOffset, nodeIndex);
		}
	}

	public static class NodeComparator implements Comparator<Node> {
		@Override
		public int compare(Node o1, Node o2) {
			if (o1 == o2) {
				return 0;
			}
			return isEarlierThan(o1, o2) ? -1 : 1;
		}
	}

	public static class SurroundingBlockTuple {
		public Node lastNode;

		private Range range;

		public Node firstNode;

		public Node prevBlock;

		public Node nextBlock;

		private TreeWalker walker;

		public Node forNode;

		boolean walkerFinished = false;

		public SurroundingBlockTuple(Node forNode) {
			this.forNode = forNode;
		}

		public void detach() {
			if (range != null) {
				range.detach();
			}
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof SurroundingBlockTuple) {
				SurroundingBlockTuple o = (SurroundingBlockTuple) obj;
				return o.firstNode == firstNode;
			}
			return false;
		}

		public Node getCommonAncestorContainer() {
			if (lastNode == firstNode) {
				return firstNode;
			}
			List<DomNode> list = DomNode.from(firstNode).ancestors().orSelf()
					.list();
			DomNode last = DomNode.from(lastNode);
			for (DomNode cursor : list) {
				if (cursor.isAncestorOf(last)) {
					return cursor.w3cNode();
				}
			}
			return null;
		}

		public String getContent() {
			StringBuilder builder = new StringBuilder();
			DomNodeTree tree = DomNode.from(firstNode).tree();
			while (true) {
				DomNode currentNode = tree.currentNode();
				if (currentNode.isText()) {
					builder.append(currentNode.textContent());
				}
				if (currentNode.w3cNode() == lastNode) {
					break;
				}
				tree.nextLogicalNode();
			}
			return Ax.ntrim(builder.toString());
		}

		public Text getCurrentTextChildAndIncrement() {
			Node n = null;
			while ((n = walker.getCurrentNode()) != null && !walkerFinished) {
				if (n == nextBlock) {
					return null;
				}
				if (walker.nextNode() == null) {
					walkerFinished = true;
				}
				if (n.getNodeType() == Node.TEXT_NODE) {
					return (Text) n;
				}
			}
			return null;
		}

		public Range getRange() {
			if (range == null) {
				range = ((DocumentRange) firstNode.getOwnerDocument())
						.createRange();
				range.setStartBefore(firstNode);
				range.setEndAfter(lastNode);
			}
			return range;
		}

		public Text provideFirstText() {
			resetWalker();
			Text textChild = getCurrentTextChildAndIncrement();
			resetWalker();
			return textChild;
		}

		public boolean provideIsAnonymousTextBlock() {
			return firstNode.getNodeType() == Node.TEXT_NODE
					&& !(prevBlock != null
							&& XmlUtils.isAncestorOf(prevBlock, firstNode));
		}

		public void resetWalker() {
			walkerFinished = false;
			Document doc = firstNode.getOwnerDocument();
			walker = ((DocumentTraversal) doc).createTreeWalker(doc,
					NodeFilter.SHOW_TEXT | NodeFilter.SHOW_ELEMENT, null, true);
			walker.setCurrentNode(firstNode);
		}

		public void setRange(Range range) {
			this.range = range;
		}
	}

	public static interface TransformerFactoryConfigurator {
		public void configure(TransformerFactory transformerFactory);
	}

	static class TransformerPool {
		private GenericObjectPool<Transformer> objectPool;

		private TransformerObjectFactory factory;

		private Source xsltSource;

		private TransformerFactoryConfigurator configurator;

		public TransformerPool(boolean withPool) {
			factory = new TransformerObjectFactory();
			if (withPool) {
				objectPool = new GenericObjectPool<Transformer>(factory);
				objectPool.setMaxTotal(10);
			}
		}

		synchronized Transformer borrow(Source xsltSource,
				TransformerFactoryConfigurator configurator) {
			this.xsltSource = xsltSource;
			this.configurator = configurator;
			try {
				if (objectPool == null) {
					return factory.create();
				} else {
					return objectPool.borrowObject();
				}
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}

		public void returnObject(Transformer transformer) {
			if (objectPool != null) {
				objectPool.returnObject(transformer);
			}
		}

		class TransformerObjectFactory
				extends BasePooledObjectFactory<Transformer> {
			@Override
			public Transformer create() throws Exception {
				TransformerFactory transformerFactory = TransformerFactory
						.newInstance();
				if (configurator != null) {
					configurator.configure(transformerFactory);
				}
				return xsltSource == null ? transformerFactory.newTransformer()
						: transformerFactory.newTransformer(xsltSource);
			}

			@Override
			public PooledObject<Transformer> wrap(Transformer transformer) {
				return new DefaultPooledObject<Transformer>(transformer);
			}
		}
	}

	static class XmlErrHandler implements ErrorHandler {
		/**
		 * @see org.xml.sax.ErrorHandler#error(org.xml.sax.SAXParseException)
		 */
		@Override
		public void error(SAXParseException exception) throws SAXException {
			log(exception);
		}

		/**
		 * @see org.xml.sax.ErrorHandler#fatalError(org.xml.sax.SAXParseException)
		 */
		@Override
		public void fatalError(SAXParseException exception)
				throws SAXException {
			log(exception);
		}

		private void log(SAXParseException exception) {
			if (!LooseContext.is(CONTEXT_MUTE_XML_SAX_EXCEPTIONS)) {
				exception.printStackTrace();
			}
		}

		/**
		 * @see org.xml.sax.ErrorHandler#warning(org.xml.sax.SAXParseException)
		 */
		@Override
		public void warning(SAXParseException exception) throws SAXException {
			log(exception);
		}
	}

	public static class XmlInsertionCursor {
		public Node node;

		int textOffset;

		public XmlInsertionCursor(Node node, int textOffset) {
			this.node = node;
			this.textOffset = textOffset;
		}

		public XmlInsertionCursor splitText() {
			String textContent = node.getTextContent();
			if (textOffset != 0 && textOffset != textContent.length()) {
				Text t1 = (Text) node;
				Text t2 = node.getOwnerDocument()
						.createTextNode(textContent.substring(textOffset));
				t1.setTextContent(textContent.substring(0, textOffset));
				XmlUtils.insertAfter(t2, t1);
			}
			return this;
		}

		public void toHighestContainingInline(IsInlinePredicate isInline,
				IsIgnorePredicate isIgnore, boolean start) {
			while (true) {
				Node parent = node.getParentNode();
				boolean continueAscent = isIgnore.isIgnore(node);
				if (parent.getNodeType() == Node.ELEMENT_NODE
						&& isInline.isInline((Element) parent)) {
					if (start && node.getPreviousSibling() == null) {
						continueAscent = true;
					}
					if (!start && node.getNextSibling() == null) {
						continueAscent = true;
					}
				}
				if (continueAscent) {
					node = parent;
				} else {
					break;
				}
			}
		}

		@Override
		public String toString() {
			return String.format("Node: %s\ntextOffset: %s\n;Xml: %s", node,
					textOffset, streamXML(node));
		}
	}

	@Registration.Singleton
	public static class XPointerConverter {
		public String toSimpleXPointer(Node n) {
			List<String> parts = new ArrayList<String>();
			while (n != null) {
				switch (n.getNodeType()) {
				case Node.DOCUMENT_NODE:
				case Node.DOCUMENT_FRAGMENT_NODE:
					parts.add("");
					break;
				default:
					String part = n.getNodeName();
					switch (n.getNodeType()) {
					case Node.TEXT_NODE:
						part = "TEXT()";
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
						if (item.getNodeType() == n.getNodeType()) {
							if (n.getNodeType() == Node.ELEMENT_NODE) {
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
	}

	public static String trimMarkup(String string, int maxHlLength) {
		if (string.length() < maxHlLength) {
			return string;
		}
		DomDocument doc = DomDocument.from(Ax.format("<div>%s</div>",
				EntityCleaner.get().htmlToUnicodeEntities(string)));
		doc.setReadonly(true);
		if (doc.getDocumentElementNode().asRange().end
				.getIndex() < maxHlLength) {
			return string;
		}
		Location clip = doc.getDocumentElementNode().asLocation()
				.textRelativeLocation(maxHlLength, true).toTextLocation(true);
		DomNode containingNode = clip.getContainingNode();
		String replaceContents = Ax.trim(containingNode.textContent(),
				maxHlLength - clip.getIndex());
		containingNode.setText(replaceContents);
		DomNodeTree tree = doc.getDocumentElementNode().tree();
		tree.setCurrentNode(containingNode);
		while (tree.hasNext()) {
			DomNode next = tree.next();
			if (next.isText() && next != containingNode) {
				next.setText("");
			}
		}
		return doc.getDocumentElementNode().getInnerMarkup();
	}

	public static void cleanNestedAnchors(Document w3cDoc) {
		DomDocument doc = DomDocument.from(w3cDoc);
		Ref<Boolean> delta = Ref.of(false);
		AtomicInteger deltaCount = new AtomicInteger(0);
		do {
			delta.set(false);
			doc.stream().filter(n -> n.tagIs("a")).forEach(a -> {
				DomNode ancestorA = a.ancestors().get("a");
				if (ancestorA != null) {
					a.strip();
					ancestorA.relative().insertAfterThis(a);
					delta.set(true);
					deltaCount.incrementAndGet();
				}
			});
		} while (delta.get());
		if (deltaCount.get() > 0) {
			logger.warn("Fixed {} nested A tags", deltaCount);
		}
	}
}
