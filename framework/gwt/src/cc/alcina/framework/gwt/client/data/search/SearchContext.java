package cc.alcina.framework.gwt.client.data.search;

import java.util.List;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.domain.search.SearchOrders;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.gwt.client.data.entity.DataDomainBase;

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

	public static SearchContext start() {
		SearchContext result = LooseContext.get(CONTEXT_INSTANCE);
		Preconditions.checkState(result == null);
		LooseContext.set(CONTEXT_INSTANCE, new SearchContext());
		return get();
	}

	public DataSearchDefinition def;

	public SearchOrders orders;

	public GroupingParameters groupingParameters;

	public ModelSearchResults modelSearchResults;

	public List<DataDomainBase> queried;
}
