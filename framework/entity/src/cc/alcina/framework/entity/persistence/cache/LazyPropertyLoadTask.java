package cc.alcina.framework.entity.persistence.cache;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.projection.EntityPersistenceHelper;

public class LazyPropertyLoadTask<T extends Entity>
		extends LazyLoadProvideTask<T> {
	private static final String CONTEXT_IN_LAZY_PROPERTY_LOAD = LazyLoadProvideTask.class
			.getName() + ".CONTEXT_IN_LAZY_PROPERTY_LOAD";

	public LazyPropertyLoadTask(Class<T> clazz, DomainStore domainStore) {
		super(clazz);
		registerStore(domainStore);
	}

	@Override
	public Stream<T> wrap(Stream<T> stream) {
		// return stream.peek(t -> lazyLoad(Collections.singletonList(t)));
		if (LooseContext.is(LazyLoadProvideTask.CONTEXT_LAZY_LOAD_DISABLED)) {
			return stream;
		}
		return super.wrap(stream);
	}

	@Override
	protected boolean checkShouldLazyLoad(List toLoad) {
		return true;
	}

	@Override
	protected Object getLockObject() {
		// transactional, no lock
		return null;
	}

	@Override
	protected void lazyLoad(Collection objects) {
		if (LooseContext.is(CONTEXT_IN_LAZY_PROPERTY_LOAD)) {
			return;
		}
		try {
			LooseContext.pushWithTrue(
					DomainStore.CONTEXT_KEEP_LOAD_TABLE_DETACHED_FROM_GRAPH);
			LooseContext
					.setTrue(DomainStore.CONTEXT_POPULATE_LAZY_PROPERTY_VALUES);
			LooseContext.setTrue(CONTEXT_IN_LAZY_PROPERTY_LOAD);
			String sqlFilter = String.format(" id in %s",
					EntityPersistenceHelper.toInClause(objects));
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
