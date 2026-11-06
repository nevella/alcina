package cc.alcina.framework.common.client.util;

import java.util.function.Function;

import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;

public interface HasStringId {
	static String nullSafeId0(HasStringId hasStringId) {
		return hasStringId == null ? null : hasStringId.stringId();
	}

	default String nullSafeId(HasStringId hasStringId) {
		return hasStringId == null ? null : hasStringId.stringId();
	}

	public String stringId();

	@Reflected
	public static class HasStringIdRenderer
			implements Function<HasStringId, String> {
		public static final HasStringIdRenderer INSTANCE = new HasStringIdRenderer();

		@Override
		public String apply(HasStringId o) {
			return o == null ? "" : o.stringId();
		}
	}
}
