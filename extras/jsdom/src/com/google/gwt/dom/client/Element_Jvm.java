package com.google.gwt.dom.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.safehtml.shared.SafeHtml;

public class Element_Jvm extends Node_Jvm
		implements DomElement, LocalDomElement {
	private String tagName;

	Element_Jvm(Document_Jvm document_Jvm, String tagName) {
		ownerDocument = document_Jvm;
		this.tagName = tagName;
	}

	private Style style;

	private String innerHtml;

	public String getPendingInnerHtml() {
		return this.innerHtml;
	}

	@Override
	public LocalDomElement create(String tagName) {
		return LocalDomBridge.get().localDomImpl.localImpl
				.createLocalElement(Document.get(), tagName)
				.provideLocalDomElement();
	}

	@Override
	public String getNodeName() {
		return getTagName();
	}

	@Override
	public short getNodeType() {
		return Node.ELEMENT_NODE;
	}

	@Override
	public String getNodeValue() {
		return getTagName();
	}

	@Override
	public String getTagName() {
		return tagName;
	}

	@Override
	public void setNodeValue(String nodeValue) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getAttribute(String name) {
		String value = attributes.get(name);
		return value != null ? value : "";
	}

	@Override
	public void setInnerText(String text) {
		new ArrayList<>(children).stream().forEach(Node_Jvm::removeFromParent);
		appendChild(ownerDocument.createTextNode(text));
	}

	@Override
	public final boolean addClassName(String className) {
		return DomElement_Static.addClassName(this, className);
	}

	@Override
	public final Element getFirstChildElement() {
		return resolveChildren().stream()
				.filter(node_jvm -> node_jvm.getNodeType() == Node.ELEMENT_NODE)
				.findFirst().map(node_jvm -> (Element) node_jvm.nodeFor())
				.orElse(null);
	}

	private List<Node_Jvm> resolveChildren() {
		if (children.isEmpty() && innerHtml != null) {
			RegExp tag = RegExp
					.compile("<([A-Za-z0-9_\\-.]+)( .+?)?>(.+)?</.+>", "m");
			RegExp tagNoContents = RegExp
					.compile("<([A-Za-z0-9_\\-.]+)( .+?)?/?>", "m");
			MatchResult matchResult = tag.exec(innerHtml);
			if (matchResult == null) {
				matchResult = tagNoContents.exec(innerHtml);
			}
			Element_Jvm element = (Element_Jvm) create(matchResult.getGroup(1));
			Element created = LocalDomBridge.nodeFor((Node_Jvm) element);
			created.setOuterHtml(innerHtml);
			node.appendChild(created);
			innerHtml = null;
		}
		return children;
	}

	@Override
	public String getInnerHTML() {
		if (children.isEmpty() && innerHtml != null) {
			return innerHtml;
		} else {
			UnsafeHtmlBuilder builder = new UnsafeHtmlBuilder();
			children.stream().forEach(node -> node.appendOuterHtml(builder));
			return builder.toSafeHtml().asString();
		}
	}

	@Override
	public final String getInnerText() {
		StringBuilder builder = new StringBuilder();
		appendTextContent(builder);
		return builder.toString();
	}

	@Override
	void appendTextContent(StringBuilder builder) {
		children.stream().forEach(node -> node.appendTextContent(builder));
	}

	@Override
	public final int getScrollLeft() {
		return DomElement_Static.getScrollLeft(this);
	}

	@Override
	public final String getString() {
		return DomElement_Static.getString(this);
	}

	@Override
	public final int getTabIndex() {
		String index = getAttribute("tabindex");
		return index.isEmpty() ? 0 : Integer.parseInt(index);
	}

	@Override
	public final boolean hasAttribute(String name) {
		return DomElement_Static.hasAttribute(this, name);
	}

	@Override
	public final boolean hasClassName(String className) {
		return DomElement_Static.hasClassName(this, className);
	}

	@Override
	public final boolean hasTagName(String tagName) {
		return DomElement_Static.hasTagName(this, tagName);
	}

	@Override
	public final boolean removeClassName(String className) {
		return DomElement_Static.removeClassName(this, className);
	}

	@Override
	public final void toggleClassName(String className) {
		DomElement_Static.toggleClassName(this, className);
	}

	@Override
	public final void replaceClassName(String oldClassName,
			String newClassName) {
		DomElement_Static.replaceClassName(this, oldClassName, newClassName);
	}

	@Override
	public final void scrollIntoView() {
		DomElement_Static.scrollIntoView(this);
	}

	@Override
	public final void setDraggable(String draggable) {
		DomElement_Static.setDraggable(this, draggable);
	}

	@Override
	public final void setInnerSafeHtml(SafeHtml html) {
		DomElement_Static.setInnerSafeHtml(this, html);
	}

	@Override
	public final void setScrollLeft(int scrollLeft) {
		DomElement_Static.setScrollLeft(this, scrollLeft);
	}

	public void blur() {
		DomElement_Static.blur(this);
	}

	public void dispatchEvent(NativeEvent evt) {
		DomElement_Static.dispatchEvent(this, evt);
	}

	public void focus() {
		DomElement_Static.focus(this);
	}

	public int getAbsoluteBottom() {
		return DomElement_Static.getAbsoluteBottom(this);
	}

	public int getAbsoluteLeft() {
		return DomElement_Static.getAbsoluteLeft(this);
	}

	public int getAbsoluteRight() {
		return DomElement_Static.getAbsoluteRight(this);
	}

	public int getAbsoluteTop() {
		return DomElement_Static.getAbsoluteTop(this);
	}

	public int getClientHeight() {
		return DomElement_Static.getClientHeight(this);
	}

	public int getClientWidth() {
		return DomElement_Static.getClientWidth(this);
	}

	public String getDir() {
		return DomElement_Static.getDir(this);
	}

	public String getDraggable() {
		return DomElement_Static.getDraggable(this);
	}

	public String getLang() {
		return DomElement_Static.getLang(this);
	}

	public int getOffsetHeight() {
		return DomElement_Static.getOffsetHeight(this);
	}

	public int getScrollHeight() {
		return DomElement_Static.getScrollHeight(this);
	}

	public int getScrollTop() {
		return DomElement_Static.getScrollTop(this);
	}

	public int getScrollWidth() {
		return DomElement_Static.getScrollWidth(this);
	}

	public void setDir(String dir) {
		DomElement_Static.setDir(this, dir);
	}

	public void setLang(String lang) {
		DomElement_Static.setLang(this, lang);
	}

	public void setScrollTop(int scrollTop) {
		DomElement_Static.setScrollTop(this, scrollTop);
	}

	@Override
	public String getClassName() {
		return getAttribute("class");
	}

	@Override
	public NodeList<Element> getElementsByTagName(String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getId() {
		return getAttribute("id");
	}

	@Override
	public int getOffsetLeft() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Element getOffsetParent() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getOffsetTop() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getOffsetWidth() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean getPropertyBoolean(String name) {
		return Boolean.valueOf(getPropertyString(name));
	}

	@Override
	public double getPropertyDouble(String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getPropertyInt(String name) {
		return Integer.parseInt(getPropertyString(name));
	}

	@Override
	public JavaScriptObject getPropertyJSO(String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object getPropertyObject(String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getPropertyString(String name) {
		return getAttribute(name);
	}

	@Override
	public Style getStyle() {
		if (style == null) {
			Style_Jvm style_Jvm = new Style_Jvm();
			style = LocalDomBridge.styleObjectFor(style_Jvm);
		}
		return style;
	}

	@Override
	public String getTitle() {
		return getAttribute("title");
	}

	@Override
	public void removeAttribute(String name) {
		attributes.remove(name);
	}

	@Override
	public void setAttribute(String name, String value) {
		attributes.put(name, value);
	}

	@Override
	public void setClassName(String className) {
		setAttribute("class", className);
	}

	@Override
	public void setId(String id) {
		setAttribute("id", id);
		LocalDomBridge.registerId(this);
	}

	@Override
	public void setInnerHTML(String html) {
		new ArrayList<>(children).stream().forEach(Node_Jvm::removeFromParent);
		this.innerHtml = html;
	}

	@Override
	public void setPropertyBoolean(String name, boolean value) {
		setPropertyString(name, String.valueOf(value));
	}

	@Override
	public void setPropertyDouble(String name, double value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setPropertyInt(String name, int value) {
		setPropertyString(name, String.valueOf(value));
	}

	@Override
	public void setPropertyJSO(String name, JavaScriptObject value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setPropertyObject(String name, Object value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setPropertyString(String name, String value) {
		setAttribute(name, value);
	}

	@Override
	public void setTabIndex(int tabIndex) {
		setAttribute("tabindex", String.valueOf(tabIndex));
	}

	@Override
	public void setTitle(String title) {
		setAttribute("title", title);
	}

	@Override
	public Element elementFor() {
		return LocalDomBridge.nodeFor(this);
	}

	@Override
	void appendOuterHtml(UnsafeHtmlBuilder builder) {
		if (eventBits != 0) {
			ensureId();
		}
		builder.appendHtmlConstantNoCheck("<");
		builder.appendHtmlConstant(tagName);
		if (!attributes.isEmpty()) {
			attributes.entrySet().forEach(e -> {
				builder.appendHtmlConstantNoCheck(" ");
				// invalid attr names will die on the voine
				builder.appendEscaped(e.getKey());
				builder.appendHtmlConstantNoCheck("=\"");
				builder.appendEscaped(e.getValue());
				builder.appendHtmlConstantNoCheck("\"");
			});
		}
		if (style != null) {
			builder.appendHtmlConstantNoCheck(" style=\"");
			((Style_Jvm) style.impl).properties.entrySet().forEach(e -> {
				builder.appendEscaped(
						LocalDomBridge.declarativeCssName(e.getKey()));
				builder.appendHtmlConstantNoCheck(":");
				builder.appendEscaped(e.getValue());
				builder.appendHtmlConstantNoCheck("; ");
			});
			builder.appendHtmlConstantNoCheck("\"");
		}
		builder.appendHtmlConstantNoCheck(">");
		children.stream().forEach(child -> child.appendOuterHtml(builder));
		if (innerHtml != null) {
			builder.appendUnsafeHtml(innerHtml);
		}
		builder.appendHtmlConstantNoCheck("</");
		builder.appendHtmlConstant(tagName);
		builder.appendHtmlConstantNoCheck(">");
	}

	int orSunkEventsOfAllChildren(int sunk) {
		for (Node_Jvm child : children) {
			if (child instanceof Element_Jvm) {
				sunk = ((Element_Jvm) child).orSunkEventsOfAllChildren(sunk);
			}
		}
		sunk |= eventBits;
		return sunk;
	}

	static int _idCounter;

	@Override
	public void ensureId() {
		if (getId().isEmpty()) {
			setId("__localdom__" + (++_idCounter));
		}
	}

	@Override
	public Map<String, String> getAttributes() {
		return attributes;
	}

	int eventBits;

	public int getEventBits() {
		return this.eventBits;
	}

	@Override
	public void sinkEvents(int eventBits) {
		this.eventBits |= eventBits;
	}

	@Override
	public final Element getNextSiblingElement() {
		boolean seen = false;
		if (parentNode == null) {
			// possibly dodgy - at least in UiBinder
			return null;
		}
		for (int idx = 0; idx < parentNode.children.size(); idx++) {
			Node_Jvm node = parentNode.children.get(idx);
			if (node == this) {
				seen = true;
			} else {
				if (seen && node.getNodeType() == Node.ELEMENT_NODE) {
					return (Element) node.nodeFor();
				}
			}
		}
		return null;
	}

	@Override
	public final Element getPreviousSiblingElement() {
		boolean seen = false;
		for (int idx = parentNode.children.size() - 1; idx >= 0; idx--) {
			Node_Jvm node = parentNode.children.get(idx);
			if (node == this) {
				seen = true;
			} else {
				if (seen && node.getNodeType() == Node.ELEMENT_NODE) {
					return (Element) node.nodeFor();
				}
			}
		}
		return null;
	}

	@Override
	public final Integer indexInParentChildren() {
		return parentNode.children.indexOf(this);
	}

	@Override
	public String toString() {
		return super.toString() + "\n\t" + getTagName();
	}

	public Node_Jso provideAncestorDomImpl() {
		Element domAncestor = ((Element) node)
				.provideAncestorElementAttachedToDom();
		return domAncestor == null ? null : domAncestor.domImpl;
	}
}
