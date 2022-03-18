package cc.alcina.framework.common.client.logic.reflection.reachability;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/*
 * Rules which limit or explain reflective class reachability for a given filter peer.
 */
public class Reachability {
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target({ ElementType.TYPE })
	public @interface Rule {
		Action action();

		Class[] classes() default {};

		String packageName() default "";

		String reason() default "";

		Class[] subtypes() default {};

	}

	public enum Action {
		INCLUDE, EXCLUDE
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target({ ElementType.TYPE })
	public @interface Rules {
		Rule[] value() default {};

		/*
		 * ruleSets are processed after directly reached rules - to order them
		 * before, encapsulate the rules in a ruleset
		 */
		Class<? extends RuleSet>[] ruleSets() default {};
	}

	/*
	 * Annotation carrier class, assists composition of rules
	 */
	public static interface RuleSet {
	}
}
