package cc.alcina.framework.entity.entityaccess.cache;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.TimeConstants;
import cc.alcina.framework.entity.projection.EntityUtils;

public class LazyPropertyLoadTask<T extends Entity>
		extends LazyLoadProvideTask<T> {
	public LazyPropertyLoadTask(Class<T> clazz, DomainStore domainStore) {
		super(5 * TimeConstants.ONE_SECOND_MS, 10, clazz);
		registerStore(domainStore);
	}

	@Override
	public Stream<T> wrap(Stream<T> stream) {
		return stream.peek(t -> lazyLoad(Collections.singletonList(t)));
	}

	@Override
	public void writeLockedCleanup() {
		// noop
	}

	@Override
	protected boolean checkShouldLazyLoad(List toLoad) {
		return true;
	}

	@Override
	protected boolean evictionDisabled() {
		return false;
	}

	@Override
	protected Object getLockObject() {
		// transactional, no lock
		return null;
	}

	@Override
	protected void lazyLoad(Collection objects) {
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
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		} finally {
			LooseContext.pop();
		}
	}

	@Override
	protected void loadDependents(List requireLoad) throws Exception {
	}

	@Override
	protected synchronized List requireLazyLoad(Collection objects) {
		return (List) objects.stream().distinct().collect(Collectors.toList());
	}
}
