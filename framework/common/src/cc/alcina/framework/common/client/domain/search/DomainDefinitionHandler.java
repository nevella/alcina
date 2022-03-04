package cc.alcina.framework.common.client.domain.search;

import cc.alcina.framework.common.client.domain.DomainFilter;
import cc.alcina.framework.common.client.logic.reflection.Reflected;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.search.SearchDefinition;

@Reflected
@Registration(DomainDefinitionHandler.class)
public abstract class DomainDefinitionHandler<SD extends SearchDefinition> {
	public abstract DomainFilter getFilter(SD sc);

	public abstract Class<SD> handlesSearchDefinition();
}
