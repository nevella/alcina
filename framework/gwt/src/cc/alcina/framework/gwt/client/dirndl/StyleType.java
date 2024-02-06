package cc.alcina.framework.gwt.client.dirndl;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.gwt.client.util.ClassNames;

public interface StyleType {
	default void addTo(UIObject uiObject) {
		uiObject.setStyleName(toName(), true);
	}

	default Widget addTo(Widget widget) {
		widget.setStyleName(toName(), true);
		return widget;
	}

	default boolean hasStyle(UIObject uiObject) {
		String name = toName();
		String current = uiObject.getStyleName();
		if (current.contains(name)) {
			return ClassNames.indexOfName(current, name) != -1;
		} else {
			return false;
		}
	}

	default String prefix() {
		return "";
	}

	default Widget removeFrom(Widget widget) {
		widget.setStyleName(toName(), false);
		return widget;
	}

	default void set(Element element) {
		element.setClassName(toName());
	}

	default void set(UIObject uiObject) {
		uiObject.setStyleName(toName());
	}

	default void set(UIObject uiObject, boolean set) {
		uiObject.setStyleName(toName(), set);
	}

	default String toName() {
		return prefix() + toString().toLowerCase().replace("_", "-");
	}
}