package cc.alcina.framework.common.client.logic.reflection;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Function;

import cc.alcina.framework.common.client.logic.reflection.reachability.ClientVisible;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.resolution.AbstractMergeStrategy;
import cc.alcina.framework.common.client.logic.reflection.resolution.Resolution;
import cc.alcina.framework.common.client.logic.reflection.resolution.Resolution.Inheritance;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;

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

		Directed[] directed() default {};

		Display[] display() default {};

		Mode mode();

		Directed.Transform[] transform() default {};

		Validator[] validator() default {};
	}

	@Reflected
	public enum Mode {
		SINGLE_ANY, SINGLE_READ, SINGLE_WRITE, MULTIPLE_ANY, MULTIPLE_READ,
		MULTIPLE_WRITE;

		private boolean isAny() {
			switch (this) {
			case SINGLE_ANY:
			case MULTIPLE_ANY:
				return true;
			default:
				return false;
			}
		}

		public boolean isMultiple() {
			return !isSingle();
		}

		private boolean isSameArity(Mode other) {
			return this.isSingle() ^ !other.isSingle();
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
	}

	@Reflected
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
