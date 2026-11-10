package cc.alcina.framework.common.client.domain.search;

import cc.alcina.framework.common.client.domain.DomainFilter;
import cc.alcina.framework.common.client.search.SearchCriterion;

public interface DomainCriterionFilter<SC extends SearchCriterion> {
	DomainFilter getFilter(SC sc);
}
