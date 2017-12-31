package cc.alcina.framework.common.client.cache;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.DelegateMapCreator;
import cc.alcina.framework.common.client.util.MultikeyMap;
import cc.alcina.framework.common.client.util.SortedMultikeyMap;
import cc.alcina.framework.common.client.util.UnsortedMultikeyMap;

public class BaseProjectionLookupBuilder {
	private boolean sorted = false;

	private MapCreator[] creators;

	private boolean navigable;

	private BaseProjection projection;

	public BaseProjectionLookupBuilder(BaseProjection projection) {
		this.projection = projection;
	}

	public <T> MultikeyMap<T> createMultikeyMap() {
		MultikeyMap<T> map = null;
		BplDelegateMapCreator mapCreator = Registry
				.impl(BplDelegateMapCreator.class);
		mapCreator.setBuilder(this);
		if (isSorted()) {
			map = new SortedMultikeyMap<T>(projection.getDepth(), 0,
					mapCreator);
		} else {
			map = new UnsortedMultikeyMap<T>(projection.getDepth(), 0,
					mapCreator);
		}
		return map;
	}

	public MapCreator[] getCreators() {
		return creators;
	}

	public BaseProjection getProjection() {
		return this.projection;
	}

	public boolean isNavigable() {
		return navigable;
	}

	public boolean isSorted() {
		return sorted;
	}

	public BaseProjectionLookupBuilder mapCreators(MapCreator... creators) {
		if (creators.length != projection.getDepth()) {
			throw new RuntimeException(
					"Mismatched creator array length and depth");
		}
		this.creators = creators;
		return this;
	}

	public BaseProjectionLookupBuilder navigable() {
		navigable = true;
		sorted = true;
		return this;
	}

	public void setProjection(BaseProjection projection) {
		this.projection = projection;
	}

	public BaseProjectionLookupBuilder sorted() {
		sorted = true;
		return this;
	}

	public BaseProjectionLookupBuilder unsorted() {
		sorted = false;
		return this;
	}

	public static abstract class BplDelegateMapCreator
			extends DelegateMapCreator {
		private BaseProjectionLookupBuilder builder;

		public BaseProjectionLookupBuilder getBuilder() {
			return builder;
		}

		public void setBuilder(BaseProjectionLookupBuilder builder) {
			this.builder = builder;
		}
	}

	public static class BplDelegateMapCreatorStd
			extends BaseProjectionLookupBuilder.BplDelegateMapCreator {
		@Override
		public Map createDelegateMap(int depthFromRoot, int depth) {
			if (getBuilder().getCreators() != null) {
				return getBuilder().getCreators()[depthFromRoot].get();
			}
			if (getBuilder().isNavigable()) {
				return new TreeMap();
			} else {
				return getBuilder().isSorted() ? new TreeMap()
						: new LinkedHashMap();
			}
		}
	}

	public interface MapCreator extends Supplier<Map> {
	}
}
