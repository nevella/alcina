package com.google.gwt.dom.client;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.behavior.ElementBehavior;
import com.google.gwt.dom.client.mutations.MutationNode;
import com.google.gwt.dom.client.mutations.MutationRecord;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.Window;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.IntPair;

/*
 * Currently an outlier in this is support for the 'value' property - which
 * provides a lot of basic editing support with little kit.
 *
 * That support could be generalised.
 */
public class ElementAttachId extends NodeAttachId implements ElementRemote {
	/*
	 * remote value caches - could be converted to an on-demand map (less memory
	 * pressure)
	 */
	String value;

	Integer selectedIndex = -1;

	ElementAttachId(Node node) {
		super(node);
	}

	@Override
	public final boolean addClassName(String className) {
		mirrorClassName();
		return false;
	}

	@Override
	public void blur() {
		invokeAsync("blur");
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
		getOwnerDocument().attachIdRemote().emitSinkBitlessEvent(elementFor(),
				eventTypeName);
	}

	public void emitSinkEvents(int eventBits) {
		getOwnerDocument().attachIdRemote().emitSinkEvents(elementFor(),
				eventBits);
	}

	@Override
	public void focus() {
		invokeAsync("focus");
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
	public short getNodeType() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getNodeValue() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getOffsetHeight() {
		// FIXME - zoom - see
		// https://stackoverflow.com/questions/43537559/javascript-getboundingclientrect-vs-offsetheight-while-calculate-element-heigh
		return (int) getBoundingClientRect().height;
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
		String propertyString = getPropertyString(name);
		return Ax.isBlank(propertyString) ? -1
				: Integer.parseInt(propertyString);
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
		switch (name) {
		case "value": {
			/*
			 * the cached value field is updated by onInput events, so if value
			 * is null the value will be the original (local dom tree) value
			 */
			if (value == null) {
				Element elem = elementFor();
				switch (elem.getNodeName()) {
				case "input":
					value = elem.getAttribute("value");
					break;
				case "textarea":
					value = elem.getInnerText();
					break;
				default:
					throw new UnsupportedOperationException();
				}
			}
			return value;
		}
		case "selectedIndex": {
			return selectedIndex == null ? null : String.valueOf(selectedIndex);
		}
		default: {
			return getAttribute(name);
		}
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
		return invokeSync("getScrollTop");
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
		return ((Element) node).getTagName();
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
		throw new UnsupportedOperationException();
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
		invokeAsync("scrollIntoView", List.of(), List.of());
	}

	@Override
	public final void scrollIntoView(int hPad, int vPad) {
		invokeAsync("scrollIntoView", List.of(int.class, int.class),
				List.of(hPad, vPad));
	}

	@Override
	public void setAttribute(String name, String value) {
		MutationRecord record = new MutationRecord();
		record.type = MutationRecord.Type.attributes;
		record.target = MutationNode.forNode(elementFor());
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
		switch (name) {
		case "value":
		case "inputValue": {
			if (Objects.equals(value, this.value)) {
			} else {
				// local caching to prevent roundtrips (although those do work)
				this.value = value;
				invokeAsync("setPropertyString",
						List.of(String.class, String.class),
						// not list.of - does not allow nulls
						Arrays.asList(name, value));
			}
			break;
		}
		case "selectedIndex": {
			Integer toValue = value == null ? null : Integer.parseInt(value);
			if (Objects.equals(toValue, this.selectedIndex)) {
			} else {
				// local caching to prevent roundtrips (although those do work)
				this.selectedIndex = toValue;
				invokeAsync("setPropertyString",
						List.of(String.class, String.class),
						// not list.of - does not allow nulls
						Arrays.asList(name, value));
			}
			break;
		}
		default: {
			// almost all other properties are attributes
			setAttribute(name, value);
			break;
		}
		}
	}

	@Override
	public final void setScrollLeft(int scrollLeft) {
		invokeAsync("setScrollLeft", List.of(int.class), List.of(scrollLeft));
	}

	@Override
	public void setScrollTop(int scrollTop) {
		invokeAsync("setScrollTop", List.of(int.class), List.of(scrollTop));
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
	public DomRect getBoundingClientRect() {
		return invokeSync("getBoundingClientRect");
	}

	@Override
	public final void toggleClassName(String className) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String toString() {
		return super.toString() + "\n\t" + getTagName();
	}

	@Override
	public void setSelectionRange(int pos, int length) {
		invokeAsync("setSelectionRange", List.of(int.class, int.class),
				List.of(pos, length));
	}

	@Override
	public ClientDomStyle getStyleRemote() {
		return new StyleAttachId(this);
	}

	@Override
	public IntPair getScrollPosition() {
		return invokeSync("getScrollPosition");
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

	@Override
	public String getComputedStyleValue(String key) {
		return invokeSync("getComputedStyleValue", List.of(String.class),
				List.of(key));
	}
}
