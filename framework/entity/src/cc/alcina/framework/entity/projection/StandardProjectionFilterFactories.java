package cc.alcina.framework.entity.projection;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry.RegistryFactory;

public class StandardProjectionFilterFactories {
	@Registration(value = CollectionProjectionFilter.class, implementation = Registration.Implementation.FACTORY)
	public static class ProjectionDomainFilterFactory
			implements RegistryFactory<CollectionProjectionFilter> {
		@Override
		public CollectionProjectionFilter impl() {
			return new CollectionProjectionFilter();
		}
	}

	@Registration(value = PermissibleFieldFilter.class, implementation = Registration.Implementation.FACTORY)
	public static class ProjectionFieldFilterFactory
			implements RegistryFactory<PermissibleFieldFilter> {
		@Override
		public PermissibleFieldFilter impl() {
			return new PermissibleFieldFilter();
		}
	}
}
