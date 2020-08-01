package cc.alcina.framework.common.client.domain;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Function;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.PropertyReflector;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.TreeResolver;

@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Target({ ElementType.METHOD })
public @interface DomainStoreProperty {
	boolean ignoreMismatchedCollectionModifications() default false;

	DomainStorePropertyLoadType loadType() default DomainStorePropertyLoadType.TRANSIENT;

	public enum DomainStorePropertyLoadType {
		TRANSIENT, LAZY, EAGER;
	}

	@RegistryLocation(registryPoint = DomainStorePropertyResolver.class, implementationType = ImplementationType.INSTANCE)
	@ClientInstantiable
	public static class DomainStorePropertyResolver
			implements DomainStoreProperty {
		// for reflection
		public DomainStorePropertyResolver() {
		}

		protected TreeResolver<DomainStoreProperty> resolver;

		public DomainStorePropertyResolver(
				DomainStorePropertyResolver childResolver) {
			resolver = createResolver(childResolver.resolver);
		}

		public DomainStorePropertyResolver(
				PropertyReflector.Location propertyLocation) {
			resolver = new TreeResolver<DomainStoreProperty>(propertyLocation,
					propertyLocation.propertyReflector
							.getAnnotation(DomainStoreProperty.class));
		}

		@Override
		public Class<? extends Annotation> annotationType() {
			return DomainStoreProperty.class;
		}

		@Override
		public boolean ignoreMismatchedCollectionModifications() {
			Function<DomainStoreProperty, Boolean> function = DomainStoreProperty::ignoreMismatchedCollectionModifications;
			return resolver.resolve(function,
					"ignoreMismatchedCollectionModifications");
		}

		@Override
		public DomainStorePropertyLoadType loadType() {
			Function<DomainStoreProperty, DomainStorePropertyLoadType> function = DomainStoreProperty::loadType;
			return resolver.resolve(function, "loadType");
		}

		protected TreeResolver<DomainStoreProperty>
				createResolver(TreeResolver<DomainStoreProperty> resolver) {
			return new TreeResolver<DomainStoreProperty>(resolver);
		}
	}
}
