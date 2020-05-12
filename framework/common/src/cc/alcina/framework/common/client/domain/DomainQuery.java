package cc.alcina.framework.common.client.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.collections.FilterOperator;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;

public abstract class DomainQuery<E extends Entity> {
	public static final String CONTEXT_DEBUG_CONSUMER = DomainQuery.class
			.getName() + ".CONTEXT_DEBUG_CONSUMER";

	private List<DomainFilter> filters = new ArrayList<DomainFilter>();

	protected boolean raw = true;

	private boolean nonTransactional;

	protected Class<E> entityClass;

	private Optional<Stream<E>> sourceStream = Optional.empty();

	public DomainQuery(Class<E> entityClass) {
		this.entityClass = entityClass;
	}

	public Set<E> asSet() {
		return stream().collect(Collectors.toSet());
	}

	public int count() {
		return raw().list().size();
	}

	public DomainQuery<E> filter(DomainFilter filter) {
		if (filter instanceof CompositeFilter) {
			CompositeFilter compositeFilter = (CompositeFilter) filter;
			if (compositeFilter.canFlatten()) {
				for (DomainFilter sub : compositeFilter.getFilters()) {
					this.filters.add(sub);
				}
				return (DomainQuery<E>) this;
			}
		}
		this.filters.add(filter);
		return this;
	}

	public DomainQuery<E> filter(Predicate p) {
		return filter(new DomainFilter(p));
	}

	public DomainQuery<E> filter(String key, Object value) {
		return filter(new DomainFilter(key, value));
	}

	public DomainQuery<E> filter(String key, Object value,
			FilterOperator operator) {
		return filter(new DomainFilter(key, value, operator));
	}

	public DomainQuery<E> filterById(long id) {
		filterByIds(Collections.singleton(id));
		return this;
	}

	public DomainQuery<E> filterByIds(Collection<Long> ids) {
		filter("id", ids);
		return this;
	}

	public E find() {
		return optional().orElse(null);
	}

	public E first() {
		return CommonUtils.first(list());
	}

	public Class<E> getEntityClass() {
		return this.entityClass;
	}

	public List<DomainFilter> getFilters() {
		return this.filters;
	}

	public Optional<Stream<E>> getSourceStream() {
		return this.sourceStream;
	}

	public boolean isNonTransactional() {
		return this.nonTransactional;
	}

	public boolean isRaw() {
		return this.raw;
	}

	public abstract List<E> list();

	public DomainQuery<E> nonTransactional() {
		this.nonTransactional = true;
		return this;
	}

	public Optional<E> optional() {
		return stream().findFirst();
	}

	public DomainQuery<E> raw() {
		this.raw = true;
		return this;
	}

	public void sourceStream(Optional<Stream<E>> sourceStream) {
		this.sourceStream = sourceStream;
	}

	public Stream<E> stream() {
		return list().stream();
	}

	@Override
	public String toString() {
		return Ax.format("DomainQuery: %s\n%s",
				entityClass == null ? "(No class)"
						: entityClass.getSimpleName(),
				CommonUtils.join(filters, ",\n"));
	}

	public static class DomainIdFilter {
	}
}
