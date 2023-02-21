package cc.alcina.framework.common.client.meta;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public interface Feature {
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
	@interface Ref {
		Class<? extends Feature> value();
	}
}
