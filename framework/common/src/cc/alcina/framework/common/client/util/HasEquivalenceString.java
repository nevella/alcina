package cc.alcina.framework.common.client.util;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.totsp.gwittir.client.beans.Converter;

public interface HasEquivalenceString {
	public String equivalenceString();

	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target(ElementType.FIELD)
	public @interface HasEquivalenceInfo {
		String value();
	}

	public static final class HasEquivalenceStringConverter implements
			Converter<HasEquivalenceString, String> {
		@Override
		public String convert(HasEquivalenceString original) {
			return original.equivalenceString();
		}
	}
}
