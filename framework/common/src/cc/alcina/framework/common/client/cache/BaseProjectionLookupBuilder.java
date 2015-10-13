package cc.alcina.framework.common.client.cache;

import java.util.Map;
import java.util.function.Supplier;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.MultikeyMap;
import cc.alcina.framework.common.client.util.MultikeyMapBase.DelegateMapCreator;
import cc.alcina.framework.common.client.util.SortedMultikeyMap;
import cc.alcina.framework.common.client.util.UnsortedMultikeyMap;

public class BaseProjectionLookupBuilder {
	private boolean sorted = false;

	private int depth;

	private MapCreator[] creators;

	private boolean navigable;

	public BaseProjectionLookupBuilder sorted() {
		sorted = true;
		return this;
	}

	public BaseProjectionLookupBuilder unsorted() {
		sorted = false;
		return this;
	}

	public BaseProjectionLookupBuilder depth(int depth) {
		this.depth = depth;
		return this;
	}

	public <T> MultikeyMap<T> createMultikeyMap() {
		MultikeyMap<T> map = null;
		BplDelegateMapCreator mapCreator = Registry
				.impl(BplDelegateMapCreator.class);
		mapCreator.setBuilder(this);
		if (isSorted()) {
			map = new SortedMultikeyMap<T>(depth, 0, mapCreator);
		} else {
			map = new UnsortedMultikeyMap<T>(depth, 0, mapCreator);
		}
		return map;
	}

	public BaseProjectionLookupBuilder mapCreators(MapCreator... creators) {
		if (creators.length != depth) {
			throw new RuntimeException(
					"Mismatched creator array length and depth");
		}
		this.creators = creators;
		return this;
	}

	public interface MapCreator extends Supplier<Map> {
	}

	public static abstract class BplDelegateMapCreator extends
			DelegateMapCreator {
		private BaseProjectionLookupBuilder builder;

		public BaseProjectionLookupBuilder getBuilder() {
			return builder;
		}

		public void setBuilder(BaseProjectionLookupBuilder builder) {
			this.builder = builder;
		}
	}

	public BaseProjectionLookupBuilder navigable() {
		navigable = true;
		sorted = true;
		return this;
	}

	public MapCreator[] getCreators() {
		return creators;
	}

	public boolean isNavigable() {
		return navigable;
	}

	public boolean isSorted() {
		return sorted;
	}
}
