package cc.alcina.framework.common.client.util;

import com.totsp.gwittir.client.ui.Renderer;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;

public interface HasStringId {
	public String stringId();

	@ClientInstantiable
	public static class HasStringIdRenderer
			implements Renderer<HasStringId, String> {
		public static final HasStringIdRenderer INSTANCE = new HasStringIdRenderer();

		@Override
		public String render(HasStringId o) {
			return o == null ? "" : o.stringId();
		}
	}

	default String nullSafeId(HasStringId hasStringId) {
		return hasStringId == null ? null : hasStringId.stringId();
	}
	static String nullSafeId0(HasStringId hasStringId) {
		return hasStringId == null ? null : hasStringId.stringId();
	}
}
