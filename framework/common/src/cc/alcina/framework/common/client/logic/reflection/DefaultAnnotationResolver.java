package cc.alcina.framework.common.client.logic.reflection;

import cc.alcina.framework.common.client.logic.reflection.AnnotationLocation.Resolver;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;

@RegistryLocation(registryPoint = Resolver.class, implementationType = ImplementationType.SINGLETON)
@ClientInstantiable
@Registration.Singleton(Resolver.class)
// FIXME
public class DefaultAnnotationResolver extends Resolver {
}