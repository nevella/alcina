package com.google.gwt.dom.client;

import java.util.List;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.TextUtils;

/**
 * All 'static' classes are a workaround for jsos not allowing default methods
 *
 * @author nick@alcina.cc
 *
 */
class DomNodeStatic {
	public static String shortLog(List<? extends ClientNode> list) {
		int idx = 0;
		StringBuilder stringBuilder = new StringBuilder();
		for (ClientNode domNode : list) {
			stringBuilder.append(Ax.format("%s%s\n", CommonUtils.padTwo(idx++),
					shortLog(domNode)));
		}
		return stringBuilder.toString();
	}

	static void callMethod(ClientNode domNode, String methodName) {
		throw new UnsupportedOperationException();
	}

	static Node getChild(ClientNode domNode, int index) {
		assert (index >= 0) && (index < domNode
				.getChildCount()) : "Child index out of bounds";
		return domNode.getChildNodes().getItem(index);
	}

	static int getChildCount(ClientNode domNode) {
		return domNode.getChildNodes().getLength();
	}

	static Element getParentElement(ClientNode domNode) {
		return DOMImpl.impl.getParentElement(domNode.node());
	}

	static boolean hasParentElement(ClientNode domNode) {
		return domNode.getParentElement() != null;
	}

	static Node insertAfter(ClientNode domNode, Node newChild, Node refChild) {
		assert (newChild != null) : "Cannot add a null child node";
		if (refChild == null) {
			// insert at start
			Node firstChild = domNode.getFirstChild();
			return domNode.insertBefore(newChild, firstChild);
		} else {
			Node next = refChild.getNextSibling();
			if (next == null) {
				return domNode.appendChild(newChild);
			} else {
				return domNode.insertBefore(newChild, next);
			}
		}
	}

	static Node insertFirst(ClientNode domNode, Node child) {
		assert (child != null) : "Cannot add a null child node";
		return domNode.insertBefore(child, domNode.getFirstChild());
	}

	static Node removeAllChildren(ClientNode domNode) {
		Node child = null;
		while ((child = domNode.getLastChild()) != null) {
			domNode.removeChild(child);
		}
		return null;// GWT impl returns null
	}

	static void removeFromParent(ClientNode domNode) {
		Element parent = domNode.getParentElement();
		if (parent != null) {
			ClientNode parentDomNode = parent.sameTreeNodeFor(domNode);
			parentDomNode.removeChild(domNode.node());
		}
	}

	static String shortLog(ClientNode node) {
		if (node == null) {
			return "(null)";
		}
		String remoteHash = "";
		if (node instanceof LocalDomNode
				&& ((LocalDomNode) node).node().linkedToRemote()) {
			remoteHash = String
					.valueOf(((LocalDomNode) node).node().remote().hashCode());
		}
		return Ax
				.format("%s%s%s%s%s%s", "    ",
						CommonUtils.padStringRight(node.getNodeName(), 12, ' '),
						"  ",
						CommonUtils.padStringRight(
								String.valueOf(node.hashCode()), 16, ' '),
						CommonUtils
								.padStringRight(remoteHash, 16, ' '),
						node.getNodeType() == Node.TEXT_NODE ? CommonUtils
								.trimToWsChars(TextUtils.normalizeWhitespace(
										node.getNodeValue()), 50, true)
								: "");
	}
}
