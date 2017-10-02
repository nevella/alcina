/*
 * Copyright 2008 Google Inc.
 * 
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
package com.google.gwt.dom.client;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JavascriptObjectEquivalent;

/**
 * The Node interface is the primary datatype for the entire Document Object
 * Model. It represents a single node in the document tree. While all objects
 * implementing the Node interface expose methods for dealing with children, not
 * all objects implementing the Node interface may have children.
 */
public abstract class Node implements JavascriptObjectEquivalent, DomNode {
	/**
	 * The node is an {@link Element}.
	 */
	public static final short ELEMENT_NODE = 1;

	/**
	 * The node is a {@link Text} node.
	 */
	public static final short TEXT_NODE = 3;

	/**
	 * The node is a {@link Document}.
	 */
	public static final short DOCUMENT_NODE = 9;

	/**
	 * Assert that the given {@link JavaScriptObject} is a DOM node and
	 * automatically typecast it.
	 */
	public static Node as(JavascriptObjectEquivalent o) {
		if (o instanceof JavaScriptObject) {
			JavaScriptObject jso = (JavaScriptObject) o;
			assert isJso(jso);
			return LocalDom.nodeFor(jso);
		} else {
			return (Node) o;
		}
	}

	public static boolean is(JavascriptObjectEquivalent o) {
		if (o instanceof JavaScriptObject) {
			JavaScriptObject jso = (JavaScriptObject) o;
			return isJso(jso);
		}
		return o instanceof Node;
	}

	/**
	 * Determines whether the given {@link JavaScriptObject} is a DOM node. A
	 * <code>null</code> object will cause this method to return
	 * <code>false</code>. The try catch is needed for the firefox permission
	 * error: "Permission denied to access property 'nodeType'"
	 */
	private static native boolean isJso(JavaScriptObject o) /*-{
        try {
            return (!!o) && (!!o.nodeType);
        } catch (e) {
            return false;
        }
	}-*/;

	protected Node() {
	}

	public <T extends Node> T appendChild(T newChild) {
		ensureRemoteCheck();
		T node = local().appendChild(newChild);
		remote().appendChild(newChild);
		return node;
	}

	@Override
	public void callMethod(String methodName) {
		DomNodeStatic.callMethod(this, methodName);
	}

	boolean fromParsedRemote;

	/**
	 * If the node was flushed, need to create a pending remote node for later
	 * resolution
	 */
	protected void ensureRemoteCheck() {
		if (!linkedToRemote()
				&& (fromParsedRemote || local().provideWasFlushed())
				&& !LocalDom.isDisableWriteCheck()) {
			LocalDom.ensureRemote((Element) this);
		}
	}

	public abstract <T extends JavascriptObjectEquivalent> T cast();

	public Node cloneNode(boolean deep) {
		// FIXME - maybe - remote should probably always be resolved (so maybe
		// ok)
		return local().cloneNode(deep);
	}

	@Override
	public Node getChild(int index) {
		return DomNodeStatic.getChild(this, index);
	}

	@Override
	public int getChildCount() {
		return DomNodeStatic.getChildCount(this);
	}

	public NodeList<Node> getChildNodes() {
		return local().getChildNodes();
	}

	public Node getFirstChild() {
		return local().getFirstChild();
	}

	public Node getLastChild() {
		return local().getLastChild();
	}

	public Node getNextSibling() {
		return local().getNextSibling();
	}

	public String getNodeName() {
		return local().getNodeName();
	}

	public short getNodeType() {
		return local().getNodeType();
	}

	public String getNodeValue() {
		return local().getNodeValue();
	}

	public Document getOwnerDocument() {
		return local().getOwnerDocument();
	}

	public Element getParentElement() {
		return local().getParentElement();
	}

	public Node getParentNode() {
		return local().getParentNode();
	}

	public Node getPreviousSibling() {
		return local().getPreviousSibling();
	}

	public boolean hasChildNodes() {
		return local().hasChildNodes();
	}

	@Override
	public boolean hasParentElement() {
		return DomNodeStatic.hasParentElement(this);
	}

	@Override
	public Node insertAfter(Node newChild, Node refChild) {
		return DomNodeStatic.insertAfter(this, newChild, refChild);
	}

	public Node insertBefore(Node newChild, Node refChild) {
		return local().insertBefore(newChild, refChild);
	}

	@Override
	public Node insertFirst(Node child) {
		return DomNodeStatic.insertFirst(this, child);
	}

	public boolean isOrHasChild(Node child) {
		return local().isOrHasChild(child);
	}

	public boolean provideIsElement() {
		return getNodeType() == ELEMENT_NODE;
	}

	@Override
	public Node removeAllChildren() {
		return DomNodeStatic.removeAllChildren(this);
	}

	public Node removeChild(Node oldChild) {
		Node result = local().removeChild(oldChild);
		remote().removeChild(oldChild);
		return result;
	}

	@Override
	public void removeFromParent() {
		DomNodeStatic.removeFromParent(this);
	}

	public Node replaceChild(Node newChild, Node oldChild) {
		Node result = local().replaceChild(newChild, oldChild);
		remote().replaceChild(newChild, oldChild);
		return result;
	}

	public void setNodeValue(String nodeValue) {
		local().setNodeValue(nodeValue);
	}

	protected abstract boolean linkedToRemote();

	protected abstract <T extends NodeLocal> T local();

	protected abstract void putRemote(NodeRemote nodeDom);

	protected abstract <T extends DomNode> T remote();
}
