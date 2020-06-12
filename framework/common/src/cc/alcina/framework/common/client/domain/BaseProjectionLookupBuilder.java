package cc.alcina.framework.common.client.domain;

import java.util.LinkedHashMap;
import java.util.Map;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.CollectionCreators;
import cc.alcina.framework.common.client.util.CollectionCreators.DelegateMapCreator;
import cc.alcina.framework.common.client.util.MultikeyMap;
import cc.alcina.framework.common.client.util.UnsortedMultikeyMap;

/**
 * This used to have sorted/navigable options - but in general those are better
 * defined by setting mapcreators for each layer of keys. Also, navigable will
 * not work under mvcc (fastutil treemaps are not navigable, although they are
 * sorted)
 */
public class BaseProjectionLookupBuilder {
	private CollectionCreators.MapCreator[] mapCreators;

	private BaseProjection projection;

	private BplDelegateMapCreator delegateMapCreator = Registry
			.impl(BplDelegateMapCreator.class);

	public BaseProjectionLookupBuilder(BaseProjection projection) {
		this.projection = projection;
	}

	public <T> MultikeyMap<T> createMultikeyMap() {
		MultikeyMap<T> map = null;
		delegateMapCreator.setBuilder(this);
		map = new UnsortedMultikeyMap<T>(projection.getDepth(), 0,
				delegateMapCreator);
		return map;
	}

	public CollectionCreators.MapCreator[] getCreators() {
		return mapCreators;
	}

	public BaseProjection getProjection() {
		return this.projection;
	}

	public void setProjection(BaseProjection projection) {
		this.projection = projection;
	}

	public BaseProjectionLookupBuilder
			withDelegateMapCreator(BplDelegateMapCreator delegateMapCreator) {
		this.delegateMapCreator = delegateMapCreator;
		return this;
	}

	public BaseProjectionLookupBuilder
			withMapCreators(CollectionCreators.MapCreator... creators) {
		if (creators.length != projection.getDepth()) {
			throw new RuntimeException(
					"Mismatched creator array length and depth");
		}
		this.mapCreators = creators;
		return this;
	}

	public static abstract class BplDelegateMapCreator
			implements DelegateMapCreator {
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
				return (Map) getBuilder().getCreators()[depthFromRoot].get();
			} else {
				return new LinkedHashMap();
			}
		}
	}
}
