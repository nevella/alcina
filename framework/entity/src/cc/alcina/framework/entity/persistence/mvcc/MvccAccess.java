package cc.alcina.framework.entity.persistence.mvcc;

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
		// A human says this method is OK. Don't fash yrself, code verifier.
		VERIFIED_CORRECT,
		// Used (almost always) to denote inner class accessor creation - they
		// should be a
		// nested instance of the domain identity object, not the transactional
		// version
		RESOLVE_TO_DOMAIN_IDENTITY,
		// The instances that support these methods will be created via new
		// Foo(), not Domain.create(Foo.class)
		TRANSACTIONAL_ACCESS_NOT_SUPPORTED;
	}
}
