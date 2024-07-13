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

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.DomNodeType;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.util.Ax;

/**
 * The Node interface is the primary datatype for the entire Document Object
 * Model. It represents a single node in the document tree. While all objects
 * implementing the Node interface expose methods for dealing with children, not
 * all objects implementing the Node interface may have children.
 */
@Reflected
public abstract class Node
		implements JavascriptObjectEquivalent, ClientDomNode, org.w3c.dom.Node {
	/**
	 * For subtypes, most of the methods assume the remote() is a NodeJso -
	 * where that's not the case, the methods should return null if the remote
	 * is a NodeRefid subtype
	 *
	 * 
	 *
	 */
	public class ImplAccess {
		public <E extends ClientDomNode> E ensureRemote() {
			LocalDom.ensureRemote(Node.this);
			return remote();
		}

		public boolean isJsoRemote() {
			return remote().isJso();
		}

		public boolean isRefidRemote() {
			return remote().isRefid();
		}

		public NodeJso jsoRemote() {
			return remote();
		}

		public <E extends ClientDomNode> E local() {
			return (E) Node.this.local();
		}

		public void putRemoteNoSideEffects(ClientDomNode remote) {
			throw new UnsupportedOperationException();
		}

		public <E extends ClientDomNode> E remote() {
			return (E) Node.this.remote();
		}
	}

	class ChildNodeList extends AbstractList<Node> {
		@Override
		public Node get(int index) {
			return local().getChildren().get(index).node();
		}

		@Override
		public int size() {
			return local().getChildren().size();
		}
	}

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

	protected int syncId;

	boolean attached;

	/*
	 * Attached nodes will have a non-zero refId, whihc is used for tree sync
	 */
	int refId;

	protected Node() {
		resetRemote();
	}

	public boolean isAttached() {
		return attached;
	}

	@Override
	public org.w3c.dom.Node appendChild(org.w3c.dom.Node arg0)
			throws DOMException {
		return appendChild((Node) arg0);
	}

	@Override
	public <T extends Node> T appendChild(T newChild) {
		validateInsert(newChild);
		doPreTreeSync(newChild);
		T node = local().appendChild(newChild);
		notify(() -> LocalDom.getLocalMutations().notifyChildListMutation(this,
				node, node.getPreviousSibling(), true));
		sync(() -> remote().appendChild(newChild));
		return node;
	}

	public DomNode asDomNode() {
		return DomNode.from(this);
	}

	@Override
	public void callMethod(String methodName) {
		ClientDomNodeStatic.callMethod(this, methodName);
	}

	@Override
	public abstract <T extends JavascriptObjectEquivalent> T cast();

	@Override
	public Node cloneNode(boolean deep) {
		// The cloned subtree won't be *worse* than the current remote/local
		// subtree sync
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
		return ClientDomNodeStatic.getChild(this, index);
	}

	@Override
	public int getChildCount() {
		return ClientDomNodeStatic.getChildCount(this);
	}

	@Override
	public NodeList<Node> getChildNodes() {
		return local().getChildNodes();
	}

	public DomNodeType getDomNodeType() {
		return DomNodeType.fromW3cNode(this);
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
		return ClientDomNodeStatic.hasParentElement(this);
	}

	public ImplAccess implAccess() {
		return new ImplAccess();
	}

	@Override
	public int indexInParentChildren() {
		return local().indexInParentChildren();
	}

	@Override
	public Node insertAfter(Node newChild, Node refChild) {
		return ClientDomNodeStatic.insertAfter(this, newChild, refChild);
	}

	@Override
	public Node insertBefore(Node newChild, Node refChild) {
		try {
			// new child first
			validateInsert(newChild);
			doPreTreeSync(newChild);
			doPreTreeSync(refChild);
			Node result = local().insertBefore(newChild, refChild);
			notify(() -> LocalDom.getLocalMutations().notifyChildListMutation(
					this, newChild, newChild.getPreviousSibling(), true));
			sync(() -> remote().insertBefore(newChild, refChild));
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
		return ClientDomNodeStatic.insertFirst(this, child);
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
	public boolean isJso() {
		return false;
	}

	@Override
	public boolean isRefid() {
		return false;
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
	public abstract Node node();

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
		getChildNodes().forEach(n -> doPreTreeSync(n));
		return ClientDomNodeStatic.removeAllChildren(this);
	}

	@Override
	public Node removeChild(Node oldChild) {
		doPreTreeSync(oldChild);
		notify(() -> LocalDom.getLocalMutations().notifyChildListMutation(this,
				oldChild, null, false));
		Node result = local().removeChild(oldChild);
		sync(() -> remote().removeChild(oldChild));
		oldChild.resetRemote();
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
		sync(() -> remote().removeFromParent());
		notify(() -> LocalDom.getLocalMutations().notifyChildListMutation(this,
				this, null, false));
		local().removeFromParent();
		resetRemote();
	}

	@Override
	public Node replaceChild(Node newChild, Node oldChild) {
		doPreTreeSync(oldChild);
		doPreTreeSync(newChild);
		sync(() -> remote().replaceChild(newChild, oldChild));
		notify(() -> LocalDom.getLocalMutations().notifyChildListMutation(this,
				oldChild, null, false));
		notify(() -> LocalDom.getLocalMutations().notifyChildListMutation(this,
				newChild, newChild.getPreviousSibling(), true));
		Node result = local().replaceChild(newChild, oldChild);
		oldChild.resetRemote();
		return result;
	}

	@Override
	public org.w3c.dom.Node replaceChild(org.w3c.dom.Node arg0,
			org.w3c.dom.Node arg1) throws DOMException {
		return replaceChild((Node) arg0, (Node) arg1);
	}

	public ClientDomNode sameTreeNodeFor(ClientDomNode domNode) {
		if (domNode == null) {
			return null;
		}
		if (!(domNode instanceof NodeRemote)) {
			return local();
		} else {
			return remote();
		}
	}

	@Override
	public void setNodeValue(String nodeValue) {
		ensureRemoteCheck();
		local().setNodeValue(nodeValue);
		notify(() -> LocalDom.getLocalMutations().notifyCharacterData(this,
				nodeValue));
		sync(() -> remote().setNodeValue(nodeValue));
	}

	@Override
	public void setPrefix(String arg0) throws DOMException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setTextContent(String arg0) throws DOMException {
		setNodeValue(arg0);
	}

	@Override
	public Object setUserData(String arg0, Object arg1, UserDataHandler arg2) {
		throw new UnsupportedOperationException();
	}

	public Stream<Node> streamChildren() {
		return getChildNodes().stream();
	}

	protected void onAttach() {
		getOwnerDocument().localDom.onAttach(this);
		streamChildren().forEach(n -> n.setAttached(true));
	}

	protected void onDetach() {
		getOwnerDocument().localDom.onDetach(this);
		// note this will be undone for the top-of-the-detach-tree (see void
		// setAttached(boolean attached) )
		resetRemote();
		streamChildren().forEach(n -> n.setAttached(false));
	}

	// FIXME - refid - with refid, this becomes simpler - just remove/reset any
	// existing remote dom on attach
	protected void doPreTreeSync(Node child) {
		if (child != null) {
			boolean ensureBecauseChildSynced = (child.wasSynced()
					|| child.linkedToRemote())
					&& (!linkedToRemote() || isPendingSync());
			if (ensureBecauseChildSynced) {
				LocalDom.ensureRemote(this);
			}
			boolean linkedBecauseFlushed = ensureRemoteCheck();
			if (linkedToRemote() && (wasSynced() || child.wasSynced())) {
				if (child.wasSynced()) {
					LocalDom.ensureRemote(child);
				} else {
					LocalDom.ensureRemoteNodeMaybePendingSync(child);
				}
			}
		}
	}

	/**
	 * If the node was flushed (i.e. part of a tree that was flushed, has a
	 * non-zero syncEventId), then we need to link it to the remote (or our
	 * local/remote will be inconsistent)
	 *
	 */
	protected boolean ensureRemoteCheck() {
		if (!linkedToRemote() && wasSynced()
				&& provideSelfOrAncestorLinkedToRemote() != null
				&& getOwnerDocument().remoteType.hasRemote()
				&& (provideIsText() || provideIsElement())) {
			LocalDom.ensureRemote(this);
			return true;
		} else {
			return false;
		}
	}

	protected boolean isPendingSync() {
		return false;
	}

	protected abstract NodeJso jsoRemote();

	protected abstract boolean linkedToRemote();

	protected abstract <T extends NodeLocal> T local();

	/**
	 * Apply the runnable (to the local mutation list) only if the mutation
	 * tracking state requires it
	 */
	protected final void notify(Runnable runnable) {
		LocalDom.notifyLocalMutations(runnable);
	}

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

	protected abstract void putRemote(ClientDomNode remote, boolean synced);

	protected abstract <T extends ClientDomNode> T remote();

	/*
	 * Populates the remote on node creation or detach (with a null remote)
	 */
	final void resetRemote() {
		resetRemote0();
		syncId = 0;
	}

	protected abstract void resetRemote0();

	/**
	 * Apply the runnable (to the remote dom) only if the node + dom states
	 * require it
	 */
	protected void sync(Runnable runnable) {
		if (remote() instanceof NodeLocalNull || !LocalDom.isApplyToRemote()) {
			return;
		}
		try {
			LocalDom.setSyncing(true);
			runnable.run();
		} finally {
			LocalDom.setSyncing(false);
		}
	}

	/*
	 * For subclasses (tables essentially) to disallow invalid *local* dom,
	 * since that would cause a local/remote mismatch
	 */
	protected void validateInsert(Node newChild) {
	}

	void setAttached(boolean attached) {
		if (attached == this.attached) {
			return;
		}
		this.attached = attached;
		if (attached) {
			onAttach();
		} else {
			// clear all tree domNodes but relink this (top of removed tree)
			// post onDetach
			ClientDomNode remote = remote();
			onDetach();
			implAccess().putRemoteNoSideEffects(remote);
		}
	}

	/**
	 * only call on reparse
	 */
	final void clearSynced() {
		syncId = 0;
	}

	void onSync(int syncId) {
		Preconditions.checkState(this.syncId == 0 || this.syncId == syncId);
		this.syncId = syncId;
	}

	boolean wasSynced() {
		return syncId > 0;
	}

	@Override
	public void setRefId(int id) {
		this.refId = id;
		remote().setRefId(id);
	}

	@Override
	public int getRefId() {
		return this.refId;
	}

	public String toNameRefId() {
		return Ax.format("%s::%s", getNodeName(), getRefId());
	}
}
