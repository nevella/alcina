package cc.alcina.framework.common.client.cache.search;

import cc.alcina.framework.common.client.cache.CacheFilter;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.search.SearchCriterion;
import cc.alcina.framework.common.client.search.SearchDefinition;

@RegistryLocation(registryPoint = MemcacheCriterionHandler.class)
@ClientInstantiable
public abstract class MemcacheCriterionHandler<SC extends SearchCriterion> {
	protected Class<SC> searchCriterionClass;

	protected Class<? extends SearchDefinition> searchDefinitionClass;

	

	public abstract CacheFilter getFilter(SC sc);

	public Class<SC> handlesSearchCriterion() {
		return searchCriterionClass;
	}

	public Class<? extends SearchDefinition> handlesSearchDefinition() {
		return searchDefinitionClass;
	}
}