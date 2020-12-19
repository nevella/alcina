package cc.alcina.framework.servlet.servlet.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.csobjects.SearchResultsBase;
import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.domain.search.DomainSearcher;
import cc.alcina.framework.common.client.domain.search.SearchOrders;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.search.SearchCriterion;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.IntPair;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.MetricLogging;
import cc.alcina.framework.entity.persistence.cache.DomainStore;
import cc.alcina.framework.entity.projection.GraphProjection;
import cc.alcina.framework.entity.projection.GraphProjections;
import cc.alcina.framework.gwt.client.entity.place.EntityPlace;
import cc.alcina.framework.gwt.client.entity.search.BindableSearchDefinition;
import cc.alcina.framework.gwt.client.entity.search.EntitySearchDefinition;
import cc.alcina.framework.gwt.client.entity.search.ModelSearchResults;
import cc.alcina.framework.gwt.client.entity.search.SearchContext;

@RegistryLocation(registryPoint = CommonSearchSupport.class, implementationType = ImplementationType.INSTANCE)
public class CommonSearchSupport {
	public static final transient String CONTEXT_DO_NOT_PROJECT_SEARCH = CommonSearchSupport.class
			.getName() + ".CONTEXT_DO_NOT_PROJECT_SEARCH";

	public static CommonSearchSupport get() {
		CommonSearchSupport singleton = Registry
				.checkSingleton(CommonSearchSupport.class);
		if (singleton == null) {
			singleton = new CommonSearchSupport();
			Registry.registerSingleton(CommonSearchSupport.class, singleton);
		}
		return singleton;
	}

	public void copySearchMetadata(SearchDefinition from, SearchDefinition to) {
		to.setResultsPerPage(from.getResultsPerPage());
	}

	public <T extends Entity> ModelSearchResults getForClass(String className,
			long objectId) {
		try {
			T t = null;
			Class<?> clazz = Class.forName(className);
			t = Domain.find(clazz, objectId);
			List<T> list = new ArrayList<>();
			list.add(t);
			return getModelSearchResults(list, null);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public ModelSearchResults searchModel(BindableSearchDefinition def) {
		if (def.getGroupingParameters() != null) {
			def.setResultsPerPage(99999999);
		}
		String key = LooseContext.runWithTrue(
				SearchCriterion.CONTEXT_ENSURE_DISPLAY_NAME,
				() -> "searchModel - " + def);
		try {
			LooseContext.push();
			SearchContext searchContext = SearchContext.startContext();
			searchContext.def = def;
			searchContext.orders = def.getSearchOrders();
			searchContext.groupingParameters = def.getGroupingParameters();
			MetricLogging.get().start(key);
			def.initialiseContext();
			Class clazz = def.queriedBindableClass();
			Optional<SearchOrders> idOrder = def.provideIdSearchOrder();
			if (idOrder.isPresent()) {
				searchContext.orders = idOrder.get();
			}
			Optional<CustomSearchHandler> customSearchHandler = Registry
					.optional(CustomSearchHandler.class, def.getClass());
			if (customSearchHandler.isPresent()) {
				return customSearchHandler.get().searchModel(searchContext);
			}
			Stream<Entity> search = new DomainSearcher().search(def, clazz,
					searchContext.orders);
			List<Entity> rows = search.collect(Collectors.toList());
			IntPair range = new IntPair(
					def.getResultsPerPage() * (def.getPageNumber()),
					def.getResultsPerPage() * (def.getPageNumber() + 1));
			range = range.intersection(new IntPair(0, rows.size()));
			searchContext.queried = range == null ? new ArrayList<Entity>()
					: new ArrayList<Entity>(rows.subList(range.i1, range.i2));
			if (searchContext.groupingParameters == null) {
				searchContext.modelSearchResults = getModelSearchResults(
						searchContext.queried, (EntitySearchDefinition) def);
			} else {
				searchContext.modelSearchResults = new ModelSearchResults();
				GroupingHandler groupingHandler = Registry.impl(
						GroupingHandler.class,
						searchContext.groupingParameters.getClass());
				searchContext.modelSearchResults.groupedResult = groupingHandler
						.process(searchContext.queried,
								searchContext.groupingParameters, def);
				groupingHandler.sort(
						searchContext.modelSearchResults.groupedResult,
						searchContext.groupingParameters);
				searchContext.modelSearchResults.queriedResultObjects = new ArrayList<>();
				if (LooseContext.is(CONTEXT_DO_NOT_PROJECT_SEARCH)) {
				} else {
					searchContext.modelSearchResults = GraphProjections
							.defaultProjections()
							.project(searchContext.modelSearchResults);
				}
			}
			searchContext.modelSearchResults.def = def;
			searchContext.modelSearchResults.pageNumber = def.getPageNumber();
			searchContext.modelSearchResults.recordCount = rows.size();
			searchContext.modelSearchResults.transformLogPosition = DomainStore
					.stores().writableStore().getPersistenceEvents().getQueue()
					.getTransformCommitPosition();
			return searchContext.modelSearchResults;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		} finally {
			MetricLogging.get().end(key);
			LooseContext.pop();
		}
	}

	public ModelSearchResults toModelSearchResults(
			SearchResultsBase searchResultsBase,
			BindableSearchDefinition toSearchDefinition) {
		ModelSearchResults result = new ModelSearchResults();
		result.queriedResultObjects = searchResultsBase.getResults();
		result.pageNumber = toSearchDefinition.getPageNumber();
		result.def = toSearchDefinition;
		result.recordCount = searchResultsBase.getTotalResultCount();
		result.resultClassName = toSearchDefinition.queriedBindableClass()
				.getName();
		return result;
	}

	private ModelSearchResults getModelSearchResults(
			List<? extends Entity> queried, EntitySearchDefinition def) {
		ModelSearchResults modelSearchResults = new ModelSearchResults();
		modelSearchResults.def = def;
		if (LooseContext.is(CONTEXT_DO_NOT_PROJECT_SEARCH)) {
		} else {
			if (def == null) {
				// single object
				Entity first = CommonUtils.first(queried);
				queried.clear();
				List untyped = queried;
				modelSearchResults.rawEntity = first;
				modelSearchResults.resultClassName = Optional.ofNullable(first)
						.map(e -> e.entityClass().getName()).orElse(null);
				if (first != null) {
					untyped.add(project(first, modelSearchResults, true));
				}
			} else {
				queried = project(queried, modelSearchResults,
						def.isReturnSingleDataObjectImplementations());
				List<EntityPlace> filterPlaces = def.provideFilterPlaces();
				modelSearchResults.filteringEntities = filterPlaces.stream()
						.map(EntityPlace::asLocator).map(Domain::find)
						.collect(Collectors.toList());
				/*
				 * Just get non-entity properties, essentially - for things like
				 * breadcrumbs client-side
				 */
				modelSearchResults.filteringEntities = GraphProjection
						.maxDepthProjection(
								modelSearchResults.filteringEntities, 1, null);
				;
			}
		}
		modelSearchResults.queriedResultObjects = queried;
		return modelSearchResults;
	}

	private <T> T project(T object, ModelSearchResults modelSearchResults,
			boolean projectAsSingleEntityDataObjects) {
		SearchResultProjector projector = Registry
				.impl(SearchResultProjector.class);
		projector.setProjectAsSingleEntityDataObjects(
				projectAsSingleEntityDataObjects);
		Class<? extends Bindable> projectedClass = projector
				.getProjectedClass(modelSearchResults);
		if (projectedClass != null) {
			modelSearchResults.resultClassName = projectedClass.getName();
		}
		return projector.project(object);
	}

	public static abstract class CustomSearchHandler<BSD extends BindableSearchDefinition> {
		private SearchContext searchContext;

		public ModelSearchResults searchModel(SearchContext searchContext) {
			this.searchContext = searchContext;
			return searchModel0();
		}

		protected BSD getSearchDefinition() {
			return (BSD) searchContext.def;
		}

		protected abstract ModelSearchResults searchModel0();
	}

	@RegistryLocation(registryPoint = SearchResultProjector.class, implementationType = ImplementationType.INSTANCE)
	public static class SearchResultProjector {
		private boolean projectAsSingleEntityDataObjects;

		public Class<? extends Bindable>
				getProjectedClass(ModelSearchResults modelSearchResults) {
			return modelSearchResults.resultClass();
		}

		public boolean isProjectAsSingleEntityDataObjects() {
			return this.projectAsSingleEntityDataObjects;
		}

		public <T> T project(T object) {
			return GraphProjections.defaultProjections().project(object);
		}

		public void setProjectAsSingleEntityDataObjects(
				boolean projectAsSingleEntityDataObjects) {
			this.projectAsSingleEntityDataObjects = projectAsSingleEntityDataObjects;
		}
	}
}
