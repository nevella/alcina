package cc.alcina.framework.entity.impl.jboss;

import org.hibernate.criterion.Criterion;

import cc.alcina.framework.common.client.search.SearchCriterion;

/**
 * These are not registered using the {@code @Registration.NonGenericSubtypes}
 * pattern because the implementations are generally non-static inner classes
 */
public abstract class SearchCriterionHandler<S extends SearchCriterion> {
	public abstract Criterion handle(S searchCriterion);
}
