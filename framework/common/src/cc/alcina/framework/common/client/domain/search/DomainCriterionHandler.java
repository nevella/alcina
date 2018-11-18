package cc.alcina.framework.common.client.domain.search;

import cc.alcina.framework.common.client.domain.DomainFilter;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.search.SearchCriterion;
import cc.alcina.framework.common.client.search.SearchDefinition;

@RegistryLocation(registryPoint = DomainCriterionHandler.class)
@ClientInstantiable
public abstract class DomainCriterionHandler<SC extends SearchCriterion> {
	protected Class<SC> searchCriterionClass;

	protected Class<? extends SearchDefinition> searchDefinitionClass;

	public abstract DomainFilter getFilter(SC sc);

	public Class<SC> handlesSearchCriterion() {
		return searchCriterionClass;
	}

	public Class<? extends SearchDefinition> handlesSearchDefinition() {
		return searchDefinitionClass;
	}
}