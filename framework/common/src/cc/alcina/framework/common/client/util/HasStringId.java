package cc.alcina.framework.common.client.util;

import com.totsp.gwittir.client.ui.Renderer;

import cc.alcina.framework.common.client.logic.reflection.Reflected;

public interface HasStringId {
	static String nullSafeId0(HasStringId hasStringId) {
		return hasStringId == null ? null : hasStringId.stringId();
	}

	public String stringId();

	default String nullSafeId(HasStringId hasStringId) {
		return hasStringId == null ? null : hasStringId.stringId();
	}

	@Reflected
	public static class HasStringIdRenderer
			implements Renderer<HasStringId, String> {
		public static final HasStringIdRenderer INSTANCE = new HasStringIdRenderer();

		@Override
		public String render(HasStringId o) {
			return o == null ? "" : o.stringId();
		}
	}
}
