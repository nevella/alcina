package cc.alcina.framework.common.client.csobjects;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.google.gwt.user.client.rpc.GwtTransient;

import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;

@Documented
@Retention(RUNTIME)
@Target(FIELD)
public @interface KnownStatusRule {
	public KnownTagAlcina area() default KnownTagAlcina.Area_Devops;

	public double errorValue() default 0;

	public KnownStatusRuleName name();

	public double warnValue() default 0;

	@Bean
	public static class KnownStatusRuleImpl
			implements KnownStatusRule, Serializable {
		@GwtTransient
		private Class<? extends Annotation> annotationType = KnownStatusRule.class;

		private KnownTagAlcina area;

		private double errorValue;

		private KnownStatusRuleName name;

		private double warnValue;

		public KnownStatusRuleImpl() {
		}

		public KnownStatusRuleImpl(KnownStatusRule ann) {
			area = ann.area();
			errorValue = ann.errorValue();
			name = ann.name();
			warnValue = ann.warnValue();
		}

		@Override
		public Class<? extends Annotation> annotationType() {
			return getAnnotationType();
		}

		@Override
		public KnownTagAlcina area() {
			return area;
		}

		@Override
		public double errorValue() {
			return errorValue;
		}

		@AlcinaTransient(AlcinaTransient.TransienceContext.CLIENT)
		public Class<? extends Annotation> getAnnotationType() {
			return annotationType;
		}

		public KnownTagAlcina getArea() {
			return area;
		}

		public double getErrorValue() {
			return errorValue;
		}

		public KnownStatusRuleName getName() {
			return name;
		}

		public double getWarnValue() {
			return warnValue;
		}

		@Override
		public KnownStatusRuleName name() {
			return name;
		}

		public void
				setAnnotationType(Class<? extends Annotation> annotationType) {
			this.annotationType = annotationType;
		}

		public void setArea(KnownTagAlcina area) {
			this.area = area;
		}

		public void setErrorValue(double errorValue) {
			this.errorValue = errorValue;
		}

		public void setName(KnownStatusRuleName name) {
			this.name = name;
		}

		public void setWarnValue(double warnValue) {
			this.warnValue = warnValue;
		}

		@Override
		public double warnValue() {
			return warnValue;
		}
	}
}
