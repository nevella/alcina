package cc.alcina.framework.entity.entityaccess.cache;

import java.lang.reflect.Proxy;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedEntityCache;
import cc.alcina.framework.entity.entityaccess.cache.AlcinaMemCache.PdOperator;

public abstract class PropertyStoreItemDescriptor extends CacheItemDescriptor {
	public PropertyStoreItemDescriptor(Class clazz) {
		super(clazz);
		createPropertyStore();
	}

	protected PropertyStore propertyStore;

	protected void createPropertyStore() {
		this.propertyStore = new PropertyStore();
	}

	@Override
	public Set<Long> evaluateFilter(DetachedEntityCache cache,
			Set<Long> existing, CollectionFilter filter) {
		filter.setContext(propertyStore.createContext(cache));
		if (existing == null) {
			System.out.println("warn - raw store query - " + filter);
			existing = propertyStore.getIds();
		}
		CollectionFilter withIdFilter = new CollectionFilter<Long>() {
			@Override
			public boolean allow(Long id) {
				// will be chained through to the store
				return filter.allow(id);
			}
		};
		return CollectionFilters.filterAsSet(existing, withIdFilter);
	}

	public String getSqlFilter() {
		return null;
	}

	public void init(List<PdOperator> pds) {
		propertyStore.init(pds);
	}

	public void addRow(ResultSet rs) throws SQLException {
		propertyStore.addRow(rs);
	}

	@Override
	public <T> List<T> getRawValues(Set<Long> ids, DetachedEntityCache cache) {
		ArrayList<T> raw = new ArrayList<T>(ids.size());
		for (Long id : ids) {
			int rowOffset = propertyStore.getRowOffset(id);
			if (rowOffset != -1) {
				T proxy = (T) createProxy(rowOffset, cache,id);
				raw.add(proxy);
			}
		}
		return raw;
	}

	protected abstract Object createProxy(int rowOffset, DetachedEntityCache cache, Long id);

	protected abstract int getRoughCount();
}
