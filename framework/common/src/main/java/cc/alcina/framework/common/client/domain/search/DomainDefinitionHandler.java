package cc.alcina.framework.common.client.domain.search;

import cc.alcina.framework.common.client.domain.BindableFilter;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.search.SearchDefinition;

@Reflected
@Registration(DomainDefinitionHandler.class)
public abstract class DomainDefinitionHandler<SD extends SearchDefinition>
		implements Registration.AllSubtypes {
	public abstract BindableFilter getFilter(SD sc);

	public abstract Class<SD> handlesSearchDefinition();
}
