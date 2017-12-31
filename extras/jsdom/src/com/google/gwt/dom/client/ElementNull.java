package com.google.gwt.dom.client;

import java.util.Map;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.safehtml.shared.SafeHtml;

public class ElementNull extends NodeLocalNull
		implements DomElement, LocalDomElement {
	static final ElementNull INSTANCE = new ElementNull();

	private ElementNull() {
	}

	@Override
	public final boolean addClassName(String className) {
		return false;
	}

	public void blur() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Node cloneNode(boolean deep) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Element createOrReturnChild(String tagName) {
		throw new UnsupportedOperationException();
	}

	public void dispatchEvent(NativeEvent evt) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Element elementFor() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void ensureId() {
		throw new UnsupportedOperationException();
	}

	public void focus() {
		throw new UnsupportedOperationException();
	}

	public int getAbsoluteBottom() {
		throw new UnsupportedOperationException();
	}

	public int getAbsoluteLeft() {
		throw new UnsupportedOperationException();
	}

	public int getAbsoluteRight() {
		throw new UnsupportedOperationException();
	}

	public int getAbsoluteTop() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getAttribute(String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Map<String, String> getAttributes() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getClassName() {
		throw new UnsupportedOperationException();
	}

	public int getClientHeight() {
		throw new UnsupportedOperationException();
	}

	public int getClientWidth() {
		throw new UnsupportedOperationException();
	}

	public String getDir() {
		throw new UnsupportedOperationException();
	}

	public String getDraggable() {
		throw new UnsupportedOperationException();
	}

	@Override
	public NodeList<Element> getElementsByTagName(String name) {
		throw new UnsupportedOperationException();
	}

	public int getEventBits() {
		throw new UnsupportedOperationException();
	}

	@Override
	public final Element getFirstChildElement() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getId() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getInnerHTML() {
		throw new UnsupportedOperationException();
	}

	@Override
	public final String getInnerText() {
		throw new UnsupportedOperationException();
	}

	public String getLang() {
		throw new UnsupportedOperationException();
	}

	@Override
	public final Element getNextSiblingElement() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getNodeName() {
		throw new UnsupportedOperationException();
	}

	@Override
	public short getNodeType() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getNodeValue() {
		throw new UnsupportedOperationException();
	}

	public int getOffsetHeight() {
		throw new UnsupportedOperationException();
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
		throw new UnsupportedOperationException();
	}

	@Override
	public final Element getPreviousSiblingElement() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean getPropertyBoolean(String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public double getPropertyDouble(String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getPropertyInt(String name) {
		throw new UnsupportedOperationException();
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
		throw new UnsupportedOperationException();
	}

	public int getScrollHeight() {
		throw new UnsupportedOperationException();
	}

	@Override
	public final int getScrollLeft() {
		throw new UnsupportedOperationException();
	}

	public int getScrollTop() {
		throw new UnsupportedOperationException();
	}

	public int getScrollWidth() {
		throw new UnsupportedOperationException();
	}

	@Override
	public final String getString() {
		throw new UnsupportedOperationException();
	}

	public Style getStyle() {
		throw new UnsupportedOperationException();
	}

	@Override
	public final int getTabIndex() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getTagName() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getTitle() {
		throw new UnsupportedOperationException();
	}

	@Override
	public final boolean hasAttribute(String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public final boolean hasClassName(String className) {
		throw new UnsupportedOperationException();
	}

	@Override
	public final boolean hasTagName(String tagName) {
		throw new UnsupportedOperationException();
	}

	public void putElement(Element element) {
	}

	@Override
	public void removeAttribute(String name) {
	}

	@Override
	public final boolean removeClassName(String className) {
		return false;
	}

	@Override
	public final void replaceClassName(String oldClassName,
			String newClassName) {
	}

	@Override
	public final void scrollIntoView() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setAttribute(String name, String value) {
	}

	@Override
	public void setClassName(String className) {
	}

	public void setDir(String dir) {
	}

	@Override
	public final void setDraggable(String draggable) {
	}

	@Override
	public void setId(String id) {
	}

	@Override
	public void setInnerHTML(String html) {
	}

	@Override
	public final void setInnerSafeHtml(SafeHtml html) {
	}

	@Override
	public void setInnerText(String text) {
	}

	public void setLang(String lang) {
	}

	@Override
	public void setNodeValue(String nodeValue) {
	}

	@Override
	public void setOuterHtml(String html) {
	}

	@Override
	public void setPropertyBoolean(String name, boolean value) {
	}

	@Override
	public void setPropertyDouble(String name, double value) {
	}

	@Override
	public void setPropertyInt(String name, int value) {
	}

	@Override
	public void setPropertyJSO(String name, JavaScriptObject value) {
	}

	@Override
	public void setPropertyObject(String name, Object value) {
	}

	@Override
	public void setPropertyString(String name, String value) {
	}

	@Override
	public final void setScrollLeft(int scrollLeft) {
	}

	public void setScrollTop(int scrollTop) {
	}

	@Override
	public void setTabIndex(int tabIndex) {
	}

	@Override
	public void setTitle(String title) {
	}

	@Override
	public void sinkEvents(int eventBits) {
	}

	@Override
	public final void toggleClassName(String className) {
	}

	@Override
	public String toString() {
		return super.toString() + "\n\t" + getTagName();
	}

	@Override
	void appendOuterHtml(UnsafeHtmlBuilder builder) {
		throw new UnsupportedOperationException();
	}

	@Override
	void appendTextContent(StringBuilder builder) {
		throw new UnsupportedOperationException();
	}

	int orSunkEventsOfAllChildren(int sunk) {
		throw new UnsupportedOperationException();
	}
}
