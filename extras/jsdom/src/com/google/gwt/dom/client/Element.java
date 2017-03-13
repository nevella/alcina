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
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.annotations.IsSafeHtml;

/**
 * All HTML element interfaces derive from this class.
 */
public class Element extends Node<DomElement, Element_Dom>
		implements DomElement {
	/**
	 * Constant returned from {@link #getDraggable()}.
	 */
	public static final String DRAGGABLE_AUTO = "auto";

	/**
	 * Constant returned from {@link #getDraggable()}.
	 */
	public static final String DRAGGABLE_FALSE = "false";

	public <T extends CastableFromJavascriptObject> T cast() {
		return (T) this;
	}

	/**
	 * Constant returned from {@link #getDraggable()}.
	 */
	public static final String DRAGGABLE_TRUE = "true";

	public <T extends Node> T appendChild(T newChild) {
		return this.impl.appendChild(newChild);
	}

	public Node cloneNode(boolean deep) {
		return this.impl.cloneNode(deep);
	}

	public boolean addClassName(String className) {
		return this.impl.addClassName(className);
	}

	public Node getChild(int index) {
		return this.impl.getChild(index);
	}

	public int getChildCount() {
		return this.impl.getChildCount();
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

	public void blur() {
		this.impl.blur();
	}

	public void dispatchEvent(NativeEvent evt) {
		this.impl.dispatchEvent(evt);
	}

	public void focus() {
		this.impl.focus();
	}

	public Document getOwnerDocument() {
		return this.impl.getOwnerDocument();
	}

	public Node getParentNode() {
		return this.impl.getParentNode();
	}

	public int getAbsoluteBottom() {
		return this.impl.getAbsoluteBottom();
	}

	public Node getPreviousSibling() {
		return this.impl.getPreviousSibling();
	}

	public boolean hasChildNodes() {
		return this.impl.hasChildNodes();
	}

	public boolean hasParentElement() {
		return this.impl.hasParentElement();
	}

	public int getAbsoluteLeft() {
		return this.impl.getAbsoluteLeft();
	}

	public int getAbsoluteRight() {
		return this.impl.getAbsoluteRight();
	}

	public int getAbsoluteTop() {
		return this.impl.getAbsoluteTop();
	}

	public Node insertAfter(Node newChild, Node refChild) {
		return this.impl.insertAfter(newChild, refChild);
	}

	public String getAttribute(String name) {
		return this.impl.getAttribute(name);
	}

	public String getClassName() {
		return this.impl.getClassName();
	}

	public int getClientHeight() {
		return this.impl.getClientHeight();
	}

	public int getClientWidth() {
		return this.impl.getClientWidth();
	}

	public String getDir() {
		return this.impl.getDir();
	}

	public String getDraggable() {
		return this.impl.getDraggable();
	}

	public NodeList<Element> getElementsByTagName(String name) {
		return this.impl.getElementsByTagName(name);
	}

	public Element getFirstChildElement() {
		return this.impl.getFirstChildElement();
	}

	public Element nodeFor() {
		return this.impl.elementFor();
	}

	public Node insertBefore(Node newChild, Node refChild) {
		return this.impl.insertBefore(newChild, refChild);
	}

	public String getId() {
		return this.impl.getId();
	}

	public String getInnerHTML() {
		return this.impl.getInnerHTML();
	}

	public Node insertFirst(Node child) {
		return this.impl.insertFirst(child);
	}

	public String getInnerText() {
		return this.impl.getInnerText();
	}

	public String getLang() {
		return this.impl.getLang();
	}

	public boolean isOrHasChild(Node child) {
		return this.impl.isOrHasChild(child);
	}

	public void removeFromParent() {
		this.impl.removeFromParent();
	}

	public Element getNextSiblingElement() {
		return this.impl.getNextSiblingElement();
	}

	public Node replaceChild(Node newChild, Node oldChild) {
		return this.impl.replaceChild(newChild, oldChild);
	}

	public int getOffsetHeight() {
		return this.impl.getOffsetHeight();
	}

	public void setNodeValue(String nodeValue) {
		this.impl.setNodeValue(nodeValue);
	}

	public Node removeChild(Node oldChild) {
		return this.impl.removeChild(oldChild);
	}

	public int getOffsetLeft() {
		return this.impl.getOffsetLeft();
	}

	public Element getOffsetParent() {
		return this.impl.getOffsetParent();
	}

	public int getOffsetTop() {
		return this.impl.getOffsetTop();
	}

	public int getOffsetWidth() {
		return this.impl.getOffsetWidth();
	}

	public Element getPreviousSiblingElement() {
		return this.impl.getPreviousSiblingElement();
	}

	public boolean getPropertyBoolean(String name) {
		return this.impl.getPropertyBoolean(name);
	}

	public double getPropertyDouble(String name) {
		return this.impl.getPropertyDouble(name);
	}

	public int getPropertyInt(String name) {
		return this.impl.getPropertyInt(name);
	}

	public JavaScriptObject getPropertyJSO(String name) {
		return this.impl.getPropertyJSO(name);
	}

	public Object getPropertyObject(String name) {
		return this.impl.getPropertyObject(name);
	}

	public String getPropertyString(String name) {
		return this.impl.getPropertyString(name);
	}

	public int getScrollHeight() {
		return this.impl.getScrollHeight();
	}

	public int getScrollLeft() {
		return this.impl.getScrollLeft();
	}

	public int getScrollTop() {
		return this.impl.getScrollTop();
	}

	public int getScrollWidth() {
		return this.impl.getScrollWidth();
	}

	public String getString() {
		return this.impl.getString();
	}

	public Style getStyle() {
		return this.impl.getStyle();
	}

	public int getTabIndex() {
		return this.impl.getTabIndex();
	}

	public String getTagName() {
		return this.impl.getTagName();
	}

	public String getTitle() {
		return this.impl.getTitle();
	}

	public boolean hasAttribute(String name) {
		return this.impl.hasAttribute(name);
	}

	public boolean hasClassName(String className) {
		return this.impl.hasClassName(className);
	}

	public boolean hasTagName(String tagName) {
		return this.impl.hasTagName(tagName);
	}

	public void removeAttribute(String name) {
		this.impl.removeAttribute(name);
	}

	public boolean removeClassName(String className) {
		return this.impl.removeClassName(className);
	}

	public void toggleClassName(String className) {
		this.impl.toggleClassName(className);
	}

	public void replaceClassName(String oldClassName, String newClassName) {
		this.impl.replaceClassName(oldClassName, newClassName);
	}

	public void scrollIntoView() {
		this.impl.scrollIntoView();
	}

	public void setAttribute(String name, String value) {
		this.impl.setAttribute(name, value);
	}

	public void setClassName(String className) {
		this.impl.setClassName(className);
	}

	public void setDir(String dir) {
		this.impl.setDir(dir);
	}

	public void setDraggable(String draggable) {
		this.impl.setDraggable(draggable);
	}

	public void setId(String id) {
		this.impl.setId(id);
	}

	public void setInnerHTML(String html) {
		this.impl.setInnerHTML(html);
	}

	public void setInnerSafeHtml(SafeHtml html) {
		this.impl.setInnerSafeHtml(html);
	}

	public void setInnerText(String text) {
		this.impl.setInnerText(text);
	}

	public void setLang(String lang) {
		this.impl.setLang(lang);
	}

	public void setPropertyBoolean(String name, boolean value) {
		this.impl.setPropertyBoolean(name, value);
	}

	public void setPropertyDouble(String name, double value) {
		this.impl.setPropertyDouble(name, value);
	}

	public void setPropertyInt(String name, int value) {
		this.impl.setPropertyInt(name, value);
	}

	public void setPropertyJSO(String name, JavaScriptObject value) {
		this.impl.setPropertyJSO(name, value);
	}

	public void setPropertyObject(String name, Object value) {
		this.impl.setPropertyObject(name, value);
	}

	public void setPropertyString(String name, String value) {
		this.impl.setPropertyString(name, value);
	}

	public void setScrollLeft(int scrollLeft) {
		this.impl.setScrollLeft(scrollLeft);
	}

	public void setScrollTop(int scrollTop) {
		this.impl.setScrollTop(scrollTop);
	}

	public void setTabIndex(int tabIndex) {
		this.impl.setTabIndex(tabIndex);
	}

	public void setTitle(String title) {
		this.impl.setTitle(title);
	}

	/**
	 * Assert that the given {@link Node} is an {@link Element} and
	 * automatically typecast it.
	 */
	public static Element as(JavaScriptObject o) {
		assert is(o);
		return VmLocalDomBridge.nodeFor(o);
	}

	/**
	 * Assert that the given {@link Node} is an {@link Element} and
	 * automatically typecast it.
	 */
	public static Element as(Node node) {
		assert is(node.domImpl);
		return (Element) node;
	}

	/**
	 * Determines whether the given {@link JavaScriptObject} can be cast to an
	 * {@link Element}. A <code>null</code> object will cause this method to
	 * return <code>false</code>.
	 */
	public static boolean is(JavaScriptObject o) {
		if (Element_Dom.is(o)) {
			return is((Element_Dom) o);
		}
		return false;
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
}
