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
	public @interface Entry {
		Rule[] exclude() default {};

		Rule[] explain() default {};

		Rule[] include() default {};
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target({ ElementType.TYPE })
	public @interface Rule {
		Class[] classes() default {};

		String packageName() default "";

		String reason() default "";

		Class[] subtypes() default {};
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target({ ElementType.TYPE })
	public @interface Rules {
		Class<? extends RuleSet>[] ruleSets() default {};

		Entry[] value() default {};
	}

	/*
	 * Annotation carrier class, assists composition of rules
	 */
	public static interface RuleSet {
	}
}
