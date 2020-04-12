package cc.alcina.framework.entity.entityaccess.cache;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import cc.alcina.framework.common.client.domain.DomainClassDescriptor;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.EntityLocator;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedEntityCache;
import cc.alcina.framework.entity.entityaccess.cache.DomainProxy.DomainProxyContext;
import cc.alcina.framework.entity.entityaccess.cache.DomainStoreLoaderDatabase.PdOperator;
import cc.alcina.framework.entity.projection.GraphProjection;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;

public abstract class PropertyStoreItemDescriptor<T extends Entity>
		extends DomainClassDescriptor<T> {
	protected PropertyStore propertyStore;

	protected DetachedEntityCache cache;

	public PropertyStoreItemDescriptor(Class clazz) {
		super(clazz);
		createPropertyStore();
	}

	@Override
	public void addRawValues(Set<Long> ids, DetachedEntityCache cache,
			Set<T> rawValues) {
		for (Long id : ids) {
			T proxy = getProxy(cache, id, false);
			if (proxy != null) {
				rawValues.add(proxy);
			}
		}
	}

	public void addRow(Object[] row) throws SQLException {
		propertyStore.addRow(row);
	}

	public Set<T> getRawValues(Set<Long> ids, DetachedEntityCache cache) {
		ObjectLinkedOpenHashSet<T> rawValues = new ObjectLinkedOpenHashSet<T>(
				ids.size());
		addRawValues(ids, cache, rawValues);
		return rawValues;
	}

	@Override
	public void index(Entity obj, boolean add) {
		propertyStore.index(obj, add);
	}

	public void init(DetachedEntityCache cache, List<PdOperator> pds) {
		if (this.cache == null) {
			this.cache = cache;
			propertyStore.init(pds);
		}
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
				EntityLocator locator = new EntityLocator(clazz, id, 0);
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
