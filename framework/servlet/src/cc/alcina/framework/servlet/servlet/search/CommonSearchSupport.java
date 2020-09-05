package cc.alcina.framework.servlet.servlet.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.domain.search.DomainSearcher;
import cc.alcina.framework.common.client.domain.search.SearchOrders;
import cc.alcina.framework.common.client.logic.domain.VersionableEntity;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.search.SearchCriterion;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.IntPair;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.MetricLogging;
import cc.alcina.framework.entity.entityaccess.cache.DomainStore;
import cc.alcina.framework.entity.projection.GraphProjection;
import cc.alcina.framework.entity.projection.GraphProjections;
import cc.alcina.framework.gwt.client.entity.place.EntityPlace;
import cc.alcina.framework.gwt.client.entity.search.EntitySearchDefinition;
import cc.alcina.framework.gwt.client.entity.search.ModelSearchResults;
import cc.alcina.framework.gwt.client.entity.search.SearchContext;

@RegistryLocation(registryPoint = CommonSearchSupport.class, implementationType = ImplementationType.INSTANCE)
public class CommonSearchSupport {
	public static CommonSearchSupport get() {
		CommonSearchSupport singleton = Registry
				.checkSingleton(CommonSearchSupport.class);
		if (singleton == null) {
			singleton = new CommonSearchSupport();
			Registry.registerSingleton(CommonSearchSupport.class, singleton);
		}
		return singleton;
	}

	public static final transient String CONTEXT_DO_NOT_PROJECT_SEARCH = CommonSearchSupport.class
			.getName() + ".CONTEXT_DO_NOT_PROJECT_SEARCH";

	public <T extends VersionableEntity> ModelSearchResults
			getForClass(String className, long objectId) {
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

	private ModelSearchResults getModelSearchResults(
			List<? extends VersionableEntity> queried,
			EntitySearchDefinition def) {
		ModelSearchResults modelSearchResults = new ModelSearchResults();
		modelSearchResults.def = def;
		if (LooseContext.is(CONTEXT_DO_NOT_PROJECT_SEARCH)) {
		} else {
			if (def == null) {
				// single object
				VersionableEntity first = CommonUtils.first(queried);
				queried.clear();
				List untyped = queried;
				modelSearchResults.rawEntity = first;
				modelSearchResults.resultClassName = Optional.ofNullable(first)
						.map(e -> e.entityClass().getName()).orElse(null);
				untyped.add(project(first, modelSearchResults,true));
			} else {
				queried = project(queried, modelSearchResults,def.isReturnSingleDataObjectImplementations());
				List<EntityPlace> filterPlaces = def.provideFilterPlaces();
				modelSearchResults.filteringEntities = filterPlaces.stream().map(EntityPlace::asLocator)
						.map(Domain::find).collect(Collectors.toList());
				modelSearchResults.filteringEntities =GraphProjection.maxDepthProjection(modelSearchResults.filteringEntities,2,null);
			}
		}
		modelSearchResults.queriedResultObjects = queried;
		return modelSearchResults;
	}

	private <T> T project(T object, ModelSearchResults modelSearchResults, boolean projectAsSingleEntityDataObjects) {
		SearchResultProjector projector = Registry
				.impl(SearchResultProjector.class);
		projector.setProjectAsSingleEntityDataObjects(projectAsSingleEntityDataObjects);
		Class<? extends Bindable> projectedClass = projector
				.getProjectedClass(modelSearchResults);
		if (projectedClass != null) {
			modelSearchResults.resultClassName = projectedClass.getName();
		}
		return projector.project(object);
	}

	@RegistryLocation(registryPoint = SearchResultProjector.class, implementationType = ImplementationType.INSTANCE)
	public static class SearchResultProjector {
		private boolean projectAsSingleEntityDataObjects;
		public boolean isProjectAsSingleEntityDataObjects() {
			return this.projectAsSingleEntityDataObjects;
		}

		public void setProjectAsSingleEntityDataObjects(
				boolean projectAsSingleEntityDataObjects) {
			this.projectAsSingleEntityDataObjects = projectAsSingleEntityDataObjects;
		}

		public <T> T project(T object) {
			return GraphProjections.defaultProjections().project(object);
		}

		public Class<? extends Bindable>
				getProjectedClass(ModelSearchResults modelSearchResults) {
			return modelSearchResults.resultClass();
		}
	}

	public ModelSearchResults searchModel(EntitySearchDefinition def,Function<SearchContext,ModelSearchResults> customSearchHandler) {
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
			Class clazz = def.resultClass();
			Optional<SearchOrders> idOrder = def.provideIdSearchOrder();
			if (idOrder.isPresent()) {
				searchContext.orders = idOrder.get();
			}
			if(customSearchHandler!=null) {
				ModelSearchResults customResults = customSearchHandler.apply(searchContext);
				if(customResults!=null) {
					return customResults;
				}
			}
			Stream<VersionableEntity> search = new DomainSearcher().search(def,
					clazz, searchContext.orders);
			List<VersionableEntity> rows = search.collect(Collectors.toList());
			IntPair range = new IntPair(
					def.getResultsPerPage() * (def.getPageNumber()),
					def.getResultsPerPage() * (def.getPageNumber() + 1));
			range = range.intersection(new IntPair(0, rows.size()));
			searchContext.queried = range == null
					? new ArrayList<VersionableEntity>()
					: new ArrayList<VersionableEntity>(
							rows.subList(range.i1, range.i2));
			if (searchContext.groupingParameters == null) {
				searchContext.modelSearchResults = getModelSearchResults(
						searchContext.queried, def);
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
					.getTransformLogPosition();
			return searchContext.modelSearchResults;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		} finally {
			MetricLogging.get().end(key);
			LooseContext.pop();
		}
	}
}
