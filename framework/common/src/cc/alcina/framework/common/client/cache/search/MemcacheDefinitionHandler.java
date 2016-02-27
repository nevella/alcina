package cc.alcina.framework.common.client.cache.search;

import cc.alcina.framework.common.client.cache.CacheFilter;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.search.SearchDefinition;

@RegistryLocation(registryPoint = MemcacheDefinitionHandler.class)
@ClientInstantiable
public abstract class MemcacheDefinitionHandler<SD extends SearchDefinition> {

	public abstract CacheFilter getFilter(SD sc);

	public abstract Class<SD> handlesSearchDefinition() ;
}