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
			return LocalDomBridge.nodeFor(jso);
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

	boolean localDomResolutionOnly;

	protected Node() {
	}

	public <T extends Node> T appendChild(T newChild) {
		T node = this.impl().appendChild(newChild);
		node.localDomResolutionOnly = localDomResolutionOnly;
		return node;
	}

	@Override
	public void callMethod(String methodName) {
		DomNode_Static.callMethod(this, methodName);
	}

	public abstract <T extends JavascriptObjectEquivalent> T cast();

	public Node cloneNode(boolean deep) {
		return this.impl().cloneNode(deep);
	}

	@Override
	public Node getChild(int index) {
		return DomNode_Static.getChild(this, index);
	}

	@Override
	public int getChildCount() {
		return DomNode_Static.getChildCount(this);
	}

	public NodeList<Node> getChildNodes() {
		return this.impl().getChildNodes();
	}

	public Node getFirstChild() {
		return this.impl().getFirstChild();
	}

	public Node getLastChild() {
		return this.impl().getLastChild();
	}

	public Node getNextSibling() {
		return this.impl().getNextSibling();
	}

	public String getNodeName() {
		return this.impl().getNodeName();
	}

	public short getNodeType() {
		return this.impl().getNodeType();
	}

	public String getNodeValue() {
		return this.impl().getNodeValue();
	}

	public Document getOwnerDocument() {
		return this.impl().getOwnerDocument();
	}

	public Element getParentElement() {
		return this.impl().getParentElement();
	}

	public Node getParentNode() {
		return this.impl().getParentNode();
	}

	public Node getPreviousSibling() {
		return this.impl().getPreviousSibling();
	}

	public boolean hasChildNodes() {
		return this.impl().hasChildNodes();
	}

	@Override
	public boolean hasParentElement() {
		return DomNode_Static.hasParentElement(this);
	}

	@Override
	public Node insertAfter(Node newChild, Node refChild) {
		return DomNode_Static.insertAfter(this, newChild, refChild);
	}

	public Node insertBefore(Node newChild, Node refChild) {
		Node node = this.impl().insertBefore(newChild, refChild);
		node.localDomResolutionOnly = localDomResolutionOnly;
		return node;
	}

	@Override
	public Node insertFirst(Node child) {
		return DomNode_Static.insertFirst(this, child);
	}

	public boolean isOrHasChild(Node child) {
		return this.impl().isOrHasChild(child);
	}

	public void localDomResolutionOnly() {
		localDomResolutionOnly = true;
		getChildNodes().forEach(Node::localDomResolutionOnly);
	}

	public Node nodeFor() {
		return this;
	}

	public boolean provideIsDom() {
		return domImpl() != null;
	}

	public boolean provideIsElement() {
		return getNodeType() == ELEMENT_NODE;
	}

	public boolean provideIsLocal() {
		return domImpl() == null || domImpl() != impl();
	}

	public abstract void putDomImpl(Node_Jso nodeDom);

	public abstract void putImpl(DomNode impl);

	@Override
	public Node removeAllChildren() {
		return DomNode_Static.removeAllChildren(this);
	}

	public Node removeChild(Node oldChild) {
		return this.impl().removeChild(oldChild);
	}

	@Override
	public void removeFromParent() {
		DomNode_Static.removeFromParent(this);
	}

	public Node replaceChild(Node newChild, Node oldChild) {
		return this.impl().replaceChild(newChild, oldChild);
	}

	public void setNodeValue(String nodeValue) {
		this.impl().setNodeValue(nodeValue);
	}

	abstract Node_Jso domImpl();

	abstract DomNode impl();
	abstract DomNode implNoResolve();
	DomNode localImpl() {
		return domImpl() != null ? null : implNoResolve();
	}
}
