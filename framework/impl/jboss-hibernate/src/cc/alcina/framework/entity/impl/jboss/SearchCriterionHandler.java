package cc.alcina.framework.entity.impl.jboss;

import org.hibernate.criterion.Criterion;

import cc.alcina.framework.common.client.search.SearchCriterion;


public abstract class SearchCriterionHandler<S extends SearchCriterion> {
	

	public abstract Criterion handle(S searchCriterion);
}
