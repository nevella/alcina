package cc.alcina.framework.common.client.domain.search;

import cc.alcina.framework.common.client.domain.DomainFilter;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.search.SearchCriterion;
import cc.alcina.framework.common.client.search.SearchDefinition;

@Registration(DomainCriterionHandler.class)
public abstract class DomainCriterionHandler<SC extends SearchCriterion> {
	public abstract DomainFilter getFilter(SC sc);

	public final Class<SC> handlesSearchCriterion() {
		return Reflections.at(getClass()).getGenericBounds().bounds.get(0);
	}

	public abstract Class<? extends SearchDefinition> handlesSearchDefinition();
}
