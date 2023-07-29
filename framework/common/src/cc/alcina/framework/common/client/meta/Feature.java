package cc.alcina.framework.common.client.meta;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Comparator;

import cc.alcina.framework.common.client.logic.reflection.Registration;

/*
 * Project planning/structure - expressed in code
 *
 */
@Registration(Feature.class)
public interface Feature extends Registration.AllSubtypes {
	@Retention(RetentionPolicy.RUNTIME)
	@Inherited
	@Documented
	@Target({ ElementType.TYPE })
	@interface After {
		Class<? extends Feature>[] value();
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Inherited
	@Documented
	@Target({ ElementType.TYPE })
	@interface Depends {
		Class<? extends Feature>[] value();
	}

	public interface Note {
		String contents();

		@Retention(RetentionPolicy.RUNTIME)
		@Inherited
		@Documented
		@Target({ ElementType.TYPE })
		@interface Ref {
			Class<? extends Feature.Note>[] value();
		}
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Inherited
	@Documented
	@Target({ ElementType.TYPE })
	@interface Parent {
		Class<? extends Feature> value();
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Inherited
	@Documented
	@Target({ ElementType.METHOD, ElementType.TYPE })
	@Repeatable(Refs.class)
	@interface Ref {
		Class<? extends Feature>[] value();
	}

	/**
	 * Container for multiple {@link Ref} annotations
	 *
	 * 
	 *
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Inherited
	@Documented
	@Target({ ElementType.METHOD, ElementType.TYPE })
	@interface Refs {
		Ref[] value();
	}

	public interface ReleaseVersion {
		public static class Cmp
				implements Comparator<Class<? extends ReleaseVersion>> {
			@Override
			public int compare(Class<? extends ReleaseVersion> o1,
					Class<? extends ReleaseVersion> o2) {
				if (o1 == null) {
					return o2 == null ? 0 : -1;
				}
				if (o2 == null) {
					return 1;
				}
				return o1.getSimpleName().compareTo(o2.getSimpleName());
			}
		}

		@Retention(RetentionPolicy.RUNTIME)
		@Inherited
		@Documented
		@Target({ ElementType.TYPE })
		@interface Ref {
			Class<? extends Feature.ReleaseVersion> value();
		}
	}

	/*
	 * @formatter:off
	 */
	public interface Status{
		public interface Complete extends Status{}
		@Retention(RetentionPolicy.RUNTIME)
		@Inherited
		@Documented
		@Target({ ElementType.TYPE })
		@interface FollowUp {
			Class<? extends Feature.Status> value();
		}
		public interface In_Progress extends Status{}
		public interface Open extends Status{}
		@Retention(RetentionPolicy.RUNTIME)
		@Inherited
		@Documented
		@Target({ ElementType.TYPE })
		@interface Ref {
			Class<? extends Feature.Status> value();
		}
	}

	public interface Tag{
		/*
		 * admin-only
		 */
		public interface Admin extends Type{}
		/*
		 * dev-only
		 */
		public interface Dev extends Type{}
		@Retention(RetentionPolicy.RUNTIME)
		@Inherited
		@Documented
		@Target({ ElementType.TYPE })
		@Repeatable(Refs.class)
		@interface Ref {
			Class<? extends Feature.Tag> value();
		}

		@Retention(RetentionPolicy.RUNTIME)
		@Inherited
		@Documented
		@Target({ ElementType.TYPE })
		@interface Refs {
			Tag.Ref[] value();
		}
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Inherited
	@Documented
	@Target({ ElementType.TYPE })
	@interface Track {
		Class<? extends Feature> value();
	}
	public interface Type{
		@Retention(RetentionPolicy.RUNTIME)
		@Inherited
		@Documented
		@Target({ ElementType.TYPE })
		@interface Ref {
			Class<? extends Feature.Type> value();
		}
		/*
		 * A restriction on a ui feature implementation, worth tracking (and/or testing) separately
		 */
		public interface Ui_constraint extends Type{}
		/*
		 * Ui_feature is tourable - others not
		 */
		public interface Ui_feature extends Type{}
		/*
		 * A detail of a ui feature implementation, worth tracking (and/or testing) separately
		 */
		public interface Ui_implementation extends Type{}
		public interface Ui_support extends Type{}
	}
}
