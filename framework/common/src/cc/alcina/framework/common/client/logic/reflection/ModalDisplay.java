package cc.alcina.framework.common.client.logic.reflection;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.logic.reflection.AnnotationLocation.Resolver;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

@Retention(RetentionPolicy.RUNTIME)
@Documented
@ClientVisible
@Target({ ElementType.METHOD })
public @interface ModalDisplay {
	Modal[] value();

	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@ClientVisible
	@Target({ ElementType.METHOD })
	public @interface Modal {
		Custom[] custom() default {};

		Display[] display() default {};

		Mode mode();

		Validator[] validator() default {};
	}

	public static class ModalResolver implements Resolver {
		public static ModalResolver multiple(boolean readOnly) {
			return new ModalResolver(
					readOnly ? Mode.MULTIPLE_READ : Mode.MULTIPLE_WRITE);
		}

		public static ModalResolver single(boolean readOnly) {
			return new ModalResolver(
					readOnly ? Mode.SINGLE_READ : Mode.SINGLE_WRITE);
		}

		private final Mode mode;

		private ModalResolver(Mode mode) {
			this.mode = Registry.impl(ModeTransformer.class).apply(mode);
		}

		@Override
		public <A extends Annotation> A getAnnotation(Class<A> annotationClass,
				AnnotationLocation location) {
			boolean customResolution = annotationClass == Display.class
					|| annotationClass == Custom.class
					|| annotationClass == Validator.class;
			A defaultResolution = Resolver.super.getAnnotation(annotationClass,
					location);
			if (customResolution) {
				RequireSpecified requireSpecified = Reflections.classLookup()
						.getAnnotationForClass(location.fallbackToClass,
								RequireSpecified.class);
				ModalDisplay modalDisplay = Resolver.super.getAnnotation(
						ModalDisplay.class, location);
				A modalResolution = null;
				Optional<Modal> matchingModal = Optional.empty();
				if (modalDisplay != null) {
					matchingModal = Arrays.stream(modalDisplay.value())
							.filter(m -> m.mode().matches(this.mode))
							.findFirst();
					if (matchingModal.isPresent()) {
						Modal modal = matchingModal.get();
						if (annotationClass == Display.class) {
							modalResolution = (A) getFirst(modal.display());
						} else if (annotationClass == Custom.class) {
							modalResolution = (A) getFirst(modal.custom());
						} else if (annotationClass == Validator.class) {
							modalResolution = (A) getFirst(modal.validator());
						}
						Preconditions.checkState(modal.custom().length <= 1);
					}
				}
				if (modalResolution != null) {
					return modalResolution;
				} else {
					if (requireSpecified == null
							|| Arrays.stream(requireSpecified.value())
									.noneMatch(m -> m.matches(mode))
							|| matchingModal.isPresent()) {
						return defaultResolution;
					} else {
						return null;
					}
				}
			} else {
				return defaultResolution;
			}
		}

		private <T> T getFirst(T[] array) {
			Preconditions.checkArgument(array.length <= 1);
			return array.length == 0 ? null : array[0];
		}
	}

	@ClientInstantiable
	public enum Mode {
		SINGLE_ANY, SINGLE_READ, SINGLE_WRITE, MULTIPLE_ANY, MULTIPLE_READ,
		MULTIPLE_WRITE;

		public boolean isMultiple() {
			return !isSingle();
		}

		public boolean isSingle() {
			switch (this) {
			case SINGLE_ANY:
			case SINGLE_READ:
			case SINGLE_WRITE:
				return true;
			default:
				return false;
			}
		}

		public boolean matches(Mode other) {
			if (this == other) {
				return true;
			} else {
				return (this.isAny() || other.isAny())
						&& this.isSameArity(other);
			}
		}

		private boolean isAny() {
			switch (this) {
			case SINGLE_ANY:
			case MULTIPLE_ANY:
				return true;
			default:
				return false;
			}
		}

		private boolean isSameArity(Mode other) {
			return this.isSingle() ^ !other.isSingle();
		}
	}

	@RegistryLocation(registryPoint = ModeTransformer.class, implementationType = ImplementationType.INSTANCE)
	@ClientInstantiable
	public static class ModeTransformer implements Function<Mode, Mode> {
		@Override
		public Mode apply(Mode mode) {
			return mode;
		}
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@ClientVisible
	@Target({ ElementType.TYPE })
	public @interface RequireSpecified {
		Mode[] value();
	}
}
