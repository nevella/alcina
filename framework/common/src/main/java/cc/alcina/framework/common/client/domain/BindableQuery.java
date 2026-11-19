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
import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.PropertyEnum;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.Topic;

public abstract class BindableQuery<B extends Bindable> {
	public static final String CONTEXT_DEBUG_CONSUMER = BindableQuery.class
			.getName() + ".CONTEXT_DEBUG_CONSUMER";

	private List<BindableFilter> filters = new ArrayList<BindableFilter>();

	protected Class<B> bindableClass;

	private Optional<Stream<B>> sourceStream = Optional.empty();

	private Map<String, Object> contextProperties = new LinkedHashMap<>();

	private Set<Hint> hints = new LinkedHashSet<>();

	private int limit = -1;

	private Comparator<B> comparator;

	private boolean parallel;

	public BindableQuery(Class<B> bindableClass) {
		this.bindableClass = bindableClass;
	}

	public Set<B> asSet() {
		return stream().collect(Collectors.toSet());
	}

	public BindableQuery<B> contextTrue(String contextProperty) {
		contextProperties.put(contextProperty, Boolean.TRUE);
		return this;
	}

	public BindableQuery<B> contextTrue(LooseContext.Key<?> contextKey) {
		contextProperties.put(contextKey.getPath(), Boolean.TRUE);
		return this;
	}

	public BindableQuery<B> filter(BindableFilter filter) {
		if (filter instanceof CompositeFilter) {
			CompositeFilter compositeFilter = (CompositeFilter) filter;
			if (compositeFilter.canFlatten()) {
				for (BindableFilter sub : compositeFilter.getFilters()) {
					this.filters.add(sub);
				}
				return (BindableQuery<B>) this;
			}
		}
		this.filters.add(filter);
		return this;
	}

	public BindableQuery<B> filter(Predicate<B> p) {
		return filter(new BindableFilter(p));
	}

	public BindableQuery<B> filter(PropertyEnum property, Object value) {
		return filter(property, value, FilterOperator.EQ);
	}

	public BindableQuery<B> filter(PropertyEnum property, Object value,
			FilterOperator operator) {
		return filter(new BindableFilter(property.name(), value, operator));
	}

	public BindableQuery<B> filter(String key, Object value) {
		return filter(new BindableFilter(key, value));
	}

	public BindableQuery<B> filter(String key, Object value,
			FilterOperator operator) {
		return filter(new BindableFilter(key, value, operator));
	}

	public FilterBuilder filterBuilder() {
		return new FilterBuilder(this);
	}

	public BindableQuery<B> filterById(long id) {
		filterByIds(Collections.singleton(id));
		return this;
	}

	public BindableQuery<B> filterByIds(Collection<Long> ids) {
		return filter(new BindableFilter("id", ids, FilterOperator.IN));
	}

	public B find() {
		return optional().orElse(null);
	}

	public Comparator<B> getComparator() {
		return this.comparator;
	}

	public Map<String, Object> getContextProperties() {
		return this.contextProperties;
	}

	public Class<B> getBindableClass() {
		return this.bindableClass;
	}

	public List<BindableFilter> getFilters() {
		return this.filters;
	}

	public Set<Hint> getHints() {
		return this.hints;
	}

	public int getLimit() {
		return this.limit;
	}

	public Optional<Stream<B>> getSourceStream() {
		return this.sourceStream;
	}

	public boolean isParallel() {
		return this.parallel;
	}

	public BindableQuery<B> limit(int limit) {
		this.limit = limit;
		return this;
	}

	public abstract List<B> list();

	public Optional<B> optional() {
		return stream().findFirst();
	}

	public BindableQuery<B> parallel(boolean parallel) {
		this.parallel = parallel;
		return this;
	}

	public BindableQuery<B> sorted(Comparator<?> comparator) {
		Comparator<B> cast = (Comparator<B>) comparator;
		if (this.comparator == null) {
			this.comparator = cast;
		} else {
			this.comparator = this.comparator.thenComparing(cast);
		}
		return this;
	}

	public void sourceStream(Optional<Stream<B>> sourceStream) {
		this.sourceStream = sourceStream;
	}

	public Stream<B> stream() {
		return list().stream();
	}

	@Override
	public String toString() {
		return Ax.format("BindableQuery: %s\n%s",
				bindableClass == null ? "(No class)"
						: bindableClass.getSimpleName(),
				CommonUtils.join(filters, ",\n"));
	}

	public BindableQuery<B> withHint(Hint hint) {
		hints.add(hint);
		return this;
	}

	public abstract static class DebugConsumer implements Consumer<Entity> {
		public Object queryToken;
	}

	public static class DomainIdFilter {
	}

	/**
	 * Helper class to generate filters from PropertyEnum path segments
	 */
	public class FilterBuilder {
		FilterOperator operator = FilterOperator.EQ;

		List<PropertyEnum> pathSegments = new ArrayList<>();

		Object value;

		BindableQuery<B> query;

		/**
		 * Construct a new FilterBuilder from a BindableQuery
		 *
		 * @param query
		 */
		private FilterBuilder(BindableQuery<B> query) {
			this.query = query;
		}

		/**
		 * Build a BindableFilter from stored parameters and append to
		 * BindableQuery
		 *
		 * @return Associated BindableQuery
		 */
		public BindableQuery<B> build() {
			String path = pathSegments.stream().map(PropertyEnum::name)
					.collect(Collectors.joining("."));
			// Return the original query
			return query.filter(new BindableFilter(path, value, operator));
		}

		/**
		 * Set the operator to add to the built BindableFilter
		 *
		 * @param operator
		 * @return This FilterBuilder
		 */
		public FilterBuilder operator(FilterOperator operator) {
			this.operator = operator;
			return this;
		}

		/**
		 * Add the PropertyEnum to the path segments to compile to a property
		 * path
		 *
		 * @param segment
		 * @return This FilterBuilder
		 */
		public FilterBuilder pathSegment(PropertyEnum segment) {
			pathSegments.add(segment);
			return this;
		}

		/**
		 * Set the value to add to the built BindableFilter
		 *
		 * @param value
		 * @return This FilterBuilder
		 */
		public FilterBuilder value(Object value) {
			this.value = value;
			return this;
		}
	}

	/*
	 * Ignoreable if server-only, client-side
	 */
	@Reflected
	public enum Hint {
		WITH_LAZY_PROPERTIES
	}

	public interface HintResolver {
		boolean resolve(Hint hint, BindableQuery query);
	}

	public Topic<Stream<B>> topic() {
		throw new UnsupportedOperationException();
	}

	public Stream<B> applyEndOfStreamOperators(Stream<B> stream) {
		if (comparator != null) {
			stream = stream.sorted(comparator);
		}
		if (limit != -1) {
			stream = stream.limit(limit);
		}
		return stream;
	}

	/**
	 * horribly naive - does not optimise with indicies -- (but good
	 * client-side)
	 * 
	 * @param stream
	 * @return
	 */
	public Stream<B> applyFilters(Stream<B> stream) {
		List<Predicate> predicates = getFilters().stream()
				.map(BindableFilter::asPredicate).collect(Collectors.toList());
		return stream.filter(e -> {
			for (Predicate predicate : predicates) {
				if (!predicate.test(e)) {
					return false;
				}
			}
			return true;
		});
	}

	public interface StreamProvider<B extends Bindable> {
		Stream<B> getStream(Class<B> bindableClass);
	}

	@Registration({ StreamProvider.class, Entity.class })
	public static class StreamProviderDomain implements StreamProvider<Entity> {
		@Override
		public Stream<Entity> getStream(Class<Entity> bindableClass) {
			return Domain.stream(bindableClass);
		}
	}
}
