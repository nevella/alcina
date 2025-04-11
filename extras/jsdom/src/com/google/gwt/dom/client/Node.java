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

import java.util.List;
import java.util.stream.Stream;

import org.w3c.dom.DOMException;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.UserDataHandler;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JavascriptObjectEquivalent;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.DomNodeType;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.traversal.DepthFirstTraversal;

/**
 * <p>
 * The Node interface is the primary datatype for the entire Document Object
 * Model. It represents a single node in the document tree. While all objects
 * implementing the Node interface expose methods for dealing with children, not
 * all objects implementing the Node interface may have children.
 * 
 * <p>
 * Nodes are manipulated and modelled primarily by their {@link NodeLocal}
 * children, and then synced to the "real" DOM tree modelled by their
 * {@link NodeRemote} children
 * <p>
 * Nodes have two boolean states:
 * </p>
 * <ul>
 * <li>{@link #attached} - attached to the document element. This is stored in a
 * boolean field on {@link Node} and corresponds to browser DOM isConnected()
 * <li>{@link #isPending()} - ({@link Element} only) synced to the remote DOM,
 * but all children are *not* and will be synced at the end of the current event
 * loop cycle via (effectively) {@link Element#setInnerHTML(String)}. This is
 * modelled by the presence of the element in the {@link LocalDom#pendingSync}
 * list (and is the main reason that the local/remote dom model is performant).
 * </ul>
 * <p>
 * For sync, note that {@link Element} has a {@link Style} child which has the
 * same pattern of client-visible model type with a local and remote
 * implementation field
 * <p>
 * Note that append/insert order is quite involved - essentially
 * remove-non-local, insert-local,insert-non-local
 */
@Reflected
public abstract class Node
		implements JavascriptObjectEquivalent, ClientDomNode, org.w3c.dom.Node {
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

	boolean attached;

	/*
	 * Attached nodes will have a non-zero attachId, which is used for tree sync
	 */
	int attachId;

	/**
	 * Use a direct reference (rather than lookup) for GWT dom - this allows (in
	 * particular) tidy location mutation tracking
	 */
	transient DomNode domNode;

	protected Node() {
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
		validateRemoteStatePreTreeMutation(newChild);
		// explicit remove-before-insert is the neatest way to cause sync events
		// to fire
		newChild.removeFromParent();
		T node = local().appendChild(newChild);
		notify(() -> LocalDom.getLocalMutations().notifyChildListMutation(this,
				node, node.getPreviousSibling(), null, true));
		sync(() -> remote().appendChild(newChild));
		return node;
	}

	public DomNode asDomNode() {
		if (domNode == null) {
			domNode = DomNode.fromGwtNode(this);
		}
		return domNode;
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
		String nodeName = getNodeName();
		int idx = nodeName.indexOf(":");
		if (idx != -1) {
			return nodeName.substring(idx + 1);
		} else {
			return nodeName;
		}
	}

	@Override
	public String getNamespaceURI() {
		String nodeName = getNodeName();
		int idx = nodeName.indexOf(":");
		if (idx != -1) {
			return nodeName.substring(0, idx);
		} else {
			return null;
		}
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
			validateRemoteStatePreTreeMutation(newChild);
			validateRemoteStatePreTreeMutation(refChild);
			newChild.removeFromParent();
			Node result = local().insertBefore(newChild, refChild);
			notify(() -> LocalDom.getLocalMutations().notifyChildListMutation(
					this, newChild, newChild.getPreviousSibling(),
					newChild.getNextSibling(), true));
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
	public boolean isAttachId() {
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

	public boolean provideIsElement() {
		return getNodeType() == ELEMENT_NODE;
	}

	public boolean provideIsText() {
		return getNodeType() == TEXT_NODE;
	}

	@Override
	public Node removeAllChildren() {
		getChildNodes().forEach(n -> validateRemoteStatePreTreeMutation(n));
		return ClientDomNodeStatic.removeAllChildren(this);
	}

	@Override
	public Node removeChild(Node oldChild) {
		validateRemoteStatePreTreeMutation(oldChild);
		notify(() -> LocalDom.getLocalMutations().notifyChildListMutation(this,
				oldChild, oldChild.getPreviousSibling(),
				oldChild.getNextSibling(), false));
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

	/**
	 * parent can be null, in which case this is a noop
	 */
	@Override
	public void removeFromParent() {
		if (getParentNode() == null) {
			return;
		}
		sync(() -> remote().removeFromParent());
		notify(() -> LocalDom.getLocalMutations().notifyChildListMutation(this,
				this, getPreviousSibling(), getNextSibling(), false));
		local().removeFromParent();
		resetRemote();
	}

	@Override
	public Node replaceChild(Node newChild, Node oldChild) {
		validateRemoteStatePreTreeMutation(oldChild);
		validateRemoteStatePreTreeMutation(newChild);
		sync(() -> remote().replaceChild(newChild, oldChild));
		notify(() -> LocalDom.getLocalMutations().notifyChildListMutation(this,
				oldChild, oldChild.getPreviousSibling(),
				oldChild.getNextSibling(), false));
		Node result = local().replaceChild(newChild, oldChild);
		notify(() -> LocalDom.getLocalMutations().notifyChildListMutation(this,
				newChild, newChild.getPreviousSibling(),
				newChild.getNextSibling(), true));
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

	public List<Node> getChildren() {
		return new NodeLocal.ChildNodeList(local());
	}

	public Stream<Node> traverse() {
		DepthFirstTraversal<Node> traversal = new DepthFirstTraversal<>(this,
				Node::getChildren);
		return traversal.stream();
	}

	public Stream<Node> streamChildren() {
		return getChildNodes().stream();
	}

	protected void onAttach() {
		getOwnerDocument().localDom.onAttach(this);
		streamChildren().forEach(n -> n.setAttached(true, false));
	}

	protected void onDetach() {
		getOwnerDocument().localDom.onDetach(this);
		// note this will be undone for the top-of-the-detach-tree (see void
		// setAttached(boolean attached) )
		resetRemote();
		streamChildren().forEach(n -> n.setAttached(false, false));
	}

	protected void validateRemoteStatePreTreeMutation(Node incomingChild) {
		if (incomingChild == null) {
			return;
		}
		if (hasRemote()) {
			// if this is pending sync, the children should remain local-only
			if (!isPendingSync()) {
				LocalDom.ensureRemoteNodeMaybePendingSync(incomingChild);
			}
		}
	}

	public boolean isElement() {
		return false;
	}

	protected boolean isPendingSync() {
		return false;
	}

	public abstract NodeJso jsoRemote();

	protected final boolean hasRemote() {
		return remote() != null;
	}

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
		if (hasRemote()) {
			return this;
		}
		if (getParentElement() != null) {
			return getParentElement().provideSelfOrAncestorLinkedToRemote();
		}
		return null;
	}

	protected abstract void putRemote(ClientDomNode remote);

	protected abstract <T extends ClientDomNode> T remote();

	/*
	 * Populates the remote on node creation or detach (with a null remote)
	 */
	final void resetRemote() {
		resetRemote0();
	}

	protected abstract void resetRemote0();

	/**
	 * Apply the runnable (to the remote dom) only if the node + dom states
	 * require it
	 */
	protected void sync(Runnable runnable) {
		if (remote() == null || !LocalDom.isApplyToRemote()) {
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

	/**
	 * 
	 * @param attached
	 * @param attachRoot
	 *            is this the root of the attach/detach op? (if so, remote will
	 *            be restored )
	 */
	void setAttached(boolean attached, boolean attachRoot) {
		if (attached == this.attached) {
			return;
		}
		this.attached = attached;
		if (attached) {
			onAttach();
		} else {
			// clear all tree domNodes but relink this (top of removed tree)
			// post onDetach so that calling code can remove from remote dom
			ClientDomNode remote = remote();
			onDetach();
			if (attachRoot) {
				if (this instanceof Text && remote != null) {
					int dbg4 = 4;
				}
				putRemote(remote);
			}
		}
	}

	@Override
	public void setAttachId(int id) {
		this.attachId = id;
		// perf - remote dom ids need not be zeroed, the source of truth is the
		// id's presence in DomIds
		if (id != 0) {
			sync(() -> remote().setAttachId(id));
		}
	}

	@Override
	public int getAttachId() {
		return this.attachId;
	}

	public String toNameAttachId() {
		return Ax.format("%s::%s", getNodeName(), getAttachId());
	}
}
