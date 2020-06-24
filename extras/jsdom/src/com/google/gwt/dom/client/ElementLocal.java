package com.google.gwt.dom.client;

import java.util.Map;
import java.util.Optional;

import com.google.common.base.Preconditions;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.safehtml.shared.SafeHtml;

import cc.alcina.framework.common.client.logic.domaintransform.lookup.LightMap;
import cc.alcina.framework.common.client.util.Ax;

public class ElementLocal extends NodeLocal
		implements DomElement, LocalDomElement {
	static int _idCounter;

	private String tagName;

	int eventBits;

	private Element element;

	protected LightMap<String, String> attributes = new LightMap<>();

	boolean requiresSync;

	boolean hasUnparsedStyle;

	ElementLocal(DocumentLocal document_Jvm, String tagName) {
		ownerDocument = document_Jvm;
		this.tagName = tagName;
		if (!GWT.isScript()) {
			Preconditions.checkArgument(tagName.matches("[A-Za-z0-9\\-]+"));
		}
	}

	@Override
	public final boolean addClassName(String className) {
		return DomElementStatic.addClassName(this, className);
	}

	@Override
	public void blur() {
		DomElementStatic.blur(this);
	}

	public void clearChildrenAndAttributes0() {
		getChildren().clear();
		attributes.clear();
	}

	@Override
	public Node cloneNode(boolean deep) {
		ElementLocal cloneLocal = new ElementLocal(ownerDocument, tagName);
		Element clone = LocalDom.createElement(tagName).putLocal(cloneLocal);
		clone.cloneLocalStyle(element);
		cloneLocal.attributes.putAll(attributes);
		cloneLocal.eventBits = eventBits;
		if (deep) {
			getChildNodes().stream()
					.forEach(cn -> clone.appendChild(cn.cloneNode(true)));
		}
		return clone;
	}

	@Override
	public Element createOrReturnChild(String tagName) {
		Optional<Node> optional = nodeFor().getChildNodes().stream()
				.filter(n -> n.getNodeName().equals(tagName)).findFirst();
		if (optional.isPresent()) {
			return (Element) optional.get();
		}
		Element newElement = nodeFor().getOwnerDocument()
				.createElement(tagName);
		nodeFor().appendChild(newElement);
		return newElement;
	}

	@Override
	public void dispatchEvent(NativeEvent evt) {
		DomElementStatic.dispatchEvent(this, evt);
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

	@Override
	public void focus() {
		DomElementStatic.focus(this);
	}

	@Override
	public int getAbsoluteBottom() {
		return DomElementStatic.getAbsoluteBottom(this);
	}

	@Override
	public int getAbsoluteLeft() {
		return DomElementStatic.getAbsoluteLeft(this);
	}

	@Override
	public int getAbsoluteRight() {
		return DomElementStatic.getAbsoluteRight(this);
	}

	@Override
	public int getAbsoluteTop() {
		return DomElementStatic.getAbsoluteTop(this);
	}

	@Override
	public String getAttribute(String name) {
		String value = attributes.get(name);
		return value != null ? value : "";
	}

	@Override
	public Map<String, String> getAttributeMap() {
		return attributes;
	}

	@Override
	public String getClassName() {
		return getAttribute("class");
	}

	@Override
	public int getClientHeight() {
		return DomElementStatic.getClientHeight(this);
	}

	@Override
	public int getClientWidth() {
		return DomElementStatic.getClientWidth(this);
	}

	@Override
	public String getDir() {
		return DomElementStatic.getDir(this);
	}

	@Override
	public String getDraggable() {
		return DomElementStatic.getDraggable(this);
	}

	@Override
	public NodeList<Element> getElementsByTagName(String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getEventBits() {
		return this.eventBits;
	}

	@Override
	public final Element getFirstChildElement() {
		return getChildNodes().stream().filter(
				nodeLocal -> nodeLocal.getNodeType() == Node.ELEMENT_NODE)
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
		UnsafeHtmlBuilder builder = new UnsafeHtmlBuilder();
		appendChildContents(builder);
		return builder.toSafeHtml().asString();
	}

	@Override
	public final String getInnerText() {
		StringBuilder builder = new StringBuilder();
		appendTextContent(builder);
		return builder.toString();
	}

	@Override
	public String getLang() {
		return DomElementStatic.getLang(this);
	}

	@Override
	public final Element getNextSiblingElement() {
		boolean seen = false;
		if (parentNode == null) {
			// possibly dodgy - at least in UiBinder
			return null;
		}
		for (int idx = 0; idx < parentNode.getChildren().size(); idx++) {
			NodeLocal node = parentNode.getChildren().get(idx);
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

	@Override
	public int getOffsetHeight() {
		return DomElementStatic.getOffsetHeight(this);
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
	public String getOuterHtml() {
		UnsafeHtmlBuilder builder = new UnsafeHtmlBuilder();
		appendOuterHtml(builder);
		return builder.toSafeHtml().asString();
	}

	@Override
	public final Element getPreviousSiblingElement() {
		boolean seen = false;
		for (int idx = parentNode.getChildren().size() - 1; idx >= 0; idx--) {
			NodeLocal node = parentNode.getChildren().get(idx);
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

	@Override
	public int getScrollHeight() {
		return DomElementStatic.getScrollHeight(this);
	}

	@Override
	public final int getScrollLeft() {
		return DomElementStatic.getScrollLeft(this);
	}

	@Override
	public int getScrollTop() {
		return DomElementStatic.getScrollTop(this);
	}

	@Override
	public int getScrollWidth() {
		return DomElementStatic.getScrollWidth(this);
	}

	@Override
	public final String getString() {
		return DomElementStatic.getString(this);
	}

	@Override
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
		return attributes.containsKey(name);
	}

	@Override
	public final boolean hasClassName(String className) {
		return DomElementStatic.hasClassName(this, className);
	}

	@Override
	public final boolean hasTagName(String tagName) {
		return DomElementStatic.hasTagName(this, tagName);
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
		return DomElementStatic.removeClassName(this, className);
	}

	@Override
	public final void replaceClassName(String oldClassName,
			String newClassName) {
		DomElementStatic.replaceClassName(this, oldClassName, newClassName);
	}

	@Override
	public final void scrollIntoView() {
		DomElementStatic.scrollIntoView(this);
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
	public void setDir(String dir) {
		DomElementStatic.setDir(this, dir);
	}

	@Override
	public final void setDraggable(String draggable) {
		DomElementStatic.setDraggable(this, draggable);
	}

	@Override
	public void setId(String id) {
		setAttribute("id", id);
	}

	@Override
	public void setInnerHTML(String html) {
		if (Ax.notBlank(html)) {
			if (!html.contains("<")) {
				appendChild(ownerDocument
						.createTextNode(HtmlParser.decodeEntities(html)));
			} else {
				getChildren().clear();
				String outerHtml = getOuterHtml();
				StringBuilder builder = new StringBuilder();
				int idx = outerHtml.indexOf("</");
				builder.append(outerHtml.substring(0, idx));
				builder.append(html);
				builder.append(outerHtml.substring(idx));
				try {
					new HtmlParser().parse(builder.toString(), element, false);
				} catch (Exception e) {
					html = LocalDom.safeParseByBrowser(html);
					builder = new StringBuilder();
					idx = outerHtml.indexOf("</");
					builder.append(outerHtml.substring(0, idx));
					builder.append(html);
					builder.append(outerHtml.substring(idx));
					new HtmlParser().parse(builder.toString(), element, false);
				}
			}
		}
	}

	@Override
	public final void setInnerSafeHtml(SafeHtml html) {
		DomElementStatic.setInnerSafeHtml(this, html);
	}

	@Override
	public void setInnerText(String text) {
		if (Ax.isBlank(text)) {
		} else {
			getChildren().clear();
			appendChild(ownerDocument.createTextNode(text));
		}
	}

	@Override
	public void setLang(String lang) {
		DomElementStatic.setLang(this, lang);
	}

	@Override
	public void setNodeValue(String nodeValue) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setOuterHtml(String html) {
		RegExp tag = RegExp.compile("<([A-Za-z0-9_\\-.]+)( .+?)?>(.+)?</.+>",
				"m");
		RegExp tagNoContents = RegExp.compile("<([A-Za-z0-9_\\-.]+)( .+?)?/?>",
				"m");
		MatchResult matchResult = tag.exec(html);
		if (matchResult == null) {
			matchResult = tagNoContents.exec(html);
		}
		String attrString = matchResult.getGroup(2);
		if (attrString != null) {
			char valueDelimiter = '-';
			AttrParseState state = AttrParseState.START;
			StringBuilder nameBuilder = null;
			StringBuilder valueBuilder = null;
			for (int idx = 0; idx < attrString.length(); idx++) {
				char c = attrString.charAt(idx);
				if (c == ' ') {
					switch (state) {
					case VALUE:
						break;
					default:
						continue;
					}
				}
				switch (state) {
				case START:
					state = AttrParseState.NAME;
					nameBuilder = new StringBuilder();
					nameBuilder.append(c);
					break;
				case NAME:
					if (c == '=') {
						state = AttrParseState.EQ;
					} else {
						nameBuilder.append(c);
					}
					break;
				case EQ:
					if (c == '\'' || c == '"') {
						valueBuilder = new StringBuilder();
						state = AttrParseState.VALUE;
						valueDelimiter = c;
					}
					break;
				case VALUE:
					if (c == valueDelimiter) {
						setAttribute(nameBuilder.toString(),
								valueBuilder.toString());
						state = AttrParseState.START;
					} else {
						valueBuilder.append(c);
					}
					break;
				}
			}
		}
		if (matchResult.getGroupCount() == 4) {
			setInnerHTML(matchResult.getGroup(3));
		}
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
		throw new UnsupportedOperationException();
	}

	@Override
	public void setScrollTop(int scrollTop) {
		throw new UnsupportedOperationException();
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
		DomElementStatic.toggleClassName(this, className);
	}

	@Override
	public String toString() {
		return super.toString() + "\n\t" + getTagName();
	}

	private void appendChildContents(UnsafeHtmlBuilder builder) {
		if (containsUnescapedText()) {
			getChildren().stream().forEach(
					node -> ((TextLocal) node).appendUnescaped(builder));
		} else {
			getChildren().stream()
					.forEach(child -> child.appendOuterHtml(builder));
		}
	}

	private boolean containsUnescapedText() {
		if (tagName.equalsIgnoreCase("style")
				|| tagName.equalsIgnoreCase("script")) {
			Preconditions.checkState(getChildren().stream()
					.allMatch(c -> c.getNodeType() == Node.TEXT_NODE));
			return true;
		} else {
			return false;
		}
	}

	@Override
	void appendOuterHtml(UnsafeHtmlBuilder builder) {
		if (eventBits != 0) {
			ensureId();
		}
		builder.appendHtmlConstantNoCheck("<");
		builder.appendHtmlConstant(tagName);
		String styleAttributeValue = attributes.get("style");
		if (!attributes.isEmpty()) {
			boolean applyStyleAttribute = !element.hasStyle()
					|| hasUnparsedStyle && getStyle().local.isEmpty();
			attributes.entrySet().forEach(e -> {
				// ignore if we have a valid style object
				if (e.getKey().equals("style") && !applyStyleAttribute) {
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
		if (element.getStyle() != null
				&& !element.getStyle().local().isEmpty()) {
			builder.appendHtmlConstantNoCheck(" style=\"");
			if (Ax.notBlank(styleAttributeValue)) {
				builder.appendUnsafeHtml(styleAttributeValue);
				builder.appendHtmlConstantNoCheck("; ");
			}
			((StyleLocal) element.getStyle().local()).properties.entrySet()
					.forEach(e -> {
						builder.appendEscaped(
								LocalDom.declarativeCssName(e.getKey()));
						builder.appendHtmlConstantNoCheck(":");
						builder.appendEscaped(e.getValue());
						builder.appendHtmlConstantNoCheck("; ");
					});
			builder.appendHtmlConstantNoCheck("\"");
		}
		builder.appendHtmlConstantNoCheck(">");
		appendChildContents(builder);
		if (!HtmlParser.isSelfClosingTag(tagName)) {
			builder.appendHtmlConstantNoCheck("</");
			builder.appendHtmlConstant(tagName);
			builder.appendHtmlConstantNoCheck(">");
		}
	}

	@Override
	void appendTextContent(StringBuilder builder) {
		getChildren().stream().forEach(node -> node.appendTextContent(builder));
	}

	int orSunkEventsOfAllChildren(int sunk) {
		for (NodeLocal child : getChildren()) {
			if (child instanceof ElementLocal) {
				sunk = ((ElementLocal) child).orSunkEventsOfAllChildren(sunk);
			}
		}
		sunk |= eventBits;
		return sunk;
	}

	enum AttrParseState {
		START, NAME, EQ, VALUE
	}
}
