package cc.alcina.framework.servlet.servlet.search;

import cc.alcina.framework.common.client.domain.search.BindableSearchDefinition;
import cc.alcina.framework.common.client.domain.search.BindableSearchDefinition.SearchPerformer;
import cc.alcina.framework.common.client.domain.search.ModelSearchResults;
import cc.alcina.framework.common.client.logic.reflection.Registration;

/**
 * Link implementations of BindableSearchDefinition.SearchPerformer
 */
public class SearchPerformers {
	@Registration({ SearchPerformer.class })
	public static class DefaultSearchPerformerImpl implements SearchPerformer {
		@Override
		public ModelSearchResults<?> search(BindableSearchDefinition def) {
			return DomainSearchHandler.get().searchModel(def);
		}
	}
}
