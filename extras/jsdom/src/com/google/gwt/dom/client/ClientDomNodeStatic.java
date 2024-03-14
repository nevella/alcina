package com.google.gwt.dom.client;

import java.util.List;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.TextUtils;

/**
 * All 'static' classes are a workaround for jsos not allowing default methods
 *
 * 
 *
 */
class ClientDomNodeStatic {
	public static String shortLog(List<? extends ClientDomNode> list) {
		int idx = 0;
		StringBuilder stringBuilder = new StringBuilder();
		for (ClientDomNode domNode : list) {
			stringBuilder.append(Ax.format("%s%s\n", CommonUtils.padTwo(idx++),
					shortLog(domNode)));
		}
		return stringBuilder.toString();
	}

	static void callMethod(ClientDomNode domNode, String methodName) {
		throw new UnsupportedOperationException();
	}

	static Node getChild(ClientDomNode domNode, int index) {
		assert (index >= 0) && (index < domNode
				.getChildCount()) : "Child index out of bounds";
		return domNode.getChildNodes().getItem(index);
	}

	static int getChildCount(ClientDomNode domNode) {
		return domNode.getChildNodes().getLength();
	}

	static Element getParentElement(ClientDomNode domNode) {
		return DOMImpl.impl.getParentElement(domNode.node());
	}

	static boolean hasParentElement(ClientDomNode domNode) {
		return domNode.getParentElement() != null;
	}

	static Node insertAfter(ClientDomNode domNode, Node newChild,
			Node refChild) {
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

	static Node insertFirst(ClientDomNode domNode, Node child) {
		assert (child != null) : "Cannot add a null child node";
		return domNode.insertBefore(child, domNode.getFirstChild());
	}

	static Node removeAllChildren(ClientDomNode domNode) {
		Node child = null;
		while ((child = domNode.getLastChild()) != null) {
			domNode.removeChild(child);
		}
		return null;// GWT impl returns null
	}

	static void removeFromParent(ClientDomNode domNode) {
		Element parent = domNode.getParentElement();
		if (parent != null) {
			ClientDomNode parentDomNode = parent.sameTreeNodeFor(domNode);
			parentDomNode.removeChild(domNode.node());
		}
	}

	static String shortLog(ClientDomNode node) {
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
