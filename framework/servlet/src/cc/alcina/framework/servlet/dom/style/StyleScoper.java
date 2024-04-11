package cc.alcina.framework.servlet.dom.style;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.util.Ax;

/**
 * Scopes a style sheet to a given prefix (by prefixing most selectors, and
 * rewriting body, html and *)
 */
public interface StyleScoper {
	String scope(String baseCss, String styleScope);

	@Registration(StyleScoper.class)
	public static class Naive implements StyleScoper {
		@Override
		public String scope(String baseCss, String styleScope) {
			if (Ax.isBlank(baseCss)) {
				return baseCss;
			} else {
				return Ax.format("%s %s", styleScope, baseCss);
			}
		}
	}
}
