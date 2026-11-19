package cc.alcina.framework.common.client.domain.search;

import cc.alcina.framework.common.client.domain.BindableFilter;
import cc.alcina.framework.common.client.search.SearchCriterion;

public interface DomainCriterionFilter<SC extends SearchCriterion> {
	BindableFilter getFilter(SC sc);
}
