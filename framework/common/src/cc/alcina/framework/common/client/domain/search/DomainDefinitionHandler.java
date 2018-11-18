package cc.alcina.framework.common.client.domain.search;

import cc.alcina.framework.common.client.domain.DomainFilter;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.search.SearchDefinition;

@RegistryLocation(registryPoint = DomainDefinitionHandler.class)
@ClientInstantiable
public abstract class DomainDefinitionHandler<SD extends SearchDefinition> {
	public abstract DomainFilter getFilter(SD sc);

	public abstract Class<SD> handlesSearchDefinition();
}