package cc.alcina.framework.entity.entityaccess.cache.mvcc;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Documented
@Retention(RUNTIME)
@Target(ElementType.METHOD)
public @interface MvccAccess {
	MvccAccessType type();

	public enum MvccAccessType {
		VERIFIED_CORRECT,
		// Used to denote inner class accessor creation - they should be a
		// nested instance of the domain identity object
		RESOLVE_TO_DOMAIN_IDENTITY, TRANSACTIONAL_ACCESS_NOT_SUPPORTED;
	}
}
