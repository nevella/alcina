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

import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JavascriptObjectEquivalent;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.LocalDomDebug;
import com.google.gwt.user.client.ui.UIObject;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.gwt.client.util.TextUtils;

/**
 * All HTML element interfaces derive from this class.
 */
public class Element extends Node implements DomElement {
	/**
	 * Constant returned from {@link #getDraggable()}.
	 */
	public static final String DRAGGABLE_AUTO = "auto";

	/**
	 * Constant returned from {@link #getDraggable()}.
	 */
	public static final String DRAGGABLE_FALSE = "false";

	/**
	 * Constant returned from {@link #getDraggable()}.
	 */
	public static final String DRAGGABLE_TRUE = "true";

	/**
	 * Assert that the given {@link Node} is an {@link Element} and
	 * automatically typecast it.
	 */
	public static Element as(JavascriptObjectEquivalent o) {
		assert is(o);
		if (o instanceof EventTarget) {
			return ((EventTarget) o).cast();
		} else if (o instanceof JavaScriptObject) {
			JavaScriptObject jso = (JavaScriptObject) o;
			return LocalDomBridge.nodeFor(jso);
		} else {
			return (Element) o;
		}
	}

	/**
	 * Assert that the given {@link Node} is an {@link Element} and
	 * automatically typecast it.
	 */
	public static Element as(Node node) {
		assert is(node);
		return (Element) node;
	}

	/**
	 * Determines whether the given {@link JavaScriptObject} can be cast to an
	 * {@link Element}. A <code>null</code> object will cause this method to
	 * return <code>false</code>.
	 */
	public static boolean is(JavascriptObjectEquivalent o) {
		if (o instanceof EventTarget) {
			if (((EventTarget) o).is(Element.class)) {
				return true;
			} else {
				return false;
			}
		} else if (o instanceof JavaScriptObject) {
			JavaScriptObject jso = (JavaScriptObject) o;
			return ElementRemote.is(jso);
		} else {
			return o instanceof Element;
		}
	}

	/**
	 * Determine whether the given {@link Node} can be cast to an
	 * {@link Element}. A <code>null</code> node will cause this method to
	 * return <code>false</code>.
	 */
	public static boolean is(Node node) {
		return (node != null) && (node.getNodeType() == Node.ELEMENT_NODE);
	}

	private DomElement impl;

	DomElement localImpl;

	ElementRemote domImpl;

	public UIObject uiObject;

	public EventListener uiObjectListener;

	private StringMap cachedAttributes;

	protected Element() {
	}

	public boolean addClassName(String className) {
		return impl().addClassName(className);
	}

	public <T extends Node> T appendChild(T newChild) {
		if (domImpl() == null && !LocalDomBridge.get().wasCreatedThisLoop(this)
				&& provideAncestorElementAttachedToDom() != null) {
			// LocalDomBridge.ensureJso(this, false);
			LocalDomBridge.ensureJso(this);
		}
		return impl().appendChild(newChild);
	}

	public void blur() {
		impl().blur();
	}

	public <T extends JavascriptObjectEquivalent> T cast() {
		return (T) this;
	}

	public Node cloneNode(boolean deep) {
		return impl().cloneNode(deep);
	}

	public void dispatchEvent(NativeEvent evt) {
		impl().dispatchEvent(evt);
	}

	public void dumpLocal() {
		dumpLocal0(0);
	}

	@Override
	public Element elementFor() {
		return nodeFor();
	}

	public Element ensureDomImpl() {
		LocalDomBridge.ensureJso(this);
		if (!(impl instanceof JavaScriptObject)) {
			if (impl == null) {
				int debug = 3;
			}
			putImpl(domImpl);
		}
		return this;
	}

	public void ensureId() {
		if (domImpl == null) {
			impl().ensureId();
		}
	}

	public ElementRemote ensureJso() {
		return ensureDomImpl().domImpl;
	}

	public ElementRemote ensureJsoNoFlush() {
		if (domImpl != null) {
			return domImpl;
		}
		return ensureJso();
	}

	public void focus() {
		LocalDomBridge.ensureJso(this);
		domImpl.focus();
	}

	public int getAbsoluteBottom() {
		return impl().getAbsoluteBottom();
	}

	public int getAbsoluteLeft() {
		return impl().getAbsoluteLeft();
	}

	public int getAbsoluteRight() {
		return impl().getAbsoluteRight();
	}

	public int getAbsoluteTop() {
		return impl().getAbsoluteTop();
	}

	private StringMap ensureCachedAttributes() {
		if (cachedAttributes == null) {
			cachedAttributes = new StringMap();
		}
		return cachedAttributes;
	}

	public String getAttribute(String name) {
		if (provideIsCacheableAttributeName(name)) {
			StringMap cachedAttributes = ensureCachedAttributes();
			if (!cachedAttributes.containsKey(name)) {
				cachedAttributes.put(name, impl().getAttribute(name));
			}
			return cachedAttributes.get(name);
		} else {
			return impl().getAttribute(name);
		}
	}

	private boolean provideIsCacheableAttributeName(String name) {
		if (LocalDomBridge.isScript) {
			return false;
		}
		if (name.startsWith("__gwtCellBasedWidgetImplDispatching")) {
			return true;
		}
		return false;
	}

	@Override
	public Map<String, String> getAttributes() {
		return impl().getAttributes();
	}

	public Node getChild(int index) {
		return impl().getChild(index);
	}

	public int getChildCount() {
		return impl().getChildCount();
	}

	public NodeList<Node> getChildNodes() {
		return impl().getChildNodes();
	}

	public String getClassName() {
		return impl().getClassName();
	}

	public int getClientHeight() {
		return impl().getClientHeight();
	}

	public int getClientWidth() {
		return impl().getClientWidth();
	}

	public String getDir() {
		return impl().getDir();
	}

	public String getDraggable() {
		return impl().getDraggable();
	}

	public NodeList<Element> getElementsByTagName(String name) {
		return impl().getElementsByTagName(name);
	}

	public Node getFirstChild() {
		return impl().getFirstChild();
	}

	public Element getFirstChildElement() {
		return typedImpl(true).getFirstChildElement();
	}

	public String getId() {
		return impl().getId();
	}

	public String getInnerHTML() {
		return impl().getInnerHTML();
	}

	public String getInnerText() {
		return impl().getInnerText();
	}

	public String getLang() {
		return impl().getLang();
	}

	public Node getLastChild() {
		return impl().getLastChild();
	}

	public Node getNextSibling() {
		return impl().getNextSibling();
	}

	public Element getNextSiblingElement() {
		return impl().getNextSiblingElement();
	}

	public String getNodeName() {
		return impl().getNodeName();
	}

	public short getNodeType() {
		return implNoResolve().getNodeType();
	}

	public String getNodeValue() {
		return impl().getNodeValue();
	}

	public int getOffsetHeight() {
		return ensureJso().getOffsetHeight();
	}

	public int getOffsetLeft() {
		return ensureJso().getOffsetLeft();
	}

	public Element getOffsetParent() {
		return ensureJso().getOffsetParent();
	}

	public int getOffsetTop() {
		return ensureJso().getOffsetTop();
	}

	public int getOffsetWidth() {
		return ensureJso().getOffsetWidth();
	}

	public Document getOwnerDocument() {
		return impl().getOwnerDocument();
	}

	public Element getParentElement() {
		// no resolution
		if (impl == null) {
			// FIXME - when does this happen (suggest box clear is one place)
			return null;
			// int debug = 3;
		}
		Element parent = impl.getParentElement();
		return parent;
	}

	public Node getParentNode() {
		return impl().getParentNode();
	}

	public Node getPreviousSibling() {
		return impl().getPreviousSibling();
	}

	public Element getPreviousSiblingElement() {
		return impl().getPreviousSiblingElement();
	}

	public boolean getPropertyBoolean(String name) {
		return impl().getPropertyBoolean(name);
	}

	public double getPropertyDouble(String name) {
		return impl().getPropertyDouble(name);
	}

	public int getPropertyInt(String name) {
		return impl().getPropertyInt(name);
	}

	public JavaScriptObject getPropertyJSO(String name) {
		return impl().getPropertyJSO(name);
	}

	public Object getPropertyObject(String name) {
		return impl().getPropertyObject(name);
	}

	public String getPropertyString(String name) {
		return impl().getPropertyString(name);
	}

	public int getScrollHeight() {
		return impl().getScrollHeight();
	}

	public int getScrollLeft() {
		return impl().getScrollLeft();
	}

	public int getScrollTop() {
		return impl().getScrollTop();
	}

	public int getScrollWidth() {
		return impl().getScrollWidth();
	}

	public String getString() {
		return impl().getString();
	}

	public Style getStyle() {
		return impl().getStyle();
	}

	public int getTabIndex() {
		return impl().getTabIndex();
	}

	public String getTagName() {
		return implNoResolve().getTagName();
	}

	public String getTitle() {
		return impl().getTitle();
	}

	public ElementRemote getdomImpl() {
		return domImpl;
	}

	public boolean hasAttribute(String name) {
		return impl().hasAttribute(name);
	}

	public boolean hasChildNodes() {
		return impl().hasChildNodes();
	}

	public boolean hasClassName(String className) {
		return impl().hasClassName(className);
	}

	public boolean hasParentElement() {
		return impl().hasParentElement();
	}

	public boolean hasTagName(String tagName) {
		return impl().hasTagName(tagName);
	}

	@Override
	public Integer indexInParentChildren() {
		return impl().indexInParentChildren();
	}

	public Node insertAfter(Node newChild, Node refChild) {
		return impl().insertAfter(newChild, refChild);
	}

	public Node insertBefore(Node newChild, Node refChild) {
		return impl().insertBefore(newChild, refChild);
	}

	public Node insertFirst(Node child) {
		return impl().insertFirst(child);
	}

	public boolean isOrHasChild(Node child) {
		return impl().isOrHasChild(child);
	}

	public int localEventBitsSunk() {
		return ((ElementLocal) impl).eventBits;
	}

	public DomElement localImpl() {
		if (localImpl != null) {
			return localImpl;
		} else {
			if (domImpl != impl) {
				return impl;
			} else {
				return null;
			}
		}
	}

	public Element nodeFor() {
		return impl().elementFor();
	}

	public Element provideAncestorElementAttachedToDom() {
		if (domImpl() != null) {
			return this;
		}
		if (getParentElement() != null) {
			return getParentElement().provideAncestorElementAttachedToDom();
		}
		return null;
	}

	public LocalDomElement provideLocalDomElement() {
		Preconditions.checkState(provideIsLocal());
		return (LocalDomElement) impl();
	}

	@Override
	public void putDomImpl(NodeRemote nodeDom) {
		domImpl = (ElementRemote) nodeDom;
		if (nodeDom != null) {
			if (domImpl.getId().length() > 0) {
				String localId = localImpl() != null ? localImpl().getId() : "";
				String message = Ax.format("[id:%s,%s]->jso:%s",
						domImpl.getId(), localId, nodeDom.hashCode());
				LocalDomBridge.log(LocalDomDebug.PUT_DOM_IMPL, message);
			}
			if (impl() instanceof LocalDomElement) {
				LocalDomElement localDomElement = (LocalDomElement) impl();
				if (localDomElement != null
						&& localDomElement.getEventBits() != 0) {
					int existingBits = DOM.getEventsSunk(this);
					DOM.sinkEvents(this,
							existingBits | localDomElement.getEventBits());
				}
			}
			LocalDomBridge.debug.checkMultipleAssignment(this, nodeDom);
		}
	}

	@Override
	public void putImpl(DomNode impl) {
		if (impl == this.impl) {
			return;
		}
		LocalDomBridge.get().checkInPreconditionList(this, impl);
		if (this.impl != null) {
			if (this.impl instanceof JavaScriptObject) {
				if (impl instanceof LocalDomNode) {
				} else {
					if (this.impl.getNodeType() == Node.ELEMENT_NODE) {
						// FIXME - pushbutton requires this (rather than a fail)
						Preconditions.checkState(((ElementRemote) this.impl)
								.getInnerHTML0()
								.equals(((ElementRemote) impl).getInnerHTML0()));
					}
				}
				// orphan - to handle direct html writing of UiBinder
				LocalDomBridge.get().javascriptObjectNodeLookup.remove(domImpl);
				domImpl = null;
			} else {
				Preconditions
						.checkState(!(this.impl instanceof JavaScriptObject));
				localImpl = (DomElement) this.impl;
			}
		}
		Preconditions.checkState(impl != null);
		this.impl = (DomElement) impl;
	}

	@Override
	public Node removeAllChildren() {
		if (domImpl() == null && !LocalDomBridge.get().wasCreatedThisLoop(this)
				&& provideAncestorElementAttachedToDom() != null) {
			ensureJso();
		}
		if (provideIsDom() && LocalDomBridge.fastRemoveAll) {
			setInnerHTML("");
			removeLocalImpl();
			return null;
		} else {
			return super.removeAllChildren();
		}
	}

	public void removeAttribute(String name) {
		impl().removeAttribute(name);
	}

	public Node removeChild(Node oldChild) {
		return impl().removeChild(oldChild);
	}

	public boolean removeClassName(String className) {
		return impl().removeClassName(className);
	}

	public void removeFromParent() {
		impl().removeFromParent();
	}

	public Node replaceChild(Node newChild, Node oldChild) {
		return impl().replaceChild(newChild, oldChild);
	}

	public void replaceClassName(String oldClassName, String newClassName) {
		impl().replaceClassName(oldClassName, newClassName);
	}

	public void resolveIfAppropriate() {
		resolveIfAppropriate(false);
	}

	public void scrollIntoView() {
		impl().scrollIntoView();
	}

	public void setAttribute(String name, String value) {
		impl().setAttribute(name, value);
		if (provideIsCacheableAttributeName(name)) {
			ensureCachedAttributes().put(name, value);
		}
	}

	public void setClassName(String className) {
		impl().setClassName(className);
	}

	public void setDir(String dir) {
		impl().setDir(dir);
	}

	public void setDraggable(String draggable) {
		impl().setDraggable(draggable);
	}

	public void setId(String id) {
		impl().setId(id);
	}

	public void setInnerHTML(String html) {
		impl().setInnerHTML(html);
	}

	public void setInnerSafeHtml(SafeHtml html) {
		impl().setInnerSafeHtml(html);
	}

	public void setInnerText(String text) {
		impl().setInnerText(text);
	}

	public void setLang(String lang) {
		impl().setLang(lang);
	}

	public void setNodeValue(String nodeValue) {
		impl().setNodeValue(nodeValue);
	}

	public void setOuterHtml(String html) {
		Preconditions.checkState(provideIsLocal());
		provideLocalDomElement().setOuterHtml(html);
	}

	public void setPropertyBoolean(String name, boolean value) {
		impl().setPropertyBoolean(name, value);
	}

	public void setPropertyDouble(String name, double value) {
		impl().setPropertyDouble(name, value);
	}

	public void setPropertyInt(String name, int value) {
		impl().setPropertyInt(name, value);
	}

	public void setPropertyJSO(String name, JavaScriptObject value) {
		impl().setPropertyJSO(name, value);
	}

	public void setPropertyObject(String name, Object value) {
		impl().setPropertyObject(name, value);
	}

	public void setPropertyString(String name, String value) {
		impl().setPropertyString(name, value);
	}

	public void setScrollLeft(int scrollLeft) {
		impl().setScrollLeft(scrollLeft);
	}

	public void setScrollTop(int scrollTop) {
		impl().setScrollTop(scrollTop);
	}

	public void setTabIndex(int tabIndex) {
		impl().setTabIndex(tabIndex);
	}

	public void setTitle(String title) {
		impl().setTitle(title);
	}

	public void sinkEvents(int eventBits) {
		impl().sinkEvents(eventBits);
	}

	public void toggleClassName(String className) {
		impl().toggleClassName(className);
	}

	@Override
	public String toString() {
		return impl() == null ? super.toString()
				: impl().toString() + (uiObject == null ? ""
						: ": " + uiObject.getClass().getSimpleName());
	}

	private void dumpLocal0(int depth) {
		String indent = CommonUtils.padStringLeft("", depth * 2, ' ');
		String message = Ax.format("%s%s [%s,%s,%s]: ", indent, getTagName(),
				hashCode(), impl().hashCode(), domImpl() == null ? "f" : "t");
		LocalDomBridge.log(LocalDomDebug.DUMP_LOCAL, message);
		for (Node node : getChildNodes()) {
			switch (node.getNodeType()) {
			case Node.TEXT_NODE:
				message = indent + CommonUtils.trimToWsChars(
						TextUtils.normalise(node.getNodeValue()), 50, true);
				LocalDomBridge.log(LocalDomDebug.DUMP_LOCAL, message);
				break;
			case ELEMENT_NODE:
				((Element) node).dumpLocal0(depth + 1);
				break;
			}
		}
	}

	private boolean isAttached() {
		Element cursor = this;
		while (cursor != null) {
			if (cursor.domImpl() != null) {
				return true;
			}
			cursor = cursor.getParentElement();
		}
		return false;
	}

	private void removeLocalImpl() {
		localImpl = null;
	}

	private void resolveIfAppropriate(boolean flushIfInnerHtml) {
		if (domImpl() == null && !LocalDomBridge.get().wasCreatedThisLoop(this)
				&& isAttached()) {
			LocalDomBridge.ensureJso(this);
		}
		if (domImpl() == null && flushIfInnerHtml
				&& LocalDomBridge.shouldUseDomNodes() && !isAttached()) {
			LocalDomBridge.replaceWithJso(this);
		}
	}

	private DomElement typedImpl(boolean flushIfInnerHtml) {
		resolveIfAppropriate(flushIfInnerHtml);
		return impl;
	}

	@Override
	NodeRemote domImpl() {
		return domImpl;
	}

	@Override
	DomElement impl() {
		return typedImpl(false);
	}

	@Override
	DomElement implNoResolve() {
		return impl;
	}

	public int getChildIndexLocal(Element child) {
		if (child.getParentElement() != this) {
			return -1;
		}
		return child.indexInParentChildren();
	}
}