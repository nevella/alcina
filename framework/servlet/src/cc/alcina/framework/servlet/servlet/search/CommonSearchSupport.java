package cc.alcina.framework.servlet.servlet.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.WrappedRuntimeException;
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
import cc.alcina.framework.entity.projection.GraphProjections;
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
		if (LooseContext.is(CONTEXT_DO_NOT_PROJECT_SEARCH)) {
		} else {
			if (def == null) {
				// single object
				VersionableEntity first = CommonUtils.first(queried);
				queried.clear();
				List untyped = queried;
				untyped.add(
						GraphProjections.defaultProjections().project(first));
			} else {
				queried = GraphProjections.defaultProjections()
						.project(queried);
			}
		}
		modelSearchResults.queriedResultObjects = queried;
		return modelSearchResults;
	}

	public ModelSearchResults searchModel(EntitySearchDefinition def) {
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
