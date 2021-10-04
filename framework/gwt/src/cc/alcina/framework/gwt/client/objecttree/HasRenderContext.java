package cc.alcina.framework.gwt.client.objecttree;

import java.util.function.Predicate;

import cc.alcina.framework.common.client.util.LooseContextInstance;

public interface HasRenderContext {
	public abstract LooseContextInstance getRenderContext();

	public static class IsHasRenderContext implements Predicate<Object> {
		public static final IsHasRenderContext INSTANCE = new IsHasRenderContext();

		@Override
		public boolean test(Object o) {
			return o instanceof HasRenderContext;
		}
	}
}