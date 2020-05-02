package cc.alcina.framework.entity;

import java.util.List;
import java.util.regex.Pattern;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.CachingMap;
import cc.alcina.framework.common.client.util.ThrowingFunction;
import cc.alcina.framework.common.client.xml.XmlNode.XpathEvaluator;

public class OptimizingXpathEvaluator implements XpathEvaluator {
	private boolean optimiseXpathEvaluationSpeed = false;

	private Node removedNode;

	private Element removedParent;

	private Node removedNextSib;

	private Pattern nonOptimise = Pattern
			.compile("\\.\\.|ancestor|parent|following::|preceding::");

	private Node node;

	private CachingMap<String, XPathExpression> expressionCache;

	private XpathHelper xpathHelper;

	public OptimizingXpathEvaluator(XpathHelper xpathHelper,
			XpathAndExpressionCache xexc) {
		this.xpathHelper = xpathHelper;
		this.expressionCache = xexc.expressionCache;
	}

	public Element getElementByXpath(String xpathStr, Node node) {
		return (Element) evaluate(xpathStr, node, XPathConstants.NODE, n -> {
			return (Element) n;
		});
	}

	public List<Element> getElementsByXpath(String xpathStr, Node node) {
		return (List<Element>) evaluate(xpathStr, node, XPathConstants.NODESET,
				o -> {
					return XmlUtils.nodeListToElementList((NodeList) o);
				});
	}

	public Node getNodeByXpath(String xpathStr, Node node) {
		return (Node) evaluate(xpathStr, node, XPathConstants.NODE, n -> {
			return (Node) n;
		});
	}

	public List<Node> getNodesByXpath(String xpathStr, Node node) {
		return (List<Node>) evaluate(xpathStr, node, XPathConstants.NODESET,
				o -> {
					return XmlUtils.nodeListToList((NodeList) o);
				});
	}

	public String getTextContentOrEmpty(String xpath, Node from) {
		Node node = getNodeByXpath(xpath, from);
		return node == null ? "" : node.getTextContent();
	}

	public XpathHelper getXpathHelper() {
		return xpathHelper;
	}

	public boolean isOptimiseXpathEvaluationSpeed() {
		return optimiseXpathEvaluationSpeed;
	}

	public void setOptimiseXpathEvaluationSpeed(
			boolean optimiseXpathEvaluationSpeed) {
		this.optimiseXpathEvaluationSpeed = optimiseXpathEvaluationSpeed;
	}

	private <T> T evaluate(String xpathStr, Node node, QName qName,
			ThrowingFunction<Object, T> mapper) {
		Document monitor = node instanceof Document ? (Document) node
				: node.getOwnerDocument();
		synchronized (monitor) {
			try {
				xpathStr = maybeRemove(xpathStr, node);
				Object result = expressionCache.get(xpathStr)
						.evaluate(this.node, qName);
				maybeReinsert();
				return mapper.apply(result);
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
	}

	private void maybeReinsert() {
		if (removedNode != null) {
			removedParent.insertBefore(removedNode, removedNextSib);
			removedNode = null;
		}
	}

	private String maybeRemove(String xpathStr, Node node) {
		while (xpathStr.startsWith("../") && node.getParentNode() != null
				&& node.getParentNode().getNodeType() == Node.ELEMENT_NODE) {
			xpathStr = xpathStr.substring(3);
			node = node.getParentNode();
		}
		this.node = node;
		if (isOptimiseXpathEvaluationSpeed()
				&& !nonOptimise.matcher(xpathStr).find()
				&& node.getNodeType() == Node.ELEMENT_NODE
				&& node.getParentNode() != null) {
			if (xpathStr.contains("sibling")) {
				node = node.getParentNode();
			}
			if (node.getParentNode().getNodeType() != Node.ELEMENT_NODE) {
				return xpathStr;
			}
			removedParent = (Element) node.getParentNode();
			removedNextSib = node.getNextSibling();
			removedNode = node;
			removedParent.removeChild(node);
		}
		return xpathStr;
	}
}
