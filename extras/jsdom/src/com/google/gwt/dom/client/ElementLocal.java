package com.google.gwt.dom.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.w3c.dom.DOMException;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.TypeInfo;

import com.google.common.base.Preconditions;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JavascriptObjectEquivalent;
import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.safehtml.shared.SafeHtml;

import cc.alcina.framework.common.client.logic.domaintransform.lookup.LightMap;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.IntCounter;

// FIXME - dirndl - move all event code from Element to here?
public class ElementLocal extends NodeLocal
		implements ClientDomElement, LocalDomElement {
	static transient IntCounter _idCounter = new IntCounter(1);

	private static final RegExp PERMITTED_TAGS = RegExp
			.compile("[A-Za-z0-9\\-_]+");

	private String tagName;

	int eventBits;

	List<String> bitlessEvents;

	private Element element;

	protected LightMap<String, String> attributes = new LightMap<>();

	boolean requiresSync;

	boolean hasUnparsedStyle;

	ElementLocal(DocumentLocal document_Jvm, String tagName) {
		ownerDocument = document_Jvm;
		this.tagName = tagName;
		if (!GWT.isScript() && GWT.isClient()) {
			// . is legal - but gets very confusing with css, so don't permit
			Preconditions.checkArgument(PERMITTED_TAGS.exec(tagName) != null);
		}
	}

	@Override
	public final boolean addClassName(String className) {
		return ClientDomElementStatic.addClassName(this, className);
	}

	private void appendChildContents(UnsafeHtmlBuilder builder) {
		if (containsUnescapedText()) {
			checkSplitTextNodesForBrowserCompatibility();
			getChildren().stream().forEach(
					node -> ((TextLocal) node).appendUnescaped(builder));
		} else {
			getChildren().stream()
					.forEach(child -> child.appendOuterHtml(builder));
		}
	}

	@Override
	void appendOuterHtml(UnsafeHtmlBuilder builder) {
		if (eventBits != 0) {
			// FIXME - dirndl 1x1e - can probably get the local element very
			// quickly from [remote-index-in-parent] -- so remove (with LDM2)
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

	@Override
	public void blur() {
		ClientDomElementStatic.blur(this);
	}

	private void checkSplitTextNodesForBrowserCompatibility() {
		int maxCharsPerTextNode = LocalDom.getMaxCharsPerTextNode();
		if (getChildren().stream().noneMatch(t -> {
			return t.getNodeValue().length() > maxCharsPerTextNode;
		})) {
			return;
		}
		List<NodeLocal> toReplace = new ArrayList<>(getChildren());
		for (int idx = toReplace.size() - 1; idx >= 0; idx--) {
			element.getChild(idx).removeFromParent();
		}
		List<TextLocal> out = new ArrayList<>();
		// copied from HtmlParser.emitText
		toReplace.forEach(n -> {
			String string = n.getNodeValue();
			int idx = 0;
			int length = string.length();
			while (idx < length) {
				int segmentLength = length - idx;
				segmentLength = Math.min(segmentLength, maxCharsPerTextNode);
				String segment = idx == 0 && segmentLength == length ? string
						: string.substring(idx, idx + segmentLength);
				Text text = Document.get().createTextNode(segment);
				element.appendChild(text);
				idx += segmentLength;
			}
		});
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
		cloneLocal.hasUnparsedStyle = hasUnparsedStyle;
		cloneLocal.attributes.putAll(attributes);
		cloneLocal.eventBits = eventBits;
		if (deep) {
			getChildNodes().stream()
					.forEach(cn -> clone.appendChild(cn.cloneNode(true)));
		}
		return clone;
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
	public Element createOrReturnChild(String tagName) {
		Optional<Node> optional = node().getChildNodes().stream()
				.filter(n -> n.getNodeName().equals(tagName)).findFirst();
		if (optional.isPresent()) {
			return (Element) optional.get();
		}
		Element newElement = node().getOwnerDocument().createElement(tagName);
		node().appendChild(newElement);
		return newElement;
	}

	@Override
	public void dispatchEvent(NativeEvent evt) {
		ClientDomElementStatic.dispatchEvent(this, evt);
	}

	@Override
	public Element elementFor() {
		return element;
	}

	@Override
	public void ensureId() {
		if (getId().isEmpty()) {
			setId("__localdom__" + (_idCounter.nextId()));
		}
	}

	@Override
	public void focus() {
		ClientDomElementStatic.focus(this);
	}

	@Override
	public int getAbsoluteBottom() {
		return ClientDomElementStatic.getAbsoluteBottom(this);
	}

	@Override
	public int getAbsoluteLeft() {
		return ClientDomElementStatic.getAbsoluteLeft(this);
	}

	@Override
	public int getAbsoluteRight() {
		return ClientDomElementStatic.getAbsoluteRight(this);
	}

	@Override
	public int getAbsoluteTop() {
		return ClientDomElementStatic.getAbsoluteTop(this);
	}

	@Override
	public String getAttribute(String name) {
		String value = attributes.get(name);
		return value != null ? value : "";
	}

	@Override
	public LightMap<String, String> getAttributeMap() {
		return attributes;
	}

	Map<String, String> getAttributeMapIncludingStyles() {
		if (element.getStyle() == null
				|| element.getStyle().local().isEmpty()) {
			return this.attributes;
		}
		LightMap<String, String> attributes = this.attributes.clone();
		StringBuilder styleBuilder = new StringBuilder();
		String styleAttributeValue = attributes.get("style");
		if (Ax.notBlank(styleAttributeValue)) {
			styleBuilder.append(styleAttributeValue);
			styleBuilder.append("; ");
		}
		((StyleLocal) element.getStyle().local()).properties.entrySet()
				.forEach(e -> {
					styleBuilder
							.append(LocalDom.declarativeCssName(e.getKey()));
					styleBuilder.append(":");
					styleBuilder.append(e.getValue());
					styleBuilder.append("; ");
				});
		attributes.put("style", styleBuilder.toString());
		return attributes;
	}

	public NamedNodeMap getAttributes() {
		return new AttributeMap();
	}

	@Override
	public String getClassName() {
		return getAttribute("class");
	}

	@Override
	public int getClientHeight() {
		return ClientDomElementStatic.getClientHeight(this);
	}

	@Override
	public int getClientWidth() {
		return ClientDomElementStatic.getClientWidth(this);
	}

	@Override
	public String getDir() {
		return ClientDomElementStatic.getDir(this);
	}

	@Override
	public String getDraggable() {
		return ClientDomElementStatic.getDraggable(this);
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
				.findFirst().map(nodeLocal -> (Element) nodeLocal.node())
				.orElse(null);
	}

	// @Override
	// public final Element getFirstChildElement() {
	// return resolveChildren().stream()
	// .filter(node_jvm -> node_jvm.getNodeType() == Node.ELEMENT_NODE)
	// .findFirst().map(node_jvm -> (Element) node_jvm.node())
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
		return ClientDomElementStatic.getLang(this);
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
					return (Element) node.node();
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
		return null;
	}

	@Override
	public int getOffsetHeight() {
		return ClientDomElementStatic.getOffsetHeight(this);
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
					return (Element) node.node();
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
		return ClientDomElementStatic.getScrollHeight(this);
	}

	@Override
	public final int getScrollLeft() {
		return ClientDomElementStatic.getScrollLeft(this);
	}

	@Override
	public int getScrollTop() {
		return ClientDomElementStatic.getScrollTop(this);
	}

	@Override
	public int getScrollWidth() {
		return ClientDomElementStatic.getScrollWidth(this);
	}

	@Override
	public final String getString() {
		return ClientDomElementStatic.getString(this);
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
		return ClientDomElementStatic.hasClassName(this, className);
	}

	@Override
	public final boolean hasTagName(String tagName) {
		return ClientDomElementStatic.hasTagName(this, tagName);
	}

	@Override
	public Node node() {
		return element;
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

	void orSunkBitlessEventsOfAllChildren(Set<String> sunk) {
		for (NodeLocal child : getChildren()) {
			if (child instanceof ElementLocal) {
				((ElementLocal) child).orSunkBitlessEventsOfAllChildren(sunk);
			}
		}
		if (tagName.equals("input")) {
			int debug = 3;
		}
		if (bitlessEvents != null) {
			sunk.addAll(bitlessEvents);
		}
	}

	public void putElement(Element element) {
		this.element = element;
	}

	/*
	 * FIXME - pathref - Rarely used - possibly never (with ids)
	 */
	public ClientDomNode queryRelativePath(Pathref path) {
		ClientDomNode cursor = this;
		for (Integer childOrdinal : path.childOrdinals()) {
			cursor = cursor.getChild(childOrdinal);
		}
		return cursor;
	}

	@Override
	public void removeAttribute(String name) {
		attributes.remove(name);
	}

	@Override
	public final boolean removeClassName(String className) {
		return ClientDomElementStatic.removeClassName(this, className);
	}

	@Override
	public final void replaceClassName(String oldClassName,
			String newClassName) {
		ClientDomElementStatic.replaceClassName(this, oldClassName,
				newClassName);
	}

	@Override
	public final void scrollIntoView() {
		ClientDomElementStatic.scrollIntoView(this);
	}

	@Override
	public void setAttribute(String name, String value) {
		attributes.put(name, value);
	}

	@Override
	public void setClassName(String className) {
		if (Ax.isBlank(className)) {
			removeAttribute("class");
		} else {
			setAttribute("class", className);
		}
	}

	@Override
	public void setDir(String dir) {
		ClientDomElementStatic.setDir(this, dir);
	}

	@Override
	public final void setDraggable(String draggable) {
		ClientDomElementStatic.setDraggable(this, draggable);
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
					new HtmlParser().parse(builder.toString(), element,
							hasTagName("html"));
				} catch (Exception e) {
					if (Document.get().remote instanceof NodeJso) {
						html = LocalDom.safeParseByBrowser(html);
					} else {
						html = "<div>(Unparseable html)</div>";
					}
					builder = new StringBuilder();
					idx = outerHtml.indexOf("</");
					builder.append(outerHtml.substring(0, idx));
					builder.append(html);
					builder.append(outerHtml.substring(idx));
					new HtmlParser().parse(builder.toString(), element,
							hasTagName("html"));
				}
			}
		}
	}

	@Override
	public final void setInnerSafeHtml(SafeHtml html) {
		ClientDomElementStatic.setInnerSafeHtml(this, html);
	}

	@Override
	public void setInnerText(String text) {
		if (Ax.isBlank(text)) {
		} else {
			getChildren().clear();
			HtmlParser.appendTextNodes(ownerDocument, this, text);
		}
	}

	@Override
	public void setLang(String lang) {
		ClientDomElementStatic.setLang(this, lang);
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
	void setParentNode(NodeLocal local) {
		super.setParentNode(local);
		if (local instanceof ElementLocal) {
			ElementLocal elementLocal = (ElementLocal) local;
			element.setAttached(elementLocal.element.attached);
		} else {
			element.setAttached(false);
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
	public void sinkBitlessEvent(String eventTypeName) {
		if (bitlessEvents == null) {
			bitlessEvents = new ArrayList<>();
		}
		if (!bitlessEvents.contains(eventTypeName)) {
			bitlessEvents.add(eventTypeName);
		}
	}

	@Override
	public void sinkEvents(int eventBits) {
		this.eventBits |= eventBits;
	}

	@Override
	public final void toggleClassName(String className) {
		ClientDomElementStatic.toggleClassName(this, className);
	}

	@Override
	public String toString() {
		FormatBuilder format = new FormatBuilder();
		format.append("[local]: ");
		format.append(getTagName());
		if (Ax.notBlank(getClassName())) {
			format.format(".%s", getClassName());
		}
		List<Entry<String, String>> attrs = getAttributeMap().entrySet()
				.stream().filter(e -> !Objects.equals("class", e.getKey()))
				.collect(Collectors.toList());
		if (attrs.size() > 0) {
			format.format(" %s", attrs);
		}
		return format.toString();
	}

	class AttributeMap implements NamedNodeMap {
		@Override
		public int getLength() {
			return attributes.size();
		}

		@Override
		public org.w3c.dom.Node getNamedItem(String name) {
			return new AttrImpl(attributes.entrySet().stream()
					.filter(e -> e.getKey().equals(name)).findFirst()
					.orElse(null));
		}

		@Override
		public org.w3c.dom.Node getNamedItemNS(String namespaceURI,
				String localName) throws DOMException {
			throw new UnsupportedOperationException();
		}

		@Override
		public org.w3c.dom.Node item(int index) {
			List<String> keys = getAttributeMap().keySet().stream()
					.collect(Collectors.toList());
			return getNamedItem(keys.get(index));
		}

		@Override
		public org.w3c.dom.Node removeNamedItem(String name)
				throws DOMException {
			org.w3c.dom.Node existing = getNamedItem(name);
			attributes.remove(name);
			return existing;
		}

		@Override
		public org.w3c.dom.Node removeNamedItemNS(String namespaceURI,
				String localName) throws DOMException {
			throw new UnsupportedOperationException();
		}

		@Override
		public org.w3c.dom.Node setNamedItem(org.w3c.dom.Node arg)
				throws DOMException {
			Preconditions.checkArgument(arg instanceof org.w3c.dom.Attr);
			attributes.put(arg.getNodeName(), arg.getNodeValue());
			return getNamedItem(arg.getNodeName());
		}

		@Override
		public org.w3c.dom.Node setNamedItemNS(org.w3c.dom.Node arg)
				throws DOMException {
			throw new UnsupportedOperationException();
		}
	}

	static class AttrImpl extends Node implements org.w3c.dom.Attr {
		Entry<String, String> entry;

		AttrImpl(Map.Entry<String, String> entry) {
			this.entry = entry;
		}

		@Override
		public <T extends JavascriptObjectEquivalent> T cast() {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getName() {
			return entry.getKey();
		}

		@Override
		public org.w3c.dom.Element getOwnerElement() {
			throw new UnsupportedOperationException();
		}

		@Override
		public TypeInfo getSchemaTypeInfo() {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean getSpecified() {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getTextContent() throws DOMException {
			return getValue();
		}

		@Override
		public String getValue() {
			return entry.getValue();
		}

		@Override
		public boolean isId() {
			throw new UnsupportedOperationException();
		}

		@Override
		protected NodeJso jsoRemote() {
			throw new UnsupportedOperationException();
		}

		@Override
		protected boolean linkedToRemote() {
			throw new UnsupportedOperationException();
		}

		@Override
		protected <T extends NodeLocal> T local() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Node node() {
			throw new UnsupportedOperationException();
		}

		@Override
		protected void putRemote(ClientDomNode remote, boolean synced) {
			throw new UnsupportedOperationException();
		}

		@Override
		protected <T extends ClientDomNode> T remote() {
			throw new UnsupportedOperationException();
		}

		@Override
		protected void resetRemote0() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setValue(String value) throws DOMException {
			entry.setValue(value);
		}
	}

	enum AttrParseState {
		START, NAME, EQ, VALUE
	}
}
