package cc.alcina.framework.common.client.meta;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/*
 * Project planning/structure - expressed in code
 *
 */
public interface Feature {
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
	@Target({ ElementType.TYPE })
	@interface Ref {
		Class<? extends Feature>[] value();
	}

	public interface ReleaseVersion {
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

	public interface Type{
		public interface Logic_support extends Type{}
		@Retention(RetentionPolicy.RUNTIME)
		@Inherited
		@Documented
		@Target({ ElementType.TYPE })
		@interface Ref {
			Class<? extends Feature.Type> value();
		}
		/*
		 * tourable - others not
		 */
		public interface Ui_feature extends Type{}
		public interface Ui_support extends Type{}
	}
	/*
	 * @formatter:on
	 */
}
