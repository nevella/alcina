package cc.alcina.framework.gwt.client.lux;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.UIObject;

public interface LuxStylesType {
	default void add(UIObject uiObject) {
		uiObject.setStyleName(toName(), true);
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
		return toString().toLowerCase().replace("_", "-");
	}
}