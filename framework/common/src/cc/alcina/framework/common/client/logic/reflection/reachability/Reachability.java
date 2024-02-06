package cc.alcina.framework.common.client.logic.reflection.reachability;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/*
 * Rules which limit or explain reflective class reachability for a given filter
 * peer.
 */
public class Reachability {
	public enum Action {
		INCLUDE, EXCLUDE
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target({ ElementType.TYPE })
	public @interface Condition {
		Class[] classes() default {};

		String packageName() default "";

		Class<? extends RuleSet> ruleSet() default RuleSet.Empty.class;

		Class[] subtypes() default {};
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target({ ElementType.TYPE })
	public @interface Rule {
		Action action();

		Condition condition();

		String reason() default "";
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target({ ElementType.TYPE })
	public @interface Rules {
		Rule[] value() default {};
	}

	/*
	 * Annotation carrier class, assists composition of rules
	 */
	public static interface RuleSet {
		public static class Empty implements RuleSet {
		}
	}
}
