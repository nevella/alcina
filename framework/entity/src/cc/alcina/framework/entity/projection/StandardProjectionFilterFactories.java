package cc.alcina.framework.entity.projection;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry.RegistryFactory;

public class StandardProjectionFilterFactories {
	@RegistryLocation(registryPoint = CollectionProjectionFilter.class, implementationType = ImplementationType.FACTORY)
	public static class ProjectionDomainFilterFactory
			implements RegistryFactory<CollectionProjectionFilter> {
		@Override
		public CollectionProjectionFilter impl() {
			return new CollectionProjectionFilter();
		}
	}

	@RegistryLocation(registryPoint = PermissibleFieldFilter.class, implementationType = ImplementationType.FACTORY)
	public static class ProjectionFieldFilterFactory
			implements RegistryFactory<PermissibleFieldFilter> {
		@Override
		public PermissibleFieldFilter impl() {
			return new PermissibleFieldFilter();
		}
	}
}
