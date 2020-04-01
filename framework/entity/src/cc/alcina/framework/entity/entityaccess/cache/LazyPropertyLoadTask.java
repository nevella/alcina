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
		super(TimeConstants.ONE_MINUTE_MS, 100, clazz);
		registerStore(domainStore);
	}

	@Override
	protected boolean checkShouldLazyLoad(List toLoad) {
		return true;
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
