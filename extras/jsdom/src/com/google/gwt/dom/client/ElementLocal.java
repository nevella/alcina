package com.google.gwt.dom.client;

import java.util.ArrayList;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.safehtml.shared.SafeHtml;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.StringMap;

public class ElementLocal extends NodeLocal
		implements DomElement, LocalDomElement {
	static int _idCounter;

	private String tagName;

	private String innerHtml;


	int eventBits;

	private Element element;

	ElementLocal(DocumentLocal document_Jvm, String tagName) {
		ownerDocument = document_Jvm;
		this.tagName = tagName;
	}
	protected Map<String, String> attributes = LocalDom.collections()
			.createStringMap();

	@Override
	public final boolean addClassName(String className) {
		return DomElement_Static.addClassName(this, className);
	}

	public void blur() {
		DomElement_Static.blur(this);
	}

	@Override
	public Node cloneNode(boolean deep) {
		ElementLocal cloneLocal = new ElementLocal(ownerDocument, tagName);
		Element clone = new Element().putLocal(cloneLocal);
		clone.cloneLocalStyle(element);
		cloneLocal.attributes = new StringMap(attributes);
		cloneLocal.eventBits = eventBits;
		if (deep) {
			cloneLocal.innerHtml = innerHtml;
			getChildNodes().stream()
					.forEach(cn -> clone.appendChild(cn.cloneNode(true)));
		}
		return clone;
	}

	public void dispatchEvent(NativeEvent evt) {
		DomElement_Static.dispatchEvent(this, evt);
	}

	@Override
	public Element elementFor() {
		return element;
	}

	@Override
	public void ensureId() {
		if (getId().isEmpty()) {
			setId("__localdom__" + (++_idCounter));
		}
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

	@Override
	public String getAttribute(String name) {
		String value = attributes.get(name);
		return value != null ? value : "";
	}

	@Override
	public Map<String, String> getAttributes() {
		return attributes;
	}

	@Override
	public String getClassName() {
		return getAttribute("class");
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

	@Override
	public NodeList<Element> getElementsByTagName(String name) {
		throw new UnsupportedOperationException();
	}

	public int getEventBits() {
		return this.eventBits;
	}

	@Override
	public final Element getFirstChildElement() {
		return getChildNodes().stream()
				.filter(nodeLocal -> nodeLocal
						.getNodeType() == Node.ELEMENT_NODE)
				.findFirst().map(nodeLocal -> (Element) nodeLocal.nodeFor())
				.orElse(null);
	}

	// @Override
	// public final Element getFirstChildElement() {
	// return resolveChildren().stream()
	// .filter(node_jvm -> node_jvm.getNodeType() == Node.ELEMENT_NODE)
	// .findFirst().map(node_jvm -> (Element) node_jvm.nodeFor())
	// .orElse(null);
	// }
	@Override
	public String getId() {
		return getAttribute("id");
	}

	@Override
	public String getInnerHTML() {
		if (children.isEmpty() && innerHtml != null) {
			return innerHtml;
		} else {
			UnsafeHtmlBuilder builder = new UnsafeHtmlBuilder();
			appendChildContents(builder);
			return builder.toSafeHtml().asString();
		}
	}

	@Override
	public final String getInnerText() {
		StringBuilder builder = new StringBuilder();
		appendTextContent(builder);
		return builder.toString();
	}

	public String getLang() {
		return DomElement_Static.getLang(this);
	}

	@Override
	public final Element getNextSiblingElement() {
		boolean seen = false;
		if (parentNode == null) {
			// possibly dodgy - at least in UiBinder
			return null;
		}
		for (int idx = 0; idx < parentNode.children.size(); idx++) {
			NodeLocal node = parentNode.children.get(idx);
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

	public int getOffsetHeight() {
		return DomElement_Static.getOffsetHeight(this);
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

	public String getPendingInnerHtml() {
		return this.innerHtml;
	}

	@Override
	public final Element getPreviousSiblingElement() {
		boolean seen = false;
		for (int idx = parentNode.children.size() - 1; idx >= 0; idx--) {
			NodeLocal node = parentNode.children.get(idx);
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

	public int getScrollHeight() {
		return DomElement_Static.getScrollHeight(this);
	}

	@Override
	public final int getScrollLeft() {
		return DomElement_Static.getScrollLeft(this);
	}

	public int getScrollTop() {
		return DomElement_Static.getScrollTop(this);
	}

	public int getScrollWidth() {
		return DomElement_Static.getScrollWidth(this);
	}

	@Override
	public final String getString() {
		return DomElement_Static.getString(this);
	}

	public Style getStyle() {
		return element.getStyle();
	}

	@Override
	public final int getTabIndex() {
		String index = getAttribute("tabindex");
		return index.isEmpty() ? 0 : Integer.parseInt(index);
	}

	@Override
	public String getTagName() {
		return tagName;
	}

	@Override
	public String getTitle() {
		return getAttribute("title");
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
	public final Integer indexInParentChildren() {
		return parentNode.children.indexOf(this);
	}

	public void putElement(Element element) {
		this.element = element;
		this.node = element;
	}

	@Override
	public void removeAttribute(String name) {
		attributes.remove(name);
	}

	@Override
	public final boolean removeClassName(String className) {
		return DomElement_Static.removeClassName(this, className);
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
	public void setAttribute(String name, String value) {
		attributes.put(name, value);
		//FIXME
//		if (name.equals("id") && value.length() > 0) {
//			LocalDomBridge.registerId(this);
//		}
	}

	@Override
	public void setClassName(String className) {
		setAttribute("class", className);
	}

	public void setDir(String dir) {
		DomElement_Static.setDir(this, dir);
	}

	@Override
	public final void setDraggable(String draggable) {
		DomElement_Static.setDraggable(this, draggable);
	}

	@Override
	public void setId(String id) {
		setAttribute("id", id);
	}

	@Override
	public void setInnerHTML(String html) {
		new ArrayList<>(children).stream().forEach(NodeLocal::removeFromParent);
		this.innerHtml = html;
	}

	@Override
	public final void setInnerSafeHtml(SafeHtml html) {
		DomElement_Static.setInnerSafeHtml(this, html);
	}

	@Override
	public void setInnerText(String text) {
		new ArrayList<>(children).stream().forEach(NodeLocal::removeFromParent);
		innerHtml = null;
		if (Ax.isBlank(text)) {
		} else {
			appendChild(ownerDocument.createTextNode(text));
		}
	}

	public void setLang(String lang) {
		DomElement_Static.setLang(this, lang);
	}

	@Override
	public void setNodeValue(String nodeValue) {
		throw new UnsupportedOperationException();
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
	public final void setScrollLeft(int scrollLeft) {
		DomElement_Static.setScrollLeft(this, scrollLeft);
	}

	public void setScrollTop(int scrollTop) {
		DomElement_Static.setScrollTop(this, scrollTop);
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
	public void sinkEvents(int eventBits) {
		this.eventBits |= eventBits;
	}

	@Override
	public final void toggleClassName(String className) {
		DomElement_Static.toggleClassName(this, className);
	}

	@Override
	public String toString() {
		return super.toString() + "\n\t" + getTagName();
	}


	private void appendChildContents(UnsafeHtmlBuilder builder) {
		if (containsUnescapedText()) {
			children.stream().forEach(
					node -> ((TextLocal) node).appendUnescaped(builder));
		} else {
			children.stream().forEach(child -> child.appendOuterHtml(builder));
		}
	}

	private boolean containsUnescapedText() {
		if (tagName.equalsIgnoreCase("style")
				|| tagName.equalsIgnoreCase("script")) {
			Preconditions.checkState(children.stream()
					.allMatch(c -> c.getNodeType() == Node.TEXT_NODE));
			return true;
		} else {
			return false;
		}
	}

	// private List<NodeLocal> resolveChildren() {
	// if (children.isEmpty() && innerHtml != null) {
	// RegExp tag = RegExp
	// .compile("<([A-Za-z0-9_\\-.]+)( .+?)?>(.+)?</.+>", "m");
	// RegExp tagNoContents = RegExp
	// .compile("<([A-Za-z0-9_\\-.]+)( .+?)?/?>", "m");
	// MatchResult matchResult = tag.exec(innerHtml);
	// if (matchResult == null) {
	// matchResult = tagNoContents.exec(innerHtml);
	// }
	// if (matchResult == null) {
	// if (innerHtml.isEmpty()) {
	// } else {
	// DomText domText = LocalDomBridge
	// .get().localDomImpl.localImpl
	// .createUnwrappedLocalText(
	// getOwnerDocument(), innerHtml);
	// node.appendChild(
	// LocalDom.nodeFor((NodeLocal) domText));
	// }
	// } else {
	// ElementLocal element = (ElementLocal) create(
	// matchResult.getGroup(1));
	// Element created = LocalDom.nodeFor((NodeLocal) element);
	// created.setOuterHtml(innerHtml);
	// node.appendChild(created);
	// }
	// innerHtml = null;
	// }
	// return children;
	// }
	@Override
	void appendOuterHtml(UnsafeHtmlBuilder builder) {
		if (eventBits != 0) {
			ensureId();
		}
		builder.appendHtmlConstantNoCheck("<");
		builder.appendHtmlConstant(tagName);
		String styleAttributeValue = attributes.get("style");
		if (!attributes.isEmpty()) {
			attributes.entrySet().forEach(e -> {
				if (e.getKey().equals("style") && element.hasStyle()) {
					return;
				}
				builder.appendHtmlConstantNoCheck(" ");
				// invalid attr names will die on the voine
				builder.appendEscaped(e.getKey());
				builder.appendHtmlConstantNoCheck("=\"");
				builder.appendEscaped(e.getValue());
				builder.appendHtmlConstantNoCheck("\"");
			});
		}
		if (element.getStyle() != null) {
			builder.appendHtmlConstantNoCheck(" style=\"");
			if (Ax.notBlank(styleAttributeValue)) {
				builder.appendUnsafeHtml(styleAttributeValue);
				builder.appendHtmlConstantNoCheck("; ");
			}
			((StyleLocal) element.getStyle().local()).properties.entrySet()
					.forEach(e -> {
						builder.appendEscaped(LocalDom
								.declarativeCssName(e.getKey()));
						builder.appendHtmlConstantNoCheck(":");
						builder.appendEscaped(e.getValue());
						builder.appendHtmlConstantNoCheck("; ");
					});
			builder.appendHtmlConstantNoCheck("\"");
		}
		builder.appendHtmlConstantNoCheck(">");
		appendChildContents(builder);
		if (innerHtml != null) {
			builder.appendUnsafeHtml(innerHtml);
		}
		builder.appendHtmlConstantNoCheck("</");
		builder.appendHtmlConstant(tagName);
		builder.appendHtmlConstantNoCheck(">");
	}

	@Override
	void appendTextContent(StringBuilder builder) {
		children.stream().forEach(node -> node.appendTextContent(builder));
	}

	int orSunkEventsOfAllChildren(int sunk) {
		for (NodeLocal child : children) {
			if (child instanceof ElementLocal) {
				sunk = ((ElementLocal) child).orSunkEventsOfAllChildren(sunk);
			}
		}
		sunk |= eventBits;
		return sunk;
	}
}
