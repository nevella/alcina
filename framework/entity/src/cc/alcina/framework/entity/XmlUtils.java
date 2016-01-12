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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 *
 * @author Nick Reddel
 */
public class XmlUtils {
	private static boolean useJAXP;

	private static Map<String, Transformer> transformerMap = new ConcurrentHashMap<String, Transformer>();

	public static boolean noTransformCaching;

	private static DocumentBuilder db;

	public static final int A4_MAX_PIXEL_WIDTH = 700;

	public static final int A4_MAX_PIXEL_HEIGHT = 950;

	public static final int A4_MAX_PIXEL_HEIGHT_PRINT = 850;

	public static String cleanXmlHeaders(String xml) {
		xml = xml.replaceAll("<\\?xml.+?\\?>", "");
		String regex = "<!DOCTYPE .+?>";
		Pattern p = Pattern.compile(regex, Pattern.MULTILINE | Pattern.DOTALL
				| Pattern.CASE_INSENSITIVE);
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

	public static Element getElementExt(Element root, String tagName,
			String attrName, String attrValue) {
		List<Element> elementList = nodeListToElementList(root
				.getElementsByTagName(tagName));
		for (Element element : elementList) {
			if (element.getAttribute(attrName).equals(attrValue)) {
				return element;
			}
		}
		return null;
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
		ByteArrayInputStream bais = null;
		if (xml.contains("encoding=\"UTF-8\"")) {
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

	public static List<Element> nodeListToElementList(NodeList nl) {
		List<Element> rVal = new ArrayList<Element>();
		for (int i = 0; i < nl.getLength(); i++) {
			if (nl.item(i).getNodeType() == Node.ELEMENT_NODE) {
				rVal.add((Element) nl.item(i));
			}
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

	public static List<Node> nodeListToList(NodeList nl) {
		List<Node> rVal = new ArrayList<Node>();
		for (int i = 0; i < nl.getLength(); i++) {
			rVal.add(nl.item(i));
		}
		return rVal;
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

	public static String removeXmlDeclaration(String xml) {
		return xml.replace("<?xml version=\"1.0\" encoding=\"UTF-8\"?>", "");
	}

	public static void setUseJAXP(boolean useJAXP) {
		XmlUtils.useJAXP = useJAXP;
	}

	public static String streamNCleanForBrowserHtmlFragment(Node n) {
		String s = streamXML(n);
		s = expandEmptyElements(s);
		s = cleanXmlHeaders(s);
		return s;
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

	public static void transformDoc(Source xmlSource, Source xsltSource,
			StreamResult sr) throws Exception {
		transformDoc(xmlSource, xsltSource, sr, null, null);
	}

	public static String transformDocToString(Source dataSource, Source trSource)
			throws Exception {
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

	public static void sysXml(Node node) {
		System.out.println(streamXML(node));
	}

	private static void _streamXML(Node n, Writer w, OutputStream s)
			throws Exception {
		transformDoc(new DOMSource(n), null, w == null ? new StreamResult(s)
				: new StreamResult(w));
	}

	private static void transformDoc(Source xmlSource, Source xsltSource,
			StreamResult sr, String cacheMarker,
			TransformerFactoryConfigurator configurator) throws Exception {
		Transformer trans = null;
		if (cacheMarker == null || !transformerMap.containsKey(cacheMarker)
				|| noTransformCaching) {
			TransformerFactory transFact = TransformerFactory.newInstance();
			if (configurator != null) {
				configurator.configure(transFact);
			}
			trans = xsltSource == null ? transFact.newTransformer() : transFact
					.newTransformer(xsltSource);
			if (cacheMarker != null) {
				transformerMap.put(cacheMarker, trans);
			}
		} else {
			trans = transformerMap.get(cacheMarker);
		}
		// TODO - something a little better...
		synchronized (trans) {
			trans.transform(xmlSource, sr);
		}
	}

	public static Document ownerDocumentOrSelf(Node item) {
		return (Document) (item.getNodeType() == Node.DOCUMENT_NODE ? item
				: item.getOwnerDocument());
	}

	public static void removeNode(Node node) {
		if (node != null) {
			node.getParentNode().removeChild(node);
		}
	}

	public static interface TransformerFactoryConfigurator {
		public void configure(TransformerFactory transformerFactory);
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

}
