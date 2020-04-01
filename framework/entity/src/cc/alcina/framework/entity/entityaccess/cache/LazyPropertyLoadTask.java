package cc.alcina.framework.entity.entityaccess.cache;

import java.util.Collection;
import java.util.List;

import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.TimeConstants;
import cc.alcina.framework.entity.projection.EntityUtils;

public class LazyPropertyLoadTask<T extends HasIdAndLocalId>
		extends LazyLoadProvideTask {
	public LazyPropertyLoadTask(Class<T> clazz, DomainStore domainStore) {
		super(5 * TimeConstants.ONE_SECOND_MS, 10, clazz);
		registerStore(domainStore);
	}

	@Override
	protected boolean checkShouldLazyLoad(List toLoad) {
		return true;
	}

	@Override
	protected void evict(EvictionToken evictionToken, Long key, boolean top) {
		T domain = Domain.find(clazz, key);
		if (domain != null) {
			((DomainStoreLoaderDatabase) domainStore.loader)
					.clearLazyPropertyValues(domain);
		}
	}

	@Override
	protected boolean evictionDisabled() {
		return false;
	}

	@Override
	protected void lazyLoad(Collection objects) throws Exception {
		try {
			LooseContext.pushWithTrue(
					DomainStore.CONTEXT_KEEP_LOAD_TABLE_DETACHED_FROM_GRAPH);
			LooseContext
					.setTrue(DomainStore.CONTEXT_POPULATE_LAZY_PROPERTY_VALUES);
			String sqlFilter = String.format(" id in %s",
					EntityUtils.hasIdsToIdClause(objects));
			ClassIdLock lock = LockUtils.obtainClassIdLock(clazz, 0);
			String key = Ax.format("load :: %s (%s)",
					getClass().getSimpleName(), objects.size());
			metric(key, false);
			List<T> values = loadTable(clazz, sqlFilter, lock);
			for (T t : values) {
				T domain = Domain.find(t);
				if (domain != null) {
					((DomainStoreLoaderDatabase) domainStore.loader)
							.copyLazyPropertyValues(t, domain);
				}
			}
			metric(key, true);
		} finally {
			LooseContext.pop();
		}
	}

	@Override
	protected void loadDependents(List requireLoad) throws Exception {
	}
}
