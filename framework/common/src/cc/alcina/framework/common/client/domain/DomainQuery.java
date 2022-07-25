package cc.alcina.framework.common.client.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.collections.FilterOperator;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.PropertyEnum;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;

public abstract class DomainQuery<E extends Entity> {
	public static final String CONTEXT_DEBUG_CONSUMER = DomainQuery.class
			.getName() + ".CONTEXT_DEBUG_CONSUMER";

	private List<DomainFilter> filters = new ArrayList<DomainFilter>();

	protected Class<E> entityClass;

	private Optional<Stream<E>> sourceStream = Optional.empty();

	private Map<String, Object> contextProperties = new LinkedHashMap<>();

	private Set<Hint> hints = new LinkedHashSet<>();

	private int limit = -1;

	private Comparator<E> comparator;

	private boolean parallel;

	public DomainQuery(Class<E> entityClass) {
		this.entityClass = entityClass;
	}

	public Set<E> asSet() {
		return stream().collect(Collectors.toSet());
	}

	public DomainQuery<E> contextTrue(String contextProperty) {
		contextProperties.put(contextProperty, Boolean.TRUE);
		return this;
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

	public DomainQuery<E> filter(Predicate<E> p) {
		return filter(new DomainFilter(p));
	}

	public DomainQuery<E> filter(PropertyEnum property, Object value) {
		return filter(new PropertyEnum[]{ property }, value, FilterOperator.EQ);
	}

	public DomainQuery<E> filter(PropertyEnum[] properties, Object value) {
		return filter(properties, value, FilterOperator.EQ);
	}

	public DomainQuery<E> filter(PropertyEnum property, Object value,
			FilterOperator operator) {
		return filter(new PropertyEnum[]{ property }, value, operator);
	}

	public DomainQuery<E> filter(PropertyEnum[] properties, Object value,
			FilterOperator operator) {
		// Generate the key from the properties array
		StringBuilder key = new StringBuilder();
		for (int i = 0; i < properties.length; i++) {
			// For all but the first property, append the dot operator
			if (i != 0) {
				key.append(".");
			}
			// Append property name
			key.append(properties[i].name());
		}

		return filter(new DomainFilter(key.toString(), value, operator));
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
		return filter(new DomainFilter("id", ids, FilterOperator.IN));
	}

	public E find() {
		return optional().orElse(null);
	}

	public Comparator<E> getComparator() {
		return this.comparator;
	}

	public Map<String, Object> getContextProperties() {
		return this.contextProperties;
	}

	public Class<E> getEntityClass() {
		return this.entityClass;
	}

	public List<DomainFilter> getFilters() {
		return this.filters;
	}

	public Set<Hint> getHints() {
		return this.hints;
	}

	public int getLimit() {
		return this.limit;
	}

	public Optional<Stream<E>> getSourceStream() {
		return this.sourceStream;
	}

	public boolean isParallel() {
		return this.parallel;
	}

	public DomainQuery<E> limit(int limit) {
		this.limit = limit;
		return this;
	}

	public abstract List<E> list();

	public Optional<E> optional() {
		return stream().findFirst();
	}

	public DomainQuery<E> parallel(boolean parallel) {
		this.parallel = parallel;
		return this;
	}

	public DomainQuery<E> sorted(Comparator<?> comparator) {
		Comparator<E> cast = (Comparator<E>) comparator;
		if (this.comparator == null) {
			this.comparator = cast;
		} else {
			this.comparator = this.comparator.thenComparing(cast);
		}
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

	public DomainQuery<E> withHint(Hint hint) {
		hints.add(hint);
		return this;
	}

	public abstract static class DebugConsumer implements Consumer<Entity> {
		public Object queryToken;
	}

	public static class DomainIdFilter {
	}

	/*
	 * Ignoreable if server-only, client-side
	 */
	@Reflected
	public enum Hint {
		WITH_LAZY_PROPERTIES
	}

	public interface HintResolver {
		boolean resolve(Hint hint, DomainQuery query);
	}
}
