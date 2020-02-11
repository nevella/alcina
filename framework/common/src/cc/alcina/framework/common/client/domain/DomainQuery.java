package cc.alcina.framework.common.client.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.collections.FilterOperator;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;

public abstract class DomainQuery<V extends HasIdAndLocalId> {
	protected Collection<Long> filterByIds = new LinkedHashSet<Long>();

	private List<DomainFilter> filters = new ArrayList<DomainFilter>();

	protected boolean raw;

	private boolean nonTransactional;

	protected Class<V> clazz;

	public DomainQuery(Class<V> clazz) {
		this.clazz = clazz;
	}

	public Set<V> asSet() {
		return stream().collect(Collectors.toSet());
	}

	public int count() {
		return raw().list().size();
	}

	public DomainQuery<V> filter(DomainFilter filter) {
		if (filter instanceof CompositeFilter) {
			CompositeFilter compositeFilter = (CompositeFilter) filter;
			if (compositeFilter.canFlatten()) {
				for (DomainFilter sub : compositeFilter.getFilters()) {
					this.filters.add(sub);
				}
				return (DomainQuery<V>) this;
			}
		}
		this.filters.add(filter);
		return this;
	}

	public DomainQuery<V> filter(Predicate p) {
		return filter(new DomainFilter(p));
	}

	public DomainQuery<V> filter(String key, Object value) {
		return filter(new DomainFilter(key, value));
	}

	public DomainQuery<V> filter(String key, Object value,
			FilterOperator operator) {
		return filter(new DomainFilter(key, value, operator));
	}

	public V find() {
		return optional().orElse(null);
	}

	public V first() {
		return CommonUtils.first(list());
	}

	public List<DomainFilter> getFilters() {
		return this.filters;
	}

	public Set<Long> getFilterByIds() {
		return (this.filterByIds instanceof Set) ? (Set<Long>) this.filterByIds
				: new LinkedHashSet<Long>(this.filterByIds);
	}

	public DomainQuery<V> filterById(long id) {
		this.filterByIds = Collections.singleton(id);
		return this;
	}

	public DomainQuery<V> filterByIds(Collection<Long> ids) {
		this.filterByIds = ids;
		return this;
	}

	public boolean isNonTransactional() {
		return this.nonTransactional;
	}

	public boolean isRaw() {
		return this.raw;
	}

	public abstract List<V> list();

	public DomainQuery<V> nonTransactional() {
		this.nonTransactional = true;
		return this;
	}

	public Optional<V> optional() {
		return stream().findFirst();
	}

	public DomainQuery<V> raw() {
		this.raw = true;
		return this;
	}

	public Stream<V> stream() {
		return list().stream();
	}

	@Override
	public String toString() {
		return Ax.format("DomainQuery: %s\n%s",
				clazz == null ? "(No class)" : clazz.getSimpleName(),
				CommonUtils.join(filters, ",\n"));
	}
}
