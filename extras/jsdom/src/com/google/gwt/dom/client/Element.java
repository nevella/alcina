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
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
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
			return Element_Jso.is(jso);
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

	private DomElement typedImpl;

	DomElement localImpl;

	Element_Jso typedDomImpl;

	public UIObject uiObject;

	public EventListener uiObjectListener;

	protected Element() {
	}

	public boolean addClassName(String className) {
		return typedImpl().addClassName(className);
	}

	public <T extends Node> T appendChild(T newChild) {
		if (domImpl == null && !LocalDomBridge.get().wasCreatedThisLoop(this)
				&& provideAncestorElementAttachedToDom() != null) {
			System.out.println("ensure, maybe falsey?");
//			LocalDomBridge.ensureJso(this, false);
			LocalDomBridge.ensureJso(this);
		}
		return typedImpl().appendChild(newChild);
	}

	public void blur() {
		typedImpl().blur();
	}

	public <T extends JavascriptObjectEquivalent> T cast() {
		return (T) this;
	}

	public Node cloneNode(boolean deep) {
		return typedImpl().cloneNode(deep);
	}

	public void dispatchEvent(NativeEvent evt) {
		typedImpl().dispatchEvent(evt);
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
		if (!(typedImpl instanceof JavaScriptObject)) {
			if (typedImpl == null) {
				int debug = 3;
			}
			putImpl(typedDomImpl);
		}
		return this;
	}

	public void ensureId() {
		if (typedDomImpl == null) {
			typedImpl().ensureId();
		}
	}

	public Element_Jso ensureJso() {
		return ensureDomImpl().typedDomImpl;
	}

	public void focus() {
		LocalDomBridge.ensureJso(this);
		typedDomImpl.focus();
	}

	public int getAbsoluteBottom() {
		return typedImpl().getAbsoluteBottom();
	}

	public int getAbsoluteLeft() {
		return typedImpl().getAbsoluteLeft();
	}

	public int getAbsoluteRight() {
		return typedImpl().getAbsoluteRight();
	}

	public int getAbsoluteTop() {
		return typedImpl().getAbsoluteTop();
	}

	public String getAttribute(String name) {
		return typedImpl().getAttribute(name);
	}

	@Override
	public Map<String, String> getAttributes() {
		return typedImpl().getAttributes();
	}

	public Node getChild(int index) {
		return typedImpl().getChild(index);
	}

	public int getChildCount() {
		return typedImpl().getChildCount();
	}

	public NodeList<Node> getChildNodes() {
		return typedImpl().getChildNodes();
	}

	public String getClassName() {
		return typedImpl().getClassName();
	}

	public int getClientHeight() {
		return typedImpl().getClientHeight();
	}

	public int getClientWidth() {
		return typedImpl().getClientWidth();
	}

	public String getDir() {
		return typedImpl().getDir();
	}

	public String getDraggable() {
		return typedImpl().getDraggable();
	}

	public NodeList<Element> getElementsByTagName(String name) {
		return typedImpl().getElementsByTagName(name);
	}

	public Node getFirstChild() {
		return typedImpl().getFirstChild();
	}

	public Element getFirstChildElement() {
		return typedImpl(true).getFirstChildElement();
	}

	public String getId() {
		return typedImpl().getId();
	}

	public String getInnerHTML() {
		return typedImpl().getInnerHTML();
	}

	public String getInnerText() {
		return typedImpl().getInnerText();
	}

	public String getLang() {
		return typedImpl().getLang();
	}

	public Node getLastChild() {
		return typedImpl().getLastChild();
	}

	public Node getNextSibling() {
		return typedImpl().getNextSibling();
	}

	public Element getNextSiblingElement() {
		return typedImpl().getNextSiblingElement();
	}

	public String getNodeName() {
		return typedImpl().getNodeName();
	}

	public short getNodeType() {
		return typedImpl().getNodeType();
	}

	public String getNodeValue() {
		return typedImpl().getNodeValue();
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
		return typedImpl().getOwnerDocument();
	}

	public Element getParentElement() {
		// no resolution
		if (typedImpl == null) {
			int debug = 3;
		}
		return typedImpl.getParentElement();
	}

	public Node getParentNode() {
		return typedImpl().getParentNode();
	}

	public Node getPreviousSibling() {
		return typedImpl().getPreviousSibling();
	}

	public Element getPreviousSiblingElement() {
		return typedImpl().getPreviousSiblingElement();
	}

	public boolean getPropertyBoolean(String name) {
		return typedImpl().getPropertyBoolean(name);
	}

	public double getPropertyDouble(String name) {
		return typedImpl().getPropertyDouble(name);
	}

	public int getPropertyInt(String name) {
		return typedImpl().getPropertyInt(name);
	}

	public JavaScriptObject getPropertyJSO(String name) {
		return typedImpl().getPropertyJSO(name);
	}

	public Object getPropertyObject(String name) {
		return typedImpl().getPropertyObject(name);
	}

	public String getPropertyString(String name) {
		return typedImpl().getPropertyString(name);
	}

	public int getScrollHeight() {
		return typedImpl().getScrollHeight();
	}

	public int getScrollLeft() {
		return typedImpl().getScrollLeft();
	}

	public int getScrollTop() {
		return typedImpl().getScrollTop();
	}

	public int getScrollWidth() {
		return typedImpl().getScrollWidth();
	}

	public String getString() {
		return typedImpl().getString();
	}

	public Style getStyle() {
		return typedImpl().getStyle();
	}

	public int getTabIndex() {
		return typedImpl().getTabIndex();
	}

	public String getTagName() {
		return typedImpl().getTagName();
	}

	public String getTitle() {
		return typedImpl().getTitle();
	}

	public Element_Jso getTypedDomImpl() {
		return typedDomImpl;
	}

	public boolean hasAttribute(String name) {
		return typedImpl().hasAttribute(name);
	}

	public boolean hasChildNodes() {
		return typedImpl().hasChildNodes();
	}

	public boolean hasClassName(String className) {
		return typedImpl().hasClassName(className);
	}

	public boolean hasParentElement() {
		return typedImpl().hasParentElement();
	}

	public boolean hasTagName(String tagName) {
		return typedImpl().hasTagName(tagName);
	}

	@Override
	public Integer indexInParentChildren() {
		return typedImpl().indexInParentChildren();
	}

	public Node insertAfter(Node newChild, Node refChild) {
		return typedImpl().insertAfter(newChild, refChild);
	}

	public Node insertBefore(Node newChild, Node refChild) {
		return typedImpl().insertBefore(newChild, refChild);
	}

	public Node insertFirst(Node child) {
		return typedImpl().insertFirst(child);
	}

	public boolean isOrHasChild(Node child) {
		return typedImpl().isOrHasChild(child);
	}

	public int localEventBitsSunk() {
		return ((Element_Jvm) impl).eventBits;
	}

	public DomElement localImpl() {
		if (localImpl != null) {
			return localImpl;
		} else {
			return typedImpl;
		}
	}

	public Element nodeFor() {
		return typedImpl().elementFor();
	}

	public Element provideAncestorElementAttachedToDom() {
		if (domImpl != null) {
			return this;
		}
		if (getParentElement() != null) {
			return getParentElement().provideAncestorElementAttachedToDom();
		}
		return null;
	}

	public LocalDomElement provideLocalDomElement() {
		Preconditions.checkState(provideIsLocal());
		return (LocalDomElement) impl;
	}

	@Override
	public void putDomImpl(Node_Jso nodeDom) {
		local = false;
		typedDomImpl = (Element_Jso) nodeDom;
		domImpl = nodeDom;
		if (impl instanceof LocalDomElement) {
			LocalDomElement localDomElement = (LocalDomElement) impl;
			if (localDomElement != null
					&& localDomElement.getEventBits() != 0) {
				DOM.sinkEvents(this, localDomElement.getEventBits());
			}
		}
		LocalDomBridge.debug.checkMultipleAssignment(this, nodeDom);
	}

	@Override
	public void putImpl(DomNode impl) {
		LocalDomBridge.get().checkInPreconditionList(this, impl);
		// debug, can remove
		if (impl == null) {
			int debug = 3;
		}
		if (impl instanceof JavaScriptObject && typedImpl == null) {
			if (impl.getNodeName().equalsIgnoreCase("tbody")) {
				int debug = 3;
			}
		}
		if (typedImpl != null) {
			if (typedImpl instanceof JavaScriptObject) {
				Preconditions.checkState(impl instanceof LocalDomNode);
				// orphan - to handle direct html writing of UiBinder
				LocalDomBridge.get().javascriptObjectNodeLookup
						.remove(typedDomImpl);
				domImpl = null;
				typedDomImpl = null;
			} else {
				localImpl = typedImpl;
			}
		}
		typedImpl = (DomElement) impl;
		this.impl = impl;
	}

	public void removeAttribute(String name) {
		typedImpl().removeAttribute(name);
	}

	public Node removeChild(Node oldChild) {
		return typedImpl().removeChild(oldChild);
	}

	public boolean removeClassName(String className) {
		return typedImpl().removeClassName(className);
	}

	public void removeFromParent() {
		typedImpl().removeFromParent();
	}

	public Node replaceChild(Node newChild, Node oldChild) {
		return typedImpl().replaceChild(newChild, oldChild);
	}

	public void replaceClassName(String oldClassName, String newClassName) {
		typedImpl().replaceClassName(oldClassName, newClassName);
	}

	public void scrollIntoView() {
		typedImpl().scrollIntoView();
	}

	public void setAttribute(String name, String value) {
		typedImpl().setAttribute(name, value);
	}

	public void setClassName(String className) {
		typedImpl().setClassName(className);
	}

	public void setDir(String dir) {
		typedImpl().setDir(dir);
	}

	public void setDraggable(String draggable) {
		typedImpl().setDraggable(draggable);
	}

	public void setId(String id) {
		typedImpl().setId(id);
	}

	public void setInnerHTML(String html) {
		typedImpl().setInnerHTML(html);
	}

	public void setInnerSafeHtml(SafeHtml html) {
		typedImpl().setInnerSafeHtml(html);
	}

	public void setInnerText(String text) {
		typedImpl().setInnerText(text);
	}

	public void setLang(String lang) {
		typedImpl().setLang(lang);
	}

	public void setNodeValue(String nodeValue) {
		typedImpl().setNodeValue(nodeValue);
	}

	public void setPropertyBoolean(String name, boolean value) {
		typedImpl().setPropertyBoolean(name, value);
	}

	public void setPropertyDouble(String name, double value) {
		typedImpl().setPropertyDouble(name, value);
	}

	public void setPropertyInt(String name, int value) {
		typedImpl().setPropertyInt(name, value);
	}

	public void setPropertyJSO(String name, JavaScriptObject value) {
		typedImpl().setPropertyJSO(name, value);
	}

	public void setPropertyObject(String name, Object value) {
		typedImpl().setPropertyObject(name, value);
	}

	public void setPropertyString(String name, String value) {
		typedImpl().setPropertyString(name, value);
	}

	public void setScrollLeft(int scrollLeft) {
		typedImpl().setScrollLeft(scrollLeft);
	}

	public void setScrollTop(int scrollTop) {
		typedImpl().setScrollTop(scrollTop);
	}

	public void setTabIndex(int tabIndex) {
		typedImpl().setTabIndex(tabIndex);
	}

	public void setTitle(String title) {
		typedImpl().setTitle(title);
	}

	public void sinkEvents(int eventBits) {
		typedImpl().sinkEvents(eventBits);
	}

	public void toggleClassName(String className) {
		typedImpl().toggleClassName(className);
	}

	@Override
	public String toString() {
		return impl == null ? super.toString() : impl.toString();
	}

	private void dumpLocal0(int depth) {
		String indent = CommonUtils.padStringLeft("", depth * 2, ' ');
		System.out.println(Ax.format("%s%s [%s,%s,%s]: ", indent, getTagName(),
				hashCode(), impl.hashCode(), domImpl == null ? "f" : "t"));
		for (Node node : getChildNodes()) {
			switch (node.getNodeType()) {
			case Node.TEXT_NODE:
				System.out.println(indent + CommonUtils.trimToWsChars(
						TextUtils.normalise(node.getNodeValue()), 50, true));
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
			if (cursor.domImpl != null) {
				return true;
			}
			cursor = cursor.getParentElement();
		}
		return false;
	}

	DomElement typedImpl() {
		return typedImpl(false);
	}

	DomElement typedImpl(boolean flushIfInnerHtml) {
		if (domImpl == null && LocalDomBridge.shouldUseDomNodes()
				&& isAttached()) {
			LocalDomBridge.ensureJso(this);
		}
		if (domImpl == null && flushIfInnerHtml
				&& LocalDomBridge.shouldUseDomNodes() && !isAttached()) {
			LocalDomBridge.replaceWithJso(this);
		}
		return typedImpl;
	}

	public void setOuterHtml(String html) {
		Preconditions.checkState(provideIsLocal());
		provideLocalDomElement().setOuterHtml(html);
	}

	public Element_Jso ensureJsoNoFlush() {
		if (typedDomImpl != null) {
			return typedDomImpl;
		}
		return ensureJso();
	}

	@Override
	public Node removeAllChildren() {
		if (domImpl == null && !LocalDomBridge.get().wasCreatedThisLoop(this)
				&& provideAncestorElementAttachedToDom() != null) {
			ensureJso();
		}
		if (provideIsDom()) {
			setInnerHTML("");
			removeLocalImpl();
			return null;
		} else {
			return super.removeAllChildren();
		}
	}

	private void removeLocalImpl() {
		localImpl = null;
	}
}