package cc.alcina.framework.servlet.servlet.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.csobjects.SearchResultsBase;
import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.domain.search.BindableSearchDefinition;
import cc.alcina.framework.common.client.domain.search.DomainSearcher;
import cc.alcina.framework.common.client.domain.search.EntitySearchDefinition;
import cc.alcina.framework.common.client.domain.search.ModelSearchResults;
import cc.alcina.framework.common.client.domain.search.SearchContext;
import cc.alcina.framework.common.client.domain.search.SearchOrders;
import cc.alcina.framework.common.client.domain.search.SearcherCollectionSource;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.search.SearchCriterion;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.IntPair;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.ObjectWrapper;
import cc.alcina.framework.entity.MetricLogging;
import cc.alcina.framework.entity.persistence.domain.DomainStore;
import cc.alcina.framework.entity.projection.CollectionProjectionFilterWithCache;
import cc.alcina.framework.entity.projection.GraphProjection;
import cc.alcina.framework.entity.projection.GraphProjection.GraphProjectionContext;
import cc.alcina.framework.entity.projection.GraphProjections;
import cc.alcina.framework.gwt.client.entity.place.EntityPlace;

@Registration.Singleton
public class CommonSearchSupport {
	public static final transient String CONTEXT_DO_NOT_PROJECT_SEARCH = CommonSearchSupport.class
			.getName() + ".CONTEXT_DO_NOT_PROJECT_SEARCH";

	public static CommonSearchSupport get() {
		return Registry.impl(CommonSearchSupport.class);
	}

	public void copySearchMetadata(SearchDefinition from, SearchDefinition to) {
		to.setResultsPerPage(from.getResultsPerPage());
		to.setPageNumber(from.getPageNumber());
	}

	public <T extends Entity> ModelSearchResults getForClass(String className,
			long objectId) {
		try {
			T t = null;
			Class<T> clazz = Reflections.forName(className);
			t = Domain.find(clazz, objectId);
			List<T> list = new ArrayList<>();
			list.add(t);
			return getModelSearchResults(list, null);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public ModelSearchResults getModelSearchResults(
			List<? extends Entity> queried, EntitySearchDefinition def) {
		ModelSearchResults<?> modelSearchResults = new ModelSearchResults();
		modelSearchResults.setDef(def);
		if (LooseContext.is(CONTEXT_DO_NOT_PROJECT_SEARCH)) {
		} else {
			if (def == null) {
				// single object
				Entity first = CommonUtils.first(queried);
				queried.clear();
				List untyped = queried;
				modelSearchResults.setRawEntity(first);
				modelSearchResults.setResultClassName(Optional.ofNullable(first)
						.map(e -> e.entityClass().getName()).orElse(null));
				if (first != null) {
					untyped.add(project(first, modelSearchResults, true));
				}
			} else {
				queried = project(queried, modelSearchResults,
						def.isReturnSingleDataObjectImplementations());
				List<EntityPlace> filterPlaces = def.provideFilterPlaces();
				modelSearchResults.setFilteringEntities((List) filterPlaces
						.stream().map(EntityPlace::asLocator).map(Domain::find)
						.collect(Collectors.toList()));
				/*
				 * Just get non-entity properties, essentially - for things like
				 * breadcrumbs client-side
				 */
				Set<Entity> alsoProject = modelSearchResults
						.getFilteringEntities().stream()
						.map(Entity.Ownership::getOwningEntities)
						.flatMap(Collection::stream)
						.collect(Collectors.toSet());
				CollectionProjectionFilterWithCache dataFilter = new CollectionProjectionFilterWithCache() {
					@Override
					public <T> T filterData(T original, T projected,
							GraphProjectionContext context,
							GraphProjection graphProjection) throws Exception {
						if (original instanceof Set
								|| original instanceof Map) {
							return null;
						}
						if (original instanceof Entity
								&& !(modelSearchResults.getFilteringEntities()
										.contains(original))
								&& !(alsoProject.contains(original))) {
							return null;
						}
						return super.filterData(original, projected, context,
								graphProjection);
					}
				};
				modelSearchResults.setFilteringEntities(GraphProjections
						.defaultProjections().dataFilter(dataFilter)
						.project(modelSearchResults.getFilteringEntities()));
			}
		}
		modelSearchResults.setQueriedResultObjects((List) queried);
		return modelSearchResults;
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
				customSearchHandler.get().prepareContext(searchContext);
				ModelSearchResults searchModel = customSearchHandler.get()
						.searchModel(searchContext);
				if (searchModel != null) {
					return searchModel;
				}
			}
			Stream<? extends Entity> search = new DomainSearcher().search(def,
					clazz, searchContext.orders);
			Registry.impl(SearcherCollectionSource.class).beforeQuery(clazz,
					def);
			// FIXME - 2022 - there may be places where we can get result set
			// size without collecting (i.e. index-only)
			ObjectWrapper<Stream<? extends Entity>> streamRef = ObjectWrapper
					.of(search);
			List<Entity> rows = DomainStore.queryPool().call(
					() -> streamRef.get().collect(Collectors.toList()),
					streamRef, true);
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
				GroupingHandler groupingHandler = Registry
						.query(GroupingHandler.class)
						.addKeys(searchContext.groupingParameters.getClass())
						.impl();
				searchContext.modelSearchResults.setGroupedResult(
						groupingHandler.process(searchContext.queried,
								searchContext.groupingParameters, def));
				groupingHandler.sort(
						searchContext.modelSearchResults.getGroupedResult(),
						searchContext.groupingParameters);
				searchContext.modelSearchResults
						.setQueriedResultObjects(new ArrayList<>());
				if (customSearchHandler.isPresent()) {
					customSearchHandler.get().beforeProjection(searchContext);
				}
				if (LooseContext.is(CONTEXT_DO_NOT_PROJECT_SEARCH)) {
				} else {
					searchContext.modelSearchResults = GraphProjections
							.defaultProjections()
							.project(searchContext.modelSearchResults);
				}
			}
			searchContext.modelSearchResults.setDef(def);
			searchContext.modelSearchResults.setPageNumber(def.getPageNumber());
			searchContext.modelSearchResults.setRecordCount(rows.size());
			searchContext.modelSearchResults.setTransformLogPosition(
					DomainStore.stores().writableStore().getPersistenceEvents()
							.getQueue().getTransformCommitPosition());
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
		result.setQueriedResultObjects(searchResultsBase.getResults());
		result.setPageNumber(toSearchDefinition.getPageNumber());
		result.setDef(toSearchDefinition);
		result.setRecordCount(searchResultsBase.getTotalResultCount());
		result.setResultClassName(
				toSearchDefinition.queriedBindableClass().getName());
		return result;
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
			modelSearchResults.setResultClassName(projectedClass.getName());
		}
		return projector.project(object);
	}

	public static abstract class CustomSearchHandler<BSD extends BindableSearchDefinition> {
		private SearchContext searchContext;

		public void beforeProjection(SearchContext searchContext2) {
			// TODO Auto-generated method stub
		}

		public void prepareContext(SearchContext searchContext) {
		}

		public ModelSearchResults searchModel(SearchContext searchContext) {
			this.searchContext = searchContext;
			return searchModel0();
		}

		protected BSD getSearchDefinition() {
			return (BSD) searchContext.def;
		}

		protected abstract ModelSearchResults searchModel0();
	}

	@Registration(SearchResultProjector.class)
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
