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
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.TypeInfo;

import com.google.common.base.Preconditions;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JavascriptObjectEquivalent;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.LocalDomDebug;
import com.google.gwt.user.client.ui.UIObject;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.TextUtils;

/**
 * All HTML element interfaces derive from this class.
 */
public class Element extends Node implements DomElement, org.w3c.dom.Element {
	public static final String REMOTE_DEFINED = "__localdom-remote-defined";

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

	@Override
	public boolean addClassName(String className) {
		ensureRemoteCheck();
		boolean result = local().addClassName(className);
		remote().addClassName(className);
		return result;
	}

	@Override
	public void blur() {
		runIfWithRemote(false, () -> remote().blur());
	}

	@Override
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

	@Override
	public void dispatchEvent(NativeEvent evt) {
		remote().dispatchEvent(evt);
	}

	public String dump(boolean linkToRemote) {
		// Ax.out("\n\n**dump:\n");
		// if (linkedToRemote()) {
		// Ax.out("Outer html: \n%s\n\n", typedRemote().getOuterHtml());
		// }
		StringBuilder stringBuilder = new StringBuilder();
		dumpLocal0(0, "0", linkToRemote, stringBuilder);
		return stringBuilder.toString();
	}

	@Override
	public Element elementFor() {
		return node();
	}

	@Override
	public void ensureId() {
		if (!linkedToRemote()) {
			local().ensureId();
		}
	}

	@Override
	public void focus() {
		ensureRemote().focus();
	}

	@Override
	public int getAbsoluteBottom() {
		return ensureRemote().getAbsoluteBottom();
	}

	@Override
	public int getAbsoluteLeft() {
		return ensureRemote().getAbsoluteLeft();
	}

	@Override
	public int getAbsoluteRight() {
		return ensureRemote().getAbsoluteRight();
	}

	@Override
	public int getAbsoluteTop() {
		return ensureRemote().getAbsoluteTop();
	}

	@Override
	public String getAttribute(String name) {
		return local().getAttribute(name);
	}

	@Override
	public Map<String, String> getAttributeMap() {
		return local().getAttributeMap();
	}

	@Override
	public Attr getAttributeNode(String arg0) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Attr getAttributeNodeNS(String arg0, String arg1)
			throws DOMException {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getAttributeNS(String arg0, String arg1) throws DOMException {
		throw new UnsupportedOperationException();
	}

	@Override
	public NamedNodeMap getAttributes() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Node getChild(int index) {
		return super.getChild(index);
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

	@Override
	public String getClassName() {
		if (DomState.domResolveSvgStyles && linkedToRemote()) {
			return getClassNameSvg();
		} else {
			return local().getClassName();
		}
	}

	@Override
	public int getClientHeight() {
		return remote().getClientHeight();
	}

	@Override
	public int getClientWidth() {
		return remote().getClientWidth();
	}

	@Override
	public String getDir() {
		return local().getDir();
	}

	@Override
	public String getDraggable() {
		return remote().getDraggable();
	}

	@Override
	public NodeList<Element> getElementsByTagName(String name) {
		ensureRemote();
		return remote().getElementsByTagName(name);
	}

	@Override
	public org.w3c.dom.NodeList getElementsByTagNameNS(String arg0, String arg1)
			throws DOMException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Element getFirstChildElement() {
		return local().getFirstChildElement();
	}

	@Override
	public String getId() {
		return local().getId();
	}

	@Override
	public String getInnerHTML() {
		return local().getInnerHTML();
	}

	@Override
	public String getInnerText() {
		return local().getInnerText();
	}

	@Override
	public String getLang() {
		return local().getLang();
	}

	@Override
	public Node getLastChild() {
		return local().getLastChild();
	}

	@Override
	public Node getNextSibling() {
		return local().getNextSibling();
	}

	@Override
	public Element getNextSiblingElement() {
		return local().getNextSiblingElement();
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
	public int getOffsetHeight() {
		return callWithRemoteOrDefault(true, () -> remote().getOffsetHeight(),
				0);
	}

	@Override
	public int getOffsetLeft() {
		return callWithRemoteOrDefault(true, () -> remote().getOffsetLeft(), 0);
	}

	@Override
	public Element getOffsetParent() {
		return callWithRemoteOrDefault(true, () -> remote().getOffsetParent(),
				null);
	}

	@Override
	public int getOffsetTop() {
		return callWithRemoteOrDefault(true, () -> remote().getOffsetTop(), 0);
	}

	@Override
	public int getOffsetWidth() {
		return callWithRemoteOrDefault(true, () -> remote().getOffsetWidth(),
				0);
	}

	@Override
	public String getOuterHtml() {
		return local().getOuterHtml();
	}

	@Override
	public Element getPreviousSiblingElement() {
		return local().getPreviousSiblingElement();
	}

	@Override
	public boolean getPropertyBoolean(String name) {
		return implForPropertyName(name).getPropertyBoolean(name);
	}

	@Override
	public double getPropertyDouble(String name) {
		return implForPropertyName(name).getPropertyDouble(name);
	}

	@Override
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

	@Override
	public String getPropertyString(String name) {
		return implForPropertyName(name).getPropertyString(name);
	}

	@Override
	public TypeInfo getSchemaTypeInfo() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getScrollHeight() {
		return callWithRemoteOrDefault(true, () -> remote().getScrollHeight(),
				0);
	}

	@Override
	public int getScrollLeft() {
		return callWithRemoteOrDefault(true, () -> remote().getScrollLeft(), 0);
	}

	@Override
	public int getScrollTop() {
		return callWithRemoteOrDefault(true, () -> remote().getScrollTop(), 0);
	}

	@Override
	public int getScrollWidth() {
		return callWithRemoteOrDefault(true, () -> remote().getScrollWidth(),
				0);
	}

	@Override
	public String getString() {
		return local().getString();
	}

	@Override
	public Style getStyle() {
		if (style == null) {
			style = new Style(this);
		}
		return style;
	}

	@Override
	public int getTabIndex() {
		return local().getTabIndex();
	}

	@Override
	public String getTagName() {
		return local().getTagName();
	}

	@Override
	public String getTextContent() throws DOMException {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getTitle() {
		return local().getTitle();
	}

	@Override
	public boolean hasAttribute(String name) {
		return local().hasAttribute(name);
	}

	@Override
	public boolean hasAttributeNS(String arg0, String arg1)
			throws DOMException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean hasClassName(String className) {
		return local().hasClassName(className);
	}

	public boolean hasStyle() {
		return style != null;
	}

	@Override
	public boolean hasTagName(String tagName) {
		return local().hasTagName(tagName);
	}

	public ElementImplAccess implAccess() {
		return new ElementImplAccess();
	}

	public int localEventBitsSunk() {
		return local().eventBits;
	}

	@Override
	public Element node() {
		return this;
	}

	@Override
	public void removeAttribute(String name) {
		ensureRemoteCheck();
		local().removeAttribute(name);
		remote().removeAttribute(name);
	}

	@Override
	public Attr removeAttributeNode(Attr arg0) throws DOMException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeAttributeNS(String arg0, String arg1)
			throws DOMException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeClassName(String className) {
		ensureRemoteCheck();
		boolean result = local().removeClassName(className);
		remote().removeClassName(className);
		return result;
	}

	@Override
	public void replaceClassName(String oldClassName, String newClassName) {
		ensureRemoteCheck();
		local().replaceClassName(oldClassName, newClassName);
		remote().replaceClassName(oldClassName, newClassName);
	}

	/**
	 * When it's quicker to redraw the whole DOM. Tree filtering springs to mind
	 */
	public void resolvedToPending() {
		implAccess().resolvedToPending();
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

	@Override
	public void scrollIntoView() {
		runIfWithRemote(true, () -> remote().scrollIntoView());
	}

	@Override
	public void setAttribute(String name, String value) {
		String current = local().getAttribute(name);
		if (Objects.equals(current, value)) {
			return;
		}
		ensureRemoteCheck();
		local().setAttribute(name, value);
		remote().setAttribute(name, value);
	}

	@Override
	public Attr setAttributeNode(Attr arg0) throws DOMException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Attr setAttributeNodeNS(Attr arg0) throws DOMException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setAttributeNS(String arg0, String arg1, String arg2)
			throws DOMException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setClassName(String className) {
		String current = local().getClassName();
		if (Objects.equals(current, className)) {
			return;
		}
		ensureRemoteCheck();
		local().setClassName(className);
		remote().setClassName(className);
	}

	@Override
	public void setDir(String dir) {
		ensureRemoteCheck();
		local().setDir(dir);
		remote().setDir(dir);
	}

	@Override
	public void setDraggable(String draggable) {
		ensureRemoteCheck();
		local().setDraggable(draggable);
		remote().setDraggable(draggable);
	}

	@Override
	public void setId(String id) {
		ensureRemoteCheck();
		local().setId(id);
		remote().setId(id);
	}

	@Override
	public void setIdAttribute(String arg0, boolean arg1) throws DOMException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setIdAttributeNode(Attr arg0, boolean arg1)
			throws DOMException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setIdAttributeNS(String arg0, String arg1, boolean arg2)
			throws DOMException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setInnerHTML(String html) {
		ensureRemoteCheck();
		clearResolved();
		List<Node> oldChildren = getChildNodes().stream()
				.collect(Collectors.toList());
		removeAllChildren();
		if (notPendingAndLinked()) {
			remote().setInnerHTML(html);
			// tbodies? foots? proudfeet?
			String remoteHtml = typedRemote().getInnerHTML0();
			local().setInnerHTML(remoteHtml);
			LocalDom.wasResolved(this);
		} else {
			local().setInnerHTML(html);
		}
	}

	public void setInnerHTMLWithValidation(String html) {
		String validatedHtml = LocalDom.validateHtml(html);
		setInnerHTML(validatedHtml);
	}

	@Override
	public void setInnerSafeHtml(SafeHtml html) {
		setInnerSafeHtml(html, true);
	}

	@Override
	public void setInnerText(String text) {
		ensureRemoteCheck();
		clearResolved();
		List<Node> oldChildren = getChildNodes().stream()
				.collect(Collectors.toList());
		removeAllChildren();
		if (notPendingAndLinked()) {
			remote().setInnerText(text);
			local().setInnerText(text);
			LocalDom.wasResolved(this);
		} else {
			local().setInnerText(text);
		}
	}

	@Override
	public void setLang(String lang) {
		ensureRemoteCheck();
		local().setLang(lang);
		remote().setLang(lang);
	}

	@Override
	public void setNodeValue(String nodeValue) {
		ensureRemoteCheck();
		local().setNodeValue(nodeValue);
		remote().setNodeValue(nodeValue);
	}

	public void setOuterHtml(String html) {
		Preconditions.checkState(!linkedToRemote());
		local().setOuterHtml(html);
	}

	@Override
	public void setPropertyBoolean(String name, boolean value) {
		ensureRemoteCheck();
		local().setPropertyBoolean(name, value);
		remote().setPropertyBoolean(name, value);
	}

	@Override
	public void setPropertyDouble(String name, double value) {
		ensureRemoteCheck();
		local().setPropertyDouble(name, value);
		remote().setPropertyDouble(name, value);
	}

	@Override
	public void setPropertyInt(String name, int value) {
		ensureRemoteCheck();
		local().setPropertyInt(name, value);
		remote().setPropertyInt(name, value);
	}

	@Override
	public void setPropertyJSO(String name, JavaScriptObject value) {
		ensureRemoteCheck();
		local().setPropertyJSO(name, value);
		remote().setPropertyJSO(name, value);
	}

	@Override
	public void setPropertyObject(String name, Object value) {
		ensureRemoteCheck();
		local().setPropertyObject(name, value);
		remote().setPropertyObject(name, value);
	}

	@Override
	public void setPropertyString(String name, String value) {
		ensureRemoteCheck();
		local().setPropertyString(name, value);
		remote().setPropertyString(name, value);
	}

	@Override
	public void setScrollLeft(int scrollLeft) {
		ensureRemote().setScrollLeft(scrollLeft);
	}

	@Override
	public void setScrollTop(int scrollTop) {
		ensureRemote().setScrollTop(scrollTop);
	}

	@Override
	public void setTabIndex(int tabIndex) {
		ensureRemoteCheck();
		local().setTabIndex(tabIndex);
		remote().setTabIndex(tabIndex);
	}

	@Override
	public void setTitle(String title) {
		ensureRemoteCheck();
		local().setTitle(title);
		remote().setTitle(title);
	}

	@Override
	public void sinkEvents(int eventBits) {
		local().sinkEvents(eventBits);
		remote().sinkEvents(eventBits);
	}

	@Override
	public void toggleClassName(String className) {
		ensureRemoteCheck();
		local().toggleClassName(className);
		remote().toggleClassName(className);
	}

	@Override
	public String toString() {
		FormatBuilder fb = new FormatBuilder();
		fb.format("%s#%s.%s - %s", local().toString(),
				Ax.blankTo(local.getId(), "---"),
				Ax.blankTo(local.getClassName(), "---"),
				(uiObject == null ? "(no uiobject)"
						: uiObject.getClass().getSimpleName()));
		if (getChildCount() != 0) {
			fb.format("\n\t");
			NodeLocal cursor = local();
			while (cursor.getChildCount() > 0) {
				cursor = cursor.getChildren().get(0);
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

	private void dumpLocal0(int depth, String prefix, boolean linkToRemote,
			StringBuilder stringBuilder) {
		String indent = CommonUtils.padStringLeft("", depth * 2, ' ');
		String paadedPrefix = CommonUtils.padStringRight(prefix, 30, ' ');
		if (!linkedToRemote() && linkToRemote) {
			implAccess().ensureRemote();
		}
		String code = !linkedToRemote() ? "f" : "t";
		if (linkedToRemote()) {
			if (local().getChildren().size() != typedRemote().getChildNodes0()
					.getLength()) {
				code = "x";
			}
		}
		String message = Ax.format("%s%s%s [%s]: ", prefix, indent,
				getTagName(), code);
		stringBuilder.append(message);
		stringBuilder.append("\n");
		LocalDom.log(LocalDomDebug.DUMP_LOCAL, message);
		int idx = 0;
		for (Node node : getChildNodes()) {
			switch (node.getNodeType()) {
			case Node.TEXT_NODE:
				message = Ax
						.format("%s%s%s [t]: %s", prefix, indent, getNodeName(),
								CommonUtils.trimToWsChars(
										TextUtils.normalizeWhitespace(
												node.getNodeValue()),
										50, true));
				stringBuilder.append(message);
				stringBuilder.append("\n");
				LocalDom.log(LocalDomDebug.DUMP_LOCAL, message);
				break;
			case Node.ELEMENT_NODE:
				((Element) node).dumpLocal0(depth + 1, prefix + "." + (idx++),
						linkToRemote, stringBuilder);
				break;
			}
		}
	}

	private DomElement implForPropertyName(String name) {
		switch (name) {
		case "clientWidth":
		case "offsetWidth":
			// TODO - warn maybe? non optimal. SliderBar one major cause
			return ensureRemote();
		}
		if (!wasResolved()) {
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

	private boolean notPendingAndLinked() {
		return linkedToRemote() && !isPendingResolution();
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
	protected boolean isPendingResolution() {
		return this.pendingResolution;
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
	protected void putRemote(NodeRemote remote, boolean resolved) {
		if (!GWT.isScript()) {
			Preconditions.checkState(
					remote.getNodeName().equalsIgnoreCase(local.getNodeName()));
		}
		Preconditions.checkState(wasResolved() == resolved);
		Preconditions.checkState(
				this.remote == ElementNull.INSTANCE || remote == this.remote);
		Preconditions.checkState(remote != null);
		this.remote = (ElementRemote) remote;
		if (remote != null) {
			if (local() != null && local().getEventBits() != 0) {
				int existingBits = DOM.getEventsSunk(this);
				DOM.sinkEvents(this, existingBits | local().getEventBits());
			}
		}
	}

	@Override
	protected DomElement remote() {
		if (LocalDom.isDisableRemoteWrite()) {
			return ElementNull.INSTANCE;
		}
		return remote;
	}

	@Override
	protected void resetRemote0() {
		this.remote = ElementNull.INSTANCE;
		if (this.hasStyle()) {
			this.style.resetRemote();
		}
	}

	protected void setInnerSafeHtml(SafeHtml html, boolean withPreRemove) {
		ensureRemoteCheck();
		clearResolved();
		List<Node> oldChildren = getChildNodes().stream()
				.collect(Collectors.toList());
		if (withPreRemove) {
			removeAllChildren();
		} else {
			local().getChildren().clear();
		}
		if (notPendingAndLinked()) {
			remote().setInnerSafeHtml(html);
			String remoteHtml = typedRemote().getInnerHTML0();
			local().setInnerHTML(remoteHtml);
			LocalDom.wasResolved(this);
		} else {
			local().setInnerSafeHtml(html);
		}
	}

	@Override
	protected ElementRemote typedRemote() {
		return (ElementRemote) remote();
	}

	final native String getClassNameSvg() /*-{
    var elem = this.@com.google.gwt.dom.client.Element::typedRemote()();
    var cn = elem.className;
    //note - someone says IE DOM objects don't support - hence try/catch
    try {
      if (cn.hasOwnProperty("baseVal")) {
        cn = cn.baseVal;
      }
      if ((typeof cn).toLowerCase() != "string") {
        if (cn && cn.toString().toLowerCase().indexOf("svg") != -1) {
          cn = 'svg-string';
        } else {
          debugger;
        }
      }
    } catch (e) {
      return "";
    }
    return cn;
	}-*/;

	void pendingResolution() {
		this.pendingResolution = true;
	}

	Element putLocal(ElementLocal local) {
		Preconditions.checkState(this.local == null);
		this.local = local;
		local.putElement(this);
		this.remote = ElementNull.INSTANCE;
		return this;
	}

	void replaceRemote(ElementRemote remote) {
		ElementRemote parentRemote = typedRemote().getParentElementRemote();
		if (parentRemote != null) {
			parentRemote.insertBefore0(remote, typedRemote());
			typedRemote().removeFromParent0();
		}
		Preconditions.checkState(remote != null);
		this.remote = remote;
	}

	public class ElementImplAccess {
		public void appendChildLocalOnly(Element localOnly) {
			// IE special case
			local.getChildren().add(localOnly.local);
		}

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

		public void resolvedToPending() {
			if (linkedToRemote()) {
				ElementRemote oldRemote = typedRemote();
				oldRemote.removeAllChildrenElement();
				local().walk(ln -> ln.node().resetRemote());
				resetRemote();
				LocalDom.ensureRemoteNodeMaybePendingResolution(Element.this);
				oldRemote.replaceWith(typedRemote());
			}
		}

		public void setRemote(ElementRemote remote) {
			LocalDom.putRemote(Element.this, remote);
		}

		public NodeRemote typedChild(int index) {
			return ensureRemote().getChildNodes0().getItem0(index);
		}

		public ElementRemote typedRemote() {
			return Element.this.typedRemote();
		}

		public ElementRemote typedRemoteOrNull() {
			return linkedToRemote() ? Element.this.typedRemote() : null;
		}

		public boolean wasResolved() {
			return Element.this.wasResolved();
		}
	}

	public void setClassName(String className, boolean present) {
		if (present) {
			addClassName(className);
		} else {
			removeClassName(className);
		}
	}
}