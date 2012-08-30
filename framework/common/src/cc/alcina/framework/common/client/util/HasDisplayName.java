package cc.alcina.framework.common.client.util;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;

import com.totsp.gwittir.client.ui.Renderer;

public interface HasDisplayName {
	public String displayName();

	@ClientInstantiable
	public static class HasDisplayNameRenderer implements
			Renderer<HasDisplayName, String> {
		public static final HasDisplayNameRenderer INSTANCE = new HasDisplayNameRenderer();

		@Override
		public String render(HasDisplayName o) {
			return o.displayName();
		}
	}
}
