package cc.alcina.framework.common.client.domain.search;

import java.util.List;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

@Registration(SearchContext.class)
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
