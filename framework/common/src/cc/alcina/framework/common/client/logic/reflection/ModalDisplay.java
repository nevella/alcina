package cc.alcina.framework.common.client.logic.reflection;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.logic.reflection.reachability.ClientVisible;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.logic.reflection.resolution.AbstractMergeStrategy;
import cc.alcina.framework.common.client.logic.reflection.resolution.AnnotationLocation;
import cc.alcina.framework.common.client.logic.reflection.resolution.Resolution;
import cc.alcina.framework.common.client.logic.reflection.resolution.Resolution.Inheritance;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.gwt.client.dirndl.layout.ContextResolver;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;
import cc.alcina.framework.gwt.client.dirndl.model.FormModel;
import cc.alcina.framework.gwt.client.dirndl.model.TableModel;

@Retention(RetentionPolicy.RUNTIME)
@Documented
@ClientVisible
@Target({ ElementType.METHOD })
@Resolution(
	inheritance = { Inheritance.PROPERTY },
	mergeStrategy = ModalDisplay.MergeStrategy.class)
public @interface ModalDisplay {
	Modal[] value();

	@Reflected
	public static class MergeStrategy extends
			AbstractMergeStrategy.SingleResultMergeStrategy.PropertyOnly<ModalDisplay> {
	}

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

	/**
	 * Applies only to the children of the node on which this is set (Form,
	 * Table)
	 *
	 * 
	 *
	 */
	public static class ModalResolver extends ContextResolver
			implements FormModel.Has {
		public static ModalResolver multiple(Node node, boolean readOnly) {
			return new ModalResolver(node,
					readOnly ? Mode.MULTIPLE_READ : Mode.MULTIPLE_WRITE);
		}

		public static ModalResolver single(Node node, boolean readOnly) {
			return new ModalResolver(node,
					readOnly ? Mode.SINGLE_READ : Mode.SINGLE_WRITE);
		}

		private FormModel formModel;

		private TableModel tableModel;

		private final Mode mode;

		private Node node;

		private ModalResolver(Node node, Mode mode) {
			super();
			init(node);
			this.node = node;
			this.mode = Registry.impl(ModeTransformer.class).apply(mode);
		}

		@Override
		public boolean equals(Object obj) {
			return obj instanceof ModalResolver
					&& ((ModalResolver) obj).mode == mode;
		}

		@Override
		public FormModel getFormModel() {
			return this.formModel;
		}

		public TableModel getTableModel() {
			return this.tableModel;
		}

		@Override
		public int hashCode() {
			return mode.hashCode();
		}

		@Override
		public synchronized <A extends Annotation> List<A> resolveAnnotations(
				Class<A> annotationClass, AnnotationLocation location) {
			if (location == node.getAnnotationLocation()) {
				return parent.resolveAnnotations(annotationClass, location);
			}
			boolean customResolution = annotationClass == Display.class
					|| annotationClass == Custom.class
					|| annotationClass == Validator.class;
			List<A> defaultResolution = super.resolveAnnotations(
					annotationClass, location);
			if (customResolution) {
				RequireSpecified requireSpecified = Reflections
						.at(location.classLocation)
						.annotation(RequireSpecified.class);
				ModalDisplay modalDisplay = super.resolveAnnotation(
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
					return List.of(modalResolution);
				} else {
					if (requireSpecified == null
							|| Arrays.stream(requireSpecified.value())
									.noneMatch(m -> m.matches(mode))
							|| matchingModal.isPresent()) {
						return defaultResolution;
					} else {
						return List.of();
					}
				}
			} else {
				return defaultResolution;
			}
		}

		public void setFormModel(FormModel formModel) {
			this.formModel = formModel;
		}

		public void setTableModel(TableModel tableModel) {
			this.tableModel = tableModel;
		}

		private <T> T getFirst(T[] array) {
			Preconditions.checkArgument(array.length <= 1);
			return array.length == 0 ? null : array[0];
		}
	}

	@Reflected
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

	@Reflected
	@Registration(ModeTransformer.class)
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
