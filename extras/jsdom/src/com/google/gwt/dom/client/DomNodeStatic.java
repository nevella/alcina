package com.google.gwt.dom.client;

import java.util.List;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.util.TextUtils;

/**
 * All 'static' classes are a workaround for jsos not allowing default methods
 * 
 * @author nick@alcina.cc
 *
 */
class DomNodeStatic {
    public static String shortLog(List<? extends DomNode> list) {
        int idx = 0;
        StringBuilder stringBuilder = new StringBuilder();
        for (DomNode domNode : list) {
            stringBuilder.append(Ax.format("%s%s\n", CommonUtils.padTwo(idx++),
                    shortLog(domNode)));
        }
        return stringBuilder.toString();
    }

    static void callMethod(DomNode domNode, String methodName) {
        throw new UnsupportedOperationException();
    }

    static Node getChild(DomNode domNode, int index) {
        assert (index >= 0) && (index < domNode
                .getChildCount()) : "Child index out of bounds";
        return domNode.getChildNodes().getItem(index);
    }

    static int getChildCount(DomNode domNode) {
        return domNode.getChildNodes().getLength();
    }

    static Element getParentElement(DomNode domNode) {
        return DOMImpl.impl.getParentElement(domNode.nodeFor());
    }

    static boolean hasParentElement(DomNode domNode) {
        return domNode.getParentElement() != null;
    }

    static Node insertAfter(DomNode domNode, Node newChild, Node refChild) {
        assert (newChild != null) : "Cannot add a null child node";
        Node next = (refChild == null) ? null : refChild.getNextSibling();
        if (next == null) {
            return domNode.appendChild(newChild);
        } else {
            return domNode.insertBefore(newChild, next);
        }
    }

    static Node insertFirst(DomNode domNode, Node child) {
        assert (child != null) : "Cannot add a null child node";
        return domNode.insertBefore(child, domNode.getFirstChild());
    }

    static Node removeAllChildren(DomNode domNode) {
        Node child = null;
        while ((child = domNode.getLastChild()) != null) {
            domNode.removeChild(child);
        }
        return null;// GWT impl returns null
    }

    static void removeFromParent(DomNode domNode) {
        Element parent = domNode.getParentElement();
        if (parent != null) {
            DomNode parentDomNode = parent.sameTreeNodeFor(domNode);
            parentDomNode.removeChild(domNode.nodeFor());
        }
    }

    static String shortLog(DomNode node) {
        if (node == null) {
            return "(null)";
        }
        String remoteHash = "";
        if (node instanceof LocalDomNode
                && ((LocalDomNode) node).nodeFor().linkedToRemote()) {
            remoteHash = String.valueOf(
                    ((LocalDomNode) node).nodeFor().remote().hashCode());
        }
        return Ax
                .format("%s%s%s%s%s", "    ",
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
