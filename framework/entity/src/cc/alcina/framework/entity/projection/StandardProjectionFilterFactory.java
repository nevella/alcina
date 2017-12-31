package cc.alcina.framework.entity.projection;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocations;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry.RegistryFactory;

@RegistryLocations({
		@RegistryLocation(registryPoint = PermissibleFieldFilter.class, implementationType = ImplementationType.FACTORY),
		@RegistryLocation(registryPoint = CollectionProjectionFilter.class, implementationType = ImplementationType.FACTORY) })
public class StandardProjectionFilterFactory
		implements RegistryFactory<Object> {
	@Override
	public Object create(Class registryPoint, Class targetObjectClass) {
		if (registryPoint == PermissibleFieldFilter.class) {
			return new PermissibleFieldFilter();
		} else {
			return new CollectionProjectionFilter();
		}
	}
}
