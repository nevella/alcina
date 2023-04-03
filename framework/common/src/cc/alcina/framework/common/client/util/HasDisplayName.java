package cc.alcina.framework.common.client.util;

import java.util.Comparator;

import com.totsp.gwittir.client.ui.Renderer;

import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.util.CommonUtils.ComparatorResult;

public interface HasDisplayName {
	public static String displayName(Object o) {
		return displayName(o, "(Undefined)");
	}

	public static String displayName(Object o, String placeholderText) {
		if (o == null) {
			return placeholderText;
		}
		if (o instanceof HasDisplayName) {
			return ((HasDisplayName) o).displayName();
		}
		if (o instanceof Enum) {
			return Ax.friendly(o);
		}
		String toString = o.toString();
		if (toString.contains("@")
				&& toString.startsWith(o.getClass().getName())) {
			return Ax.format("[%s]", o.getClass().getSimpleName());
		} else {
			return toString;
		}
	}

	public String displayName();

	/*
	 * Used for registration lookup (e.g. display name of modeleventclasses)
	 */
	@Reflected
	public static abstract class ClassDisplayName implements HasDisplayName {
	}

	public static class HasDisplayNameComparator
			implements Comparator<HasDisplayName> {
		@Override
		public int compare(HasDisplayName o1, HasDisplayName o2) {
			ComparatorResult result = CommonUtils.compareNullCheck(o1, o2);
			if (result != ComparatorResult.BOTH_NON_NULL) {
				return result.direction();
			}
			String name1 = o1.displayName();
			String name2 = o2.displayName();
			result = CommonUtils.compareNullCheck(name1, name2);
			if (result != ComparatorResult.BOTH_NON_NULL) {
				return result.direction();
			}
			return name1.compareTo(name2);
		}
	}

	@Reflected
	public static class HasDisplayNameRenderer
			implements Renderer<HasDisplayName, String> {
		public static final HasDisplayNameRenderer INSTANCE = new HasDisplayNameRenderer();

		@Override
		public String render(HasDisplayName o) {
			return o == null ? "" : o.displayName();
		}
	}

	@Reflected
	public static class HasDisplayNameRendererNull
			implements Renderer<HasDisplayName, String> {
		public static final HasDisplayNameRenderer INSTANCE = new HasDisplayNameRenderer();

		@Override
		public String render(HasDisplayName o) {
			return o == null ? "-----" : o.displayName();
		}
	}

	@Reflected
	public static class PreferDisplayNameRenderer
			implements Renderer<Object, String> {
		public static final PreferDisplayNameRenderer INSTANCE = new PreferDisplayNameRenderer();

		@Override
		public String render(Object o) {
			return o == null ? "" : displayName(o);
		}
	}

	public static interface Settable extends HasDisplayName {
		default void putDisplayName(String name) {
			throw new UnsupportedOperationException();
		}
	}
}
