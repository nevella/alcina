package cc.alcina.framework.gwt.client.entity.search;

import java.util.List;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.domain.search.SearchOrders;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.LooseContext;

@RegistryLocation(registryPoint = SearchContext.class, implementationType = ImplementationType.INSTANCE)
public class SearchContext {
	public static final String CONTEXT_INSTANCE = SearchContext.class
			+ ".CONTEXT_INSTANCE";

	public static SearchContext get() {
		SearchContext result = LooseContext.get(CONTEXT_INSTANCE);
		Preconditions.checkState(result != null);
		return result;
	}

	public static boolean has() {
		return LooseContext.get(CONTEXT_INSTANCE) != null;
	}

	public static SearchContext startContext() {
		SearchContext result = LooseContext.get(CONTEXT_INSTANCE);
		Preconditions.checkState(result == null);
		SearchContext impl = Registry.impl(SearchContext.class);
		LooseContext.set(CONTEXT_INSTANCE, impl);
		impl.start();
		return get();
	}

	public BindableSearchDefinition def;

	public SearchOrders orders;

	public GroupingParameters groupingParameters;

	public ModelSearchResults modelSearchResults;

	public List<Entity> queried;

	public void end() {
	}

	public void start() {
	}
}
