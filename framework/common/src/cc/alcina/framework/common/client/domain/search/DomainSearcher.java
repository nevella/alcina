package cc.alcina.framework.common.client.domain.search;

import java.util.Comparator;
import java.util.function.Predicate;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.collections.FilterOperator;
import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.domain.DomainFilter;
import cc.alcina.framework.common.client.domain.DomainQuery;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.ClearStaticFieldsOnAppShutdown;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.search.SearchCriterion;
import cc.alcina.framework.common.client.search.SearchDefinition;

/**
 * 
 * <p>
 * This class takes a SearchDefinition def and a Comparator Order and returns a
 * Stream of entities.
 * 
 * <p>
 * It sets up a DomainStore query, looks up reflectively registered
 * DomainDefinitionHandler classes for the SearchDefinition (rarely used, since
 * these operate on the whole definition), and then looks up the criterion
 * handler classes for the [SearchDefinition, SearchCriterion] combinations.
 * Those criterion handler classes generate filters which are applied to the
 * DomainStore query.
 * 
 * <p>
 * The reason handlers are registered for [SearchDefinition, SearchCriterion]
 * tuples is that it improves code reuse, since SearchCriterion classes can be
 * used in multiple SearchDefinition types.
 * 
 * 
 *
 * @param <T>
 *            The entity type being searched
 */
@Registration(ClearStaticFieldsOnAppShutdown.class)
public class DomainSearcher<T extends Entity> {
	public static final String CONTEXT_HINT = DomainSearcher.class.getName()
			+ ".CONTEXT_HINT";

	public static boolean useSequentialSearch = true;

	private DomainQuery<T> query;

	public DomainSearcher() {
	}

	public Stream<T> search(SearchDefinition def, Class<T> clazz,
			Comparator<? super T> order) {
		query = Domain.query(clazz);
		query.sourceStream(Registry.impl(SearcherCollectionSource.class)
				.getSourceStreamFor(clazz, def));
		SearchHandlers.ensureHandlers();
		SearchHandlers.processDefinitionHandler(def, this::addFilter);
		SearchHandlers.processHandlers(def, this::addFilter);
		Stream<T> stream = query.stream();
		stream = stream.filter(
				Registry.impl(DomainSearcherAppFilter.class).filter(def));
		stream = stream.sorted(order);
		return stream;
	}

	void addFilter(DomainFilter filter) {
		query.filter(filter);
	}

	@Registration(DomainSearcherAppFilter.class)
	public static abstract class DomainSearcherAppFilter {
		public abstract <T extends Entity> Predicate<T>
				filter(SearchDefinition def);
	}

	public static class DomainSearcherAppFilter_DefaultImpl
			extends DomainSearcherAppFilter {
		@Override
		public <T extends Entity> Predicate<T> filter(SearchDefinition def) {
			return t -> true;
		}
	}

	public static class DomainSearcherFilter extends DomainFilter {
		public SearchCriterion criterion;

		public DomainFilter filter;

		public DomainSearcherFilter(DomainFilter filter,
				SearchCriterion criterion) {
			this.filter = filter;
			this.criterion = criterion;
		}

		@Override
		public Predicate asPredicate() {
			return this.filter.asPredicate();
		}

		@Override
		public boolean canFlatten() {
			return this.filter.canFlatten();
		}

		@Override
		public FilterOperator getFilterOperator() {
			return this.filter.getFilterOperator();
		}

		@Override
		public Predicate getPredicate() {
			return this.filter.getPredicate();
		}

		@Override
		public String getPropertyPath() {
			return this.filter.getPropertyPath();
		}

		@Override
		public Object getPropertyValue() {
			return this.filter.getPropertyValue();
		}

		@Override
		public DomainFilter invertIf(boolean invert) {
			return this.filter.invertIf(invert);
		}

		@Override
		public void setFilterOperator(FilterOperator filterOperator) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setPredicate(Predicate predicate) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setPropertyPath(String propertyPath) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setPropertyValue(Object propertyValue) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String toString() {
			return this.filter.toString();
		}
	}
}
