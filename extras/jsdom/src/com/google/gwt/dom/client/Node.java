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
import java.util.stream.Stream;

import org.w3c.dom.DOMException;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.UserDataHandler;

import com.google.common.base.Preconditions;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JavascriptObjectEquivalent;

/**
 * The Node interface is the primary datatype for the entire Document Object
 * Model. It represents a single node in the document tree. While all objects
 * implementing the Node interface expose methods for dealing with children, not
 * all objects implementing the Node interface may have children.
 */
public abstract class Node
		implements JavascriptObjectEquivalent, DomNode, org.w3c.dom.Node {
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

	@Override
	public org.w3c.dom.Node appendChild(org.w3c.dom.Node arg0)
			throws DOMException {
		return appendChild((Node) arg0);
	}

	@Override
	public <T extends Node> T appendChild(T newChild) {
		validateInsert(newChild);
		doPreTreeResolution(newChild);
		T node = local().appendChild(newChild);
		remote().appendChild(newChild);
		return node;
	}

	@Override
	public void callMethod(String methodName) {
		DomNodeStatic.callMethod(this, methodName);
	}

	@Override
	public abstract <T extends JavascriptObjectEquivalent> T cast();

	@Override
	public Node cloneNode(boolean deep) {
		// FIXME - maybe - remote should probably always be resolved (so maybe
		// ok)
		return local().cloneNode(deep);
	}

	@Override
	public short compareDocumentPosition(org.w3c.dom.Node arg0)
			throws DOMException {
		throw new UnsupportedOperationException();
	}

	@Override
	public NamedNodeMap getAttributes() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getBaseURI() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Node getChild(int index) {
		return DomNodeStatic.getChild(this, index);
	}

	@Override
	public int getChildCount() {
		return DomNodeStatic.getChildCount(this);
	}

	@Override
	public NodeList<Node> getChildNodes() {
		return local().getChildNodes();
	}

	@Override
	public Object getFeature(String arg0, String arg1) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Node getFirstChild() {
		return local().getFirstChild();
	}

	@Override
	public Node getLastChild() {
		return local().getLastChild();
	}

	@Override
	public String getLocalName() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getNamespaceURI() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Node getNextSibling() {
		return local().getNextSibling();
	}

	@Override
	public String getNodeName() {
		return local().getNodeName();
	}

	@Override
	public short getNodeType() {
		return local().getNodeType();
	}

	@Override
	public String getNodeValue() {
		return local().getNodeValue();
	}

	@Override
	public Document getOwnerDocument() {
		return local().getOwnerDocument();
	}

	@Override
	public Element getParentElement() {
		return local().getParentElement();
	}

	@Override
	public Node getParentNode() {
		return local().getParentNode();
	}

	@Override
	public String getPrefix() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Node getPreviousSibling() {
		return local().getPreviousSibling();
	}

	@Override
	public Object getUserData(String arg0) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean hasAttributes() {
		throw new UnsupportedOperationException();
	}

	@Override
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

	@Override
	public Node insertBefore(Node newChild, Node refChild) {
		try {
			// new child first
			validateInsert(newChild);
			doPreTreeResolution(newChild);
			doPreTreeResolution(refChild);
			Node result = local().insertBefore(newChild, refChild);
			remote().insertBefore(newChild, refChild);
			return result;
		} catch (Exception e) {
			throw new LocalDomException(e);
		}
	}

	@Override
	public Node insertBefore(org.w3c.dom.Node arg0, org.w3c.dom.Node arg1)
			throws DOMException {
		return insertBefore((Node) arg0, (Node) arg1);
	}

	@Override
	public Node insertFirst(Node child) {
		return DomNodeStatic.insertFirst(this, child);
	}

	@Override
	public boolean isDefaultNamespace(String arg0) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isEqualNode(org.w3c.dom.Node arg0) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isOrHasChild(Node child) {
		return local().isOrHasChild(child);
	}

	@Override
	public boolean isSameNode(org.w3c.dom.Node arg0) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isSupported(String arg0, String arg1) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String lookupNamespaceURI(String arg0) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String lookupPrefix(String arg0) {
		throw new UnsupportedOperationException();
	}

	@Override
	public abstract Node nodeFor();

	@Override
	public void normalize() {
		throw new UnsupportedOperationException();
	}

	public List<Node> provideChildNodeList() {
		return new ChildNodeList();
	}

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

	@Override
	public Node removeChild(Node oldChild) {
		doPreTreeResolution(oldChild);
		Node result = local().removeChild(oldChild);
		remote().removeChild(oldChild);
		LocalDom.detach(oldChild);
		return result;
	}

	@Override
	public org.w3c.dom.Node removeChild(org.w3c.dom.Node arg0)
			throws DOMException {
		return removeChild((Node) arg0);
	}

	@Override
	public void removeFromParent() {
		ensureRemoteCheck();
		remote().removeFromParent();
		local().removeFromParent();
	}

	@Override
	public Node replaceChild(Node newChild, Node oldChild) {
		doPreTreeResolution(oldChild);
		doPreTreeResolution(newChild);
		remote().replaceChild(newChild, oldChild);
		Node result = local().replaceChild(newChild, oldChild);
		LocalDom.detach(oldChild);
		return result;
	}

	@Override
	public org.w3c.dom.Node replaceChild(org.w3c.dom.Node arg0,
			org.w3c.dom.Node arg1) throws DOMException {
		return replaceChild((Node) arg0, (Node) arg1);
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

	@Override
	public void setNodeValue(String nodeValue) {
		ensureRemoteCheck();
		local().setNodeValue(nodeValue);
		remote().setNodeValue(nodeValue);
	}

	@Override
	public void setPrefix(String arg0) throws DOMException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setTextContent(String arg0) throws DOMException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object setUserData(String arg0, Object arg1, UserDataHandler arg2) {
		throw new UnsupportedOperationException();
	}

	public Stream<Node> streamChildren() {
		return getChildNodes().stream();
	}

	protected void doPreTreeResolution(Node child) {
		if (child != null) {
			boolean ensureBecauseChildResolved = (child.wasResolved()
					|| child.linkedToRemote())
					&& (!linkedToRemote() || isPendingResolution());
			if (ensureBecauseChildResolved) {
				LocalDom.ensureRemote(this);
			}
			boolean linkedBecauseFlushed = ensureRemoteCheck();
			if (linkedToRemote() && (wasResolved() || child.wasResolved())) {
				if (child.wasResolved()) {
					LocalDom.ensureRemote(child);
				} else {
					LocalDom.ensureRemoteNodeMaybePendingResolution(child);
				}
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

	protected boolean isPendingResolution() {
		return false;
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

	protected void resetRemote() {
		clearResolved();
		resetRemote0();
	}

	protected abstract void resetRemote0();

	protected abstract NodeRemote typedRemote();

	protected void validateInsert(Node newChild) {
	}

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

	class ChildNodeList extends AbstractList<Node> {
		@Override
		public Node get(int index) {
			return local().getChildren().get(index).node;
		}

		@Override
		public int size() {
			return local().getChildren().size();
		}
	}
}
