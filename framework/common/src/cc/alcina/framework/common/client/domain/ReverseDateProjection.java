package cc.alcina.framework.common.client.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.CollectionCreators;
import cc.alcina.framework.common.client.util.MultikeyMap;

public abstract class ReverseDateProjection<T extends Entity>
		extends BaseProjection<T> {
	public ReverseDateProjection(Class initialType, Class... secondaryTypes) {
		super(initialType, secondaryTypes);
	}

	public List<T> getSince(Date date) {
		List<T> result = new ArrayList<>();
		Set<Date> keys = (Set) getLookup().keySet();
		for (Date key : keys) {
			if (key == null) {
				// at end (reversed), so nothing matches
				break;
			}
			if (date == null || key.after(date)) {
				result.add(get(key));
			} else {
				break;
			}
		}
		return result;
	}

	@Override
	protected MultikeyMap<T> createLookup() {
		return new BaseProjectionLookupBuilder(this)
				.mapCreators(new CollectionCreators.MapCreator[] { Registry
						.impl(CollectionCreators.TreeMapRevCreator.class)
						.withTypes(types) })
				.createMultikeyMap();
	}

	protected abstract Date getDate(T t);

	@Override
	protected int getDepth() {
		return 1;
	}

	@Override
	protected Object[] project(T t) {
		return new Object[] { getDate(t), t };
	}
}