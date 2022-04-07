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
		return (o instanceof HasDisplayName)
				? ((HasDisplayName) o).displayName()
				: (o instanceof Enum) ? Ax.friendly(o) : o.toString();
	}

	public String displayName();

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

	public static interface Settable extends HasDisplayName {
		default void putDisplayName(String name) {
			throw new UnsupportedOperationException();
		}
	}
}
