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
import java.util.function.Supplier;

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
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.gwt.client.util.TextUtils;
import cc.alcina.framework.gwt.client.util.WidgetUtils;

/**
 * All HTML element interfaces derive from this class.
 */
public class Element extends Node implements DomElement {
	private static final String REMOTE_DEFINED = "remote-defined";

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
			return LocalDom.nodeFor(jso);
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

	private ElementLocal local;

	private DomElement remote;

	private Style style;

	public UIObject uiObject;

	public EventListener uiObjectListener;

	private boolean pendingResolution;

	protected Element() {
	}

	public boolean addClassName(String className) {
		boolean result = local().addClassName(className);
		remote().addClassName(className);
		return result;
	}

	public void blur() {
		runIfWithRemote(false, () -> remote().blur());
	}

	public <T extends JavascriptObjectEquivalent> T cast() {
		return (T) this;
	}

	public void cloneLocalStyle(Element from) {
		if (from.hasStyle()) {
			getStyle().cloneStyleFrom(from.getStyle());
		}
	}

	public String debugLocalDom() {
		return local().provideLocalDomTree();
	}

	public void dispatchEvent(NativeEvent evt) {
		remote().dispatchEvent(evt);
	}

	public void dump() {
		if (linkedToRemote()) {
			Ax.out("Outer html: \n%s\n\n", typedRemote().getOuterHtml());
		}
		dumpLocal0(0);
	}

	@Override
	public Element elementFor() {
		return nodeFor();
	}

	public void ensureId() {
		if (!linkedToRemote()) {
			local().ensureId();
		}
	}

	public void focus() {
		ensureRemote().focus();
	}

	public int getAbsoluteBottom() {
		return ensureRemote().getAbsoluteBottom();
	}

	public int getAbsoluteLeft() {
		return ensureRemote().getAbsoluteLeft();
	}

	public int getAbsoluteRight() {
		return ensureRemote().getAbsoluteRight();
	}

	public int getAbsoluteTop() {
		return ensureRemote().getAbsoluteTop();
	}

	public String getAttribute(String name) {
		return local().getAttribute(name);
	}

	@Override
	public Map<String, String> getAttributes() {
		return local().getAttributes();
	}

	public Element getChildElement(int index) {
		for (int idx = 0; idx < getChildCount(); idx++) {
			Node child = getChild(idx);
			if (child.provideIsElement()) {
				if (index-- == 0) {
					return (Element) child;
				}
			}
		}
		return null;
	}

	public int getChildIndexLocal(Element child) {
		if (child.getParentElement() != this) {
			return -1;
		}
		return child.indexInParentChildren();
	}

	public String getClassName() {
		return local().getClassName();
	}

	public int getClientHeight() {
		return remote().getClientHeight();
	}

	public int getClientWidth() {
		return remote().getClientWidth();
	}

	public String getDir() {
		return local().getDir();
	}

	public String getDraggable() {
		return remote().getDraggable();
	}

	public NodeList<Element> getElementsByTagName(String name) {
		if (!linkedToRemote()) {
			throw new UnsupportedOperationException();
		}
		return remote().getElementsByTagName(name);
	}

	public Element getFirstChildElement() {
		return resolveLocal().getFirstChildElement();
	}

	public String getId() {
		return local().getId();
	}

	public String getInnerHTML() {
		return local().getInnerHTML();
	}

	public String getInnerText() {
		return local().getInnerText();
	}

	public String getLang() {
		return local().getLang();
	}

	public Node getLastChild() {
		return local().getLastChild();
	}

	public Node getNextSibling() {
		return local().getNextSibling();
	}

	public Element getNextSiblingElement() {
		return local().getNextSiblingElement();
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

	public int getOffsetHeight() {
		return callWithRemoteOrDefault(true, () -> remote().getOffsetHeight(),
				0);
	}

	public int getOffsetLeft() {
		return callWithRemoteOrDefault(true, () -> remote().getOffsetLeft(), 0);
	}

	public Element getOffsetParent() {
		return callWithRemoteOrDefault(true, () -> remote().getOffsetParent(),
				null);
	}

	public int getOffsetTop() {
		return callWithRemoteOrDefault(true, () -> remote().getOffsetTop(), 0);
	}

	public int getOffsetWidth() {
		return callWithRemoteOrDefault(true, () -> remote().getOffsetWidth(),
				0);
	}

	@Override
	public String getOuterHtml() {
		throw new UnsupportedOperationException();
	}

	public Element getPreviousSiblingElement() {
		return local().getPreviousSiblingElement();
	}

	public boolean getPropertyBoolean(String name) {
		return implForPropertyName(name).getPropertyBoolean(name);
	}

	public double getPropertyDouble(String name) {
		return implForPropertyName(name).getPropertyDouble(name);
	}

	public int getPropertyInt(String name) {
		return implForPropertyName(name).getPropertyInt(name);
	}

	@Override
	public JavaScriptObject getPropertyJSO(String name) {
		return remote().getPropertyJSO(name);
	}

	@Override
	public Object getPropertyObject(String name) {
		return implForPropertyName(name).getPropertyObject(name);
	}

	public String getPropertyString(String name) {
		return implForPropertyName(name).getPropertyString(name);
	}

	public int getScrollHeight() {
		return callWithRemoteOrDefault(true, () -> remote().getScrollHeight(),
				0);
	}

	public int getScrollLeft() {
		return callWithRemoteOrDefault(true, () -> remote().getScrollLeft(), 0);
	}

	public int getScrollTop() {
		return callWithRemoteOrDefault(true, () -> remote().getScrollTop(), 0);
	}

	public int getScrollWidth() {
		return callWithRemoteOrDefault(true, () -> remote().getScrollWidth(),
				0);
	}

	public String getString() {
		return local().getString();
	}

	public Style getStyle() {
		if (style == null) {
			style = new Style(this);
		}
		return style;
	}

	public int getTabIndex() {
		return local().getTabIndex();
	}

	public String getTagName() {
		return local().getTagName();
	}

	public String getTitle() {
		return local().getTitle();
	}

	public boolean hasAttribute(String name) {
		return local().hasAttribute(name);
	}

	public boolean hasClassName(String className) {
		return local().hasClassName(className);
	}

	public boolean hasStyle() {
		return style != null;
	}

	public boolean hasTagName(String tagName) {
		return local().hasTagName(tagName);
	}

	public ElementImplAccess implAccess() {
		return new ElementImplAccess();
	}

	@Override
	public int indexInParentChildren() {
		return local().indexInParentChildren();
	}

	public boolean isPendingResolution() {
		return this.pendingResolution;
	}

	public int localEventBitsSunk() {
		return local().eventBits;
	}

	public Element nodeFor() {
		return this;
	}

	public void pendingResolution() {
		this.pendingResolution = true;
	}

	@Override
	public void putRemote(NodeRemote remote) {
		Preconditions.checkState(
				this.remote == ElementNull.INSTANCE || remote == this.remote);
		this.remote = (ElementRemote) remote;
		if (remote != null) {
			if (local() != null && local().getEventBits() != 0) {
				int existingBits = DOM.getEventsSunk(this);
				DOM.sinkEvents(this, existingBits | local().getEventBits());
			}
		}
	}

	@Override
	public Node removeAllChildren() {
		local().removeAllChildren();
		remote().removeAllChildren();
		return this;
	}

	public void removeAttribute(String name) {
		local().removeAttribute(name);
		remote().removeAttribute(name);
	}

	public boolean removeClassName(String className) {
		boolean result = local().removeClassName(className);
		remote().removeClassName(className);
		return result;
	}

	public void replaceClassName(String oldClassName, String newClassName) {
		local().replaceClassName(oldClassName, newClassName);
		remote().replaceClassName(oldClassName, newClassName);
	}

	public void resolvePending() {
		pendingResolution = false;
	}

	public boolean resolveRemoteDefined() {
		if (getClassName().contains(REMOTE_DEFINED)) {
			Ax.out("resolve remote defined: %s", hashCode());
			ensureRemote();
			LocalDom.syncToRemote(this);
			UIObject.setStyleName(this, REMOTE_DEFINED, false);
			return true;
		} else {
			return false;
		}
	}

	public void scrollIntoView() {
		runIfWithRemote(true, () -> remote().scrollIntoView());
	}

	public void setAttribute(String name, String value) {
		local().setAttribute(name, value);
		remote().setAttribute(name, value);
	}

	public void setClassName(String className) {
		local().setClassName(className);
		remote().setClassName(className);
	}

	public void setDir(String dir) {
		local().setDir(dir);
		remote().setDir(dir);
	}

	public void setDraggable(String draggable) {
		local().setDraggable(draggable);
		remote().setDraggable(draggable);
	}

	public void setId(String id) {
		local().setId(id);
		remote().setId(id);
	}

	public void setInnerHTML(String html) {
		ensureRemoteCheck();
		clearResolved();
		local().setInnerHTML(html);
		remote().setInnerHTML(html);
		LocalDom.checkRequiresSync(local());
	}

	public void setInnerSafeHtml(SafeHtml html) {
		ensureRemoteCheck();
		clearResolved();
		local().setInnerSafeHtml(html);
		remote().setInnerSafeHtml(html);
		LocalDom.checkRequiresSync(local());
	}

	public void setInnerText(String text) {
		ensureRemoteCheck();
		clearResolved();
		local().setInnerText(text);
		remote().setInnerText(text);
	}

	public void setLang(String lang) {
		local().setLang(lang);
		remote().setLang(lang);
	}

	public void setNodeValue(String nodeValue) {
		local().setNodeValue(nodeValue);
		remote().setNodeValue(nodeValue);
	}

	public void setOuterHtml(String html) {
		Preconditions.checkState(!linkedToRemote());
		local().setOuterHtml(html);
	}

	public void setPropertyBoolean(String name, boolean value) {
		local().setPropertyBoolean(name, value);
		remote().setPropertyBoolean(name, value);
	}

	public void setPropertyDouble(String name, double value) {
		local().setPropertyDouble(name, value);
		remote().setPropertyDouble(name, value);
	}

	public void setPropertyInt(String name, int value) {
		local().setPropertyInt(name, value);
		remote().setPropertyInt(name, value);
	}

	public void setPropertyJSO(String name, JavaScriptObject value) {
		local().setPropertyJSO(name, value);
		remote().setPropertyJSO(name, value);
	}

	public void setPropertyObject(String name, Object value) {
		local().setPropertyObject(name, value);
		remote().setPropertyObject(name, value);
	}

	public void setPropertyString(String name, String value) {
		local().setPropertyString(name, value);
		remote().setPropertyString(name, value);
	}

	public void setScrollLeft(int scrollLeft) {
		ensureRemote().setScrollLeft(scrollLeft);
	}

	public void setScrollTop(int scrollTop) {
		ensureRemote().setScrollTop(scrollTop);
	}

	public void setTabIndex(int tabIndex) {
		local().setTabIndex(tabIndex);
		remote().setTabIndex(tabIndex);
	}

	public void setTitle(String title) {
		local().setTitle(title);
		remote().setTitle(title);
	}

	public void sinkEvents(int eventBits) {
		local().sinkEvents(eventBits);
		remote().sinkEvents(eventBits);
	}

	public void toggleClassName(String className) {
		local().toggleClassName(className);
		remote().toggleClassName(className);
	}

	@Override
	public String toString() {
		FormatBuilder fb = new FormatBuilder();
		fb.format("%s - %s", local().toString(), (uiObject == null
				? "(no uiobject)" : uiObject.getClass().getSimpleName()));
		if (getChildCount() != 0) {
			fb.format("\n\t");
			NodeLocal cursor = local();
			while (cursor.getChildCount() > 0) {
				cursor = cursor.children.get(0);
				fb.format("%s ", cursor.getNodeName());
			}
		}
		return fb.toString();
	}

	private <T> T callWithRemoteOrDefault(boolean flush, Supplier<T> supplier,
			T defaultValue) {
		if (!linkedToRemote() && flush) {
			ensureRemote();
		}
		if (linkedToRemote()) {
			return supplier.get();
		} else {
			return defaultValue;
		}
	}

	private void dumpLocal0(int depth) {
		String indent = CommonUtils.padStringLeft("", depth * 2, ' ');
		String message = Ax.format("%s%s [%s,%s,%s]: ", indent, getTagName(),
				hashCode(), local().hashCode(), !linkedToRemote() ? "f" : "t");
		LocalDom.log(LocalDomDebug.DUMP_LOCAL, message);
		for (Node node : getChildNodes()) {
			switch (node.getNodeType()) {
			case Node.TEXT_NODE:
				message = indent + CommonUtils.trimToWsChars(
						TextUtils.normalise(node.getNodeValue()), 50, true);
				LocalDom.log(LocalDomDebug.DUMP_LOCAL, message);
				break;
			case ELEMENT_NODE:
				((Element) node).dumpLocal0(depth + 1);
				break;
			}
		}
	}

	private DomElement implForPropertyName(String name) {
		if (!provideWasFlushed()) {
			return local();
		}
		ensureRemoteCheck();
		if (linkedToRemote()) {
			return remote();
		} else {
			return local();
		}
		// may need to make more conservative
		// return ensureRemote();
	}

	private ElementLocal resolveLocal() {
		return local().resolveLocal();
	}

	private void runIfWithRemote(boolean flush, Runnable runnable) {
		if (!linkedToRemote() && flush) {
			ensureRemote();
		}
		if (linkedToRemote()) {
			runnable.run();
		}
	}

	protected ElementRemote ensureRemote() {
		LocalDom.flush();
		LocalDom.ensureRemote(this);
		return typedRemote();
	}

	@Override
	protected boolean linkedToRemote() {
		return remote() != ElementNull.INSTANCE;
	}

	@Override
	protected ElementLocal local() {
		return local;
	}

	@Override
	protected DomElement remote() {
		if (LocalDom.isDisableRemoteWrite()) {
			return ElementNull.INSTANCE;
		}
		return remote;
	}

	Element putLocal(ElementLocal local) {
		Preconditions.checkState(this.local == null);
		this.local = local;
		local.putElement(this);
		this.remote = ElementNull.INSTANCE;
		return this;
	}

	void replaceRemote(ElementRemote remote) {
		ElementRemote parentRemote = typedRemote().getParentElement0();
		if (parentRemote != null) {
			parentRemote.insertBefore0(remote, typedRemote());
			typedRemote().removeFromParent0();
		}
		this.remote = remote;
	}

	ElementRemote typedRemote() {
		return (ElementRemote) remote();
	}

	public class ElementImplAccess {
		public ElementRemote ensureRemote() {
			return Element.this.ensureRemote();
		}

		public boolean linkedToRemote() {
			return Element.this.linkedToRemote();
		}

		public ElementLocal local() {
			return Element.this.local();
		}

		public Node provideSelfOrAncestorLinkedToRemote() {
			return Element.this.provideSelfOrAncestorLinkedToRemote();
		}

		public DomElement remote() {
			return Element.this.remote();
		}

		public ElementRemote typedRemote() {
			return Element.this.typedRemote();
		}

		public ElementRemote typedRemoteOrNull() {
			return linkedToRemote() ? Element.this.typedRemote() : null;
		}
	}
}