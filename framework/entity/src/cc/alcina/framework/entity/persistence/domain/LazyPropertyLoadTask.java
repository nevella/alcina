package cc.alcina.framework.entity.persistence.domain;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.projection.EntityPersistenceHelper;

public class LazyPropertyLoadTask<T extends Entity>
		extends LazyLoadProvideTask<T> {
	private static final String CONTEXT_IN_LAZY_PROPERTY_LOAD = LazyLoadProvideTask.class
			.getName() + ".CONTEXT_IN_LAZY_PROPERTY_LOAD";

	public static final String CONTEXT_POPULATE_STREAM_ELEMENT_LAZY_PROPERTIES = LazyLoadProvideTask.class
			.getName() + ".CONTEXT_POPULATE_STREAM_ELEMENT_LAZY_PROPERTIES";

	public static boolean inLazyPropertyLoad() {
		return LooseContext.is(CONTEXT_IN_LAZY_PROPERTY_LOAD);
	}

	public LazyPropertyLoadTask(Class<T> clazz, DomainStore domainStore) {
		super(clazz);
		registerStore(domainStore);
	}

	@Override
	public Stream<T> wrap(Stream<T> stream) {
		if (LooseContext.is(LazyLoadProvideTask.CONTEXT_LAZY_LOAD_DISABLED)) {
			return stream;
		}
		if (LooseContext
				.is(DomainStore.CONTEXT_DO_NOT_POPULATE_LAZY_PROPERTY_VALUES)) {
			return stream;
		}
		if (!LooseContext.is(
				LazyPropertyLoadTask.CONTEXT_POPULATE_STREAM_ELEMENT_LAZY_PROPERTIES)) {
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
		if (inLazyPropertyLoad()) {
			return;
		}
		if (LooseContext
				.is(DomainStore.CONTEXT_DO_NOT_POPULATE_LAZY_PROPERTY_VALUES)) {
			return;
		}
		try {
			LooseContext.pushWithTrue(CONTEXT_IN_LAZY_PROPERTY_LOAD);
			String inClause = EntityPersistenceHelper.toInClause(objects);
			String sqlFilter = String.format(" id in %s", inClause);
			String key = Ax.format("load :: %s (%s) (%s)",
					clazz.getSimpleName(), objects.size(),
					CommonUtils.trimToWsChars(inClause, 100));
			metric(key, false);
			List<T> values = loadTable(clazz, sqlFilter, true);
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
