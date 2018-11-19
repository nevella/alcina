package cc.alcina.framework.common.client.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import cc.alcina.framework.common.client.domain.BaseProjectionLookupBuilder.MapCreator;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.util.MultikeyMap;

public abstract class ReverseDateProjection<T extends HasIdAndLocalId>
		extends BaseProjection<T> {
	public ReverseDateProjection() {
	}

	public List<T> getSince(Date date) {
		List<T> result = new ArrayList<>();
		Set<Date> keys = (Set) getLookup().keySet();
		for (Date key : keys) {
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
		return new BaseProjectionLookupBuilder(this).navigable()
				.mapCreators(new MapCreator[] { new TreeMapRevCreator() })
				.createMultikeyMap();
	}

	@Override
	protected int getDepth() {
		return 1;
	}

	@Override
	protected Object[] project(T t) {
		return new Object[] { getDate(t), t };
	}

	protected abstract Date getDate(T t);

	public static class TreeMapRevCreator
			implements BaseProjectionLookupBuilder.MapCreator {
		@Override
		public Map get() {
			return new TreeMap<>(Comparator.reverseOrder());
		}
	}
}