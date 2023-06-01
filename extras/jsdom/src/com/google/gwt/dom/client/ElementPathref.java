package com.google.gwt.dom.client;

import java.util.Map;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.safehtml.shared.SafeHtml;

public class ElementPathref extends NodePathref implements ClientDomElement {
	ElementPathref(Node node) {
		super(node);
	}

	@Override
	public final boolean addClassName(String className) {
		return false;
	}

	@Override
	public void blur() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Node cloneNode(boolean deep) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void dispatchEvent(NativeEvent evt) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Element elementFor() {
		throw new UnsupportedOperationException();
	}

	public void emitSinkBitlessEvent(String eventTypeName) {
		getOwnerDocument().implAccess().pathrefRemote()
				.emitSinkBitlessEvent(this, eventTypeName);
	}

	public void emitSinkEvents(int eventBits) {
		getOwnerDocument().implAccess().pathrefRemote().emitSinkEvents(this,
				eventBits);
	}

	@Override
	public void ensureId() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void focus() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getAbsoluteBottom() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getAbsoluteLeft() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getAbsoluteRight() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getAbsoluteTop() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getAttribute(String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Map<String, String> getAttributeMap() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getClassName() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getClientHeight() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getClientWidth() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getDir() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getDraggable() {
		throw new UnsupportedOperationException();
	}

	@Override
	public NodeList<Element> getElementsByTagName(String name) {
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

	@Override
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

	@Override
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

	@Override
	public int getScrollHeight() {
		throw new UnsupportedOperationException();
	}

	@Override
	public final int getScrollLeft() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getScrollTop() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getScrollWidth() {
		throw new UnsupportedOperationException();
	}

	@Override
	public final String getString() {
		throw new UnsupportedOperationException();
	}

	@Override
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

	@Override
	public int indexInParentChildren() {
		// TODO Auto-generated method stub
		return 0;
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

	@Override
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

	@Override
	public void setLang(String lang) {
	}

	@Override
	public void setNodeValue(String nodeValue) {
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

	@Override
	public void setScrollTop(int scrollTop) {
	}

	@Override
	public void setTabIndex(int tabIndex) {
	}

	@Override
	public void setTitle(String title) {
	}

	@Override
	public void sinkBitlessEvent(String eventTypeName) {
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

	int orSunkEventsOfAllChildren(int sunk) {
		throw new UnsupportedOperationException();
	}
}
