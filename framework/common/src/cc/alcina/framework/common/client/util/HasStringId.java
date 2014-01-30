package cc.alcina.framework.common.client.util;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;

import com.totsp.gwittir.client.ui.Renderer;

public interface HasStringId {
	public String stringId();

	@ClientInstantiable
	public static class HasStringIdRenderer implements
			Renderer<HasStringId, String> {
		public static final HasStringIdRenderer INSTANCE = new HasStringIdRenderer();

		@Override
		public String render(HasStringId o) {
			return o == null ? "" : o.stringId();
		}
	}
}
