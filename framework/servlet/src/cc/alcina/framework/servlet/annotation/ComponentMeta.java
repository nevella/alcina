package cc.alcina.framework.servlet.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public interface ComponentMeta {
	/**
	 * Describes the build process for an artifact
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.PACKAGE)
	@Documented
	public @interface Build {
		String value();
	}

	/**
	 * Describes the deploy process for an artifact
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.PACKAGE)
	@Documented
	public @interface Deploy {
		String value();
	}

	/**
	 * Describes the test process for an artifact
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.PACKAGE)
	@Documented
	public @interface Test {
		String value();
	}
}
