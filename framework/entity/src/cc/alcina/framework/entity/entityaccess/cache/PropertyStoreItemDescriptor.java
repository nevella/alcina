package cc.alcina.framework.entity.entityaccess.cache;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.common.client.domain.DomainClassDescriptor;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.HiliLocator;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedEntityCache;
import cc.alcina.framework.entity.entityaccess.cache.DomainProxy.DomainProxyContext;
import cc.alcina.framework.entity.entityaccess.cache.DomainStore.PdOperator;
import cc.alcina.framework.entity.projection.GraphProjection;

public abstract class PropertyStoreItemDescriptor<T extends HasIdAndLocalId>
		extends DomainClassDescriptor<T> {
	protected PropertyStore propertyStore;

	protected DetachedEntityCache cache;

	public PropertyStoreItemDescriptor(Class clazz) {
		super(clazz);
		createPropertyStore();
	}

	public void addRow(ResultSet rs) throws SQLException {
		propertyStore.addRow(rs);
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

	@Override
	public List<T> getRawValues(Set<Long> ids, DetachedEntityCache cache) {
		ArrayList<T> raw = new ArrayList<T>(ids.size());
		for (Long id : ids) {
			T proxy = getProxy(cache, id, false);
			if (proxy != null) {
				raw.add(proxy);
			}
		}
		return raw;
	}

	public String getSqlFilter() {
		return null;
	}

	@Override
	public void index(HasIdAndLocalId obj, boolean add) {
		propertyStore.index(obj, add);
	}

	public void init(DetachedEntityCache cache, List<PdOperator> pds) {
		this.cache = cache;
		propertyStore.init(pds);
	}

	@Override
	public boolean isTransactional() {
		return false;
	}

	public void remove(long id) {
		propertyStore.remove(id);
	}

	protected void createPropertyStore() {
		this.propertyStore = new PropertyStore();
	}

	protected abstract Object createProxy(int rowOffset,
			DetachedEntityCache cache, Long id);

	protected void ensureProxyModificationChecker(DomainProxy proxy) {
	}

	protected abstract int getRoughCount();

	T getProxy(DetachedEntityCache cache, Long id, boolean create) {
		int rowOffset = propertyStore.getRowOffset(id);
		if (rowOffset == -1 && create) {
			rowOffset = propertyStore.ensureRow(id);
		}
		if (rowOffset != -1) {
			DomainProxyContext ctx = GraphProjection.getContextObject(
					DomainProxy.CONTEXT_DOMAIN_PROXY_CONTEXT,
					DomainProxyContext.SUPPLIER);
			if (ctx == null) {
				return (T) createProxy(rowOffset, cache, id);
			} else {
				HiliLocator locator = new HiliLocator(clazz, id, 0);
				T proxy = (T) ctx.projectionProxies.get(locator);
				if (proxy == null) {
					proxy = (T) createProxy(rowOffset, cache, id);
					ctx.projectionProxies.put(locator, (DomainProxy) proxy);
				}
				return proxy;
			}
		} else {
			return null;
		}
	}
}
