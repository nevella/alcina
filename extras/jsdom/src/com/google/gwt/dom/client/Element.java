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
import com.google.gwt.dom.client.AttachIds.IdList;
import com.google.gwt.dom.client.DocumentAttachId.InvokeProxy;
import com.google.gwt.dom.client.DocumentAttachId.InvokeProxy.Flag;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.HasHandlers;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.impl.TextBoxImpl;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.reflection.ClassReflector.TypeInvoker;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.LooseContext;

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
	public static final Predicate<Element> DISPLAY_NONE = e -> e.jsoRemote()
			.getComputedStyle().getDisplayTyped() == Style.Display.NONE;

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
	 * attachId) listeners
	 *
	 * Current hacky approach works - prettier is easy
	 */
	public EventListener eventListener;

	/*
	 * For compatibility with widget events
	 */
	public Object uiObject;

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

	<T> T callWithRemoteOrDefault(boolean flush, Supplier<T> supplier,
			T defaultValue) {
		if (!hasRemote() || isPendingSync()) {
			LocalDom.flush();
		}
		if (hasRemote()) {
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
		runIfWithRemote(true, () -> remote().focus());
	}

	@Override
	public int getAbsoluteBottom() {
		return callWithRemoteOrDefault(true, () -> remote().getAbsoluteBottom(),
				0);
	}

	@Override
	public int getAbsoluteLeft() {
		return callWithRemoteOrDefault(true, () -> remote().getAbsoluteLeft(),
				0);
	}

	@Override
	public int getAbsoluteRight() {
		return callWithRemoteOrDefault(true, () -> remote().getAbsoluteRight(),
				0);
	}

	@Override
	public int getAbsoluteTop() {
		return callWithRemoteOrDefault(true, () -> remote().getAbsoluteTop(),
				0);
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

	@Override
	public DomRect getBoundingClientRect() {
		return callWithRemoteOrDefault(true,
				() -> remote().getBoundingClientRect(), null);
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
		if (Document.get().resolveSvgStyles && hasRemote()) {
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

	@Deprecated
	@Override
	public NodeList<Element> getElementsByTagName(String name) {
		List<ElementLocal> list = traverse().filter(Node::isElement)
				.map(Node::local).map(ElementLocal.class::cast)
				.collect(Collectors.toList());
		return new NodeList(new NodeListLocal(list));
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
		return getOuterHtml(false);
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
		StringBuilder builder = new StringBuilder();
		getTextContent0(builder);
		return builder.toString();
	}

	void getTextContent0(StringBuilder builder) throws DOMException {
		streamChildren().forEach(n -> {
			if (n.getNodeType() == Node.TEXT_NODE) {
				builder.append(n.getNodeValue());
			} else if (n.getNodeType() == Node.ELEMENT_NODE) {
				((Element) n).getTextContent0(builder);
			}
		});
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

	private ClientDomElement implForPropertyName(String name) {
		switch (name) {
		case "clientWidth":
		case "offsetWidth":
			// TODO - warn maybe? non optimal. SliderBar one major cause
			return jsoRemote();
		}
		if (hasRemote()) {
			return remote();
		} else {
			return local();
		}
		// may need to make more conservative
		// return ensureRemote();
	}

	@Override
	protected boolean isPendingSync() {
		return LocalDom.isPendingSync(this);
	}

	@Override
	public ElementJso jsoRemote() {
		if (remote == null) {
			LocalDom.flush();
		}
		return (ElementJso) remote();
	}

	boolean linkedAndNotPending() {
		return hasRemote() && !isPendingSync();
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
	@Override
	protected void onAttach() {
		// Event hookup code
		if (this.eventListener == null) {
			this.eventListener = uiObject instanceof EventListener
					? (EventListener) uiObject
					: this;
		}
		DOM.setEventListener(this, eventListener);
		super.onAttach();
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
	@Override
	protected void onDetach() {
		/*
		 * Note that this doesn't use the same ordering as Widget (assumes no
		 * fails - and note that no events/side-effects are produced, this code
		 * is just concerned with listener attach/detach)
		 */
		/*
		 * And - since the detach was just an artifact to handle GC issues on
		 * IE, we don't need it (nice, since it's a perf hit in devmode)
		 */
		// DOM.setEventListener(this, null);
		super.onDetach();
	}

	protected ElementAttachId attachIdRemote() {
		LocalDom.flush();
		return (ElementAttachId) remote();
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
		resetRemote();
		return this;
	}

	@Override
	protected void putRemote(ClientDomNode remote) {
		if (this.remote == remote) {
			return;
		}
		if (this.remote == null || remote == null) {
			// correct
		} else {
			throw new IllegalStateException(
					"Changing remote - which should be invariant");
		}
		this.remote = (ClientDomElement) remote;
		if (remote != null) {
			if (local() != null) {
				if (local().getEventBits() != 0) {
					int existingBits = DOM.getEventsSunk(this);
					DOM.sinkEvents(this, existingBits | local().getEventBits());
				}
				if (local().bitlessEvents != null) {
					local().bitlessEvents.forEach(eventTypeName -> DOM
							.sinkBitlessEvent(this, eventTypeName));
				}
			}
		}
	}

	@Override
	protected ClientDomElement remote() {
		return remote;
	}

	@Override
	public void removeAttribute(String name) {
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
		boolean result = local().removeClassName(className);
		notify(() -> LocalDom.getLocalMutations().notifyAttributeModification(
				this, "class", local().getClassName()));
		sync(() -> remote().removeClassName(className));
		return result;
	}

	@Override
	public void replaceClassName(String oldClassName, String newClassName) {
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
		this.remote = null;
		if (this.hasStyle()) {
			this.style.resetRemote();
		}
	}

	boolean isAttachIdRemote() {
		return remote != null && remote().isAttachId();
	}

	boolean isJsoRemote() {
		return remote != null && remote().isJso();
	}

	public void resolvePendingSync() {
		if (isAttachIdRemote()) {
			/*
			 * all descendants are attachId remotes
			 */
			try {
				LooseContext.push();
				LooseContext.set(DOM.CONTEXT_SINK_ATTACH_ID_PENDING, this);
				NodeAttachId.ensureAttachIdRemote(this);
			} finally {
				LooseContext.pop();
			}
		}
	}

	private void runIfWithRemote(boolean flush, Runnable runnable) {
		if (!hasRemote() && flush) {
			LocalDom.flush();
		}
		if (hasRemote()) {
			runnable.run();
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
		local().setDir(dir);
		notify(() -> LocalDom.getLocalMutations()
				.notifyAttributeModification(this, "dir", local().getDir()));
		sync(() -> remote().setDir(dir));
	}

	@Override
	public void setDraggable(String draggable) {
		local().setDraggable(draggable);
		notify(() -> LocalDom.getLocalMutations().notifyAttributeModification(
				this, "draggable", local().getDraggable()));
		sync(() -> remote().setDraggable(draggable));
	}

	@Override
	public void setId(String id) {
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
		setInnerHTML(html, null);
	}

	void setInnerHTML(String html, AttachIds.IdList idList) {
		removeAllChildren();
		/*
		 * this is the most conjoined part of localdom really - the idList (if
		 * present) can come from a remote env - so the whole process (set html,
		 * optionally using remote ids) is passed to LocalDom to avoid mixing
		 * control flows
		 */
		LocalDom.setInnerHtml(this, html, idList);
	}

	@Override
	public void setInnerSafeHtml(SafeHtml html) {
		setInnerHTML(html.asString());
	}

	@Override
	public boolean isElement() {
		return true;
	}

	@Override
	public void setInnerText(String text) {
		setInnerHTML(SafeHtmlUtils.htmlEscape(text));
	}

	@Override
	public void setLang(String lang) {
		local().setLang(lang);
		notify(() -> LocalDom.getLocalMutations()
				.notifyAttributeModification(this, "lang", local().getLang()));
		sync(() -> remote().setLang(lang));
	}

	@Override
	public void setNodeValue(String nodeValue) {
		throw new UnsupportedOperationException();
	}

	public void setOuterHtml(String html) {
		Preconditions.checkState(!hasRemote());
		local().setOuterHtml(html);
	}

	@Override
	public void setPropertyBoolean(String name, boolean value) {
		local().setPropertyBoolean(name, value);
		notify(() -> LocalDom.getLocalMutations().notifyAttributeModification(
				this, name, String.valueOf(value)));
		sync(() -> remote().setPropertyBoolean(name, value));
	}

	@Override
	public void setPropertyDouble(String name, double value) {
		local().setPropertyDouble(name, value);
		notify(() -> LocalDom.getLocalMutations().notifyAttributeModification(
				this, name, String.valueOf(value)));
		sync(() -> remote().setPropertyDouble(name, value));
	}

	@Override
	public void setPropertyInt(String name, int value) {
		local().setPropertyInt(name, value);
		notify(() -> LocalDom.getLocalMutations().notifyAttributeModification(
				this, name, String.valueOf(value)));
		sync(() -> remote().setPropertyInt(name, value));
	}

	@Override
	public void setPropertyJSO(String name, JavaScriptObject value) {
		sync(() -> remote().setPropertyJSO(name, value));
	}

	@Override
	public void setPropertyObject(String name, Object value) {
		local().setPropertyObject(name, value);
		notify(() -> LocalDom.getLocalMutations().notifyAttributeModification(
				this, name, String.valueOf(value)));
		sync(() -> remote().setPropertyObject(name, value));
	}

	@Override
	public void setPropertyString(String name, String value) {
		local().setPropertyString(name, value);
		notify(() -> LocalDom.getLocalMutations().notifyAttributeModification(
				this, name, String.valueOf(value)));
		sync(() -> remote().setPropertyString(name, value));
	}

	@Override
	public void setScrollLeft(int scrollLeft) {
		runIfWithRemote(true, () -> remote().setScrollLeft(scrollLeft));
	}

	@Override
	public void setScrollTop(int scrollTop) {
		runIfWithRemote(true, () -> remote().setScrollTop(scrollTop));
	}

	@Override
	public void setTabIndex(int tabIndex) {
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

	@Override
	public void toggleClassName(String className) {
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

	public ElementImplAccess implAccess() {
		return new ElementImplAccess();
	}

	/**
	 * Most of these methods assume the remote() is a NodeJso
	 *
	 *
	 *
	 */
	public class ElementImplAccess {
		public void emitSinkBitlessEvent(String eventTypeName) {
			ClientDomElement remote = remote();
			if (remote instanceof ElementAttachId) {
				((ElementAttachId) remote).emitSinkBitlessEvent(eventTypeName);
			}
		}

		public void emitSinkEvents(int eventBits) {
			ClientDomElement remote = remote();
			if (remote instanceof ElementAttachId) {
				((ElementAttachId) remote).emitSinkEvents(eventBits);
			}
		}

		public NodeJso jsoChild(int index) {
			return jsoRemote().getChildNodes0().getItem0(index);
		}

		public ElementJso jsoRemoteOrNull() {
			if (linkedToRemote()) {
				ClientDomElement remote = remote();
				if (remote instanceof NodeAttachId) {
					return null;
				} else {
					return (ElementJso) remote;
				}
			} else {
				return null;
			}
		}

		public boolean linkedToRemote() {
			return Element.this.hasRemote();
		}

		public ElementLocal local() {
			return Element.this.local();
		}

		public ElementAttachId attachIdRemote() {
			return Element.this.attachIdRemote();
		}

		public Node provideSelfOrAncestorLinkedToRemote() {
			return Element.this.provideSelfOrAncestorLinkedToRemote();
		}

		public ClientDomElement remote() {
			return Element.this.remote();
		}

		public boolean hasRemote() {
			return Element.this.hasRemote();
		}

		public boolean isJsoRemote() {
			return remote() != null && remote().isJso();
		}

		public ElementJso ensureJsoRemote() {
			return Element.this.ensureJsoRemote();
		}

		public void setInnerHTML(String html, IdList idList) {
			Element.this.setInnerHTML(html, idList);
		}
	}

	ElementJso ensureJsoRemote() {
		LocalDom.flush();
		return (ElementJso) remote();
	}

	ClientDomElement ensureRemote() {
		LocalDom.flush();
		return remote();
	}

	@Override
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
			case "scrollIntoView":
				Preconditions.checkArgument(argumentTypes.isEmpty());
				elem.scrollIntoView();
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

	public AttachIds.IdList getSubtreeIds() {
		return getOwnerDocument().localDom.domIds.getSubtreeIds(this);
	}

	public String getOuterHtml(boolean pretty) {
		return local().getOuterHtml(pretty);
	}

	@Override
	public ClientDomStyle getStyleRemote() {
		throw new UnsupportedOperationException();
	}

	/*
	 * newChild is already attached (mirroring remote/jso dom), this syncs the
	 * local structure
	 */
	void insertAttachedBefore(Node newChild, Node refChild) {
		local().insertBefore(newChild, refChild);
		notify(() -> LocalDom.getLocalMutations().notifyChildListMutation(this,
				newChild, newChild.getPreviousSibling(), true));
	}
}