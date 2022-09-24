package cc.alcina.framework.gwt.client.gwittir;

import cc.alcina.framework.common.client.logic.reflection.Display;
import cc.alcina.framework.common.client.provider.TextProvider;
import cc.alcina.framework.common.client.reflection.Property;

public class RenderedProperty {
	public static String displayName(Property property) {
		return TextProvider.get().getLabelText(property.getOwningType(), property);
	}

	public static int orderingHint(Property p) {
		return p.has(Display.class) ? p.annotation(Display.class).orderingHint()
				: 0;
	}
}