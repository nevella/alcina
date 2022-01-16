package cc.alcina.framework.gwt.client.gwittir;

import java.util.function.Function;

import cc.alcina.framework.common.client.logic.reflection.Display;
import cc.alcina.framework.common.client.provider.TextProvider;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.util.LooseContext;

public class RenderedProperty {
	public static int orderingHint(Property p) {
		return p.has(Display.class) ? p.annotation(Display.class).orderingHint()
				: 0;
	}

	public static String displayName(Property p) {
		String name = p.has(Display.class) ? p.annotation(Display.class).name()
				: p.getName();
		if (LooseContext.has(TextProvider.CONTEXT_NAME_TRANSLATOR)) {
			name = ((Function<String, String>) LooseContext
					.get(TextProvider.CONTEXT_NAME_TRANSLATOR))
							.apply(p.getName());
		}
		return name;
	}
}