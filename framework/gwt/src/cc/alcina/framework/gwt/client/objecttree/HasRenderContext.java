package cc.alcina.framework.gwt.client.objecttree;

import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.util.LooseContextInstance;

public interface HasRenderContext {
	public abstract LooseContextInstance getRenderContext();

	public static class IsHasRenderContext implements CollectionFilter<Object> {
		public static final IsHasRenderContext INSTANCE = new IsHasRenderContext();

		@Override
		public boolean allow(Object o) {
			return o instanceof HasRenderContext;
		}
	}
}