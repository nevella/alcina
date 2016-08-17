package cc.alcina.framework.entity;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;

import cc.alcina.framework.common.client.WrappedRuntimeException;

public class XpathHelper {
	private Document primaryDoc;

	private Map<Document, XPath> docXpathMap = new LinkedHashMap<Document, XPath>();

	public XpathHelper() {
	}

	public XpathHelper(Node node) {
		registerPrimary(ownerDoc(node));
	}

	public Document ownerDoc(Node node) {
		return XmlUtils.ownerDocumentOrSelf(node);
	}

	public Element getElementByXpath(String xpathStr, Node node) {
		return getElementByXpathDoc(primaryDoc, xpathStr, node);
	}

	public Element getElementByXpathDoc(Document doc, String xpathStr,
			Node node) {
		if (ownerDoc(node) != doc) {
			throw new RuntimeException("reusing xpath for different documen");
		}
		try {
			return (Element) getXpath(doc).evaluate(xpathStr, node,
					XPathConstants.NODE);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public List<Element> getElementsByXpath(String xpathStr, Node node) {
		return getElementsByXpathDoc(primaryDoc, xpathStr, node);
	}

	public List<Element> elements(String xpathStr) {
		return getElementsByXpathDoc(primaryDoc, xpathStr, primaryDoc);
	}

	public Element element(String xpathStr) {
		return getElementByXpathDoc(primaryDoc, xpathStr, primaryDoc);
	}

	public List<Element> getElementsByXpathDoc(Document doc, String xpathStr,
			Node node) {
		if (ownerDoc(node) != doc) {
			throw new RuntimeException("reusing xpath for different documen");
		}
		try {
			return XmlUtils.nodeListToElementList((NodeList) getXpath(doc)
					.evaluate(xpathStr, node, XPathConstants.NODESET));
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public Node getNodeByXpath(String xpathStr, Node node) throws Exception {
		return getNodeByXpathDoc(primaryDoc, xpathStr, node);
	}

	public List<Node> getNodesByXpath(String xpathStr, Node node)
			throws Exception {
		if (ownerDoc(node) != primaryDoc) {
			throw new RuntimeException("reusing xpath for different documen");
		}
		return XmlUtils.nodeListToList((NodeList) getXpath(primaryDoc)
				.evaluate(xpathStr, node, XPathConstants.NODESET));
	}

	public Node getNodeByXpathDoc(Document doc, String xpathStr, Node node) {
		if (ownerDoc(node) != doc) {
			throw new RuntimeException("reusing xpath for different documen");
		}
		try {
			return (Node) getXpath(doc).evaluate(xpathStr, node,
					XPathConstants.NODE);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public void registerPrimary(Document doc) {
		this.primaryDoc = doc;
		register(doc);
	}

	public void registerSecondary(Document doc) {
		register(doc);
	}

	public void releaseSecondary(Document doc) {
		docXpathMap.remove(doc);
	}

	public static interface XpathHelperExt {
		default void configureXpath(XPath xpath) {
			synchronized (this) {
			}
		}
	}

	public static XpathHelperExt ext = new XpathHelperExt() {
	};

	private XPath createXpath() {
		XPathFactory factory = XPathFactory.newInstance();
		XPath xpath = factory.newXPath();
		ext.configureXpath(xpath);
		return xpath;
	}

	private XPath getXpath(Document doc) {
		return docXpathMap.get(doc);
	}

	private void register(Document doc) {
		if (docXpathMap.containsKey(doc)) {
			throw new RuntimeException("Doc already registered");
		}
		docXpathMap.put(doc, createXpath());
	}

	public OptimizingXpathEvaluator createOptimisedEvaluator(Node node) {
		OptimizingXpathEvaluator evaluator = new OptimizingXpathEvaluator(
				docXpathMap.get(ownerDoc(node)));
		evaluator.setOptimiseXpathEvaluationSpeed(true);
		return evaluator;
	}

	public boolean isFor(Document ownerDocument) {
		return primaryDoc == ownerDocument;
	}

	public String getTextContentOrEmpty(String xpath, Node from) {
		try {
			Node node = getNodeByXpath(xpath, from);
			return node == null ? "" : node.getTextContent();
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public void removeMatches(String xpath) throws Exception {
		List<Node> nodes = getNodesByXpath(xpath, primaryDoc);
		for (Node node : nodes) {
			XmlUtils.removeNode(node);
		}
	}

	public <T extends Node> List<T> nodes(String xpathStr) {
		try {
			return (List<T>) (List) getNodesByXpath(xpathStr, primaryDoc);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
		
	}
}
