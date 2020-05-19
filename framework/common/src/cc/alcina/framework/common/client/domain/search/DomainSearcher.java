package cc.alcina.framework.common.client.domain.search;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.domain.CompositeFilter;
import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.domain.DomainFilter;
import cc.alcina.framework.common.client.domain.DomainQuery;
import cc.alcina.framework.common.client.logic.FilterCombinator;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.ClearStaticFieldsOnAppShutdown;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.search.CriteriaGroup;
import cc.alcina.framework.common.client.search.SearchCriterion;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.UnsortedMultikeyMap;

@RegistryLocation(registryPoint = ClearStaticFieldsOnAppShutdown.class)
public class DomainSearcher<T extends Entity> {
	public static final String CONTEXT_HINT = DomainSearcher.class.getName()
			+ ".CONTEXT_HINT";

	private static UnsortedMultikeyMap<DomainCriterionHandler> handlers = new UnsortedMultikeyMap<DomainCriterionHandler>(
			2);

	private static Map<Class, DomainDefinitionHandler> definitionHandlers = new LinkedHashMap<>();

	public static boolean useSequentialSearch = true;

	private static synchronized void setupHandlers() {
		Logger logger = LoggerFactory.getLogger(DomainSearcher.class);
		if (handlers.isEmpty()) {
			List<DomainCriterionHandler> impls = Registry
					.impls(DomainCriterionHandler.class);
			for (DomainCriterionHandler handler : impls) {
				handlers.put(handler.handlesSearchDefinition(),
						handler.handlesSearchCriterion(), handler);
				logger.debug(
						"registering search criterion handler: \n{} => {} :: {}",
						handler.getClass().getName(),
						handler.handlesSearchDefinition() == null
								? "(null defs)"
								: handler.handlesSearchDefinition()
										.getSimpleName(),
						handler.handlesSearchCriterion().getSimpleName());
			}
			List<DomainDefinitionHandler> defImpls = Registry
					.impls(DomainDefinitionHandler.class);
			for (DomainDefinitionHandler handler : defImpls) {
				definitionHandlers.put(handler.handlesSearchDefinition(),
						handler);
				logger.debug(
						"registering search definition handler: \n{} => {} ",
						handler.getClass().getName(),
						handler.handlesSearchDefinition() == null ? "(null)"
								: handler.handlesSearchDefinition()
										.getSimpleName());
			}
		}
	}

	private SearchDefinition def;

	private DomainQuery<T> query;

	public DomainSearcher() {
	}

	public Stream<T> search(SearchDefinition def, Class<T> clazz,
			Comparator<? super T> order) {
		query = Domain.query(clazz);
		query.sourceStream(Registry.impl(SearcherCollectionSource.class)
				.getSourceStreamFor(clazz, def));
		this.def = def;
		setupHandlers();
		processDefinitionHandler();
		processHandlers();
		Stream<T> stream = query.stream();
		stream = stream.filter(
				Registry.impl(DomainSearcherAppFilter.class).filter(def));
		stream = stream.sorted(order);
		return stream;
	}

	private DomainCriterionHandler getCriterionHandler(SearchCriterion sc) {
		return handlers.get(def.getClass(), sc.getClass());
	}

	private void processDefinitionHandler() {
		DomainDefinitionHandler handler = definitionHandlers
				.get(def.getClass());
		if (handler != null) {
			query.filter(handler.getFilter(def));
		}
	}

	protected void processHandlers() {
		Set<CriteriaGroup> criteriaGroups = def.getCriteriaGroups();
		for (CriteriaGroup cg : criteriaGroups) {
			if (!cg.provideIsEmpty()) {
				boolean or = cg.getCombinator() == FilterCombinator.OR;
				CompositeFilter cgFilter = new CompositeFilter(or);
				boolean added = false;
				for (SearchCriterion sc : (Set<SearchCriterion>) cg
						.getCriteria()) {
					DomainCriterionHandler handler = getCriterionHandler(sc);
					if (handler == null) {
						System.err.println(
								Ax.format("No handler for def/class %s - %s\n",
										def.getClass().getSimpleName(),
										sc.getClass().getSimpleName()));
						continue;
					}
					DomainFilter filter = handler.getFilter(sc);
					DomainSearcherFilter searcherFilter = new DomainSearcherFilter(
							filter, sc);
					if (filter != null) {
						if (or) {
							cgFilter.add(filter);
						} else {
							query.filter(searcherFilter);
						}
						added = true;
					}
				}
				if (added && or) {
					query.filter(cgFilter);
				}
			}
		}
	}

	@RegistryLocation(registryPoint = DomainLocker.class, implementationType = ImplementationType.SINGLETON)
	public static class DomainLocker {
		public void readLock(boolean lock) {
		}
	}

	@RegistryLocation(registryPoint = DomainSearcherAppFilter.class, implementationType = ImplementationType.INSTANCE)
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
		public CollectionFilter asCollectionFilter() {
			return this.filter.asCollectionFilter();
		}

		@Override
		public boolean canFlatten() {
			return this.filter.canFlatten();
		}

		@Override
		public DomainFilter invertIf(boolean invert) {
			return this.filter.invertIf(invert);
		}

		@Override
		public String toString() {
			return this.filter.toString();
		}
	}
}
