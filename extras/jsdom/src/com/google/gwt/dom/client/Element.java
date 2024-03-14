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
import java.util.Optional;
import java.util.function.Predicate;
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
import com.google.gwt.dom.client.DocumentPathref.InvokeProxy;
import com.google.gwt.dom.client.DocumentPathref.InvokeProxy.Flag;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.HasHandlers;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.impl.TextBoxImpl;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.reflection.ClassReflector.TypeInvoker;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.TextUtils;

/**
 * All HTML element interfaces derive from this class.
 *
 * Note that the event-related code in Widget has been moved here
 *
 * TODO - eventListener + uiObject are legacy (widget) system compatiblity refs,
 * if widget was removed, they would be too
 */
public class Element extends Node implements ClientDomElement,
		org.w3c.dom.Element, EventListener, HasHandlers {
	public static final String REMOTE_DEFINED = "__localdom-remote-defined";

	public static final Predicate<Element> DISPLAY_NONE = e -> e.implAccess()
			.ensureJsoRemote().getComputedStyle()
			.getDisplayTyped() == Style.Display.NONE;

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
			return ElementJso.is(jso);
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

	private ClientDomElement remote;

	private Style style;

	/*
	 * FIXME - romcom - all event dispatch code should not directly reference
	 * this.
	 *
	 * Every element is 'attached' - so this is only for dispatch to (widget;
	 * pathref) listeers
	 *
	 * Current hacky approach works - prettier is easy
	 */
	public EventListener eventListener;

	/*
	 * For compatibility with widget events
	 */
	public Object uiObject;

	private boolean pendingSync;

	boolean attached;

	private HandlerManager handlerManager;

	protected Element() {
	}

	/**
	 * For <a href=
	 * "http://code.google.com/p/google-web-toolkit/wiki/UnderstandingMemoryLeaks"
	 * >browsers which do not leak</a>, adds a native event handler to the
	 * widget. Note that, unlike the
	 * {@link #addDomHandler(EventHandler, com.google.gwt.event.dom.client.DomEvent.Type)}
	 * implementation, there is no need to attach the widget to the DOM in order
	 * to cause the event handlers to be attached.
	 *
	 * @param <H>
	 *            the type of handler to add
	 * @param type
	 *            the event key
	 * @param handler
	 *            the handler
	 * @return {@link HandlerRegistration} used to remove the handler
	 */
	public final <H extends EventHandler> HandlerRegistration
			addBitlessDomHandler(final H handler, DomEvent.Type<H> type) {
		assert handler != null : "handler must not be null";
		assert type != null : "type must not be null";
		sinkBitlessEvent(type.getName());
		return ensureHandlers().addHandler(type, handler);
	}

	@Override
	public boolean addClassName(String className) {
		ensureRemoteCheck();
		boolean result = local().addClassName(className);
		notify(() -> LocalDom.getLocalMutations().notifyAttributeModification(
				this, "class", local().getClassName()));
		sync(() -> remote().addClassName(className));
		return result;
	}

	/**
	 * Adds a native event handler to the widget and sinks the corresponding
	 * native event. If you do not want to sink the native event, use the
	 * generic addHandler method instead.
	 *
	 * @param <H>
	 *            the type of handler to add
	 * @param type
	 *            the event key
	 * @param handler
	 *            the handler
	 * @return {@link HandlerRegistration} used to remove the handler
	 */
	public final <H extends EventHandler> HandlerRegistration
			addDomHandler(final H handler, DomEvent.Type<H> type) {
		assert handler != null : "handler must not be null";
		assert type != null : "type must not be null";
		int typeInt = Event.getTypeInt(type.getName());
		if (typeInt == -1) {
			sinkBitlessEvent(type.getName());
		} else {
			sinkEvents(typeInt);
		}
		return ensureHandlers().addHandler(type, handler);
	}

	/**
	 * Adds this handler to the widget.
	 *
	 * @param <H>
	 *            the type of handler to add
	 * @param type
	 *            the event type
	 * @param handler
	 *            the handler
	 * @return {@link HandlerRegistration} used to remove the handler
	 */
	public final <H extends EventHandler> HandlerRegistration
			addHandler(final H handler, GwtEvent.Type<H> type) {
		return ensureHandlers().addHandler(type, handler);
	}

	public void addStyleName(String cssClass) {
		UIObject.setStyleName(this, cssClass, true);
	}

	@Override
	public void blur() {
		runIfWithRemote(false, () -> remote().blur());
	}

	ElementRemote ensureRemote() {
		return implAccess().ensureRemote();
	}

	private <T> T callWithRemoteOrDefault(boolean flush, Supplier<T> supplier,
			T defaultValue) {
		if (!linkedToRemote() && flush) {
			ensureJsoRemote();
		}
		if (linkedToRemote()) {
			return supplier.get();
		} else {
			return defaultValue;
		}
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

	private void dumpLocal0(int depth, String prefix, boolean linkToRemote,
			StringBuilder stringBuilder) {
		String indent = CommonUtils.padStringLeft("", depth * 2, ' ');
		String paadedPrefix = CommonUtils.padStringRight(prefix, 30, ' ');
		if (!linkedToRemote() && linkToRemote) {
			implAccess().ensureJsoRemote();
		}
		String code = !linkedToRemote() ? "f" : "t";
		if (linkedToRemote()) {
			if (local().getChildren().size() != jsoRemote().getChildNodes0()
					.getLength()) {
				code = "x";
			}
		}
		String message = Ax.format("%s%s%s [%s]: ", prefix, indent,
				getTagName(), code);
		stringBuilder.append(message);
		stringBuilder.append("\n");
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
				Ax.out(stringBuilder);
				break;
			case Node.ELEMENT_NODE:
				((Element) node).dumpLocal0(depth + 1, prefix + "." + (idx++),
						linkToRemote, stringBuilder);
				break;
			}
		}
	}

	@Override
	public Element elementFor() {
		return node();
	}

	/**
	 * Ensures the existence of the handler manager.
	 *
	 * @return the handler manager
	 */
	HandlerManager ensureHandlers() {
		/*
		 * *Either* the widget or the element is a handler/event hookup, not
		 * both
		 */
		Preconditions.checkState(!(uiObject instanceof EventListener));
		return handlerManager == null
				? handlerManager = new HandlerManager(this)
				: handlerManager;
	}

	@Override
	public void ensureId() {
		if (!linkedToRemote()) {
			local().ensureId();
		}
	}

	protected ElementJso ensureJsoRemote() {
		LocalDom.flush();
		LocalDom.ensureRemote(this);
		return jsoRemote();
	}

	protected ElementPathref ensurePathrefRemote() {
		LocalDom.flush();
		LocalDom.ensureRemote(this);
		return pathrefRemote();
	}

	@Override
	public void fireEvent(GwtEvent<?> event) {
		if (handlerManager != null) {
			handlerManager.fireEvent(event);
		} else if (uiObject != null && uiObject instanceof HasHandlers) {
			((HasHandlers) uiObject).fireEvent(event);
		}
	}

	public Optional<Element> firstInAncestry(Predicate<Element> predicate) {
		Element cursor = this;
		do {
			if (predicate.test(cursor)) {
				return Optional.of(cursor);
			}
			cursor = cursor.getParentElement();
		} while (cursor != null);
		return Optional.empty();
	}

	@Override
	public void focus() {
		((ClientDomElement) implAccess().ensureRemote()).focus();
	}

	@Override
	public int getAbsoluteBottom() {
		return ensureJsoRemote().getAbsoluteBottom();
	}

	@Override
	public int getAbsoluteLeft() {
		return ensureJsoRemote().getAbsoluteLeft();
	}

	@Override
	public int getAbsoluteRight() {
		return ensureJsoRemote().getAbsoluteRight();
	}

	@Override
	public int getAbsoluteTop() {
		return ensureJsoRemote().getAbsoluteTop();
	}

	@Override
	public String getAttribute(String name) {
		return local().getAttribute(name);
	}

	@Override
	public Map<String, String> getAttributeMap() {
		return local.getAttributeMapIncludingStyles();
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
		return local().getAttributes();
	}

	public DomRect getBoundingClientRect() {
		return ensureRemote().getBoundingClientRect();
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

	public int getChildIndexLocal(Node child) {
		if (child.getParentElement() != this) {
			return -1;
		}
		return child.indexInParentChildren();
	}

	@Override
	public String getClassName() {
		if (Document.get().resolveSvgStyles && linkedToRemote()) {
			return getClassNameSvg();
		} else {
			return local().getClassName();
		}
	}

	final native String getClassNameSvg() /*-{
    var elem = this.@com.google.gwt.dom.client.Element::jsoRemote()();
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
		ensureJsoRemote();
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

	// public to allow legacy (widget) access only
	public HandlerManager getHandlerManager() {
		return this.handlerManager;
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

	@Override
	public ElementImplAccess implAccess() {
		return new ElementImplAccess();
	}

	private ClientDomElement implForPropertyName(String name) {
		switch (name) {
		case "clientWidth":
		case "offsetWidth":
			// TODO - warn maybe? non optimal. SliderBar one major cause
			return ensureRemote();
		}
		if (!wasSynced()) {
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

	@Override
	protected boolean isPendingSync() {
		return this.pendingSync;
	}

	@Override
	protected ElementJso jsoRemote() {
		return (ElementJso) remote();
	}

	private boolean linkedAndNotPending() {
		return linkedToRemote() && !isPendingSync();
	}

	@Override
	protected boolean linkedToRemote() {
		return remote() != ElementNull.INSTANCE;
	}

	@Override
	protected ElementLocal local() {
		return local;
	}

	public List<String> localBitlessEventsSunk() {
		return local().bitlessEvents;
	}

	public int localEventBitsSunk() {
		return local().eventBits;
	}

	@Override
	public Element node() {
		return this;
	}

	/*
	 * See Widget.onAttach. Functionality moved from Widget to here
	 *
	 * Note that element attach/detach is always called before Widget
	 * attach/detach
	 */
	protected void onAttach() {
		// Event hookup code
		if (this.eventListener == null) {
			this.eventListener = uiObject instanceof EventListener
					? (EventListener) uiObject
					: this;
		}
		DOM.setEventListener(this, eventListener);
		streamChildren().filter(Node::provideIsElement)
				.forEach(n -> ((Element) n).setAttached(true));
	}

	@Override
	public void onBrowserEvent(Event event) {
		if (eventListener != this) {
			eventListener.onBrowserEvent(event);
			return;
		}
		switch (DOM.eventGetType(event)) {
		case Event.ONMOUSEOVER:
			// Only fire the mouse over event if it's coming from outside
			// this
			// widget.
		case Event.ONMOUSEOUT:
			// Only fire the mouse out event if it's leaving this
			// widget.
			EventTarget relatedEventTarget = event.getRelatedEventTarget();
			if (relatedEventTarget != null && Element.is(relatedEventTarget)) {
				Element related = relatedEventTarget.cast();
				if (related != null && isOrHasChild(related)) {
					return;
				}
			}
			break;
		}
		DomEvent.fireNativeEvent(event, this, this);
	}

	/*
	 * See Widget.onDetach. Functionality moved from Widget to here
	 *
	 * Note that element attach/detach is always called before Widget
	 * attach/detach
	 */
	protected void onDetach() {
		/*
		 * Note that this doesn't use the same ordering as Widget (assumes no
		 * fails - and note that no events/side-effects are produced, this code
		 * is just concerned with listener attach/detach)
		 */
		DOM.setEventListener(this, null);
		streamChildren().filter(Node::provideIsElement)
				.forEach(n -> ((Element) n).setAttached(false));
	}

	protected ElementPathref pathrefRemote() {
		return (ElementPathref) remote();
	}

	void pendingSync() {
		this.pendingSync = true;
	}

	public boolean provideIsAncestorOf(Element potentialChild,
			boolean includeSelf) {
		return potentialChild
				.firstInAncestry(
						e -> e == this && (e != potentialChild || includeSelf))
				.isPresent();
	}

	Element putLocal(ElementLocal local) {
		Preconditions.checkState(this.local == null);
		this.local = local;
		local.putElement(this);
		this.remote = ElementNull.INSTANCE;
		return this;
	}

	@Override
	protected void putRemote(ClientDomNode remote, boolean synced) {
		if (!GWT.isScript() && GWT.isClient()) {
			// hosted mode (dev) check
			String nodeName = remote.getNodeName();
			Preconditions
					.checkState(nodeName.equalsIgnoreCase(local.getNodeName()));
		}
		Preconditions.checkState(wasSynced() == synced);
		Preconditions.checkState(
				this.remote == ElementNull.INSTANCE || remote == this.remote);
		Preconditions.checkState(remote != null);
		if (this.remote == ElementNull.INSTANCE) {
			this.remote = (ClientDomElement) remote;
			if (remote != null) {
				if (local() != null) {
					if (local().getEventBits() != 0) {
						int existingBits = DOM.getEventsSunk(this);
						DOM.sinkEvents(this,
								existingBits | local().getEventBits());
					}
					if (local().bitlessEvents != null) {
						local().bitlessEvents.forEach(eventTypeName -> DOM
								.sinkBitlessEvent(this, eventTypeName));
					}
				}
			}
		}
	}

	@Override
	protected ClientDomElement remote() {
		if (getOwnerDocument().remoteType.hasRemote()) {
			return remote;
		} else {
			return ElementNull.INSTANCE;
		}
	}

	@Override
	public void removeAttribute(String name) {
		ensureRemoteCheck();
		local().removeAttribute(name);
		// FIXME - dirndl - dodesn't actually remove
		notify(() -> LocalDom.getLocalMutations()
				.notifyAttributeModification(this, name, ""));
		sync(() -> remote().removeAttribute(name));
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
		notify(() -> LocalDom.getLocalMutations().notifyAttributeModification(
				this, "class", local().getClassName()));
		sync(() -> remote().removeClassName(className));
		return result;
	}

	@Override
	public void replaceClassName(String oldClassName, String newClassName) {
		ensureRemoteCheck();
		local().replaceClassName(oldClassName, newClassName);
		notify(() -> LocalDom.getLocalMutations().notifyAttributeModification(
				this, "class", local().getClassName()));
		sync(() -> remote().replaceClassName(oldClassName, newClassName));
	}

	void replaceRemote(ElementJso remote) {
		ElementJso parentRemote = jsoRemote().getParentElementJso();
		if (parentRemote != null) {
			parentRemote.insertBefore0(remote, jsoRemote());
			jsoRemote().removeFromParent0();
		}
		Preconditions.checkState(remote != null);
		this.remote = remote;
	}

	@Override
	protected void resetRemote0() {
		this.remote = ElementNull.INSTANCE;
		if (this.hasStyle()) {
			this.style.resetRemote();
		}
	}

	public void resolvePendingSync() {
		pendingSync = false;
	}

	public boolean resolveRemoteDefined() {
		try {
			if (getClassName() != null
					&& getClassName().contains(REMOTE_DEFINED)) {
				Ax.out("resolve remote defined: %s", hashCode());
				ensureJsoRemote();
				LocalDom.syncToRemote(this);
				UIObject.setStyleName(this, REMOTE_DEFINED, false);
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			// FIXME - dirndl 1x1e - localdom - probably get a sample of these.
			// For
			// some apps, there are a lot - but correlated to
			// LOCALDOM_UNABLE_TO_PARSE_ISSUE
			return false;
		}
	}

	private void runIfWithRemote(boolean flush, Runnable runnable) {
		if (!linkedToRemote() && flush) {
			ensureJsoRemote();
		}
		if (linkedToRemote()) {
			runnable.run();
		}
	}

	@Override
	public void scrollIntoView() {
		runIfWithRemote(true, () -> remote().scrollIntoView());
	}

	void setAttached(boolean attached) {
		if (attached == this.attached) {
			return;
		}
		this.attached = attached;
		if (attached) {
			onAttach();
		} else {
			onDetach();
		}
	}

	@Override
	public void setAttribute(String name, String value) {
		String current = local().getAttribute(name);
		if (Objects.equals(current, value)) {
			return;
		}
		ensureRemoteCheck();
		local().setAttribute(name, value);
		notify(() -> LocalDom.getLocalMutations()
				.notifyAttributeModification(this, name, value));
		sync(() -> remote().setAttribute(name, value));
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
		notify(() -> LocalDom.getLocalMutations().notifyAttributeModification(
				this, "class", local().getClassName()));
		sync(() -> remote().setClassName(className));
	}

	public void setClassName(String className, boolean present) {
		if (present) {
			addClassName(className);
		} else {
			removeClassName(className);
		}
	}

	@Override
	public void setDir(String dir) {
		ensureRemoteCheck();
		local().setDir(dir);
		notify(() -> LocalDom.getLocalMutations()
				.notifyAttributeModification(this, "dir", local().getDir()));
		sync(() -> remote().setDir(dir));
	}

	@Override
	public void setDraggable(String draggable) {
		ensureRemoteCheck();
		local().setDraggable(draggable);
		notify(() -> LocalDom.getLocalMutations().notifyAttributeModification(
				this, "draggable", local().getDraggable()));
		sync(() -> remote().setDraggable(draggable));
	}

	@Override
	public void setId(String id) {
		ensureRemoteCheck();
		local().setId(id);
		notify(() -> LocalDom.getLocalMutations()
				.notifyAttributeModification(this, "id", local().getId()));
		sync(() -> remote().setId(id));
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
		clearSynced();
		List<Node> oldChildren = getChildNodes().stream()
				.collect(Collectors.toList());
		removeAllChildren();
		if (linkedAndNotPending()) {
			remote().setInnerHTML(html);
			// tbodies? foots? proudfeet?
			String remoteHtml = jsoRemote().getInnerHTML0();
			local().setInnerHTML(remoteHtml);
			LocalDom.wasSynced(this);
		} else {
			local().setInnerHTML(html);
		}
	}

	@Override
	public void setInnerSafeHtml(SafeHtml html) {
		setInnerSafeHtml(html, true);
	}

	protected void setInnerSafeHtml(SafeHtml html, boolean withPreRemove) {
		ensureRemoteCheck();
		clearSynced();
		List<Node> oldChildren = getChildNodes().stream()
				.collect(Collectors.toList());
		if (withPreRemove) {
			removeAllChildren();
		} else {
			local().getChildren().clear();
		}
		if (linkedAndNotPending()) {
			remote().setInnerSafeHtml(html);
			String remoteHtml = jsoRemote().getInnerHTML0();
			local().setInnerHTML(remoteHtml);
			LocalDom.wasSynced(this);
		} else {
			local().setInnerSafeHtml(html);
		}
	}

	@Override
	public void setInnerText(String text) {
		ensureRemoteCheck();
		clearSynced();
		List<Node> oldChildren = getChildNodes().stream()
				.collect(Collectors.toList());
		removeAllChildren();
		if (linkedAndNotPending()) {
			remote().setInnerText(text);
			local().setInnerText(text);
			LocalDom.wasSynced(this);
		} else {
			local().setInnerText(text);
		}
	}

	@Override
	public void setLang(String lang) {
		ensureRemoteCheck();
		local().setLang(lang);
		notify(() -> LocalDom.getLocalMutations()
				.notifyAttributeModification(this, "lang", local().getLang()));
		sync(() -> remote().setLang(lang));
	}

	@Override
	public void setNodeValue(String nodeValue) {
		// ensureRemoteCheck();
		// local().setNodeValue(nodeValue);
		// sync(() -> remote().setNodeValue(nodeValue));
		throw new UnsupportedOperationException();
	}

	public void setOuterHtml(String html) {
		Preconditions.checkState(!linkedToRemote());
		local().setOuterHtml(html);
	}

	@Override
	public void setPropertyBoolean(String name, boolean value) {
		ensureRemoteCheck();
		local().setPropertyBoolean(name, value);
		notify(() -> LocalDom.getLocalMutations().notifyAttributeModification(
				this, name, String.valueOf(value)));
		sync(() -> remote().setPropertyBoolean(name, value));
	}

	@Override
	public void setPropertyDouble(String name, double value) {
		ensureRemoteCheck();
		local().setPropertyDouble(name, value);
		notify(() -> LocalDom.getLocalMutations().notifyAttributeModification(
				this, name, String.valueOf(value)));
		sync(() -> remote().setPropertyDouble(name, value));
	}

	@Override
	public void setPropertyInt(String name, int value) {
		ensureRemoteCheck();
		local().setPropertyInt(name, value);
		notify(() -> LocalDom.getLocalMutations().notifyAttributeModification(
				this, name, String.valueOf(value)));
		sync(() -> remote().setPropertyInt(name, value));
	}

	@Override
	public void setPropertyJSO(String name, JavaScriptObject value) {
		ensureRemoteCheck();
		sync(() -> remote().setPropertyJSO(name, value));
	}

	@Override
	public void setPropertyObject(String name, Object value) {
		ensureRemoteCheck();
		local().setPropertyObject(name, value);
		notify(() -> LocalDom.getLocalMutations().notifyAttributeModification(
				this, name, String.valueOf(value)));
		sync(() -> remote().setPropertyObject(name, value));
	}

	@Override
	public void setPropertyString(String name, String value) {
		ensureRemoteCheck();
		local().setPropertyString(name, value);
		notify(() -> LocalDom.getLocalMutations().notifyAttributeModification(
				this, name, String.valueOf(value)));
		sync(() -> remote().setPropertyString(name, value));
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
		notify(() -> LocalDom.getLocalMutations().notifyAttributeModification(
				this, "tabIndex", String.valueOf(tabIndex)));
		sync(() -> remote().setTabIndex(tabIndex));
	}

	@Override
	public void setTextContent(String textContent) throws DOMException {
		Preconditions.checkState(getChildCount() == 0);
		Text text = getOwnerDocument().createTextNode(textContent);
		appendChild(text);
	}

	@Override
	public void setTitle(String title) {
		ensureRemoteCheck();
		local().setTitle(title);
		notify(() -> LocalDom.getLocalMutations()
				.notifyAttributeModification(this, "title", title));
		sync(() -> remote().setTitle(title));
	}

	@Override
	public void sinkBitlessEvent(String eventTypeName) {
		local().sinkBitlessEvent(eventTypeName);
		sync(() -> remote().sinkBitlessEvent(eventTypeName));
	}

	@Override
	public void sinkEvents(int eventBits) {
		local().sinkEvents(eventBits);
		sync(() -> remote().sinkEvents(eventBits));
	}

	/**
	 * When it's quicker to redraw the whole DOM. Tree filtering springs to mind
	 */
	public void syncedToPending() {
		implAccess().syncedToPending();
	}

	@Override
	public void toggleClassName(String className) {
		ensureRemoteCheck();
		local().toggleClassName(className);
		notify(() -> LocalDom.getLocalMutations()
				.notifyAttributeModification(this, "class", getClassName()));
		sync(() -> remote().toggleClassName(className));
	}

	@Override
	public String toString() {
		FormatBuilder fb = new FormatBuilder();
		fb.format("%s#%s.%s", local().toString(),
				Ax.blankTo(local.getId(), "---"),
				Ax.blankTo(local.getClassName(), "---"));
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

	/**
	 * Most of these methods assume the remote() is a NodeJso
	 *
	 *
	 *
	 */
	public class ElementImplAccess extends Node.ImplAccess {
		public void appendChildLocalOnly(Element localOnly) {
			// IE special case
			local.getChildren().add(localOnly.local);
		}

		public void emitSinkBitlessEvent(String eventTypeName) {
			ClientDomElement remote = remote();
			if (remote instanceof ElementPathref) {
				((ElementPathref) remote).emitSinkBitlessEvent(eventTypeName);
			}
		}

		public void emitSinkEvents(int eventBits) {
			ClientDomElement remote = remote();
			if (remote instanceof ElementPathref) {
				((ElementPathref) remote).emitSinkEvents(eventBits);
			}
		}

		public ElementJso ensureJsoRemote() {
			return Element.this.ensureJsoRemote();
		}

		public ElementPathref ensurePathrefRemote() {
			return Element.this.ensurePathrefRemote();
		}

		public NodeJso jsoChild(int index) {
			return ensureJsoRemote().getChildNodes0().getItem0(index);
		}

		@Override
		public ElementJso jsoRemote() {
			return Element.this.jsoRemote();
		}

		public ElementJso jsoRemoteOrNull() {
			if (linkedToRemote()) {
				ClientDomElement remote = remote();
				if (remote instanceof NodePathref) {
					return null;
				} else {
					return (ElementJso) remote;
				}
			} else {
				return null;
			}
		}

		public boolean linkedToRemote() {
			return Element.this.linkedToRemote();
		}

		@Override
		public ElementLocal local() {
			return Element.this.local();
		}

		public ElementPathref pathrefRemote() {
			return Element.this.pathrefRemote();
		}

		public Node provideSelfOrAncestorLinkedToRemote() {
			return Element.this.provideSelfOrAncestorLinkedToRemote();
		}

		@Override
		public void putRemote(ClientDomNode remote) {
			Element.this.remote = (ClientDomElement) remote;
		}

		@Override
		public ClientDomElement remote() {
			return Element.this.remote();
		}

		public void syncedToPending() {
			if (linkedToRemote()) {
				ElementJso oldRemote = jsoRemote();
				sync(() -> oldRemote.removeAllChildren0());
				local().walk(ln -> ln.node().resetRemote());
				resetRemote();
				LocalDom.ensureRemoteNodeMaybePendingSync(Element.this);
				oldRemote.replaceWith(jsoRemote());
			}
		}

		public boolean wasSynced() {
			return Element.this.wasSynced();
		}
	}

	public void setSelectionRange(int pos, int length) {
		ensureRemote().setSelectionRange(pos, length);
	}

	/*
	 * Invokes RPC messages on the browser client element
	 */
	@Registration({ TypeInvoker.class, Element.class })
	public static class TypeInvokerImpl extends TypeInvoker<Element> {
		@Override
		public Object invoke(Element elem, String methodName,
				List<Class> argumentTypes, List<?> arguments, List<?> flags) {
			List<InvokeProxy.Flag> typedFlags = (List<Flag>) flags;
			if (typedFlags.contains(InvokeProxy.Flag.invoke_on_element_style)) {
				return Reflections.at(elem.getStyle()).invoke(elem.getStyle(),
						methodName, argumentTypes, arguments, null);
			}
			switch (methodName) {
			case "focus":
				Preconditions.checkArgument(argumentTypes.isEmpty());
				elem.focus();
				return null;
			case "getPropertyString":
				if (argumentTypes.size() == 1) {
					return elem.getPropertyString((String) arguments.get(0));
				}
			case "setPropertyString":
				if (argumentTypes.size() == 2) {
					elem.setPropertyString((String) arguments.get(0),
							(String) arguments.get(1));
					return null;
				}
			case "setSelectionRange":
				if (argumentTypes.size() == 2) {
					TextBoxImpl.setTextBoxSelectionRange(elem,
							(int) arguments.get(0), (int) arguments.get(1));
					return null;
				}
			default:
				break;
			}
			return invokeReflective(elem, methodName, argumentTypes, arguments,
					flags);
		}
	}
}