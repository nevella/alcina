package cc.alcina.framework.entity.persistence.domain;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.projection.EntityPersistenceHelper;

public class LazyPropertyLoadTask<T extends Entity>
		extends LazyLoadProvideTask<T> {
	public static final LooseContext.Key<?> CONTEXT_POPULATE_LAZY_PROPERTIES = LooseContext
			.key(LazyLoadProvideTask.class,
					".CONTEXT_POPULATE_LAZY_PROPERTIES");

	private static final LooseContext.Key<?> CONTEXT_IN_LAZY_PROPERTY_LOAD = LooseContext
			.key(LazyLoadProvideTask.class, ".CONTEXT_IN_LAZY_PROPERTY_LOAD");

	public static boolean inLazyPropertyLoad() {
		return CONTEXT_IN_LAZY_PROPERTY_LOAD.is();
	}

	public LazyPropertyLoadTask(Class<T> clazz, DomainStore domainStore) {
		super(clazz);
		registerStore(domainStore);
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
	public void run(Class clazz, Collection<T> objects, boolean topLevel) {
		if (!checkContext()) {
			return;
		}
		super.run(clazz, objects, topLevel);
	}

	boolean checkContext() {
		return CONTEXT_POPULATE_LAZY_PROPERTIES.is() && !inLazyPropertyLoad();
	}

	@Override
	protected void lazyLoad(Collection objects) {
		if (!checkContext()) {
			return;
		}
		try {
			LooseContext.pushWithTrue(CONTEXT_IN_LAZY_PROPERTY_LOAD.getPath());
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
	protected void loadDependents(List requireLoad) {
	}

	@Override
	protected synchronized List requireLazyLoad(Collection objects) {
		return (List) objects.stream().distinct().collect(Collectors.toList());
	}

	@Override
	public Stream<T> wrap(Stream<T> stream) {
		if (!checkContext()) {
			return stream;
		}
		return super.wrap(stream);
	}
}
