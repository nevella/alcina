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

import java.util.AbstractList;
import java.util.List;

import com.google.common.base.Preconditions;
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

	private int resolvedEventId;

	protected Node() {
	}

	public <T extends Node> T appendChild(T newChild) {
		doPreTreeResolution(newChild);
		T node = local().appendChild(newChild);
		remote().appendChild(newChild);
		return node;
	}

	@Override
	public void callMethod(String methodName) {
		DomNodeStatic.callMethod(this, methodName);
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

	public List<Node> provideChildNodeList() {
		return new ChildNodeList();
	}

	class ChildNodeList extends AbstractList<Node> {
		@Override
		public Node get(int index) {
			return local().children.get(index).node;
		}

		@Override
		public int size() {
			return local().children.size();
		}
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
	public int indexInParentChildren() {
		return local().indexInParentChildren();
	}

	@Override
	public Node insertAfter(Node newChild, Node refChild) {
		return DomNodeStatic.insertAfter(this, newChild, refChild);
	}

	public Node insertBefore(Node newChild, Node refChild) {
		// new child first
		doPreTreeResolution(newChild);
		doPreTreeResolution(refChild);
		Node result = local().insertBefore(newChild, refChild);
		remote().insertBefore(newChild, refChild);
		return result;
	}

	@Override
	public Node insertFirst(Node child) {
		return DomNodeStatic.insertFirst(this, child);
	}

	public boolean isOrHasChild(Node child) {
		return local().isOrHasChild(child);
	}

	@Override
	public abstract Node nodeFor();

	public boolean provideIsElement() {
		return getNodeType() == ELEMENT_NODE;
	}

	public boolean provideIsText() {
		return getNodeType() == TEXT_NODE;
	}

	@Override
	public Node removeAllChildren() {
		getChildNodes().forEach(n -> doPreTreeResolution(n));
		return DomNodeStatic.removeAllChildren(this);
	}

	public Node removeChild(Node oldChild) {
		doPreTreeResolution(oldChild);
		Node result = local().removeChild(oldChild);
		remote().removeChild(oldChild);
		LocalDom.detach(oldChild);
		return result;
	}

	@Override
	public void removeFromParent() {
		ensureRemoteCheck();
		remote().removeFromParent();
		local().removeFromParent();
	}

	public Node replaceChild(Node newChild, Node oldChild) {
		doPreTreeResolution(oldChild);
		doPreTreeResolution(newChild);
		remote().replaceChild(newChild, oldChild);
		Node result = local().replaceChild(newChild, oldChild);
		LocalDom.detach(oldChild);
		return result;
	}

	public DomNode sameTreeNodeFor(DomNode domNode) {
		if (domNode == null) {
			return null;
		}
		if (domNode instanceof LocalDomNode) {
			return local();
		} else {
			return remote();
		}
	}

	public void setNodeValue(String nodeValue) {
		ensureRemoteCheck();
		local().setNodeValue(nodeValue);
		remote().setNodeValue(nodeValue);
		;
	}

	protected void doPreTreeResolution(Node child) {
		if (child != null) {
			boolean ensureBecauseChildResolved = child.wasResolved()
					&& !linkedToRemote();
			if (ensureBecauseChildResolved) {
				LocalDom.ensureRemote(this);
			}
			boolean linkedBecauseFlushed = ensureRemoteCheck();
			if (linkedToRemote() && (wasResolved() || child.wasResolved())) {
				LocalDom.ensureRemote(child);
			}
		}
	}

	/**
	 * If the node was flushed, then we need to link to the remote (or our
	 * local/remote will be inconsistent)
	 * 
	 */
	protected boolean ensureRemoteCheck() {
		if (!linkedToRemote() && wasResolved()
				&& provideSelfOrAncestorLinkedToRemote() != null
				&& !LocalDom.isDisableRemoteWrite()
				&& (provideIsText() || provideIsElement())) {
			LocalDom.ensureRemote(this);
			return true;
		} else {
			return false;
		}
	}

	protected abstract boolean linkedToRemote();

	protected abstract <T extends NodeLocal> T local();

	protected Node provideRoot() {
		if (getParentElement() != null) {
			return getParentElement().provideRoot();
		}
		return this;
	}

	protected Node provideSelfOrAncestorLinkedToRemote() {
		if (linkedToRemote()) {
			return this;
		}
		if (getParentElement() != null) {
			return getParentElement().provideSelfOrAncestorLinkedToRemote();
		}
		return null;
	}

	protected abstract void putRemote(NodeRemote nodeDom, boolean resolved);

	protected abstract <T extends DomNode> T remote();

	protected abstract NodeRemote typedRemote();

	/**
	 * only call on reparse
	 */
	void clearResolved() {
		resolvedEventId = 0;
	}

	void resolved(int wasResolvedEventId) {
		Preconditions.checkState(this.resolvedEventId == 0
				|| this.resolvedEventId == wasResolvedEventId);
		this.resolvedEventId = wasResolvedEventId;
	}

	boolean wasResolved() {
		return resolvedEventId > 0;
	}
}
