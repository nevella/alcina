package cc.alcina.framework.gwt.client.dirndl.layout;

import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.util.HasDisplayName.HasDisplayNameRenderer;
import cc.alcina.framework.gwt.client.gwittir.renderer.FriendlyEnumRenderer;
import cc.alcina.framework.gwt.client.gwittir.renderer.ShortDateRenderer;

/**
 * A collection of simple functions (really aliases to legacy GWT/Gwittir
 * Renderers) for use as tostring transforms
 */
public class DirectedTransform {
	@Reflected
	public static class FriendlyEnum extends FriendlyEnumRenderer {
	}

	@Reflected
	public static class DisplayName extends HasDisplayNameRenderer {
	}

	@Reflected
	public static class FriendlyEnumNullBlank extends FriendlyEnumRenderer {
		@Override
		public Object apply(Object t) {
			return t == null ? "" : super.apply(t);
		}
	}

	@Reflected
	public static class ShortDate extends ShortDateRenderer {
	}
}
