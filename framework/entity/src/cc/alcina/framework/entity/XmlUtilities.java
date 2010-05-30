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
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.w3c.dom.ranges.DocumentRange;
import org.w3c.dom.ranges.Range;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.NodeIterator;
import org.w3c.dom.traversal.TreeWalker;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import cc.alcina.framework.common.client.util.CommonUtils;

/**
 * 
 * @author Nick Reddel
 */
public class XmlUtilities {
	public static final String A_EMPTY = "A-EMPTY";

	private static boolean useJAXP;

	private static final String HTML_BLOCKS = ",ADDRESS,BLOCKQUOTE,DIV,DL,H1,H2,H3,H4,H5,"
			+ "H6,IFRAME,ILAYER,LAYER,OL,TABLE,TR,UL,TD,P,HR,BR,LI,";

	private static final String HTML_INDENTS = ",BLOCKQUOTE,OL,UL,";

	private static Map<String, Transformer> transformerMap = new HashMap<String, Transformer>();

	private static DocumentBuilder db;

	public static String cleanXmlHeaders(String htmlContent) {
		htmlContent = htmlContent.replaceAll("<\\?xml.+?\\?>", "");
		String regex = "<!DOCTYPE .+?>";
		Pattern p = Pattern.compile(regex, Pattern.MULTILINE | Pattern.DOTALL
				| Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(htmlContent);
		htmlContent = m.replaceAll("");
		return htmlContent;
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

	public static int getDepth(Node n) {
		int depth = 0;
		while (n.getParentNode() != null) {
			depth++;
			n = n.getParentNode();
		}
		return depth;
	}

	public static String elementNamesToLowerCase(String s) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			sb.append(c);
			if (c == '<') {
				if (s.charAt(i + 1) == '/'
						|| Character.isUpperCase(s.charAt(i + 1))) {
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
		String regex = "<([a-zA-Z]+)([^<]*?)/>";
		Pattern p = Pattern.compile(regex, Pattern.MULTILINE | Pattern.DOTALL
				| Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(s);
		s = m.replaceAll("<$1$2></$1>");
		return s;
	}

	public static Element getContainingBlock(Node n) {
		while (n != null) {
			if (n.getNodeType() == Node.ELEMENT_NODE
					&& isBlockHTMLElementSimplistic((Element) n)) {
				return (Element) n;
			}
			n = n.getParentNode();
		}
		return null;
	}

	public static Element getFirstElement(NodeList nl) {
		for (int i = 0; i < nl.getLength(); i++) {
			if (nl.item(i).getNodeType() == Node.ELEMENT_NODE) {
				return (Element) nl.item(i);
			}
		}
		return null;
	}

	public static Text getFirstTextChild(Node node) {
		NodeList nl = node.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			node = nl.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Text t = getFirstTextChild(node);
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
				if (!SEUtilities.isWhitespace(node.getTextContent())) {
					return (Text) node;
				}
			}
		}
		return null;
	}

	public static Range getRangeSurroundedByBlocks(Node node) {
		return getSurroundingBlockTuple(node).range;
	}

	public static SurroundingBlockTuple getSurroundingBlockTuple(Node node) {
		Element element = getParentElementWithTagName(node, "*", true);
		Node prev = element;
		Node next = element;
		SurroundingBlockTuple tuple = new SurroundingBlockTuple();
		while (true) {
			Node sib = previousSibOrParentSibNode(prev);
			if (sib != null && sib.getNodeType() == Node.ELEMENT_NODE
					&& isBlockHTMLElementSimplistic((Element) sib)) {
				tuple.prevBlock = (Element) sib;
				break;
			} else {
				prev = sib;
			}
		}
		while (true) {
			Node sib = nextSibOrParentSibNode(next);
			if (sib == null || sib.getNodeType() == Node.ELEMENT_NODE
					&& isBlockHTMLElementSimplistic((Element) sib)) {
				tuple.nextBlock = (Element) sib;
				break;
			} else {
				next = sib;
			}
		}
		Range r = ((DocumentRange) element.getOwnerDocument()).createRange();
		r.setStartBefore(prev);
		r.setEndAfter(next);
		tuple.firstNode = prev;
		tuple.range = r;
		return tuple;
	}

	public static class SurroundingBlockTuple {
		public Range range;

		public Node firstNode;

		public Element prevBlock;

		public Element nextBlock;
	}

	public static Element getParentElementWithTagName(Node n, String tagName) {
		return getParentElementWithTagName(n, tagName, false);
	}

	public static Element getParentElementWithTagName(Node n, String tagName,
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

	public static Element getPreContainingBlock(Element elt) {
		Element preContainer = elt;
		while (elt != null) {
			if (isBlockHTMLElementSimplistic(elt)) {
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

	public static Element getSoleElement(NodeList nl, String tagName) {
		return getSoleElement(nl, tagName, new ArrayList<String>());
	}

	public static Element getSoleElement(NodeList nl, String tagName,
			List<String> ignores) {
		Element found = null;
		for (int i = 0; i < nl.getLength(); i++) {
			Node item = nl.item(i);
			if (item.getNodeType() == Node.ELEMENT_NODE) {
				if (found != null
						|| !item.getNodeName().equalsIgnoreCase(tagName)) {
					boolean ignore = false;
					ignore |= ignores != null
							&& ignores.contains(item.getNodeName());
					ignore |= ignores != null && ignores.contains(A_EMPTY)
							&& item.getNodeName().equalsIgnoreCase("A")
							&& item.getTextContent().trim().length() == 0;
					if (!ignore) {
						return null;
					}
				} else {
					found = (Element) item;
				}
			}
		}
		return found;
	}

	public static Element getSoleElementChain(Element elt, String string,
			List<String> ignores) {
		String[] parts = string.split("/");
		if (!elt.getTagName().equalsIgnoreCase(parts[0])) {
			return null;
		}
		for (int i = 1; i < parts.length; i++) {
			elt = getSoleElement(elt.getChildNodes(), parts[i], ignores);
			if (elt == null) {
				break;
			}
		}
		return elt;
	}

	public static Element getSoleElementChains(Element elt,
			List<String> chains, List<String> ignores) {
		for (String chain : chains) {
			Element chElt = getSoleElementChain(elt, chain, ignores);
			if (chElt != null) {
				return chElt;
			}
		}
		return null;
	}

	public static String innerText(Node n) {
		return new InnerTexta().innerText(n);
	}

	public static void insertAfter(Element el, Node newNode, Node insertAfter) {
		NodeList nl = el.getChildNodes();
		boolean foundAfter = false;
		for (int i = 0; i < nl.getLength(); i++) {
			Node n = nl.item(i);
			if (foundAfter) {
				el.insertBefore(newNode, n);
				return;
			}
			if (n == insertAfter) {
				foundAfter = true;
			}
		}
		el.appendChild(newNode);
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

	public static boolean isBlockHTMLElementSimplistic(Element e) {
		return HTML_BLOCKS.contains("," + e.getTagName().toUpperCase() + ",");
	}

	public static boolean isCompleteBlock(Element elt) {
		Element block = getContainingBlock(elt);
		if (block != null
				&& elt.getTextContent().trim().equals(
						block.getTextContent().trim())) {
			return true;
		}
		Element preContainer = getPreContainingBlock(elt);
		if (preContainer.getTextContent().trim().equals(
				elt.getTextContent().trim())) {
			Element prev = previousSibOrParentSib(preContainer);
			Element next = nextSibOrParentSib(preContainer);
			if (isBlockHTMLElementSimplistic(prev)
					&& isBlockHTMLElementSimplistic(next)) {
				return true;
			}
		}
		return false;
	}

	public static boolean isFirstElementChildOf(Element e, Element parent) {
		NodeList nl = parent.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			Node n = nl.item(i);
			if (n.getNodeType() == Node.ELEMENT_NODE) {
				return e == n;
			}
		}
		return false;
	}

	public static boolean isIndentElement(Element elt) {
		return HTML_INDENTS
				.contains("," + elt.getTagName().toUpperCase() + ",");
	}

	public static boolean isUseJAXP() {
		return useJAXP;
	}

	public static Element lastChild(Element element) {
		NodeList childNodes = element.getChildNodes();
		for (int i = childNodes.getLength() - 1; i >= 0; i--) {
			Node node = childNodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				return lastChild((Element) node);
			}
		}
		return element;
	}

	public static Document loadDocument(File f) throws Exception {
		return loadDocument(new FileInputStream(f));
	}

	public static Document loadDocument(InputStream stream) throws Exception {
		if (useJAXP) {
			DocumentBuilderFactory factory = DocumentBuilderFactory
					.newInstance();
			factory.setNamespaceAware(true); // never forget this!
			DocumentBuilder builder = factory.newDocumentBuilder();
			return builder.parse(stream);
		}
		DOMParser parser = new DOMParser();
		parser.setErrorHandler(new XmlErrHandler());
		parser
				.setFeature(
						"http://apache.org/xml/features/nonvalidating/load-dtd-grammar",
						false);
		parser
				.setFeature(
						"http://apache.org/xml/features/nonvalidating/load-external-dtd",
						false);
		parser.parse(new InputSource(stream));
		return parser.getDocument();
	}

	public static Document loadDocument(String xml) throws Exception {
		ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes());
		return loadDocument(bais);
	}

	public static Document loadDocument(URL url) throws Exception {
		return loadDocument(url.openStream());
	}

	public static DOMLocation locationOfTextIndex(List<Text> texts, int index) {
		DOMLocation result = new DOMLocation();
		Text save = null;
		for (Text t : texts) {
			String s = t.getTextContent();
			if (s.length() >= index) {
				result.characterOffset = index;
				result.node = t;
			}
			if (s.length() > index) {
				return result;
			}
			index -= s.length();
			result.nodeIndex++;
		}
		return result.node == null ? null : result;
	}

	public static DOMLocation locationOfTextIndex(Node container, int index) {
		DOMLocation result = new DOMLocation();
		TreeWalker walker = ((DocumentTraversal) container.getOwnerDocument())
				.createTreeWalker(container, NodeFilter.SHOW_TEXT, null, true);
		Text t = null;
		Text save = null;
		Map<Node, Integer> indiciesInContainers = new HashMap<Node, Integer>();
		while ((t = (Text) walker.nextNode()) != null) {
			String s = t.getTextContent();
			if (s.length() >= index) {
				result.characterOffset = index;
				result.node = t;
			}
			if (s.length() > index) {
				return result;
			}
			index -= s.length();
			Node parentNode = t.getParentNode();
			if (!indiciesInContainers.containsKey(parentNode)) {
				indiciesInContainers.put(parentNode, 0);
			}
			indiciesInContainers.put(parentNode, indiciesInContainers
					.get(parentNode) + 1);
			result.nodeIndex = indiciesInContainers.get(result.node
					.getParentNode());
		}
		if (result.node != null && index == 0 && t == null) {
			// edge case
			result.nodeIndex--;
		}
		return result.node == null ? null : result;
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

	public static int nodeDistance(Node n1, Node n2) {
		Document doc = n1.getOwnerDocument();
		NodeIterator itr = ((DocumentTraversal) doc).createNodeIterator(doc
				.getDocumentElement(), NodeFilter.SHOW_ALL, null, true);
		int x = 0;
		boolean foundN1 = false;
		boolean foundN2 = false;
		for (Node n; (n = itr.nextNode()) != null;) {
			x += foundN1 ? 1 : foundN2 ? -1 : 0;
			foundN1 |= n == n1;
			foundN2 |= n == n2;
			if (foundN1 && foundN2) {
				break;
			}
		}
		return Math.abs(x);
	}

	public static List<Element> nodeListToElementList(NodeList nl) {
		List<Element> rVal = new ArrayList<Element>();
		for (int i = 0; i < nl.getLength(); i++) {
			if (nl.item(i).getNodeType() == Node.ELEMENT_NODE) {
				rVal.add((Element) nl.item(i));
			}
		}
		return rVal;
	}

	public static List<Node> nodeListToList(NodeList nl) {
		List<Node> rVal = new ArrayList<Node>();
		for (int i = 0; i < nl.getLength(); i++) {
			rVal.add(nl.item(i));
		}
		return rVal;
	}

	public static List<Element> nodeListToElementList(NodeList nl,
			String tagname) {
		List<Element> rVal = new ArrayList<Element>();
		for (int i = 0; i < nl.getLength(); i++) {
			if (nl.item(i).getNodeType() == Node.ELEMENT_NODE) {
				Element elt = (Element) nl.item(i);
				if (elt.getTagName().equalsIgnoreCase(tagname)) {
					rVal.add(elt);
				}
			}
		}
		return rVal;
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
		return (Element) fromNode.getParentNode();
	}

	public static String removeXmlDeclaration(String xml) {
		return xml.replace("<?xml version=\"1.0\" encoding=\"UTF-8\"?>", "");
	}

	public static void setUseJAXP(boolean useJAXP) {
		XmlUtilities.useJAXP = useJAXP;
	}

	public synchronized static String streamNCleanForBrowserHtmlFragment(Node n) {
		String s = streamXML(n);
		s = expandEmptyElements(s);
		s = cleanXmlHeaders(s);
		return s;
	}

	public synchronized static String streamXML(Node n) {
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

	public synchronized static void streamXML(Node n, OutputStream os)
			throws Exception {
		_streamXML(n, null, os);
	}

	public synchronized static void streamXML(Node n, Writer w)
			throws Exception {
		_streamXML(n, w, null);
	}

	// http://cse-mjmcl.cse.bris.ac.uk/blog/2007/02/14/1171465494443.html
	public static String stripNonValidXMLCharacters(String in) {
		StringBuffer out = new StringBuffer(); // Used to hold the output.
		char current; // Used to reference the current character.
		if (in == null || ("".equals(in)))
			return ""; // vacancy test.
		for (int i = 0; i < in.length(); i++) {
			current = in.charAt(i); // NOTE: No IndexOutOfBoundsException caught
			// here; it should not happen.
			if ((current == 0x9) || (current == 0xA) || (current == 0xD)
					|| ((current >= 0x20) && (current <= 0xD7FF))
					|| ((current >= 0xE000) && (current <= 0xFFFD))
					|| ((current >= 0x10000) && (current <= 0x10FFFF)))
				out.append(current);
		}
		return out.toString();
	}

	public static void stripOrReplaceNode(Node oldNode, Node newNode) {
		Document doc = oldNode.getOwnerDocument();
		newNode = newNode == null ? doc.createDocumentFragment() : newNode;
		NodeList nl = oldNode.getChildNodes();
		Node refChild = null;
		for (int i = nl.getLength() - 1; i >= 0; i--) {
			Node child = oldNode.removeChild(nl.item(i));
			newNode.insertBefore(child, refChild);
			refChild = child;
		}
		oldNode.getParentNode().insertBefore(newNode, oldNode);
		oldNode.getParentNode().removeChild(oldNode);
	}

	public static String toSimpleXPointer(Node n) {
		List<String> parts = new ArrayList<String>();
		while (n != null) {
			switch (n.getNodeType()) {
			case Node.DOCUMENT_NODE:
				parts.add("");
				break;
			default:
				String part = n.getNodeName();
				switch (n.getNodeType()) {
				case Node.TEXT_NODE:
					part = "text()";
				}
				NodeList childNodes = n.getParentNode().getChildNodes();
				int pos = -1;
				int count = 0;
				for (int i = 0; i < childNodes.getLength(); i++) {
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

	public static void transformDoc(Source xmlSource, Source xsltSource,
			StreamResult sr) throws Exception {
		transformDoc(xmlSource, xsltSource, sr, null);
	}

	public static String transformDocToString(Source dataSource, Source trSource)
			throws Exception {
		return transformDocToString(dataSource, trSource, null);
	}

	public static String transformDocToString(Source dataSource,
			Source trSource, String marker) throws Exception {
		StringWriter wr = new StringWriter();
		StreamResult streamResult = new StreamResult(wr);
		transformDoc(dataSource, trSource, streamResult, marker);
		return wr.toString();
	}

	private synchronized static void _streamXML(Node n, Writer w, OutputStream s)
			throws Exception {
		transformDoc(new DOMSource(n), null, w == null ? new StreamResult(s)
				: new StreamResult(w));
	}

	private static void transformDoc(Source xmlSource, Source xsltSource,
			StreamResult sr, String cacheMarker) throws Exception {
		Transformer trans = null;
		if (cacheMarker == null || !transformerMap.containsKey(cacheMarker)) {
			TransformerFactory transFact = TransformerFactory.newInstance();
			trans = xsltSource == null ? transFact.newTransformer() : transFact
					.newTransformer(xsltSource);
			transformerMap.put(cacheMarker, trans);
		} else {
			trans = transformerMap.get(cacheMarker);
		}
		trans.transform(xmlSource, sr);
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

		public Node node;

		public int characterOffset;

		public int nodeIndex;

		public DOMLocation() {
		}

		public DOMLocation(Node node, int characterOffset, int nodeIndex) {
			this.node = node;
			this.characterOffset = characterOffset;
			this.nodeIndex = nodeIndex;
		}

		@Override
		public String toString() {
			return XmlUtilities.toSimpleXPointer(node) + "[" + characterOffset
					+ "]";
		}
	}

	public static class InnerTexta {
		public static final int SHOW_LIST_ITEMS = 1;

		public static final int SHOW_NEWLINES = 2;

		private static final String nlMarker = new String(new char[] { 9000,
				9001, 9009, 10455, 39876 });

		private static final String tabMarker = new String(new char[] { 9000,
				9001, 9009, 10455, 39877 });

		private static final String HTML_LISTS = ",UL,OL,";

		public static boolean isListElement(Element e) {
			return HTML_LISTS.contains("," + e.getTagName() + ",");
		}

		public String innerText(Node n) {
			return innerText(n, 0);
		}

		public String innerText(Node n, int flags) {
			Document doc = n.getOwnerDocument();
			NodeIterator nodeIterator = ((DocumentTraversal) doc)
					.createNodeIterator(n, NodeFilter.SHOW_TEXT
							| NodeFilter.SHOW_ELEMENT, null, true);
			StringBuffer result = new StringBuffer();
			Node n2 = null;
			Stack<Element> listParentStack = new Stack<Element>();
			Map<Element, Integer> listIndicies = new HashMap<Element, Integer>();
			boolean showNls = (flags & SHOW_NEWLINES) != 0;
			boolean showLis = (flags & SHOW_LIST_ITEMS) != 0;
			while ((n2 = nodeIterator.nextNode()) != null) {
				if (n2.getNodeType() == Node.ELEMENT_NODE) {
					Element elt = (Element) n2;
					String tag = elt.getTagName().toLowerCase();
					if (XmlUtilities.isBlockHTMLElementSimplistic(elt)) {
						if (showNls) {
							result.append(nlMarker);
						}
					}
					if (isListElement(elt)) {
						popForNonParent(elt, listParentStack);
						listParentStack.push(elt);
						listIndicies.put(elt, 1);
					}
					if (tag.equals("li")) {
						popForNonParent(elt, listParentStack);
						if (!listParentStack.isEmpty() && showLis) {
							Element listElt = listParentStack.peek();
							for (int i = 1; i < listParentStack.size(); i++) {
								result.append(tabMarker);
							}
							Integer listIndex = listIndicies.get(listElt);
							result.append((listElt.getTagName()
									.equalsIgnoreCase("UL") ? "*" : listIndex
									+ ".")
									+ " ");
							listIndicies.put(listElt, listIndex + 1);
						}
					}
				} else {
					result.append(n2.getNodeValue());
				}
			}
			String s = result.toString().replaceAll("\\s+", " ");
			if (flags == 0) {
				return s;
			}
			s = s.replace(nlMarker, "\n");
			s = s.replace(tabMarker, "\t");
			s = s.replaceAll("\\s*\\n+\\s*", "\n");
			Matcher matcher = Pattern.compile("\\n*(.*?)\\n*", Pattern.DOTALL)
					.matcher(s);
			matcher.matches();
			s = matcher.group(1);
			return s;
		}

		private void popForNonParent(Element elt, Stack<Element> listParentStack) {
			while (!listParentStack.isEmpty()) {
				if (XmlUtilities.isAncestorOf(listParentStack.peek(), elt)) {
					return;
				}
				listParentStack.pop();
			}
		}
	}

	static class XmlErrHandler implements ErrorHandler {
		/**
		 * @see org.xml.sax.ErrorHandler#error(org.xml.sax.SAXParseException)
		 */
		public void error(SAXParseException exception) throws SAXException {
			exception.printStackTrace();
		}

		/**
		 * @see org.xml.sax.ErrorHandler#fatalError(org.xml.sax.SAXParseException)
		 */
		public void fatalError(SAXParseException exception) throws SAXException {
			exception.printStackTrace();
		}

		/**
		 * @see org.xml.sax.ErrorHandler#warning(org.xml.sax.SAXParseException)
		 */
		public void warning(SAXParseException exception) throws SAXException {
			exception.printStackTrace();
		}
	}

	public static Element getSoleElementExceptingWhitespace(Element parent) {
		NodeList nl = parent.getChildNodes();
		Element found = null;
		for (int i = 0; i < nl.getLength(); i++) {
			Node item = nl.item(i);
			if (item.getNodeType() == Node.ELEMENT_NODE) {
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

	public static Range nodeToRange(Node container) {
		Range r = ((DocumentRange) container.getOwnerDocument()).createRange();
		r.setStart(container, 0);
		r.setEndAfter(container);
		return r;
	}
}
