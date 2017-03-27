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

import com.google.gwt.core.client.JavascriptObjectEquivalent;

import java.util.Map;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.annotations.IsSafeHtml;
import com.google.gwt.user.client.EventListener;

/**
 * All HTML element interfaces derive from this class.
 */
public class Element extends Node implements DomElement {
	DomElement typedImpl;

	Element_Jso typedDomImpl;

	public EventListener uiObjectListener;

	/**
	 * Constant returned from {@link #getDraggable()}.
	 */
	public static final String DRAGGABLE_AUTO = "auto";

	/**
	 * Constant returned from {@link #getDraggable()}.
	 */
	public static final String DRAGGABLE_FALSE = "false";

	public <T extends JavascriptObjectEquivalent> T cast() {
		return (T) this;
	}

	/**
	 * Constant returned from {@link #getDraggable()}.
	 */
	public static final String DRAGGABLE_TRUE = "true";

	public <T extends Node> T appendChild(T newChild) {
		return typedImpl.appendChild(newChild);
	}

	public Node cloneNode(boolean deep) {
		return typedImpl.cloneNode(deep);
	}

	public boolean addClassName(String className) {
		return typedImpl.addClassName(className);
	}

	public Node getChild(int index) {
		return typedImpl.getChild(index);
	}

	public int getChildCount() {
		return typedImpl.getChildCount();
	}

	public NodeList<Node> getChildNodes() {
		return typedImpl.getChildNodes();
	}

	public Node getFirstChild() {
		return typedImpl.getFirstChild();
	}

	public Node getLastChild() {
		return typedImpl.getLastChild();
	}

	public Node getNextSibling() {
		return typedImpl.getNextSibling();
	}

	public String getNodeName() {
		return typedImpl.getNodeName();
	}

	public short getNodeType() {
		return typedImpl.getNodeType();
	}

	public String getNodeValue() {
		return typedImpl.getNodeValue();
	}

	public Element getParentElement() {
		return typedImpl.getParentElement();
	}

	public void blur() {
		typedImpl.blur();
	}

	public void dispatchEvent(NativeEvent evt) {
		typedImpl.dispatchEvent(evt);
	}

	public void focus() {
		typedImpl.focus();
	}

	public Document getOwnerDocument() {
		return typedImpl.getOwnerDocument();
	}

	public Node getParentNode() {
		return typedImpl.getParentNode();
	}

	public int getAbsoluteBottom() {
		return typedImpl.getAbsoluteBottom();
	}

	public Node getPreviousSibling() {
		return typedImpl.getPreviousSibling();
	}

	public boolean hasChildNodes() {
		return typedImpl.hasChildNodes();
	}

	public boolean hasParentElement() {
		return typedImpl.hasParentElement();
	}

	public int getAbsoluteLeft() {
		return typedImpl.getAbsoluteLeft();
	}

	public int getAbsoluteRight() {
		return typedImpl.getAbsoluteRight();
	}

	public int getAbsoluteTop() {
		return typedImpl.getAbsoluteTop();
	}

	public Node insertAfter(Node newChild, Node refChild) {
		return typedImpl.insertAfter(newChild, refChild);
	}

	public String getAttribute(String name) {
		return typedImpl.getAttribute(name);
	}

	public String getClassName() {
		return typedImpl.getClassName();
	}

	public int getClientHeight() {
		return typedImpl.getClientHeight();
	}

	public int getClientWidth() {
		return typedImpl.getClientWidth();
	}

	public String getDir() {
		return typedImpl.getDir();
	}

	public String getDraggable() {
		return typedImpl.getDraggable();
	}

	public NodeList<Element> getElementsByTagName(String name) {
		return typedImpl.getElementsByTagName(name);
	}

	public Element getFirstChildElement() {
		return typedImpl.getFirstChildElement();
	}

	public Element nodeFor() {
		return typedImpl.elementFor();
	}

	public Node insertBefore(Node newChild, Node refChild) {
		return typedImpl.insertBefore(newChild, refChild);
	}

	public String getId() {
		return typedImpl.getId();
	}

	public String getInnerHTML() {
		return typedImpl.getInnerHTML();
	}

	public Node insertFirst(Node child) {
		return typedImpl.insertFirst(child);
	}

	public String getInnerText() {
		return typedImpl.getInnerText();
	}

	public String getLang() {
		return typedImpl.getLang();
	}

	public boolean isOrHasChild(Node child) {
		return typedImpl.isOrHasChild(child);
	}

	public void removeFromParent() {
		typedImpl.removeFromParent();
	}

	public Element getNextSiblingElement() {
		return typedImpl.getNextSiblingElement();
	}

	public Node replaceChild(Node newChild, Node oldChild) {
		return typedImpl.replaceChild(newChild, oldChild);
	}

	public int getOffsetHeight() {
		return typedImpl.getOffsetHeight();
	}

	public void setNodeValue(String nodeValue) {
		typedImpl.setNodeValue(nodeValue);
	}

	public Node removeChild(Node oldChild) {
		return typedImpl.removeChild(oldChild);
	}

	public int getOffsetLeft() {
		return typedImpl.getOffsetLeft();
	}

	public Element getOffsetParent() {
		return typedImpl.getOffsetParent();
	}

	public int getOffsetTop() {
		return typedImpl.getOffsetTop();
	}

	public int getOffsetWidth() {
		return typedImpl.getOffsetWidth();
	}

	public Element getPreviousSiblingElement() {
		return typedImpl.getPreviousSiblingElement();
	}

	public boolean getPropertyBoolean(String name) {
		return typedImpl.getPropertyBoolean(name);
	}

	public double getPropertyDouble(String name) {
		return typedImpl.getPropertyDouble(name);
	}

	public int getPropertyInt(String name) {
		return typedImpl.getPropertyInt(name);
	}

	public JavaScriptObject getPropertyJSO(String name) {
		return typedImpl.getPropertyJSO(name);
	}

	public Object getPropertyObject(String name) {
		return typedImpl.getPropertyObject(name);
	}

	public String getPropertyString(String name) {
		return typedImpl.getPropertyString(name);
	}

	public int getScrollHeight() {
		return typedImpl.getScrollHeight();
	}

	public int getScrollLeft() {
		return typedImpl.getScrollLeft();
	}

	public int getScrollTop() {
		return typedImpl.getScrollTop();
	}

	public int getScrollWidth() {
		return typedImpl.getScrollWidth();
	}

	public String getString() {
		return typedImpl.getString();
	}

	public Style getStyle() {
		return typedImpl.getStyle();
	}

	public int getTabIndex() {
		return typedImpl.getTabIndex();
	}

	public String getTagName() {
		return typedImpl.getTagName();
	}

	public String getTitle() {
		return typedImpl.getTitle();
	}

	public boolean hasAttribute(String name) {
		return typedImpl.hasAttribute(name);
	}

	public boolean hasClassName(String className) {
		return typedImpl.hasClassName(className);
	}

	public boolean hasTagName(String tagName) {
		return typedImpl.hasTagName(tagName);
	}

	public void removeAttribute(String name) {
		typedImpl.removeAttribute(name);
	}

	public boolean removeClassName(String className) {
		return typedImpl.removeClassName(className);
	}

	public void toggleClassName(String className) {
		typedImpl.toggleClassName(className);
	}

	public void replaceClassName(String oldClassName, String newClassName) {
		typedImpl.replaceClassName(oldClassName, newClassName);
	}

	public void scrollIntoView() {
		typedImpl.scrollIntoView();
	}

	public void setAttribute(String name, String value) {
		typedImpl.setAttribute(name, value);
	}

	public void setClassName(String className) {
		typedImpl.setClassName(className);
	}

	public void setDir(String dir) {
		typedImpl.setDir(dir);
	}

	public void setDraggable(String draggable) {
		typedImpl.setDraggable(draggable);
	}

	public void setId(String id) {
		typedImpl.setId(id);
	}

	public void setInnerHTML(String html) {
		typedImpl.setInnerHTML(html);
	}

	public void setInnerSafeHtml(SafeHtml html) {
		typedImpl.setInnerSafeHtml(html);
	}

	public void setInnerText(String text) {
		typedImpl.setInnerText(text);
	}

	public void setLang(String lang) {
		typedImpl.setLang(lang);
	}

	public void setPropertyBoolean(String name, boolean value) {
		typedImpl.setPropertyBoolean(name, value);
	}

	public void setPropertyDouble(String name, double value) {
		typedImpl.setPropertyDouble(name, value);
	}

	public void setPropertyInt(String name, int value) {
		typedImpl.setPropertyInt(name, value);
	}

	public void setPropertyJSO(String name, JavaScriptObject value) {
		typedImpl.setPropertyJSO(name, value);
	}

	public void setPropertyObject(String name, Object value) {
		typedImpl.setPropertyObject(name, value);
	}

	public void setPropertyString(String name, String value) {
		typedImpl.setPropertyString(name, value);
	}

	public void setScrollLeft(int scrollLeft) {
		typedImpl.setScrollLeft(scrollLeft);
	}

	public void setScrollTop(int scrollTop) {
		typedImpl.setScrollTop(scrollTop);
	}

	public void setTabIndex(int tabIndex) {
		typedImpl.setTabIndex(tabIndex);
	}

	public void setTitle(String title) {
		typedImpl.setTitle(title);
	}

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
			return VmLocalDomBridge.nodeFor(jso);
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

	protected Element() {
	}

	@Override
	public Element elementFor() {
		return nodeFor();
	}

	@Override
	public Map<String, String> getAttributes() {
		return typedImpl.getAttributes();
	}

	public void sinkEvents(int eventBits) {
		typedImpl.sinkEvents(eventBits);
	}

	public Element_Jso getTypedDomImpl() {
		return typedDomImpl;
	}

	@Override
	public void putDomImpl(Node_Jso nodeDom) {
		typedDomImpl = (Element_Jso) nodeDom;
		domImpl = nodeDom;
	}

	@Override
	public void putImpl(DomNode impl) {
		typedImpl = (DomElement) impl;
		this.impl = impl;
	}
}
