package com.google.gwt.dom.client;

import java.util.Map;
import java.util.Objects;

import com.google.common.base.Preconditions;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.mutations.MutationNode;
import com.google.gwt.dom.client.mutations.MutationRecord;
import com.google.gwt.dom.client.mutations.MutationRecord.Type;
import com.google.gwt.safehtml.shared.SafeHtml;

import cc.alcina.framework.common.client.util.Ax;

/*
 * Currently an outlier in this is support for the 'value' property - which
 * provides a lot of basic editing support with little kit.
 *
 * That support could be generalised.
 */
public class ElementPathref extends NodePathref implements ClientDomElement {
	String valueProperty;

	ElementPathref(Node node) {
		super(node);
	}

	@Override
	public final boolean addClassName(String className) {
		mirrorClassName();
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
		return (Element) node();
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
		return elementFor().getAttribute(name);
	}

	@Override
	public Map<String, String> getAttributeMap() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getClassName() {
		return elementFor().getClassName();
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
		if (Objects.equals(name, "value")) {
			if (valueProperty != null) {
				return valueProperty;
			} else {
				// FIXME - romcom - probably won't work for <textarea>, etc
				return ((ClientDomElement) node()).getAttribute("value");
			}
		} else {
			throw new UnsupportedOperationException();
		}
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
		setAttribute(name, null);
	}

	@Override
	public final boolean removeClassName(String className) {
		mirrorClassName();
		return false;
	}

	@Override
	public final void replaceClassName(String oldClassName,
			String newClassName) {
		mirrorClassName();
	}

	@Override
	public final void scrollIntoView() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setAttribute(String name, String value) {
		MutationRecord record = new MutationRecord();
		record.type = Type.attributes;
		record.target = MutationNode.pathref(elementFor());
		record.attributeName = name;
		record.newValue = value;
		emitMutation(record);
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
		throw new UnsupportedOperationException();
	}

	@Override
	public final void setDraggable(String draggable) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setId(String id) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setInnerHTML(String html) {
		throw new UnsupportedOperationException();
	}

	@Override
	public final void setInnerSafeHtml(SafeHtml html) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setInnerText(String text) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setLang(String lang) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setNodeValue(String nodeValue) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setPropertyBoolean(String name, boolean value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setPropertyDouble(String name, double value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setPropertyInt(String name, int value) {
		throw new UnsupportedOperationException();
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
		Preconditions.checkArgument(Objects.equals(name, "value"));
		this.valueProperty = value;
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
		throw new UnsupportedOperationException();
	}

	@Override
	public void setTitle(String title) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void sinkBitlessEvent(String eventTypeName) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void sinkEvents(int eventBits) {
		throw new UnsupportedOperationException();
	}

	@Override
	public final void toggleClassName(String className) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String toString() {
		return super.toString() + "\n\t" + getTagName();
	}

	void mirrorClassName() {
		/*
		 * mirror from local, since there's no other copy of the remote value
		 */
		setClassName(getClassName());
	}

	int orSunkEventsOfAllChildren(int sunk) {
		throw new UnsupportedOperationException();
	}
}
