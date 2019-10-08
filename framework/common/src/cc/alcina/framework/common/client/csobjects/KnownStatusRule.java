package cc.alcina.framework.common.client.csobjects;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.google.gwt.user.client.rpc.GwtTransient;

@Documented
@Retention(RUNTIME)
@Target(FIELD)
public @interface KnownStatusRule {
	public KnownTagAlcina area() default KnownTagAlcina.Area_Devops;

	public double errorValue() default 0;

	public KnownStatusRuleName name();

	public double warnValue() default 0;

	public static class KnownStatusRuleImpl
			implements KnownStatusRule, Serializable {
		public KnownStatusRuleImpl() {
		}

		public KnownStatusRuleImpl(KnownStatusRule ann) {
			area = ann.area();
			errorValue = ann.errorValue();
			name = ann.name();
			warnValue = ann.warnValue();
		}

		@GwtTransient
		public Class<? extends Annotation> annotationType = KnownStatusRule.class;

		public KnownTagAlcina area;

		public double errorValue;

		public KnownStatusRuleName name;

		public double warnValue;

		@Override
		public Class<? extends Annotation> annotationType() {
			return annotationType;
		}

		@Override
		public KnownTagAlcina area() {
			return area;
		}

		@Override
		public double errorValue() {
			return errorValue;
		}

		@Override
		public KnownStatusRuleName name() {
			return name;
		}

		@Override
		public double warnValue() {
			return warnValue;
		}
	}
}
