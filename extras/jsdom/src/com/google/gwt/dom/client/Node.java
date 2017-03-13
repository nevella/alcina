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

import com.google.gwt.core.client.CastableFromJavascriptObject;
import com.google.gwt.core.client.JavaScriptObject;

/**
 * The Node interface is the primary datatype for the entire Document Object
 * Model. It represents a single node in the document tree. While all objects
 * implementing the Node interface expose methods for dealing with children, not
 * all objects implementing the Node interface may have children.
 */
public abstract class Node<DN extends DomNode, ND extends Node_Dom>
		implements CastableFromJavascriptObject, DomNode {
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

	protected DN impl = null;

	public  ND domImpl = null;

	protected boolean resolved;

	public <T extends Node> T appendChild(T newChild) {
		return this.impl.appendChild(newChild);
	}

	public Node cloneNode(boolean deep) {
		return this.impl.cloneNode(deep);
	}

	public NodeList<Node> getChildNodes() {
		return this.impl.getChildNodes();
	}

	public Node getFirstChild() {
		return this.impl.getFirstChild();
	}

	public Node getLastChild() {
		return this.impl.getLastChild();
	}

	public Node getNextSibling() {
		return this.impl.getNextSibling();
	}

	public String getNodeName() {
		return this.impl.getNodeName();
	}

	public short getNodeType() {
		return this.impl.getNodeType();
	}

	public String getNodeValue() {
		return this.impl.getNodeValue();
	}

	public Element getParentElement() {
		return this.impl.getParentElement();
	}

	public Document getOwnerDocument() {
		return this.impl.getOwnerDocument();
	}

	public Node getParentNode() {
		return this.impl.getParentNode();
	}

	public Node getPreviousSibling() {
		return this.impl.getPreviousSibling();
	}

	public boolean hasChildNodes() {
		return this.impl.hasChildNodes();
	}

	public Node insertBefore(Node newChild, Node refChild) {
		return this.impl.insertBefore(newChild, refChild);
	}

	public boolean isOrHasChild(Node child) {
		return this.impl.isOrHasChild(child);
	}

	public Node replaceChild(Node newChild, Node oldChild) {
		return this.impl.replaceChild(newChild, oldChild);
	}

	public void setNodeValue(String nodeValue) {
		this.impl.setNodeValue(nodeValue);
	}


	/**
	 * Assert that the given {@link JavaScriptObject} is a DOM node and
	 * automatically typecast it.
	 */
	public static Node as(JavaScriptObject o) {
		assert is(o);
		return VmLocalDomBridge.nodeFor(o);
	}

	/**
	 * Determines whether the given {@link JavaScriptObject} is a DOM node. A
	 * <code>null</code> object will cause this method to return
	 * <code>false</code>. The try catch is needed for the firefox permission
	 * error: "Permission denied to access property 'nodeType'"
	 */
	public static native boolean is(JavaScriptObject o) /*-{
        try {
            return (!!o) && (!!o.nodeType);
        } catch (e) {
            return false;
        }
	}-*/;

	protected Node() {
	}

	public Node nodeFor() {
		return this;
	}

	public Node removeChild(Node oldChild) {
		return this.impl.removeChild(oldChild);
	}

	public boolean provideIsElement() {
		return getNodeType() == ELEMENT_NODE;
	}
	
	
}
