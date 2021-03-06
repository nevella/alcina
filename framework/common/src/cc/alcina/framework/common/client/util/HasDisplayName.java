package cc.alcina.framework.common.client.util;

import com.totsp.gwittir.client.ui.Renderer;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;

public interface HasDisplayName {
	public static String displayName(Object o) {
		return displayName(o, "(Undefined)");
	}

	public static String displayName(Object o, String placeholderText) {
		if (o == null) {
			return placeholderText;
		}
		return (o instanceof HasDisplayName)
				? ((HasDisplayName) o).displayName()
				: (o instanceof Enum) ? Ax.friendly(o) : o.toString();
	}

	public String displayName();

	@ClientInstantiable
	public static class HasDisplayNameRenderer
			implements Renderer<HasDisplayName, String> {
		public static final HasDisplayNameRenderer INSTANCE = new HasDisplayNameRenderer();

		@Override
		public String render(HasDisplayName o) {
			return o == null ? "" : o.displayName();
		}
	}

	@ClientInstantiable
	public static class HasDisplayNameRendererNull
			implements Renderer<HasDisplayName, String> {
		public static final HasDisplayNameRenderer INSTANCE = new HasDisplayNameRenderer();

		@Override
		public String render(HasDisplayName o) {
			return o == null ? "-----" : o.displayName();
		}
	}

	public static interface Settable extends HasDisplayName {
		default void putDisplayName(String name) {
			throw new UnsupportedOperationException();
		}
	}
}
